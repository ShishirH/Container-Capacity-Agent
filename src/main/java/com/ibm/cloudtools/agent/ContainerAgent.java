package com.ibm.cloudtools.agent;

import com.amihaiemil.eoyaml.YamlMapping;
import com.cedarsoftware.util.io.JsonWriter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Paths;

@SuppressWarnings("InfiniteLoopStatement unchecked")
class ContainerAgent extends Thread
{
    double targetMultiplier;
    static long buffer;
    int config;
    double maxMemSize;
    double meanMemSize;
    MetricCollector metricCollector;
    private JSONObject monitorObject;
    private JSONObject analysisObject;
    String comments;
    static int NO_OF_VALUES_CURRENT = 0;
    static int NO_OF_ITERATIONS = 0;

    private JSONObject summaryObject;
    int governorPowersaveFlag = 0;


    private ContainerAgent()
    {

        /* creating directories */
        try
        {
            Files.createDirectories(Paths.get("Output"));
            Files.createDirectories(Paths.get(separatorsToSystem("Output/Charts")));
            Files.createDirectories(Paths.get(separatorsToSystem("Output/JSONs")));
        }

        catch (IOException e)
        {
            System.err.println("IO EXCEPTION: COULD NOT CREATE REQUIRED DIRECTORIES");
            return;
        }

        try
        {
            PrintStream printStream = new PrintStream(new FileOutputStream(separatorsToSystem("Output/dump.txt"), true));
            System.setErr(printStream);
        }

        catch(FileNotFoundException e)
        {
            System.err.println("File was not found");
        }

        SystemDump.printSystemLog();

    }

