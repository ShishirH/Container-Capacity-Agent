/*
    Collects information from Prometheus

    Makes use of DescriptiveStatistics from Apache Commons-Math library that allows calculation of Mean, Median, Variance, Min, Max
    and Standard Deviation

 */
package com.ibm.cloudtools.agent;

import com.ibm.java.lang.management.internal.MemoryMXBeanImpl;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.lang.management.MemoryPoolMXBean;
import java.util.List;

class MetricCollector
{
    static double heapSumValues = 0;
    static double nativeSumValues = 0;
    static double residentSumValues = 0;
    static double cpuLoadValues = 0;
    static double maxCpuLoad = 0;

    CpuMetricsImpl cpuMetricsImpl = new CpuMetricsImpl();
    MemoryMetricsImpl memoryMetrics;

    private MemoryMXBeanImpl memoryMXBean = MemoryMXBeanImpl.getInstance();
    private List<MemoryPoolMXBean> memoryPoolMXBeans = memoryMXBean.getMemoryPoolMXBeans(false);
    private int memTypesLength = Constants.MEM_TYPE_LENGTH;

    String [] memDivisionNames = new String[memoryPoolMXBeans.size()];
    double [] time = new double[Constants.MAX_NUMBER_OF_VALUES];
    double [][] heapMemory = new double[memTypesLength][Constants.MAX_NUMBER_OF_VALUES];
    double [][] nativeMemory = new double[memTypesLength][Constants.MAX_NUMBER_OF_VALUES];
    DescriptiveStatistics [][] memDivisions = new DescriptiveStatistics[memTypesLength][memDivisionNames.length];

    int hyperThreadingInfo = cpuMetricsImpl.getHyperthreadingInfo();
    String [] cpuGovernors = cpuMetricsImpl.getCpuGovernors();
    String cpuModel = cpuMetricsImpl.getCpuModels();

    DescriptiveStatistics residentMemoryStat = new DescriptiveStatistics();
    DescriptiveStatistics [] heapStat = new DescriptiveStatistics[memTypesLength];
    DescriptiveStatistics [] nativeStat = new DescriptiveStatistics[memTypesLength];

    double maxHeapSize = -1;
    double meanHeapSize = -1;
    double maxNativeSize = -1;
    double meanNativeSize = -1;
    double totalMemory = -1;

    double maxHeapOverIterations = 0;
    double maxNativeOverIterations = 0;
    double maxResidentOverIterations = 0;

    MetricCollector()
    {
        for(int i = 0; i < Constants.MEM_TYPE_LENGTH; i++)
        {
            heapStat[i] = new DescriptiveStatistics();
            nativeStat[i] = new DescriptiveStatistics();
        }

        for(int i = 0; i < memDivisionNames.length; i++)
        {
            memDivisionNames[i] = memoryPoolMXBeans.get(i).getName();
        }

        memoryMetrics = new MemoryMetricsImpl();
    }

    double[] getTime()
    {
        return time;
    }

    DescriptiveStatistics[][] getMemDivisions()
    {
        return memDivisions;
    }

    DescriptiveStatistics[] getHeapStat()
    {
        return heapStat;
    }

    DescriptiveStatistics[] getNativeStat()
    {
        return nativeStat;
    }

    DescriptiveStatistics getResidentMemoryStat()
    {
        return residentMemoryStat;
    }

}
