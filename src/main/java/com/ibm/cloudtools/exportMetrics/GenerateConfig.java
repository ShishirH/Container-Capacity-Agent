/*
 * ******************************************************************************
 *  * Copyright (c) 2012, 2018 IBM Corp. and others
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
import com.ibm.cloudtools.agent.*;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class GenerateConfig
{
    public static String name;
    public static String apiVersion;
    private static DecimalFormat decimalFormat = new DecimalFormat("#.###");
    private static DecimalFormat integerFormat = new DecimalFormat("#");

    public static YamlMapping createYamlConfig()
    {
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
                                        .add("value", getMaxRamPercentage()
                                                + generateXmnGencon()
                                                + generateXmoGencon()
                                                + setGCPolicy())
                                        .build())
                                .add("resources", Yaml.createYamlMappingBuilder()
                                        .add("requests", Yaml.createYamlMappingBuilder()
                                                .add("memory",
                                                        decimalFormat.format(Util.additionalBuffer(getMeanFromIterations(MetricCollector.residentSumValues))) + "MB")
                                                .add("cpu",
                                                        decimalFormat.format(Util.additionalBuffer(getMeanFromIterations(MetricCollector.cpuLoadValues) * ContainerAgent.targetMultiplier)))
                                                .build()
                                        )
                                        .add("limits", Yaml.createYamlMappingBuilder()
                                                .add("memory",
                                                        decimalFormat.format(Util.additionalBuffer(MetricCollector.maxResidentOverIterations)) + "MB")
                                                .add("cpu",
                                                        decimalFormat.format(Util.additionalBuffer(MetricCollector.maxCpuLoadOverIterations * ContainerAgent.targetMultiplier)))
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

    private static String getMaxRamPercentage()
    {
        decimalFormat.setRoundingMode(RoundingMode.CEILING);
        return "-XX:MaxRAMPercentage=" + decimalFormat.format((Util.additionalBuffer
                ((MetricCollector.maxHeapOverIterations * 100.0)) / MetricCollector.maxResidentOverIterations));
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
        double xmn = Util.additionalBuffer(MetricCollector.nurseryAllocatedMax + MetricCollector.nurserySurvivorMax);
        return " -Xmns" + integerFormat.format((Util.convertToMB(xmn))) + "M";
    }

    private static String generateXmoGencon()
    {
        integerFormat.setRoundingMode(RoundingMode.CEILING);
        double xmo = Util.additionalBuffer(MetricCollector.tenuredLOAMax + MetricCollector.tenuredSOAMax);
        return " -Xmos" + integerFormat.format((Util.convertToMB(xmo))) + "M";
    }

    /*TODO
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

}