    /* reads the input JSON, and starts getDetails() */
    public static void premain(String args, Instrumentation instrumentation)
    {
        try
        {
            JSONObject inputObject = (JSONObject) new JSONParser().parse(new FileReader(args));
            final ContainerAgent containerAgent = new ContainerAgent();
            containerAgent.targetMultiplier = (Double) inputObject.get("targetMultiplier");
            final String configuration = (String) inputObject.get("config");
            ContainerAgent.buffer = (Long) inputObject.get("buffer");
            containerAgent.config = (configuration.equals("perf")) ? 1 : 0;

            containerAgent.setDaemon(true);
            Runtime.getRuntime().addShutdownHook(new Thread()
            {
                public void run()
                {
                    exportDetails(containerAgent, false);
                    System.out.println("Current number of values: " + ContainerAgent.NO_OF_VALUES_CURRENT);
                    generateYamlFile(containerAgent);
                }
            });
            containerAgent.start();
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void run()
    {
        try
        {
            getDetails(this);
        }

        catch(Exception e)
        {
            e.printStackTrace();
        }
    }


    private static void getDetails(ContainerAgent containerAgent)
    {
        while(true)
        {
            containerAgent.monitorObject = new JSONObject();
            containerAgent.analysisObject = new JSONObject();
            containerAgent.summaryObject = new JSONObject();

            containerAgent.metricCollector = new MetricCollector();
            containerAgent.metricCollector.memoryMetrics.getMemoryMetrics(containerAgent.metricCollector);
            exportDetails(containerAgent ,true);
            ContainerAgent.NO_OF_VALUES_CURRENT = 0;
            ContainerAgent.NO_OF_ITERATIONS++;
        }
    }

    private static void exportDetails(ContainerAgent containerAgent, boolean needGraph)
    {
        /* creating RawData.json that contains raw data */
        createRawJSON(containerAgent);
        printToFile(separatorsToSystem("Output/JSONs/RawData") + NO_OF_ITERATIONS + ".json", JsonWriter.formatJson(containerAgent.monitorObject.toJSONString()));

        /* adding statistical data to Statistics.json */
        createAnalysisJSON(containerAgent);
        printToFile(separatorsToSystem("Output/JSONs/Analysis") + NO_OF_ITERATIONS + ".json", JsonWriter.formatJson(containerAgent.analysisObject.toJSONString()));

        /* cherry picked important data needed to make resource calculations */
        createSummaryJSON(containerAgent);
        printToFile(separatorsToSystem("Output/JSONs/Summary") + NO_OF_ITERATIONS + ".json", JsonWriter.formatJson(containerAgent.summaryObject.toJSONString()));

        //graphs will not be created when application exits
        if(needGraph)
        {
            createGraphs(containerAgent);
        }
        System.out.println("EXPORTED DETAILS: " + ContainerAgent.NO_OF_ITERATIONS);
        /* removing existing contents */
        containerAgent.monitorObject = new JSONObject();
        containerAgent.analysisObject = new JSONObject();
        containerAgent.summaryObject = new JSONObject();
    }

    private static void generateYamlFile(ContainerAgent containerAgent)
    {
        /* creating the YAML file */
        YamlMapping yamlMapping = GenerateConfig.createYamlConfig(containerAgent);
        printToFile(separatorsToSystem("Output/configuration.yml"), containerAgent.comments + yamlMapping.toString());
    }

    private static void printToFile(String fileName, String content)
    {
        File file = new File(fileName);
        PrintWriter printWriter;
        try
        {
            printWriter = new PrintWriter(file);
        }

        catch (FileNotFoundException e)
        {
            System.err.println("IO EXCEPTION: COULD NOT WRITE TO " + fileName);
            return;
        }
        printWriter.write(content);
        printWriter.flush();
        printWriter.close();
    }

    private static void createSummaryJSON(ContainerAgent containerAgent)
    {
        Summary.getSummaryCPU(containerAgent.summaryObject, containerAgent);
        Summary.getSummaryMemory(containerAgent.summaryObject, containerAgent);
    }

    private static void createAnalysisJSON(ContainerAgent containerAgent)
    {
        JSONObject memoryAnalysisObject = new JSONObject();
        JSONObject cpuAnalysisObject = new JSONObject();
        Analysis.analyzeMemory(memoryAnalysisObject, containerAgent.metricCollector);
        Analysis.analyzeCPU(cpuAnalysisObject, containerAgent.metricCollector);
        containerAgent.analysisObject.put("Memory", memoryAnalysisObject);
        containerAgent.analysisObject.put("CPU", cpuAnalysisObject);
    }

    private static void createRawJSON(ContainerAgent containerAgent)
    {
        RawData.addMemoryToMonitor(containerAgent.monitorObject, containerAgent, containerAgent.metricCollector);
        RawData.addCpuToMonitor(containerAgent.monitorObject, containerAgent);
    }

    private static void createGraphs(ContainerAgent containerAgent)
    {
        double [] time = containerAgent.metricCollector.getTime();
        DescriptiveStatistics [] cpuFrequency = containerAgent.metricCollector.cpuMetricsImpl.getFreqStat();
        double [] cpuSeconds = containerAgent.metricCollector.cpuMetricsImpl.getCpuSecondsStat().getValues();

        /* initializing and labelling charts */
        final XYChart timeSpentChart = new XYChartBuilder().width(1000).height(1000).title("Total user and system CPU time spent in seconds").xAxisTitle("Time").yAxisTitle("Seconds spend").build();
        final XYChart cpuFrequencyChart = new XYChartBuilder().width(1000).height(1000).title("CPU Frequency").xAxisTitle("Time").yAxisTitle("Frequency").build();
        final XYChart cpuLoadChart = new XYChartBuilder().width(1000).height(1000).title("CPU Load").xAxisTitle("Time").yAxisTitle("Load").build();

        timeSpentChart.addSeries("y(x)", time, cpuSeconds);
        cpuLoadChart.addSeries("y(x)", time, containerAgent.metricCollector.cpuMetricsImpl.getCpuLoad().getValues());

        for(int j = 0; j < Constants.NO_OF_CORES; j++)
        {
            cpuFrequencyChart.addSeries("CPU" + j, time, cpuFrequency[j].getValues());
        }

        /* exporting chart to PNG. */
        try
        {
            BitmapEncoder.saveBitmapWithDPI(timeSpentChart, separatorsToSystem("Output/Charts/timeSpentCPU") + NO_OF_ITERATIONS, BitmapEncoder.BitmapFormat.PNG, 300);
            BitmapEncoder.saveBitmapWithDPI(cpuFrequencyChart, separatorsToSystem("Output/Charts/cpuFrequency") + NO_OF_ITERATIONS, BitmapEncoder.BitmapFormat.PNG, 300);
            BitmapEncoder.saveBitmapWithDPI(cpuLoadChart, separatorsToSystem("Output/Charts/cpuLoad") + NO_OF_ITERATIONS, BitmapEncoder.BitmapFormat.PNG, 300);
        }

        catch (IOException e)
        {
            System.err.println("IO ERROR: COULD NOT WRITE CHARTS TO FILE.");
        }
    }

    private static String separatorsToSystem(String res)
    {
        if (res==null) return null;
        if (File.separatorChar=='\\') {
            // From Windows to Linux/Mac
            return res.replace('/', File.separatorChar);
        } else {
            // From Linux/Mac to Windows
            return res.replace('\\', File.separatorChar);
        }
    }

}
