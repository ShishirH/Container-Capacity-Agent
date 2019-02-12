package com.ibm.cloudtools.agent;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.ibm.lang.management.internal.ExtendedOperatingSystemMXBeanImpl;

import java.math.RoundingMode;
import java.text.DecimalFormat;

class GenerateConfig
{
    static YamlMapping createYamlConfig()
    {
        ContainerAgent.comments = (ContainerAgent.config == 0) ? "#OPTIMIZED FOR PERFORMANCE\n" : "#OPTIMIZED FOR LESS RESOURCE USAGE\n";

        ContainerAgent.comments += (ContainerAgent.governorPowersaveFlag == 0) ? "" : "#WARNING: CPU GOVERNOR SET TO " +
                "POWERSAVE. RESULTS MIGHT BE UNRELIABLE\n";

        ContainerAgent.comments += (DetectVM.identifyVM()) ? "#RUNNING ON VM. SOME INFO MIGHT BE UNAVAILABLE\n" : "";

        /* to round up, upto 3 decimal places */
        DecimalFormat decimalFormat = new DecimalFormat("#.###");
        decimalFormat.setRoundingMode(RoundingMode.CEILING);

        return Yaml.createYamlMappingBuilder()
                .add("apiVersion", "v1")
                .add("spec", Yaml.createYamlMappingBuilder()
                        .add("containers", Yaml.createYamlMappingBuilder()
                                .add("- name", "java")
                                .add("env", Yaml.createYamlMappingBuilder()
                                        .add("- name", "JAVA_TOOL_OPTIONS")
                                        .add("value"," -XX:MaxRAMPercentage=" + decimalFormat.format(GenerateConfig.getHeapPercentageFromTotal()))
                                        .build())
                                .add("resources", Yaml.createYamlMappingBuilder()
                                        .add("requests", Yaml.createYamlMappingBuilder()
                                                .add("memory", decimalFormat.format(additionalBuffer(getMeanFromIterations(MetricCollector.residentSumValues))) + "MB")
                                                .add("cpu", decimalFormat.format(additionalBuffer(getMeanFromIterations(MetricCollector.cpuLoadValues)* ContainerAgent.targetMultiplier)))
                                                .build()
                                        )
                                        .add("limits", Yaml.createYamlMappingBuilder()
                                                .add("memory", decimalFormat.format(additionalBuffer(MetricCollector.maxResidentOverIterations)) + "MB")
                                                .add("cpu", decimalFormat.format(additionalBuffer(MetricCollector.maxCpuLoad * ContainerAgent.targetMultiplier)))
                                                .build()
                                        )
                                        .build()
                                )
                                .build())
                        .build())
                .build();
    }

    private static double getHeapPercentageFromTotal()
    {
        ExtendedOperatingSystemMXBeanImpl extendedOperatingSystemMXBean =  ExtendedOperatingSystemMXBeanImpl.getInstance();
        double totalMemory = extendedOperatingSystemMXBean.getTotalPhysicalMemorySize();
        return (additionalBuffer(MetricCollector.maxHeapOverIterations * 100.0)) / (totalMemory/ 1000000.0);
    }

    private static double getMeanFromIterations(double value)
    {
        System.out.println("Value is: " + value);
        double numberOfValues = ContainerAgent.NO_OF_VALUES_CURRENT + (Constants.MAX_NUMBER_OF_VALUES * ContainerAgent.NO_OF_ITERATIONS);
        return value / numberOfValues;
    }

    static double additionalBuffer(double value)
    {
        value = (value * (ContainerAgent.buffer + 100.0)) / 100.0;
        return value;
    }

}
