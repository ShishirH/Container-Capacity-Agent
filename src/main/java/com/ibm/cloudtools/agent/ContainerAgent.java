/*
 * ******************************************************************************
 *  * Copyright (c) 2012, 2019 IBM Corp. and others
 *  *
 *  * This program and the accompanying materials are made available under
 *  * the terms of the Eclipse Public License 2.0 which accompanies this
 *  * distribution and is available at https://www.eclipse.org/legal/epl-2.0/
 *  * or the Apache License, Version 2.0 which accompanies this distribution and
 *  * is available at https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * This Source Code may also be made available under the following
 *  * Secondary Licenses when the conditions for such availability set
 *  * forth in the Eclipse Public License, v. 2.0 are satisfied: GNU
 *  * General Public License, version 2 with the GNU Classpath
 *  * Exception [1] and GNU General Public License, version 2 with the
 *  * OpenJDK Assembly Exception [2].
 *  *
 *  * [1] https://www.gnu.org/software/classpath/license.html
 *  * [2] http://openjdk.java.net/legal/assembly-exception.html
 *  *
 *  * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0 OR GPL-2.0 WITH Classpath-exception-2.0 OR LicenseRef-GPL-2.0 WITH Assembly-exception
 *  ******************************************************************************
 */

package com.ibm.cloudtools.agent;

import com.amihaiemil.eoyaml.YamlMapping;
import com.cedarsoftware.util.io.JsonWriter;
import com.ibm.cloudtools.exportMetrics.Analysis;
import com.ibm.cloudtools.exportMetrics.GenerateConfig;
import com.ibm.cloudtools.exportMetrics.RawData;
import com.ibm.cloudtools.exportMetrics.Summary;
import com.ibm.cloudtools.system.SystemDump;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Paths;

@SuppressWarnings("InfiniteLoopStatement unchecked")
public class ContainerAgent extends Thread
{
    public static double cpuTargetMultiplier;
    static long buffer;
    public static int config;
    public static int governorPowersaveFlag = 0;
    public static String comments;
    public static int NO_OF_VALUES_CURRENT = 0;
    public static int NO_OF_ITERATIONS = 0;

    public MetricCollector metricCollector;
    private JSONObject monitorObject;
    private JSONObject analysisObject;
    private JSONObject summaryObject;
    public static DescriptiveStatistics resValues = new DescriptiveStatistics();
    public static DescriptiveStatistics heapValues = new DescriptiveStatistics();
    public static DescriptiveStatistics nativeValues = new DescriptiveStatistics();

