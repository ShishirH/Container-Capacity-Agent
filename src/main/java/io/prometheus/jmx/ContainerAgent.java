package io.prometheus.jmx;

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

@SuppressWarnings("unchecked")
class ContainerAgent extends Thread
{
    double targetMultiplier;
    long buffer;
    private int config;
    double maxMemSize;
    double meanMemSize;
    MetricCollector metricCollector;
    private JSONObject monitorObject;
    private JSONObject analysisObject;
    private String comments;
    private JSONObject summaryObject;
    int governorPowersaveFlag = 0;


    private ContainerAgent()
    {
        metricCollector = new MetricCollector();
        monitorObject = new JSONObject();
        analysisObject = new JSONObject();
        summaryObject = new JSONObject();
    }

    /* reads the input JSON, and starts getDetails() */
    public static void premain(String args, Instrumentation instrumentation)
    {
        try
        {
            JSONObject inputObject = (JSONObject) new JSONParser().parse(new FileReader(args));
            ContainerAgent containerAgent = new ContainerAgent();
            containerAgent.targetMultiplier = (Double) inputObject.get("targetMultiplier");
            String configuration = (String) inputObject.get("config");
            containerAgent.buffer = (Long) inputObject.get("buffer");
            containerAgent.config = (configuration.equals("perf")) ? 1 : 0;

            if(containerAgent.config == 0)
            {
                containerAgent.comments = "#OPTIMIZED FOR PERFORMANCE\n";
            }

            else
            {
                containerAgent.comments = "#OPTIMIZED FOR LESS RESOURCE USAGE\n";
            }

            containerAgent.setDaemon(true);
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
        /* CPU information required is collected from Prometheus and by reading the required files */
        containerAgent.metricCollector.prometheusAgent.getMetrics(containerAgent.metricCollector);

        /* creating directories */
        try
        {
            Files.createDirectories(Paths.get("./Charts"));
            Files.createDirectories(Paths.get("./JSONs"));
        }
        catch (IOException e)
        {
            System.err.println("IO EXCEPTION: COULD NOT CREATE REQUIRED DIRECTORIES");
            return;
        }

        /* creating RawData.json that contains raw data */
        createRawJSON(containerAgent);
        printToFile("./JSONs/RawData.json", JsonWriter.formatJson(containerAgent.monitorObject.toJSONString()));

        /* adding statistical data to Statistics.json */
        createAnalysisJSON(containerAgent);
        printToFile("./JSONs/Analysis.json", JsonWriter.formatJson(containerAgent.analysisObject.toJSONString()));

        /* cherry picked important data needed to make resource calculations */
        createSummaryJSON(containerAgent);
        printToFile("./JSONs/Summary.json", JsonWriter.formatJson(containerAgent.summaryObject.toJSONString()));

        /* creating the YAML file */
        YamlMapping yamlMapping = GenerateConfig.createYamlConfig(containerAgent);
        containerAgent.comments += (containerAgent.governorPowersaveFlag == 0) ? "" : "#WARNING: CPU GOVERNOR SET TO " +
                "POWERSAVE. RESULTS MIGHT BE UNRELIABLE\n";
        printToFile("./configuration.yml", containerAgent.comments + yamlMapping.toString());

        createGraphs(containerAgent);
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
        RawData.addMemoryToMonitor(containerAgent.monitorObject, containerAgent);
        RawData.addCpuToMonitor(containerAgent.monitorObject, containerAgent);
    }

    private static void createGraphs(ContainerAgent containerAgent)
    {
        double [] time = containerAgent.metricCollector.getTime();
        double [][] frequencies = containerAgent.metricCollector.cpuMetricsImpl.getCpuCurrentFrequency();
        double [] cpuSeconds = containerAgent.metricCollector.getCpuSecondsStat().getValues();

        //initializing and labelling charts
        final XYChart timeSpentChart = new XYChartBuilder().width(1000).height(1000).title("Total user and system CPU time spent in seconds").xAxisTitle("Time").yAxisTitle("Seconds spend").build();
        final XYChart cpuFrequencyChart = new XYChartBuilder().width(1000).height(1000).title("CPU Frequency").xAxisTitle("Time").yAxisTitle("Frequency").build();
        final XYChart cpuLoadChart = new XYChartBuilder().width(1000).height(1000).title("CPU Load").xAxisTitle("Time").yAxisTitle("Load").build();

        timeSpentChart.addSeries("y(x)", time, cpuSeconds);
        cpuLoadChart.addSeries("y(x)", time, containerAgent.metricCollector.cpuMetricsImpl.getCpuLoad().getValues());

        for(int j = 0; j < Constants.NO_OF_CORES; j++)
        {
            cpuFrequencyChart.addSeries("CPU" + j, time, frequencies[j]);
        }

        //exporting chart to PNG.
        try
        {
            BitmapEncoder.saveBitmapWithDPI(timeSpentChart, "./Charts/timeSpentCPU", BitmapEncoder.BitmapFormat.PNG, 300);
            BitmapEncoder.saveBitmapWithDPI(cpuFrequencyChart, "./Charts/cpuFrequency", BitmapEncoder.BitmapFormat.PNG, 300);
            BitmapEncoder.saveBitmapWithDPI(cpuLoadChart, "./Charts/cpuLoad", BitmapEncoder.BitmapFormat.PNG, 300);
        }

        catch (IOException e)
        {
            System.err.println("IO ERROR: COULD NOT WRITE CHARTS TO FILE.");
        }
    }

}
