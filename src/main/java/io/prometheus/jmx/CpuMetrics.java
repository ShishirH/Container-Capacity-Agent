package io.prometheus.jmx;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public interface CpuMetrics
{
    int[] getHyperthreadingInfo();
    String[] getCpuGovernors();
    double[][] getCpuCurrentFrequency();
    String[] getCpuModels();
    DescriptiveStatistics getCpuLoad();
}
