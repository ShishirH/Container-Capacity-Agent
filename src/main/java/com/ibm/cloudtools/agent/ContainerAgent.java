/*
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      https://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 */

package com.ibm.cloudtools.agent;

import com.amihaiemil.eoyaml.YamlMapping;
import com.cedarsoftware.util.io.JsonWriter;
import com.ibm.cloudtools.exportMetrics.GenerateConfig;
import com.ibm.cloudtools.exportMetrics.Summary;
import com.ibm.cloudtools.system.SystemDump;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.json.simple.JSONObject;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("InfiniteLoopStatement StatementWithEmptyBody")
/*
 * Main class of the tool. Collects metrics through CpuMetricsImpl and MemoryMetricsImpl
 * from com.ibm.cloudtools.metrics package, stores them in MetricCollector.java class,
 * and once the main program completes executing, exports the metrics.
 */
public class ContainerAgent extends Thread
{
    private static int NO_OF_ITERATIONS = 0;

    public MetricCollector metricCollector;
    private JSONObject summaryObject;

    /* Objects used for generating charts if enableDiagnostics is 1 in the input.json file. */
    private static DescriptiveStatistics chartResidentStat = new DescriptiveStatistics();
    private static DescriptiveStatistics chartHeapUsedStat = new DescriptiveStatistics();
    private static DescriptiveStatistics chartNativeUsedStat = new DescriptiveStatistics();
    private static DescriptiveStatistics chartCpuLoadStat = new DescriptiveStatistics();

    /* Used for concurrency */
    private static final AtomicBoolean isAgentFinished = new AtomicBoolean(false);
    public static final AtomicBoolean isProgramRunning = new AtomicBoolean(true);

    private ContainerAgent()
    {
        /* creating directories */
        if (!createDirs()) return;

        /* print the state of the system */
        SystemDump.printSystemLog();
    }

