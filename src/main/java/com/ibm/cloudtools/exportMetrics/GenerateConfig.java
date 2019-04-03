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

package com.ibm.cloudtools.exportMetrics;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.ibm.cloudtools.agent.Constants;
import com.ibm.cloudtools.agent.InputParams;
import com.ibm.cloudtools.agent.MetricCollector;
import com.ibm.cloudtools.agent.Util;
import com.ibm.cloudtools.metrics.memory.MemoryMetricsImpl;
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

        try {
            resList =
                    Files.readAllLines(
                            Paths.get(Util.separatorsToSystem("Output/Dump/resValues.txt")), Charset.forName("utf-8"));
            copyFromList(resList, resStat);
            heapList =
                    Files.readAllLines(
                            Paths.get(Util.separatorsToSystem("Output/Dump/heapCommittedValues.txt")),
                            Charset.forName("utf-8"));
            copyFromList(heapList, heapStat);
            nativeList =
                    Files.readAllLines(
                            Paths.get(Util.separatorsToSystem("Output/Dump/nativeUsedValues.txt")),
                            Charset.forName("utf-8"));
            copyFromList(nativeList, nativeStat);
            cpuList =
                    Files.readAllLines(
                            Paths.get(Util.separatorsToSystem("Output/Dump/cpuLoad.txt")), Charset.forName("utf-8"));
            copyFromList(cpuList, cpuLoadStat);
        } catch (IOException e) {
            System.err.println("COULD NOT CREATE YAML CONFIGURATION!");
            return Yaml.createYamlMappingBuilder().build();
        }

        DescriptiveStatistics resStatNoOutliers = DetectOutlier.removeOutliers(resStat);
        DescriptiveStatistics heapStatNoOutliers = DetectOutlier.removeOutliers(heapStat);
        DescriptiveStatistics cpuLoadStatNoOutliers = DetectOutlier.removeOutliers(cpuLoadStat);

        /* to round up, upto 3 decimal places */
        precisionThree.setRoundingMode(RoundingMode.CEILING);
        addComments();

        YamlMapping requestsValue = requestYAML(resStatNoOutliers, cpuLoadStatNoOutliers).build();

        YamlMapping limitsValue = limitsYAML(resStat, cpuLoadStat).build();

        YamlMapping resourcesValue =
                Yaml.createYamlMappingBuilder()
                        .add("requests", requestsValue)
                        .add("limits", limitsValue)
                        .build();

        YamlMapping JVMOptionsValue = JVMOptionsYAML(resStatNoOutliers, heapStatNoOutliers).build();

        YamlMapping containersValues =
                Yaml.createYamlMappingBuilder()
                        .add("- name", InputParams.getName())
                        .add("env", JVMOptionsValue)
                        .add("resources", resourcesValue)
                        .build();

        YamlMapping specValue =
                Yaml.createYamlMappingBuilder().add("containers", containersValues).build();

        return Yaml.createYamlMappingBuilder()
                .add("apiVersion", InputParams.getApiVersion())
                .add("spec", specValue)
                .build();
    }

    private static YamlMappingBuilder requestYAML(
            DescriptiveStatistics resStatNoOutliers, DescriptiveStatistics cpuLoadStatNoOutliers)
    {
        return Yaml.createYamlMappingBuilder()
                .add(
                        "memory",
                        precisionThree.format(Util.additionalBuffer(resStatNoOutliers.getPercentile(50)))
                                + "MB")
                .add(
                        "cpu",
                        precisionThree.format(
                                Util.additionalBuffer(
                                        cpuLoadStatNoOutliers.getPercentile(50)
                                                * InputParams.getCpuTargetMultiplier())));
    }

    private static YamlMappingBuilder JVMOptionsYAML(
            DescriptiveStatistics resStatNoOutliers, DescriptiveStatistics heapStatNoOutliers)
    {
        return Yaml.createYamlMappingBuilder()
                .add("- name", "JAVA_TOOL_OPTIONS")
                .add(
                        "value",
                        getRAMPercentage(resStatNoOutliers, heapStatNoOutliers)
                                + generateXmnGencon()
                                + generateXmoGencon()
                                + setGCPolicy(heapStatNoOutliers));
    }

    private static YamlMappingBuilder limitsYAML(
            DescriptiveStatistics resStat, DescriptiveStatistics cpuLoadStat)
    {
        return Yaml.createYamlMappingBuilder()
                .add("memory", precisionThree.format(Util.additionalBuffer(resStat.getMax())) + "MB")
                .add(
                        "cpu",
                        precisionThree.format(
                                Util.additionalBuffer(
                                        cpuLoadStat.getMax() * InputParams.getCpuTargetMultiplier())));
    }

    private static void addComments()
    {
        comments =
                (InputParams.getConfig() == InputParams.PERFORMANCE_CONFIG)
                        ? "#OPTIMIZED FOR PERFORMANCE\n"
                        : "#OPTIMIZED FOR " + "LESS RESOURCE USAGE\n";

        comments +=
                (MetricCollector.governorPowersaveFlag == 0)
                        ? ""
                        : "#WARNING: CPU GOVERNOR SET TO " + "POWERSAVE. RESULTS MIGHT BE UNRELIABLE\n";

        comments += (DetectVM.identifyVM()) ? "#RUNNING ON VM. SOME INFO MIGHT BE UNAVAILABLE\n" : "";
    }

    private static String getRAMPercentage(
            DescriptiveStatistics resNoOutliers, DescriptiveStatistics heapNoOutliers)
    {
        precisionThree.setRoundingMode(RoundingMode.CEILING);
        String percentage =
                precisionThree.format(
                        (Util.additionalBuffer((heapNoOutliers.getMax()) * 100) / resNoOutliers.getMax()));
        return "-XX:InitialRAMPercentage=" + percentage + " -XX:MaxRAMPercentage=" + percentage;
    }

    /* Currently only for gencon
       gencon: nursery-allocate + nursery-survivor
       balanced: nursery-allocate
    */
    private static String generateXmnGencon()
    {
        integerFormat.setRoundingMode(RoundingMode.CEILING);
        double xmn =
                Util.additionalBuffer(
                        MemoryMetricsImpl.nurseryAllocatedMax + MemoryMetricsImpl.nurserySurvivorMax);
        return " -Xmns" + integerFormat.format((xmn / Constants.ONE_MB)) + "M";
    }

    private static String generateXmoGencon()
    {
        integerFormat.setRoundingMode(RoundingMode.CEILING);
        double xmo =
                Util.additionalBuffer(MemoryMetricsImpl.tenuredLOAMax + MemoryMetricsImpl.tenuredSOAMax);
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

        if (SystemDump.hardwareAbstractionLayer.getProcessor().isCpu64bit()) {
            weight++;
        }

        if (heapStat.getMax() > (Constants.ONE_GB * 4)) {
            weight++;
        }

        if (weight > 3) {
            policy = "balanced";
        }
        return " -Xgcpolicy:" + policy;
    }

    private static void copyFromList(List<String> list, DescriptiveStatistics descriptiveStatistics)
    {
        for (String value : list) {
            descriptiveStatistics.addValue(Double.parseDouble(value));
        }
    }
}
