/*
   Collects information from Prometheus

   Makes use of DescriptiveStatistics from Apache Commons-Math library that allows calculation of Mean, Median,
   Variance, Min, Max
   and Standard Deviation

*/
package com.ibm.cloudtools.agent;

import com.ibm.java.lang.management.internal.MemoryMXBeanImpl;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.lang.management.MemoryPoolMXBean;
import java.util.List;

class MetricCollector {
  /* Used for the overall mean */
  static double heapSumValues = 0;
  static double nativeSumValues = 0;
  static double residentSumValues = 0;
  static double cpuLoadValues = 0;

  /* Maximum over all iterations */
  static double maxCpuLoadOverIterations = 0;
  static double maxHeapOverIterations = 0;
  static double maxNativeOverIterations = 0;
  static double maxResidentOverIterations = 0;

  static DescriptiveStatistics chartCpuLoadStat = new DescriptiveStatistics();
  static DescriptiveStatistics chartResidentStat = new DescriptiveStatistics();
  static int nurseryAllocatedIndex = -1;
  static int nurserySurvivorIndex = -1;
  static int tenuredSOAIndex = -1;
  static int tenuredLOAIndex = -1;
  static double nurseryAllocatedMax = -1;
  static double nurserySurvivorMax = -1;
  static double tenuredSOAMax = -1;
  static double tenuredLOAMax = -1;
  CpuMetricsImpl cpuMetricsImpl = new CpuMetricsImpl();
  MemoryMetricsImpl memoryMetrics;
  int hyperThreadingInfo = cpuMetricsImpl.getHyperthreadingInfo();
  String[] cpuGovernors = cpuMetricsImpl.getCpuGovernors();
  String cpuModel = cpuMetricsImpl.getCpuModels();
  DescriptiveStatistics residentMemoryStat = new DescriptiveStatistics();
  private MemoryMXBeanImpl memoryMXBean = MemoryMXBeanImpl.getInstance();
  private List<MemoryPoolMXBean> memoryPoolMXBeans = memoryMXBean.getMemoryPoolMXBeans(false);
  String[] memDivisionNames = new String[memoryPoolMXBeans.size()];
  private int memTypesLength = Constants.MEM_TYPE_LENGTH;
  DescriptiveStatistics[][] memDivisions =
      new DescriptiveStatistics[memTypesLength][memDivisionNames.length];
  DescriptiveStatistics[] heapStat = new DescriptiveStatistics[memTypesLength];
  DescriptiveStatistics[] nativeStat = new DescriptiveStatistics[memTypesLength];

  MetricCollector() {
    for (int i = 0; i < Constants.MEM_TYPE_LENGTH; i++) {
      heapStat[i] = new DescriptiveStatistics();
      nativeStat[i] = new DescriptiveStatistics();
    }

    for (int i = 0; i < memDivisionNames.length; i++) {
      memDivisionNames[i] = memoryPoolMXBeans.get(i).getName();
      if (memDivisionNames[i].equals("nursery-allocate")) nurseryAllocatedIndex = i;

      if (memDivisionNames[i].equals("nursery-survivor")) nurserySurvivorIndex = i;

      if (memDivisionNames[i].equals("tenured-SOA")) tenuredSOAIndex = i;

      if (memDivisionNames[i].equals("tenured-LOA")) tenuredLOAIndex = i;
    }

    memoryMetrics = new MemoryMetricsImpl();
  }

  DescriptiveStatistics[][] getMemDivisions() {
    return memDivisions;
  }

  DescriptiveStatistics[] getHeapStat() {
    return heapStat;
  }

  DescriptiveStatistics[] getNativeStat() {
    return nativeStat;
  }

  DescriptiveStatistics getResidentMemoryStat() {
    return residentMemoryStat;
  }
}
