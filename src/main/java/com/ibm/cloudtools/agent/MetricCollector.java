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

/*
   Collects information from Prometheus

   Makes use of DescriptiveStatistics from Apache Commons-Math library that allows calculation of Mean, Median,
   Variance, Min, Max
   and Standard Deviation

*/
package com.ibm.cloudtools.agent;

import com.ibm.cloudtools.metrics.cpu.AbstractCpuMetricsImpl;
import com.ibm.cloudtools.metrics.memory.MemoryMetricsImpl;
import com.ibm.java.lang.management.internal.MemoryMXBeanImpl;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.lang.management.MemoryPoolMXBean;
import java.util.List;

/*
 * Class where all metrics are stored, so that they can be accessed later.
 */
public class MetricCollector
{

    public static int governorPowersaveFlag = 0;

    public AbstractCpuMetricsImpl cpuMetricsImpl = AbstractCpuMetricsImpl.getCpuMetrics();
    MemoryMetricsImpl memoryMetrics;

    private int memTypesLength = Constants.MEM_TYPES.values().length;
    public DescriptiveStatistics residentMemoryStat = new DescriptiveStatistics();
    public DescriptiveStatistics[] heapStat = new DescriptiveStatistics[memTypesLength];
    public DescriptiveStatistics[] nativeStat = new DescriptiveStatistics[memTypesLength];
    public DescriptiveStatistics cpuLoadValues = new DescriptiveStatistics();

    private MemoryMXBeanImpl memoryMXBean = MemoryMXBeanImpl.getInstance();
    private List<MemoryPoolMXBean> memoryPoolMXBeans = memoryMXBean.getMemoryPoolMXBeans(false);
    private String[] memDivisionNames = new String[memoryPoolMXBeans.size()];
    public DescriptiveStatistics[][] memDivisions =
            new DescriptiveStatistics[memTypesLength][memDivisionNames.length];

    MetricCollector()
    {
        for (int i = 0; i < Constants.MEM_TYPES.values().length; i++) {
            heapStat[i] = new DescriptiveStatistics();
            nativeStat[i] = new DescriptiveStatistics();
        }

        for (int i = 0; i < memDivisionNames.length; i++) {
            memDivisionNames[i] = memoryPoolMXBeans.get(i).getName();
            if (memDivisionNames[i].equals("nursery-allocate")) MemoryMetricsImpl.nurseryAllocatedIndex = i;

            if (memDivisionNames[i].equals("nursery-survivor")) MemoryMetricsImpl.nurserySurvivorIndex = i;

            if (memDivisionNames[i].equals("tenured-SOA")) MemoryMetricsImpl.tenuredSOAIndex = i;

            if (memDivisionNames[i].equals("tenured-LOA")) MemoryMetricsImpl.tenuredLOAIndex = i;
        }

        memoryMetrics = new MemoryMetricsImpl();
    }
}
