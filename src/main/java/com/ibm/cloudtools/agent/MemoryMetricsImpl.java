package com.ibm.cloudtools.agent;

import com.ibm.java.lang.management.internal.MemoryMXBeanImpl;
import com.ibm.lang.management.internal.ExtendedOperatingSystemMXBeanImpl;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.lang.management.MemoryPoolMXBean;
import java.util.List;
import java.util.concurrent.TimeUnit;

class MemoryMetricsImpl
{
    private void getHeapAndNative(MetricCollector metricCollector, MemoryMXBeanImpl memoryMXBean)
    {
        long heapCommitted = memoryMXBean.getHeapMemoryUsage().getCommitted();
        long nativeCommitted = memoryMXBean.getNonHeapMemoryUsage().getCommitted();

        metricCollector.heapStat[0].addValue(heapCommitted);
        metricCollector.heapStat[1].addValue(memoryMXBean.getHeapMemoryUsage().getUsed());
        metricCollector.heapStat[2].addValue(memoryMXBean.getHeapMemoryUsage().getMax());
        metricCollector.heapStat[3].addValue(memoryMXBean.getHeapMemoryUsage().getInit());


        metricCollector.nativeStat[0].addValue(nativeCommitted);
        metricCollector.nativeStat[1].addValue(memoryMXBean.getNonHeapMemoryUsage().getUsed());
        metricCollector.nativeStat[2].addValue(memoryMXBean.getNonHeapMemoryUsage().getMax());
        metricCollector.nativeStat[3].addValue(memoryMXBean.getNonHeapMemoryUsage().getInit());

        if (MetricCollector.maxNativeOverIterations < Util.convertToMB(nativeCommitted))
        {
            MetricCollector.maxNativeOverIterations = Util.convertToMB(nativeCommitted);
        }

        if (MetricCollector.maxHeapOverIterations < Util.convertToMB(heapCommitted))
        {
            MetricCollector.maxHeapOverIterations = Util.convertToMB(heapCommitted);
        }

    }

    //0: Committed 1: Used 2: Max 3: Init
    private void getDivision(MetricCollector metricCollector, List<MemoryPoolMXBean> memoryPoolMXBeans)
    {
        int division = 0;
        for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans)
        {
            long committed = memoryPoolMXBean.getUsage().getCommitted();
            metricCollector.memDivisions[0][division].addValue(committed);
            metricCollector.memDivisions[1][division].addValue(memoryPoolMXBean.getUsage().getUsed());
            metricCollector.memDivisions[2][division].addValue(memoryPoolMXBean.getUsage().getMax());
            metricCollector.memDivisions[3][division].addValue(memoryPoolMXBean.getUsage().getInit());

            if(division == MetricCollector.nurseryAllocatedIndex)
            {
                if(MetricCollector.nurseryAllocatedMax < committed)
                    MetricCollector.nurseryAllocatedMax = committed;
            }

            if(division == MetricCollector.nurserySurvivorIndex)
            {
                if(MetricCollector.nurserySurvivorMax < committed)
                    MetricCollector.nurserySurvivorMax = committed;
            }

            if(division == MetricCollector.tenuredLOAIndex)
            {
                if(MetricCollector.tenuredLOAMax < committed)
                    MetricCollector.tenuredLOAMax = committed;
            }

            if(division == MetricCollector.tenuredSOAIndex)
            {
                if(MetricCollector.tenuredSOAMax < committed)
                    MetricCollector.tenuredSOAMax = committed;
            }


            division++;
        }
    }

    void getMemoryMetrics(MetricCollector metricCollector)
    {
        MemoryMXBeanImpl memoryMXBean;
        List<MemoryPoolMXBean> memoryPoolMXBeans;

        ExtendedOperatingSystemMXBeanImpl extendedOperatingSystemMXBean =
                ExtendedOperatingSystemMXBeanImpl.getInstance();
        extendedOperatingSystemMXBean.getProcessCpuLoad();

        memoryMXBean = MemoryMXBeanImpl.getInstance();
        memoryPoolMXBeans = memoryMXBean.getMemoryPoolMXBeans(false);

        for (int division = 0; division < memoryPoolMXBeans.size(); division++)
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
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        for (int i = 0; i < Constants.MAX_NUMBER_OF_VALUES; i++)
        {
            ContainerAgent.NO_OF_VALUES_CURRENT++;
            memoryMXBean = MemoryMXBeanImpl.getInstance();
            memoryPoolMXBeans = memoryMXBean.getMemoryPoolMXBeans(false);

            double cpuLoad = extendedOperatingSystemMXBean.getProcessCpuLoad() * Constants.NO_OF_CORES;
            metricCollector.cpuMetricsImpl.loadStat.addValue(cpuLoad);
            MetricCollector.cpuLoadValues += cpuLoad;
            if (MetricCollector.maxCpuLoadOverIterations < cpuLoad)
            {
                MetricCollector.maxCpuLoadOverIterations = cpuLoad;
            }

            getDivision(metricCollector, memoryPoolMXBeans);
            getHeapAndNative(metricCollector, memoryMXBean);

            long processPhysicalMemorySize = extendedOperatingSystemMXBean.getProcessPhysicalMemorySize();
            metricCollector.residentMemoryStat.addValue(processPhysicalMemorySize);
            MetricCollector.residentSumValues += processPhysicalMemorySize / (1024.0 * 1024.0);

            if (MetricCollector.maxResidentOverIterations < (processPhysicalMemorySize / (1024.0 * 1024.0)))
            {
                MetricCollector.maxResidentOverIterations = (processPhysicalMemorySize / (1024.0 * 1024.0));
            }


            metricCollector.cpuMetricsImpl.getCpuCurrentFrequency();

            try
            {
                TimeUnit.SECONDS.sleep(Constants.TIME_TO_SLEEP);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

}
