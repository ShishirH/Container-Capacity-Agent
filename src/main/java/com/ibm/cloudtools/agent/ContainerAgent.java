package com.ibm.cloudtools.agent;

import com.amihaiemil.eoyaml.YamlMapping;
import com.cedarsoftware.util.io.JsonWriter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
    static double targetMultiplier;
    static long buffer;
    static int config;
    static int governorPowersaveFlag = 0;
    static String comments;
    static int NO_OF_VALUES_CURRENT = 0;
    static int NO_OF_ITERATIONS = 0;

    MetricCollector metricCollector;
    private JSONObject monitorObject;
    private JSONObject analysisObject;
    private JSONObject summaryObject;

    private ContainerAgent()
    {

        /* creating directories */
        if (generateDirs()) return;

        try
        {
            PrintStream printStream = new PrintStream(new FileOutputStream(Util.separatorsToSystem("Output/dump.txt"),
                    true));
            System.setErr(printStream);
        }
        catch (FileNotFoundException e)
        {
            System.err.println("File was not found");
        }

        SystemDump.printSystemLog();

    }

    public static void premain(String args, Instrumentation instrumentation)
    {
        try
        {
            readInputJSON(args);

            final ContainerAgent containerAgent = new ContainerAgent();
            addShutdownHook(containerAgent);
            containerAgent.start();
        }
        catch (Exception e)
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
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static void readInputJSON(String args) throws IOException, ParseException
    {
        JSONObject inputObject = (JSONObject) new JSONParser().parse(new FileReader(args));
        final String configuration = (String) inputObject.get("config");

        GenerateConfig.name = inputObject.containsKey("name") ? (String) inputObject.get("name") : "java";

        GenerateConfig.apiVersion = inputObject.containsKey("apiVersion") ? (String) inputObject.get("apiVersion") : "v1";

        ContainerAgent.buffer = inputObject.containsKey("buffer") ? (Long) inputObject.get("buffer") : 10;

        ContainerAgent.targetMultiplier = (Double) inputObject.get("targetMultiplier");

        /*TODO*/
        ContainerAgent.config = (configuration.equals("perf")) ? 0 : 1;
    }

    private static void getDetails(ContainerAgent containerAgent)
    {
        while (true)
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
        printToFile(Util.separatorsToSystem("Output/JSONs/RawData") + NO_OF_ITERATIONS + ".json",
                JsonWriter.formatJson(containerAgent.monitorObject.toJSONString()));

        /* adding statistical data to Statistics.json */
        createAnalysisJSON(containerAgent);
        printToFile(Util.separatorsToSystem("Output/JSONs/Analysis") + NO_OF_ITERATIONS + ".json",
                JsonWriter.formatJson(containerAgent.analysisObject.toJSONString()));

        /* cherry picked important data needed to make resource calculations */
        createSummaryJSON(containerAgent);
        printToFile(Util.separatorsToSystem("Output/JSONs/Summary") + NO_OF_ITERATIONS + ".json",
                JsonWriter.formatJson(containerAgent.summaryObject.toJSONString()));

        System.out.println("EXPORTED DETAILS: " + ContainerAgent.NO_OF_ITERATIONS);
    }

    private static void generateYamlFile()
    {
        /* creating the YAML file */
        YamlMapping yamlMapping = GenerateConfig.createYamlConfig();
        printToFile(Util.separatorsToSystem("Output/configuration.yml"), ContainerAgent.comments + yamlMapping.toString());
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

    private static void createCharts()
    {
        double[] time;
        System.out.println("Number of values currently are: " + NO_OF_VALUES_CURRENT);


        if (NO_OF_VALUES_CURRENT > 0)
        {
            time = new double[ContainerAgent.NO_OF_ITERATIONS + 1];
        }
        else
        {
            time = new double[ContainerAgent.NO_OF_ITERATIONS];
        }

        int i;
        for (i = 0; i < time.length - 1; i++)
        {
            time[i] = Constants.TIME_TO_SLEEP * Constants.MAX_NUMBER_OF_VALUES * i;
        }

        time[time.length - 1] =
                Constants.TIME_TO_SLEEP * Constants.MAX_NUMBER_OF_VALUES * i + Constants.TIME_TO_SLEEP * Constants.MAX_NUMBER_OF_VALUES;

        //DescriptiveStatistics [] cpuFrequency = containerAgent.metricCollector.cpuMetricsImpl.getFreqStat();

/*
        double[] cpuLoad =
                Arrays.stream(MetricCollector.chartCpuLoadStat.getValues()).filter(x -> !Double.isNaN(x)).toArray();
        double[] residentSize =
                Arrays.stream(MetricCollector.chartResidentStat.getValues()).filter(x -> !Double.isNaN(x)).toArray();
*/

        double[] cpuLoad = MetricCollector.chartCpuLoadStat.getValues();
        double[] residentSize = MetricCollector.chartResidentStat.getValues();
        /* initializing and labelling charts */
        //final XYChart cpuFrequencyChart = new XYChartBuilder().width(1000).height(1000).title("CPU Frequency")
        // .xAxisTitle("Time").yAxisTitle("Frequency").build();
        final XYChart cpuLoadChart = new XYChartBuilder().width(1000).height(1000).title("CPU Load").xAxisTitle("Time"
        ).yAxisTitle("Load").build();
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
            //BitmapEncoder.saveBitmapWithDPI(cpuFrequencyChart, separatorsToSystem("Output/Charts/cpuFrequency") +
            // NO_OF_ITERATIONS, BitmapEncoder.BitmapFormat.PNG, 300);
            BitmapEncoder.saveBitmapWithDPI(residentMemChart, Util.separatorsToSystem("Output/Charts/residentMemory"),
                    BitmapEncoder.BitmapFormat.PNG, 300);
            BitmapEncoder.saveBitmapWithDPI(cpuLoadChart, Util.separatorsToSystem("Output/Charts/cpuLoad"),
                    BitmapEncoder.BitmapFormat.PNG, 300);
        }
        catch (IOException e)
        {
            System.err.println("IO ERROR: COULD NOT WRITE CHARTS TO FILE.");
        }
    }

    private boolean generateDirs()
    {
        try
        {
            Files.createDirectories(Paths.get("Output"));
            Files.createDirectories(Paths.get(Util.separatorsToSystem("Output/Charts")));
            Files.createDirectories(Paths.get(Util.separatorsToSystem("Output/JSONs")));
        }
        catch (IOException e)
        {
            System.err.println("IO EXCEPTION: COULD NOT CREATE REQUIRED DIRECTORIES");
            return true;
        }
        return false;
    }

    private static void addShutdownHook(ContainerAgent containerAgent)
    {
        containerAgent.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
        {
            if (NO_OF_VALUES_CURRENT == 0 && NO_OF_ITERATIONS == 0)
            {
                return;
            }

            generateYamlFile();
            if (NO_OF_VALUES_CURRENT > 0)
            {
                exportDetails(containerAgent);
            }

            try
            {
                createCharts();
            }
            catch (Exception e)
            {
                System.err.println("COULD NOT CREATE GRAPHS");
            }
        }));
    }

}