    private ContainerAgent()
    {
        /* creating directories */
        if (!createDirs()) return;

        try
        {
            PrintStream printStream =
                    new PrintStream(new FileOutputStream(Util.separatorsToSystem("Output/dump.txt"), true));
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

    private static void readInputJSON(String args) throws IOException, ParseException
    {
        JSONObject inputObject = (JSONObject) new JSONParser().parse(new FileReader(args));
        final String configuration = (String) inputObject.get("config");

        GenerateConfig.name =
                inputObject.containsKey("name") ? (String) inputObject.get("name") : "java";

        GenerateConfig.apiVersion =
                inputObject.containsKey("apiVersion") ? (String) inputObject.get("apiVersion") : "v1";

        ContainerAgent.buffer =
                inputObject.containsKey("buffer") ? (Long) inputObject.get("buffer") : 10;

        ContainerAgent.cpuTargetMultiplier = (Double) inputObject.get("cpuTargetMultiplier");

        /*TODO Look at what ideal values for the differnet configurations would be*/
        ContainerAgent.config = (configuration.equals("perf")) ? 0 : 1;
    }

    private static void getMetrics(ContainerAgent containerAgent)
    {
        while (true)
        {
            containerAgent.monitorObject = new JSONObject();
            containerAgent.analysisObject = new JSONObject();
            containerAgent.summaryObject = new JSONObject();

            containerAgent.metricCollector = new MetricCollector();
            containerAgent.metricCollector.memoryMetrics.getMemoryMetrics(containerAgent.metricCollector);

            System.err.println("Resident size: " + SystemDump.getResidentSize() / (1024 * 1024)
                    + "\tHeap used max: " + containerAgent.metricCollector.heapStat[1].getMax()
                    + "\tNative used max: " + containerAgent.metricCollector.nativeStat[1].getMax());

            System.err.println("Heap committed max: " + containerAgent.metricCollector.heapStat[0].getMax()
                    + "\tNative committed max: " + containerAgent.metricCollector.nativeStat[0].getMax());

            System.err.println("-------------------------------------------------------------------------------------");
            System.err.println();

            MetricCollector.heapSumValues += containerAgent.metricCollector.getHeapStat()[0].getMean();
            MetricCollector.nativeSumValues += containerAgent.metricCollector.getNativeStat()[0].getMean();
            MetricCollector.chartResidentStat.addValue(Util.convertToMB(containerAgent.metricCollector.getResidentMemoryStat().getPercentile(50)));

            PrintStream resValueStream;
            PrintStream heapValueStream;
            PrintStream nativeValueStream;
            try
            {
                resValueStream = new PrintStream(new FileOutputStream(Util.separatorsToSystem("Output/resValues.txt") , true));
                heapValueStream = new PrintStream(new FileOutputStream(Util.separatorsToSystem("Output/heapValues.txt") , true));
                nativeValueStream = new PrintStream(new FileOutputStream(Util.separatorsToSystem("Output/nativeValues.txt") , true));
            }
            catch (FileNotFoundException e)
            {
                System.err.println("COULD NOT CREATE FILE!");
                ContainerAgent.NO_OF_VALUES_CURRENT = 0;
                ContainerAgent.NO_OF_ITERATIONS++;
                continue;
            }

            for(double resValue : ContainerAgent.resValues.getValues())
            {
                resValueStream.println(resValue);
            }

            for(double heapValue : ContainerAgent.heapValues.getValues())
            {
                heapValueStream.println(heapValue);
            }

            for(double nativeValue : ContainerAgent.nativeValues.getValues())
            {
                nativeValueStream.println(nativeValue);
            }

            resValueStream.flush();
            resValueStream.close();

            heapValueStream.flush();
            heapValueStream.close();

            nativeValueStream.flush();
            nativeValueStream.close();

            ContainerAgent.resValues = new DescriptiveStatistics();
            ContainerAgent.nativeValues = new DescriptiveStatistics();
            ContainerAgent.heapValues = new DescriptiveStatistics();

            ContainerAgent.NO_OF_VALUES_CURRENT = 0;
            ContainerAgent.NO_OF_ITERATIONS++;

        }
    }

    private static void exportJSONs(ContainerAgent containerAgent)
    {
        /* creating JSON file that contains raw data */
        createRawJSON(containerAgent);
        printToFile(
                Util.separatorsToSystem("Output/JSONs/RawData") + NO_OF_ITERATIONS + ".json",
                JsonWriter.formatJson(containerAgent.monitorObject.toJSONString()));

        /* creating JSON file that contains statistical data */
        createAnalysisJSON(containerAgent);
        printToFile(
                Util.separatorsToSystem("Output/JSONs/Analysis") + NO_OF_ITERATIONS + ".json",
                JsonWriter.formatJson(containerAgent.analysisObject.toJSONString()));

        /* cherry picked important data needed to make resource calculations */
        createSummaryJSON(containerAgent);
        printToFile(
                Util.separatorsToSystem("Output/JSONs/Summary") + NO_OF_ITERATIONS + ".json",
                JsonWriter.formatJson(containerAgent.summaryObject.toJSONString()));

        System.out.println("EXPORTED DETAILS: " + ContainerAgent.NO_OF_ITERATIONS);
    }

    private static void generateYamlFile()
    {
        /* creating the YAML file */
        YamlMapping yamlMapping = GenerateConfig.createYamlConfig();
        printToFile(
                Util.separatorsToSystem("Output/configuration.yml"),
                ContainerAgent.comments + yamlMapping.toString());
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
        RawData.addMemoryToMonitor(
                containerAgent.monitorObject, containerAgent, containerAgent.metricCollector);
        RawData.addCpuToMonitor(containerAgent.monitorObject, containerAgent);
    }

    private static void addShutdownHook(ContainerAgent containerAgent)
    {
        containerAgent.setDaemon(true);
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(() ->
                        {
                            if (NO_OF_VALUES_CURRENT == 0 && NO_OF_ITERATIONS == 0)
                            {
                                return;
                            }

                            generateYamlFile();
/*
                            if (NO_OF_VALUES_CURRENT > 0)
                            {
                                exportJSONs(containerAgent);
                            }

                            try
                            {
                                createCharts();
                            }
                            catch (Exception e)
                            {
                                System.err.println("COULD NOT CREATE GRAPHS");
                            }
*/
                        }));
    }

    public void run()
    {
        try
        {
            getMetrics(this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private boolean createDirs()
    {
        try
        {
            Files.createDirectories(Paths.get("Output"));
            Files.createDirectories(Paths.get(Util.separatorsToSystem("Output/Charts")));
            //Files.createDirectories(Paths.get(Util.separatorsToSystem("Output/JSONs")));
        }
        catch (IOException e)
        {
            System.err.println("IO EXCEPTION: COULD NOT CREATE REQUIRED DIRECTORIES");
            return false;
        }
        return true;
    }
}
