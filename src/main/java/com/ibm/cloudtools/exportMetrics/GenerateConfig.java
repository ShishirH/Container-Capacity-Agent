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

package com.ibm.cloudtools.exportMetrics;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.ibm.cloudtools.agent.Constants;
import com.ibm.cloudtools.agent.ContainerAgent;
import com.ibm.cloudtools.agent.MetricCollector;
import com.ibm.cloudtools.agent.Util;
import com.ibm.cloudtools.statistics.DetectOutlier;
import com.ibm.cloudtools.system.DetectVM;
import com.ibm.cloudtools.system.SystemDump;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

import java.io.IOException;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

public class GenerateConfig
{
    public static String name;
    public static String apiVersion;
    private static DecimalFormat decimalFormat = new DecimalFormat("#.###");
    private static DecimalFormat integerFormat = new DecimalFormat("#");

    public static YamlMapping createYamlConfig()
    {
        List<String> resList = null;
        List<String> heapList = null;
        List<String> nativeList = null;

        try
        {
            resList = Files.readAllLines(Paths.get(Util.separatorsToSystem("Output/resValues.txt")), Charset.forName("utf-8"));
            heapList = Files.readAllLines(Paths.get(Util.separatorsToSystem("Output/heapValues.txt")), Charset.forName("utf-8"));
            nativeList = Files.readAllLines(Paths.get(Util.separatorsToSystem("Output/nativeValues.txt")), Charset.forName("utf-8"));
            System.err.println("Old list: " + resList);
        } catch (IOException e)
        {
            System.err.println("COULD NOT CREATE YAML CONFIGURATION!");
        }

        DescriptiveStatistics resStat = new DescriptiveStatistics();
        DescriptiveStatistics heapStat = new DescriptiveStatistics();
        DescriptiveStatistics nativeStat = new DescriptiveStatistics();


        createGraphs(resList, "Resident Memory", "Time", "RES", resStat, "Output/Charts/residentMemory");
        createGraphs(heapList, "Heap Memory", "Time", "Heap", heapStat, "Output/Charts/Heap");
        createGraphs(nativeList, "Native Memory", "Time", "NativeMem", nativeStat, "Output/Charts/Native");

        resStat = DetectOutlier.removeOutliers(resStat);
        heapStat = DetectOutlier.removeOutliers(heapStat);
        nativeStat = DetectOutlier.removeOutliers(nativeStat);


        createGraphs(null, "Resident Memory", "Time", "RES", resStat, "Output/Charts/residentMemoryNoOutliers");
        createGraphs(null, "Heap Memory", "Time", "Heap", heapStat, "Output/Charts/HeapNoOutliers");
        createGraphs(null, "Native Memory", "Time", "NativeMem", nativeStat, "Output/Charts/NativeNoOutliers");

        System.err.println("New list is: " + Arrays.toString(resStat.getValues()));

        System.err.println("Max Heap: " + MetricCollector.maxHeapOverIterations);
        System.err.println("Max Native: " + MetricCollector.maxNativeOverIterations);
        System.err.println("Max Resident: " + MetricCollector.maxResidentOverIterations);

        /* to round up, upto 3 decimal places */
        decimalFormat.setRoundingMode(RoundingMode.CEILING);
        addComments();

        return Yaml.createYamlMappingBuilder()
                .add("apiVersion", apiVersion)
                .add("spec", Yaml.createYamlMappingBuilder()
                        .add("containers", Yaml.createYamlMappingBuilder()
                                .add("- name", name)
                                .add("env", Yaml.createYamlMappingBuilder()
                                        .add("- name", "JAVA_TOOL_OPTIONS")
                                        .add("value", getMaxRamPercentage(resStat)
                                                + generateXmnGencon()
                                                + generateXmoGencon()
                                                + setGCPolicy())
                                        .build())
                                .add("resources", Yaml.createYamlMappingBuilder()
                                        .add("requests", Yaml.createYamlMappingBuilder()
                                                .add("memory",
                                                        decimalFormat.format(Util.additionalBuffer(resStat.getPercentile(50))) + "MB")
                                                .add("cpu",
                                                        decimalFormat.format(Util.additionalBuffer(getMeanFromIterations(MetricCollector.cpuLoadValues) * ContainerAgent.cpuTargetMultiplier)))
                                                .build()
                                        )
                                        .add("limits", Yaml.createYamlMappingBuilder()
                                                .add("memory",
                                                        decimalFormat.format(Util.additionalBuffer(MetricCollector.maxResidentOverIterations)) + "MB")
                                                .add("cpu",
                                                        decimalFormat.format(Util.additionalBuffer(MetricCollector.maxCpuLoadOverIterations * ContainerAgent.cpuTargetMultiplier)))
                                                .build()
                                        )
                                        .build()
                                )
                                .build())
                        .build())
                .build();
    }

