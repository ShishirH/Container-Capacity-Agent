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

package com.ibm.cloudtools.metrics.memory;

import com.ibm.cloudtools.agent.Constants;
import com.ibm.cloudtools.agent.ContainerAgent;
import com.ibm.cloudtools.agent.MetricCollector;
import com.ibm.cloudtools.system.SystemDump;
import com.ibm.java.lang.management.internal.MemoryMXBeanImpl;
import com.ibm.lang.management.internal.ExtendedOperatingSystemMXBeanImpl;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.lang.management.MemoryPoolMXBean;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MemoryMetricsImpl {
    public static int nurseryAllocatedIndex = -1;
    public static int nurserySurvivorIndex = -1;
    public static int tenuredSOAIndex = -1;
    public static int tenuredLOAIndex = -1;

    public static double nurseryAllocatedMax = -1;
    public static double nurserySurvivorMax = -1;
    public static double tenuredSOAMax = -1;
    public static double tenuredLOAMax = -1;

    private void getHeapAndNative(MetricCollector metricCollector, MemoryMXBeanImpl memoryMXBean)
    {
        long heapCommitted = memoryMXBean.getHeapMemoryUsage().getCommitted();
        long nativeCommitted = memoryMXBean.getNonHeapMemoryUsage().getCommitted();

        metricCollector.heapStat[0].addValue(heapCommitted / Constants.ONE_MB);
        metricCollector.heapStat[1].addValue(
                memoryMXBean.getHeapMemoryUsage().getUsed() / Constants.ONE_MB);
        metricCollector.heapStat[2].addValue(
                memoryMXBean.getHeapMemoryUsage().getMax() / Constants.ONE_MB);
        metricCollector.heapStat[3].addValue(
                memoryMXBean.getHeapMemoryUsage().getInit() / Constants.ONE_MB);

        metricCollector.nativeStat[0].addValue(nativeCommitted / Constants.ONE_MB);
        metricCollector.nativeStat[1].addValue(
                memoryMXBean.getNonHeapMemoryUsage().getUsed() / Constants.ONE_MB);
        metricCollector.nativeStat[2].addValue(
                memoryMXBean.getNonHeapMemoryUsage().getMax() / Constants.ONE_MB);
        metricCollector.nativeStat[3].addValue(
                memoryMXBean.getNonHeapMemoryUsage().getInit() / Constants.ONE_MB);
    }

    private void getDivision(
            MetricCollector metricCollector, List<MemoryPoolMXBean> memoryPoolMXBeans)
    {
        int category = 0;
        for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
            // 0: Committed 1: Used 2: Max 3: Init
            long committed = memoryPoolMXBean.getUsage().getCommitted();
            metricCollector.memDivisions[0][category].addValue(committed);
            metricCollector.memDivisions[1][category].addValue(memoryPoolMXBean.getUsage().getUsed());
            metricCollector.memDivisions[2][category].addValue(memoryPoolMXBean.getUsage().getMax());
            metricCollector.memDivisions[3][category].addValue(memoryPoolMXBean.getUsage().getInit());

            if (category == nurseryAllocatedIndex) {
                if (nurseryAllocatedMax < committed) nurseryAllocatedMax = committed;
            }

            if (category == nurserySurvivorIndex) {
                if (nurserySurvivorMax < committed) nurserySurvivorMax = committed;
            }

            if (category == tenuredLOAIndex) {
                if (tenuredLOAMax < committed) tenuredLOAMax = committed;
            }

            if (category == tenuredSOAIndex) {
                if (tenuredSOAMax < committed) tenuredSOAMax = committed;
            }

            category++;
        }
    }

    public void getMetrics(MetricCollector metricCollector)
    {
        MemoryMXBeanImpl memoryMXBean;
        List<MemoryPoolMXBean> memoryPoolMXBeans;

        ExtendedOperatingSystemMXBeanImpl extendedOperatingSystemMXBean =
                ExtendedOperatingSystemMXBeanImpl.getInstance();
        extendedOperatingSystemMXBean.getProcessCpuLoad();

        memoryMXBean = MemoryMXBeanImpl.getInstance();
        memoryPoolMXBeans = memoryMXBean.getMemoryPoolMXBeans(false);

        for (int division = 0; division < memoryPoolMXBeans.size(); division++) {
            metricCollector.memDivisions[0][division] = new DescriptiveStatistics();
            metricCollector.memDivisions[1][division] = new DescriptiveStatistics();
            metricCollector.memDivisions[2][division] = new DescriptiveStatistics();
            metricCollector.memDivisions[3][division] = new DescriptiveStatistics();
        }

        /* Initial sleep to get accurate CPU Load values */
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            System.err.println("COULD NOT SLEEP! INTERRUPTED EXCEPTION");
        }

        for (int i = 0; i < Constants.MAX_NUMBER_OF_VALUES; i++) {
            memoryMXBean = MemoryMXBeanImpl.getInstance();
            memoryPoolMXBeans = memoryMXBean.getMemoryPoolMXBeans(false);

            double cpuLoad = extendedOperatingSystemMXBean.getProcessCpuLoad() * Constants.NO_OF_CORES;
            metricCollector.cpuLoadValues.addValue(cpuLoad);

            getDivision(metricCollector, memoryPoolMXBeans);
            getHeapAndNative(metricCollector, memoryMXBean);

            long processPhysicalMemorySize = SystemDump.getResidentSize();
            metricCollector.residentMemoryStat.addValue(processPhysicalMemorySize / Constants.ONE_MB);

            metricCollector.cpuMetricsImpl.getCpuCurrentFrequency();

            try {
                TimeUnit.SECONDS.sleep(Constants.TIME_TO_SLEEP);
            } catch (InterruptedException interruptedException) {
                ContainerAgent.isProgramRunning.set(false);
                return;
            }
        }
    }
}