    public static void premain(String args, Instrumentation instrumentation)
    {
        try {
            /* read the input parameters from the input.json file */
            InputParams.readInputJSON(args);
            ContainerAgent containerAgent = new ContainerAgent();
            containerAgent.setDaemon(true);
            addShutdownHook(containerAgent);
            containerAgent.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getMetrics(ContainerAgent containerAgent)
    {
        while (isProgramRunning.get()) {
            containerAgent.summaryObject = new JSONObject();

            containerAgent.metricCollector = new MetricCollector();
            containerAgent.metricCollector.memoryMetrics.getMetrics(containerAgent.metricCollector);

            if (InputParams.getEnableDiagnostics() == 1) {
                createSummaryJSON(containerAgent);
            }

            // Saving the median values of the iteration for graph generation in the end.
            chartResidentStat.addValue(
                    (containerAgent.metricCollector.residentMemoryStat.getPercentile(50)) / Constants.ONE_MB);
            chartHeapUsedStat.addValue(
                    (containerAgent.metricCollector.heapStat[1].getPercentile(50)) / Constants.ONE_MB);
            chartNativeUsedStat.addValue(
                    (containerAgent.metricCollector.nativeStat[1].getPercentile(50)) / Constants.ONE_MB);
            chartCpuLoadStat.addValue(
                    containerAgent.metricCollector.cpuLoadValues.getPercentile(50));

            dumpValues("Output/Dump/resValues.txt", containerAgent.metricCollector.residentMemoryStat.getValues());
            dumpValues("Output/Dump/heapCommittedValues.txt", containerAgent.metricCollector.heapStat[1].getValues());
            dumpValues("Output/Dump/nativeUsedValues.txt", containerAgent.metricCollector.nativeStat[1].getValues());
            dumpValues(
                    "Output/Dump/cpuLoad.txt", containerAgent.metricCollector.cpuLoadValues.getValues());

            ContainerAgent.NO_OF_ITERATIONS++;
        }
        isAgentFinished.set(true);
    }

    private static void generateYamlFile()
    {
        /* creating the YAML file */
        YamlMapping yamlMapping = GenerateConfig.createYamlConfig();
        printToFile(
                Util.separatorsToSystem("Output/configuration.yml"),
                GenerateConfig.comments + yamlMapping.toString());
    }

    /* Dump existing values of the run to the disk in order to avoid keeping them in memory */
    private static void dumpValues(String filename, double[] values)
    {
        PrintStream printStream;
        try {
            printStream = new PrintStream(new FileOutputStream(Util.separatorsToSystem(filename), true));

            for (double value : values) {
                printStream.println(value);
            }

            printStream.flush();
            printStream.close();
        } catch (FileNotFoundException e) {
            System.err.println("COULD NOT DUMP VALUES: FILE NOT FOUND EXCEPTION");
        }
    }

    private static void printToFile(String fileName, String content)
    {
        File file = new File(fileName);
        PrintWriter printWriter;
        try {
            printWriter = new PrintWriter(file);
        } catch (FileNotFoundException e) {
            System.err.println("IO EXCEPTION: COULD NOT WRITE TO " + fileName);
            return;
        }
        printWriter.write(content);
        printWriter.flush();
        printWriter.close();
    }

    /* Summary of the metrics collected in one iteration of the run */
    private static void createSummaryJSON(ContainerAgent containerAgent)
    {
        Summary.getSummaryCPU(containerAgent.summaryObject, containerAgent);
        Summary.getSummaryMemory(containerAgent.summaryObject, containerAgent);

        printToFile(
                Util.separatorsToSystem("Output/JSONs/Summary") + NO_OF_ITERATIONS + ".json",
                JsonWriter.formatJson(containerAgent.summaryObject.toJSONString()));
    }

    private static void addShutdownHook(ContainerAgent containerAgent)
    {
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    System.err.println("GENERATING CONFIGURATION FILE..");
                                    isProgramRunning.set(false);
                                    containerAgent.interrupt();
                                    // Wait until the main thread finishes its execution.
                                    while (isAgentFinished.get()) ;

                                    generateYamlFile();

                                    if (InputParams.getEnableDiagnostics() == 1) {
                                        try {
                                            exportGraphs(
                                                    "Resident Memory",
                                                    "Iteration",
                                                    "Memory",
                                                    chartResidentStat,
                                                    "Output/Charts/res");
                                            exportGraphs(
                                                    "Heap Used",
                                                    "Iteration",
                                                    "Memory",
                                                    chartHeapUsedStat,
                                                    "Output/Charts/heapUsed");
                                            exportGraphs(
                                                    "Native Used",
                                                    "Iteration",
                                                    "Memory",
                                                    chartNativeUsedStat,
                                                    "Output/Charts/nativeUsed");
                                            exportGraphs(
                                                    "CPU Load",
                                                    "iteration",
                                                    "CpuLoad",
                                                    chartCpuLoadStat,
                                                    "Output/Charts/cpuLoad");
                                        } catch (Exception e) {
                                            System.err.println("COULD NOT CREATE GRAPHS");
                                            e.printStackTrace();
                                        }
                                    } else {
                                        // delete all the files that were used to generate the configuration.yaml file.
                                        try {
                                            Files.deleteIfExists(Paths.get(Util.separatorsToSystem("Output/JSONs")));
                                            Files.deleteIfExists(Paths.get(Util.separatorsToSystem("Output/Charts")));

                                            File dumpDir = new File("Output/Dump/");
                                            for (File file : dumpDir.listFiles())
                                            {
                                                file.delete();
                                            }
                                            Files.deleteIfExists(Paths.get(Util.separatorsToSystem("Output/Dump")));
                                        } catch (IOException | NullPointerException e) {
                                            System.err.println("COULD NOT REMOVE FILES!");
                                            e.printStackTrace();
                                        }
                                    }
                                }));
    }

    private static void exportGraphs(
            String title,
            String xAxisTitle,
            String yAxisTitle,
            DescriptiveStatistics stats,
            String fileName)
    {

        double[] time = new double[stats.getValues().length];

        time[0] = 0;

        for (int i = 1; i < time.length; i++) {
            time[i] = Constants.TIME_TO_SLEEP + time[i - 1];
        }

        try {
            final XYChart chart =
                    new XYChartBuilder()
                            .width(1000)
                            .height(1000)
                            .title(title)
                            .xAxisTitle(xAxisTitle)
                            .yAxisTitle(yAxisTitle)
                            .build();

            chart.addSeries("y(x)", time, stats.getValues());

            BitmapEncoder.saveBitmapWithDPI(
                    chart, Util.separatorsToSystem(fileName), BitmapEncoder.BitmapFormat.PNG, 600);
        } catch (Exception e) {
            System.err.println("IO ERROR: COULD NOT WRITE CHARTS TO FILE.");
        }
    }

    public void run()
    {
        try {
            getMetrics(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean createDirs()
    {
        try {
            Files.createDirectories(Paths.get("Output"));
            Files.createDirectories(Paths.get(Util.separatorsToSystem("Output/Charts")));
            Files.createDirectories(Paths.get(Util.separatorsToSystem("Output/JSONs")));
            Files.createDirectories(Paths.get(Util.separatorsToSystem("Output/Dump")));
        } catch (IOException e) {
            System.err.println("IO EXCEPTION: COULD NOT CREATE REQUIRED DIRECTORIES");
            return false;
        }
        return true;
    }
}
