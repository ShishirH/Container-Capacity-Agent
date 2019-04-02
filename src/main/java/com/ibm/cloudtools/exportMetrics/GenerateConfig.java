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
import com.ibm.cloudtools.agent.InputParams;
import com.ibm.cloudtools.agent.MetricCollector;
import com.ibm.cloudtools.agent.Util;
import com.ibm.cloudtools.statistics.DetectOutlier;
import com.ibm.cloudtools.system.DetectVM;
import com.ibm.cloudtools.system.SystemDump;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.IOException;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.List;

public class GenerateConfig
{
    public static String comments;
    private static DecimalFormat precisionThree = new DecimalFormat("#.###");
    private static DecimalFormat integerFormat = new DecimalFormat("#");

    public static YamlMapping createYamlConfig()
    {
        List<String> resList;
        List<String> heapList;
        List<String> nativeList;
        List<String> cpuList;

        DescriptiveStatistics resStat = new DescriptiveStatistics();
        DescriptiveStatistics heapStat = new DescriptiveStatistics();
        DescriptiveStatistics nativeStat = new DescriptiveStatistics();
        DescriptiveStatistics cpuLoadStat = new DescriptiveStatistics();


        try
        {
            resList = Files.readAllLines(Paths.get(Util.separatorsToSystem("Output/resValues.txt")), Charset.forName("utf-8"));
            copyFromList(resList, resStat);
            heapList = Files.readAllLines(Paths.get(Util.separatorsToSystem("Output/heapUsedValues.txt")), Charset.forName("utf-8"));
            copyFromList(heapList, heapStat);
            nativeList = Files.readAllLines(Paths.get(Util.separatorsToSystem("Output/nativeUsedValues.txt")), Charset.forName("utf-8"));
            copyFromList(nativeList, nativeStat);
            cpuList = Files.readAllLines(Paths.get(Util.separatorsToSystem("Output/cpuLoad.txt")), Charset.forName("utf-8"));
            copyFromList(cpuList, cpuLoadStat);
        }

        catch (IOException e)
        {
            System.err.println("COULD NOT CREATE YAML CONFIGURATION!");
            return Yaml.createYamlMappingBuilder().build();
        }

        DescriptiveStatistics resStatNoOutliers = DetectOutlier.removeOutliers(resStat);
        DescriptiveStatistics heapStatNoOutliers = DetectOutlier.removeOutliers(heapStat);
        DescriptiveStatistics cpuLoadStatNoOutliers = DetectOutlier.removeOutliers(cpuLoadStat);

        /* to round up, upto 3 decimal places */
        precisionThree.setRoundingMode(RoundingMode.CEILING);
        addComments();

        return Yaml.createYamlMappingBuilder()
                .add("apiVersion", InputParams.apiVersion)
                .add("spec", Yaml.createYamlMappingBuilder()
                        .add("containers", Yaml.createYamlMappingBuilder()
                                .add("- name", InputParams.name)
                                .add("env", Yaml.createYamlMappingBuilder()
                                        .add("- name", "JAVA_TOOL_OPTIONS")
                                        .add("value", getMaxRamPercentage(resStatNoOutliers, heapStatNoOutliers)
                                                + generateXmnGencon()
                                                + generateXmoGencon()
                                                + setGCPolicy(heapStat))
                                        .build())
                                .add("resources", Yaml.createYamlMappingBuilder()
                                        .add("requests", Yaml.createYamlMappingBuilder()
                                                .add("memory",
                                                        precisionThree.format(Util.additionalBuffer(resStatNoOutliers.getPercentile(50))) + "MB")
                                                .add("cpu",
                                                        precisionThree.format(Util.additionalBuffer(cpuLoadStatNoOutliers.getPercentile(50)
                                                                * InputParams.cpuTargetMultiplier)))
                                                .build()
                                        )
                                        .add("limits", Yaml.createYamlMappingBuilder()
                                                .add("memory",
                                                        precisionThree.format(Util.additionalBuffer(resStat.getMax())) + "MB")
                                                .add("cpu",
                                                        precisionThree.format(Util.additionalBuffer(cpuLoadStat.getMax()
                                                                * InputParams.cpuTargetMultiplier)))
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
        comments = (InputParams.config == 0) ? "#OPTIMIZED FOR PERFORMANCE\n" : "#OPTIMIZED FOR " +
                "LESS RESOURCE USAGE\n";

        comments += (MetricCollector.governorPowersaveFlag == 0) ? "" : "#WARNING: CPU GOVERNOR SET TO " +
                "POWERSAVE. RESULTS MIGHT BE UNRELIABLE\n";

        comments += (DetectVM.identifyVM()) ? "#RUNNING ON VM. SOME INFO MIGHT BE UNAVAILABLE\n" : "";
    }

    private static String getMaxRamPercentage(DescriptiveStatistics resNoOutliers, DescriptiveStatistics heapNoOutliers)
    {
        precisionThree.setRoundingMode(RoundingMode.CEILING);
        return "-XX:MaxRAMPercentage=" + precisionThree.format((Util.additionalBuffer
                ((heapNoOutliers.getMax()) * 100) / resNoOutliers.getMax()));
    }

    /* Currently only for gencon
       gencon: nursery-allocate + nursery-survivor
       balanced: nursery-allocate
    */
    private static String generateXmnGencon()
    {
        integerFormat.setRoundingMode(RoundingMode.CEILING);
        double xmn = Util.additionalBuffer(MetricCollector.nurseryAllocatedMax + MetricCollector.nurserySurvivorMax);
        return " -Xmns" + integerFormat.format((xmn / Constants.ONE_MB)) + "M";
    }

    private static String generateXmoGencon()
    {
        integerFormat.setRoundingMode(RoundingMode.CEILING);
        double xmo = Util.additionalBuffer(MetricCollector.tenuredLOAMax + MetricCollector.tenuredSOAMax);
        return " -Xmos" + integerFormat.format((xmo / Constants.ONE_MB)) + "M";
    }

    /*TODO Look into setting GC policies based on the application
    When to use balanced GC policy?
    - It is 64 bit
    - Heap size is greater than 4GB
    - NUMA and multithreaded application
    - When there are long global garbage collection pause times
    - The application does not use many large arrays( > 0.1% of heap size)
    */

    private static String setGCPolicy(DescriptiveStatistics heapStat)
    {
        String policy = "gencon";
        int weight = 0;

        if (SystemDump.hardwareAbstractionLayer.getProcessor().isCpu64bit())
        {
            weight++;
        }

        if (heapStat.getMax() > (Constants.ONE_GB * 4))
        {
            weight++;
        }

        if (weight > 3)
        {
            policy = "balanced";
        }
        return " -Xgcpolicy:" + policy;
    }

    private static void copyFromList(List<String> list, DescriptiveStatistics descriptiveStatistics)
    {
        for (String value : list)
        {
            descriptiveStatistics.addValue(Double.parseDouble(value));
        }

    }

}
