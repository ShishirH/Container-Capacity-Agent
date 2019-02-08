package com.ibm.cloudtools.agent;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public interface CpuMetrics
{
    int getHyperthreadingInfo();
    String[] getCpuGovernors();
    void getCpuCurrentFrequency();
    String getCpuModels();
    DescriptiveStatistics getCpuLoad();
}
