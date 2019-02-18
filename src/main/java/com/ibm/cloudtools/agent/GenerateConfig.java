package com.ibm.cloudtools.agent;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;

import java.math.RoundingMode;
import java.text.DecimalFormat;

class GenerateConfig {
  static String name;
  static String apiVersion;
  private static DecimalFormat decimalFormat = new DecimalFormat("#.###");
  private static DecimalFormat integerFormat = new DecimalFormat("#");

  static YamlMapping createYamlConfig() {
    System.err.println("Max Heap: " + MetricCollector.maxHeapOverIterations);
    System.err.println("Max Native: " + MetricCollector.maxNativeOverIterations);
    System.err.println("Max Resident: " + MetricCollector.maxResidentOverIterations);

    /* to round up, upto 3 decimal places */
    decimalFormat.setRoundingMode(RoundingMode.CEILING);
    addComments();

    return Yaml.createYamlMappingBuilder()
        .add("apiVersion", apiVersion)
        .add(
            "spec",
            Yaml.createYamlMappingBuilder()
                .add(
                    "containers",
                    Yaml.createYamlMappingBuilder()
                        .add("- name", name)
                        .add(
                            "env",
                            Yaml.createYamlMappingBuilder()
                                .add("- name", "JAVA_TOOL_OPTIONS")
                                .add(
                                    "value",
                                    getMaxRamPercentage()
                                        + generateXmnGencon()
                                        + generateXmoGencon()
                                        + setGCPolicy())
                                .build())
                        .add(
                            "resources",
                            Yaml.createYamlMappingBuilder()
                                .add(
                                    "requests",
                                    Yaml.createYamlMappingBuilder()
                                        .add(
                                            "memory",
                                            decimalFormat.format(
                                                    Util.additionalBuffer(
                                                        getMeanFromIterations(
                                                            MetricCollector.residentSumValues)))
                                                + "MB")
                                        .add(
                                            "cpu",
                                            decimalFormat.format(
                                                Util.additionalBuffer(
                                                    getMeanFromIterations(
                                                            MetricCollector.cpuLoadValues)
                                                        * ContainerAgent.targetMultiplier)))
                                        .build())
                                .add(
                                    "limits",
                                    Yaml.createYamlMappingBuilder()
                                        .add(
                                            "memory",
                                            decimalFormat.format(
                                                    Util.additionalBuffer(
                                                        MetricCollector.maxResidentOverIterations))
                                                + "MB")
                                        .add(
                                            "cpu",
                                            decimalFormat.format(
                                                Util.additionalBuffer(
                                                    MetricCollector.maxCpuLoadOverIterations
                                                        * ContainerAgent.targetMultiplier)))
                                        .build())
                                .build())
                        .build())
                .build())
        .build();
  }

  private static void addComments() {
    ContainerAgent.comments =
        (ContainerAgent.config == 0)
            ? "#OPTIMIZED FOR PERFORMANCE\n"
            : "#OPTIMIZED FOR " + "LESS RESOURCE USAGE\n";

    ContainerAgent.comments +=
        (ContainerAgent.governorPowersaveFlag == 0)
            ? ""
            : "#WARNING: CPU GOVERNOR SET TO " + "POWERSAVE. RESULTS MIGHT BE UNRELIABLE\n";

    ContainerAgent.comments +=
        (DetectVM.identifyVM()) ? "#RUNNING ON VM. SOME INFO MIGHT BE UNAVAILABLE\n" : "";
  }

  private static String getMaxRamPercentage() {
    decimalFormat.setRoundingMode(RoundingMode.CEILING);
    return "-XX:MaxRAMPercentage="
        + decimalFormat.format(
            (Util.additionalBuffer((MetricCollector.maxHeapOverIterations * 100.0))
                / MetricCollector.maxResidentOverIterations));
  }

  private static double getMeanFromIterations(double value) {
    double numberOfValues =
        ContainerAgent.NO_OF_VALUES_CURRENT
            + (Constants.MAX_NUMBER_OF_VALUES * ContainerAgent.NO_OF_ITERATIONS);
    return value / numberOfValues;
  }

  /* Currently only for gencon
     gencon: nursery-allocate + nursery-survivor
     balanced: nursery-allocate
  */
  private static String generateXmnGencon() {
    integerFormat.setRoundingMode(RoundingMode.CEILING);
    double xmn =
        Util.additionalBuffer(
            MetricCollector.nurseryAllocatedMax + MetricCollector.nurserySurvivorMax);
    return " -Xmns" + integerFormat.format((Util.convertToMB(xmn))) + "M";
  }

  private static String generateXmoGencon() {
    integerFormat.setRoundingMode(RoundingMode.CEILING);
    double xmo =
        Util.additionalBuffer(MetricCollector.tenuredLOAMax + MetricCollector.tenuredSOAMax);
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

  private static String setGCPolicy() {
    String policy = "gencon";
    int weight = 0;

    if (SystemDump.hardwareAbstractionLayer.getProcessor().isCpu64bit()) {
      weight++;
    }

    if (MetricCollector.maxHeapOverIterations > (Constants.ONE_GB * 4)) {
      weight++;
    }

    if (weight > 3) {
      policy = "balanced";
    }
    return " -Xgcpolicy:" + policy;
  }
}