    private static void addComments()
    {
        ContainerAgent.comments = (ContainerAgent.config == 0) ? "#OPTIMIZED FOR PERFORMANCE\n" : "#OPTIMIZED FOR " +
                "LESS RESOURCE USAGE\n";

        ContainerAgent.comments += (ContainerAgent.governorPowersaveFlag == 0) ? "" : "#WARNING: CPU GOVERNOR SET TO " +
                "POWERSAVE. RESULTS MIGHT BE UNRELIABLE\n";

        ContainerAgent.comments += (DetectVM.identifyVM()) ? "#RUNNING ON VM. SOME INFO MIGHT BE UNAVAILABLE\n" : "";
    }

    private static String getMaxRamPercentage(DescriptiveStatistics resNoOutliers)
    {
        decimalFormat.setRoundingMode(RoundingMode.CEILING);
        return "-XX:MaxRAMPercentage=" + decimalFormat.format((Util.additionalBuffer
                ((MetricCollector.maxHeapOverIterations * 100.0)) / resNoOutliers.getMax()));
    }

    private static double getMeanFromIterations(double value)
    {
        double numberOfValues =
                ContainerAgent.NO_OF_VALUES_CURRENT + (Constants.MAX_NUMBER_OF_VALUES * ContainerAgent.NO_OF_ITERATIONS);
        return value / numberOfValues;
    }

    /* Currently only for gencon
       gencon: nursery-allocate + nursery-survivor
       balanced: nursery-allocate
    */
    private static String generateXmnGencon()
    {
        integerFormat.setRoundingMode(RoundingMode.CEILING);
        System.err.println("Max Nursery Allocated: " + MetricCollector.nurseryAllocatedMax);
        System.err.println("Max Nursery Survivor: " + MetricCollector.nurserySurvivorMax);
        System.err.println("Xmns: " + (MetricCollector.nurseryAllocatedMax + MetricCollector.nurserySurvivorMax));
        double xmn = Util.additionalBuffer(MetricCollector.nurseryAllocatedMax + MetricCollector.nurserySurvivorMax);
        return " -Xmns" + integerFormat.format((Util.convertToMB(xmn))) + "M";
    }

    private static String generateXmoGencon()
    {
        integerFormat.setRoundingMode(RoundingMode.CEILING);
        System.err.println("Max Tenured LOA: " + MetricCollector.tenuredLOAMax);
        System.err.println("Max Tenured SOA: " + MetricCollector.tenuredSOAMax);
        System.err.println("xmos: " + (MetricCollector.tenuredSOAMax + MetricCollector.tenuredLOAMax));
        double xmo = Util.additionalBuffer(MetricCollector.tenuredLOAMax + MetricCollector.tenuredSOAMax);
        return " -Xmos" + integerFormat.format((Util.convertToMB(xmo))) + "M";
    }

    /*TODO Look into setting GC policies based on the application
    When to use balanced GC policy?
    - It is 64 bit
    - Heap size is greater than 4GB
    - NUMA and multithreaded application
    - When there are long global garbage collection pause times
    - The application does not use many large arrays( > 0.1% of heap size)
    */

    private static String setGCPolicy()
    {
        String policy = "gencon";
        int weight = 0;

        if (SystemDump.hardwareAbstractionLayer.getProcessor().isCpu64bit())
        {
            weight++;
        }

        if (MetricCollector.maxHeapOverIterations > (Constants.ONE_GB * 4))
        {
            weight++;
        }

        if (weight > 3)
        {
            policy = "balanced";
        }
        return " -Xgcpolicy:" + policy;
    }

    private static void createGraphs(List<String> list, String title, String xAxisTitle, String yAxisTitle,
                                     DescriptiveStatistics stats, String fileName)
    {
        try
        {
            for (String value : list)
            {
                stats.addValue(Double.parseDouble(value));
            }
        }

        catch (Exception e)
        {
            System.err.println("Printing without outliers");
        }


        double[] time = new double[stats.getValues().length];

        time[0] = 0;

        for (int i = 1; i < time.length; i++)
        {
            time[i] = Constants.TIME_TO_SLEEP + time[i - 1];
        }

        try
        {
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
                    chart,
                    Util.separatorsToSystem(fileName),
                    BitmapEncoder.BitmapFormat.PNG,
                    600);
        }

        catch (Exception e)
        {
            System.err.println("IO ERROR: COULD NOT WRITE CHARTS TO FILE.");
        }

    }

}
