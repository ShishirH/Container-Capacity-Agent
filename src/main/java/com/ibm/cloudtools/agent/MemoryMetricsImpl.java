package com.ibm.cloudtools.agent;

import com.ibm.java.lang.management.internal.MemoryMXBeanImpl;
import com.ibm.lang.management.internal.ExtendedOperatingSystemMXBeanImpl;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.lang.management.MemoryPoolMXBean;
import java.util.List;
import java.util.concurrent.TimeUnit;

class MemoryMetricsImpl
{
    private void getHeapAndNative(MetricCollector metricCollector, int iteration, MemoryMXBeanImpl memoryMXBean)
    {
        metricCollector.heapMemory[0][iteration] = memoryMXBean.getHeapMemoryUsage().getCommitted();
        metricCollector.heapMemory[1][iteration] = memoryMXBean.getHeapMemoryUsage().getUsed();
        metricCollector.heapMemory[2][iteration] = memoryMXBean.getHeapMemoryUsage().getMax();
        metricCollector.heapMemory[3][iteration] = memoryMXBean.getHeapMemoryUsage().getInit();

        metricCollector.heapStat[0].addValue(metricCollector.heapMemory[0][iteration]);
        metricCollector.heapStat[1].addValue(metricCollector.heapMemory[1][iteration]);
        metricCollector.heapStat[2].addValue(metricCollector.heapMemory[2][iteration]);
        metricCollector.heapStat[3].addValue(metricCollector.heapMemory[3][iteration]);

        metricCollector.nativeMemory[0][iteration] = memoryMXBean.getNonHeapMemoryUsage().getCommitted();
        metricCollector.nativeMemory[1][iteration] = memoryMXBean.getNonHeapMemoryUsage().getUsed();
        metricCollector.nativeMemory[2][iteration] = memoryMXBean.getNonHeapMemoryUsage().getMax();
        metricCollector.nativeMemory[3][iteration] = memoryMXBean.getNonHeapMemoryUsage().getInit();

        metricCollector.nativeStat[0].addValue(metricCollector.nativeMemory[0][iteration]);
        metricCollector.nativeStat[1].addValue(metricCollector.nativeMemory[1][iteration]);
        metricCollector.nativeStat[2].addValue(metricCollector.nativeMemory[2][iteration]);
        metricCollector.nativeStat[3].addValue(metricCollector.nativeMemory[3][iteration]);
    }

    //0: Committed 1: Used 2: Max 3: Init
    private void getDivision(MetricCollector metricCollector, List<MemoryPoolMXBean> memoryPoolMXBeans)
    {
        int division = 0;
        for(MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans)
        {
            metricCollector.memDivisions[0][division].addValue(memoryPoolMXBean.getUsage().getCommitted());
            metricCollector.memDivisions[1][division].addValue(memoryPoolMXBean.getUsage().getUsed());
            metricCollector.memDivisions[2][division].addValue(memoryPoolMXBean.getUsage().getMax());
            metricCollector.memDivisions[3][division].addValue(memoryPoolMXBean.getUsage().getInit());
            division++;
        }
    }

    void getMemoryMetrics(MetricCollector metricCollector)
    {
        MemoryMXBeanImpl memoryMXBean;
        List<MemoryPoolMXBean> memoryPoolMXBeans;

        ExtendedOperatingSystemMXBeanImpl extendedOperatingSystemMXBean =  ExtendedOperatingSystemMXBeanImpl.getInstance();
        extendedOperatingSystemMXBean.getProcessCpuLoad();

        memoryMXBean = MemoryMXBeanImpl.getInstance();
        memoryPoolMXBeans = memoryMXBean.getMemoryPoolMXBeans(false);

        for(int division = 0; division < memoryPoolMXBeans.size(); division++)
        {
            metricCollector.memDivisions[0][division] = new DescriptiveStatistics();
            metricCollector.memDivisions[1][division] = new DescriptiveStatistics();
            metricCollector.memDivisions[2][division] = new DescriptiveStatistics();
            metricCollector.memDivisions[3][division] = new DescriptiveStatistics();
        }

        try
        {
            TimeUnit.SECONDS.sleep(1);
        }

        catch(InterruptedException e)
        {
            e.printStackTrace();
        }

        for(int i = 0; i < Constants.MAX_NUMBER_OF_VALUES; i++)
        {
            ContainerAgent.NO_OF_VALUES_CURRENT++;
            memoryMXBean = MemoryMXBeanImpl.getInstance();
            memoryPoolMXBeans = memoryMXBean.getMemoryPoolMXBeans(false);

            double cpuLoad = extendedOperatingSystemMXBean.getProcessCpuLoad() * Constants.NO_OF_CORES;
            metricCollector.cpuMetricsImpl.loadStat.addValue(cpuLoad);
            MetricCollector.cpuLoadValues += cpuLoad;
            if(MetricCollector.maxCpuLoad < cpuLoad)
            {
                MetricCollector.maxCpuLoad = cpuLoad;
            }

            getDivision(metricCollector, memoryPoolMXBeans);
            getHeapAndNative(metricCollector, i, memoryMXBean);

            long processPhysicalMemorySize = extendedOperatingSystemMXBean.getProcessPhysicalMemorySize();
            metricCollector.residentMemoryStat.addValue(processPhysicalMemorySize);
            MetricCollector.residentSumValues += processPhysicalMemorySize / 1000000.0;

            if(MetricCollector.maxResidentOverIterations < (processPhysicalMemorySize / 1000000.0))
            {
                MetricCollector.maxResidentOverIterations = (processPhysicalMemorySize / 1000000.0);
            }

            metricCollector.cpuMetricsImpl.getCpuCurrentFrequency();

            try
            {
                metricCollector.time[i] = i * Constants.TIME_TO_SLEEP;
                TimeUnit.SECONDS.sleep(Constants.TIME_TO_SLEEP);
            }

            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

}
