package com.ibm.cloudtools.agent;

import com.amihaiemil.eoyaml.YamlMapping;
import com.cedarsoftware.util.io.JsonWriter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

@SuppressWarnings("InfiniteLoopStatement unchecked")
class ContainerAgent extends Thread
{
    static double targetMultiplier;
    static long buffer;
    static int config;
    static int governorPowersaveFlag = 0;

    double maxMemSize;
    double meanMemSize;
    MetricCollector metricCollector;

    private JSONObject monitorObject;
    private JSONObject analysisObject;
    private JSONObject summaryObject;

    static String comments;
    static int NO_OF_VALUES_CURRENT = 0;
    static int NO_OF_ITERATIONS = 0;

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
            final String configuration = (String) inputObject.get("config");

            ContainerAgent.targetMultiplier = (Double) inputObject.get("targetMultiplier");
            ContainerAgent.buffer = (configuration.equals("perf")) ? 20 : 15;
            ContainerAgent.config = (configuration.equals("perf")) ? 1 : 0;

            containerAgent.setDaemon(true);
            Runtime.getRuntime().addShutdownHook(new Thread(() ->
            {
                if(NO_OF_VALUES_CURRENT == 0 && NO_OF_ITERATIONS == 0)
                {
                    return;
                }

                generateYamlFile();
                if(NO_OF_VALUES_CURRENT > 0)
                {
                    exportDetails(containerAgent);
                }
                createGraphs();
            }));
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
            exportDetails(containerAgent);
            ContainerAgent.NO_OF_VALUES_CURRENT = 0;
            ContainerAgent.NO_OF_ITERATIONS++;
        }
    }

    private static void exportDetails(ContainerAgent containerAgent)
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

        System.out.println("EXPORTED DETAILS: " + ContainerAgent.NO_OF_ITERATIONS);
        /* removing existing contents */
        containerAgent.monitorObject = new JSONObject();
        containerAgent.analysisObject = new JSONObject();
        containerAgent.summaryObject = new JSONObject();
    }

    private static void generateYamlFile()
    {
        /* creating the YAML file */
        YamlMapping yamlMapping = GenerateConfig.createYamlConfig();
        printToFile(separatorsToSystem("Output/configuration.yml"), ContainerAgent.comments + yamlMapping.toString());
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

    private static void createGraphs()
    {
        double [] time;
        System.out.println("Number of values currently are: " + NO_OF_VALUES_CURRENT);


        if(NO_OF_VALUES_CURRENT > 0)
        {
            time = new double[ContainerAgent.NO_OF_ITERATIONS + 1];
        }

        else
        {
            time = new double[ContainerAgent.NO_OF_ITERATIONS];
        }

        int i;
        for(i = 0; i < time.length - 1; i++)
        {
            time[i] = Constants.TIME_TO_SLEEP * Constants.MAX_NUMBER_OF_VALUES * i;
        }

        time[time.length - 1] = Constants.TIME_TO_SLEEP * Constants.MAX_NUMBER_OF_VALUES * i + Constants.TIME_TO_SLEEP * Constants.MAX_NUMBER_OF_VALUES;

        System.err.println("NUMBER OF VALUES CURRENTLY: " + NO_OF_VALUES_CURRENT);
        System.err.println("NUMBER OF ITERATIONS: " + NO_OF_ITERATIONS);

        //DescriptiveStatistics [] cpuFrequency = containerAgent.metricCollector.cpuMetricsImpl.getFreqStat();

        double [] cpuLoad =  Arrays.stream(MetricCollector.chartCpuLoadStat.getValues()).filter(x -> !Double.isNaN(x)).toArray();
        double [] residentSize = Arrays.stream(MetricCollector.chartResidentStat.getValues()).filter(x -> !Double.isNaN(x)).toArray();

        /* initializing and labelling charts */
        //final XYChart cpuFrequencyChart = new XYChartBuilder().width(1000).height(1000).title("CPU Frequency").xAxisTitle("Time").yAxisTitle("Frequency").build();
        final XYChart cpuLoadChart = new XYChartBuilder().width(1000).height(1000).title("CPU Load").xAxisTitle("Time").yAxisTitle("Load").build();
        final XYChart residentMemChart = new XYChartBuilder().width(1000).height(1000).title("Resident Memory")
                .xAxisTitle("Time").yAxisTitle("Resident Memory").build();

        cpuLoadChart.addSeries("y(x)", time, cpuLoad);
        residentMemChart.addSeries("y(x)", time, residentSize);

/*
        for(int j = 0; j < Constants.NO_OF_CORES; j++)
        {
            cpuFrequencyChart.addSeries("CPU" + j, time, cpuFrequency[j].getValues());
        }
*/

        /* exporting chart to PNG. */
        try
        {
            //BitmapEncoder.saveBitmapWithDPI(cpuFrequencyChart, separatorsToSystem("Output/Charts/cpuFrequency") + NO_OF_ITERATIONS, BitmapEncoder.BitmapFormat.PNG, 300);
            BitmapEncoder.saveBitmapWithDPI(residentMemChart, separatorsToSystem("Output/Charts/residentMemory"), BitmapEncoder.BitmapFormat.PNG, 300);
            BitmapEncoder.saveBitmapWithDPI(cpuLoadChart, separatorsToSystem("Output/Charts/cpuLoad"), BitmapEncoder.BitmapFormat.PNG, 300);
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
