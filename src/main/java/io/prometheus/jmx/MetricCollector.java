/*
    Collects information from Prometheus

    Makes use of DescriptiveStatistics from Apache Commons-Math library that allows calculation of Mean, Median, Variance, Min, Max
    and Standard Deviation

 */

package io.prometheus.jmx;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.regex.Pattern;

class MetricCollector
{
    String out = null;
    private Constants constants = new Constants();
    CpuMetricsImpl cpuMetricsImpl = new CpuMetricsImpl();
    PrometheusAgent prometheusAgent = new PrometheusAgent();
    String[] lines = null;
    private int memTypesLength = Constants.MEM_TYPE_LENGTH;
    double [] time = new double[Constants.MAX_NUMBER_OF_VALUES];
    double [][] heapMemory = new double[memTypesLength][Constants.MAX_NUMBER_OF_VALUES];
    double [][] nativeMemory = new double[memTypesLength][Constants.MAX_NUMBER_OF_VALUES];
    double[][][] heapTypes = new double[memTypesLength][Constants.HEAP_TYPES.length][Constants.MAX_NUMBER_OF_VALUES];
    DescriptiveStatistics residentMemoryStat = new DescriptiveStatistics();
    DescriptiveStatistics cpuSecondsStat = new DescriptiveStatistics();
    DescriptiveStatistics [] heapStat = new DescriptiveStatistics[memTypesLength];
    DescriptiveStatistics [] nativeStat = new DescriptiveStatistics[memTypesLength];
    Pattern doublePattern = Pattern.compile(constants.constPatterns.DOUBLE);
    Pattern scientificDoublePattern = Pattern.compile(constants.constPatterns.SCIENTIFIC_DOUBLE);

    MetricCollector()
    {
        for(int i = 0; i < Constants.MEM_TYPE_LENGTH; i++)
        {
            heapStat[i] = new DescriptiveStatistics();
            nativeStat[i] = new DescriptiveStatistics();
        }
        prometheusAgent.refreshURL(this);
    }

    double[] getTime()
    {
        return time;
    }

    double[][] getHeapMemory()
    {
        return heapMemory;
    }

    double[][] getNativeMemory()
    {
        return nativeMemory;
    }

    double[][][] getHeapTypes()
    {
        return heapTypes;
    }

    DescriptiveStatistics getCpuSecondsStat()
    {
        return cpuSecondsStat;
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
