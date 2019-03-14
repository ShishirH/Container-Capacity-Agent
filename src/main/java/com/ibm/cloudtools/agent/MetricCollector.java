/*
 * ******************************************************************************
 *  * Copyright (c) 2012, 2019 IBM Corp. and others
 *  *
 *  * This program and the accompanying materials are made available under
 *  * the terms of the Eclipse Public License 2.0 which accompanies this
 *  * distribution and is available at https://www.eclipse.org/legal/epl-2.0/
 *  * or the Apache License, Version 2.0 which accompanies this distribution and
 *  * is available at https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * This Source Code may also be made available under the following
 *  * Secondary Licenses when the conditions for such availability set
 *  * forth in the Eclipse Public License, v. 2.0 are satisfied: GNU
 *  * General Public License, version 2 with the GNU Classpath
 *  * Exception [1] and GNU General Public License, version 2 with the
 *  * OpenJDK Assembly Exception [2].
 *  *
 *  * [1] https://www.gnu.org/software/classpath/license.html
 *  * [2] http://openjdk.java.net/legal/assembly-exception.html
 *  *
 *  * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0 OR GPL-2.0 WITH Classpath-exception-2.0 OR LicenseRef-GPL-2.0 WITH Assembly-exception
 *  ******************************************************************************
 */

/*
   Collects information from Prometheus

   Makes use of DescriptiveStatistics from Apache Commons-Math library that allows calculation of Mean, Median,
   Variance, Min, Max
   and Standard Deviation

*/
package com.ibm.cloudtools.agent;

import com.ibm.cloudtools.cpu.AbstractCpuMetricsImpl;
import com.ibm.cloudtools.memory.MemoryMetricsImpl;
import com.ibm.java.lang.management.internal.MemoryMXBeanImpl;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.lang.management.MemoryPoolMXBean;
import java.util.List;

public class MetricCollector
{
    /* Used for the overall mean */
    public static double heapSumValues = 0;
    public static double nativeSumValues = 0;
    public static double residentSumValues = 0;
    public static double cpuLoadValues = 0;

    /* Maximum over all iterations */
    public static double maxCpuLoadOverIterations = 0;
    public static double maxHeapOverIterations = 0;
    public static double maxNativeOverIterations = 0;
    public static double maxResidentOverIterations = 0;

    public static DescriptiveStatistics chartCpuLoadStat = new DescriptiveStatistics();
    public static DescriptiveStatistics chartResidentStat = new DescriptiveStatistics();
    public static int nurseryAllocatedIndex = -1;
    public static int nurserySurvivorIndex = -1;
    public static int tenuredSOAIndex = -1;
    public static int tenuredLOAIndex = -1;
    public static double nurseryAllocatedMax = -1;
    public static double nurserySurvivorMax = -1;
    public static double tenuredSOAMax = -1;
    public static double tenuredLOAMax = -1;

    public AbstractCpuMetricsImpl cpuMetricsImpl = AbstractCpuMetricsImpl.getCpuMetrics();
    MemoryMetricsImpl memoryMetrics;

    public final int hyperThreadingInfo = cpuMetricsImpl.getHyperthreadingInfo();
    public String[] cpuGovernors = cpuMetricsImpl.getCpuGovernors();
    public String cpuModel = cpuMetricsImpl.getCpuModels();
    public DescriptiveStatistics residentMemoryStat = new DescriptiveStatistics();

    private MemoryMXBeanImpl memoryMXBean = MemoryMXBeanImpl.getInstance();
    private List<MemoryPoolMXBean> memoryPoolMXBeans = memoryMXBean.getMemoryPoolMXBeans(false);
    public String[] memDivisionNames = new String[memoryPoolMXBeans.size()];
    private int memTypesLength = Constants.MEM_TYPE_LENGTH;
    public DescriptiveStatistics[][] memDivisions =
            new DescriptiveStatistics[memTypesLength][memDivisionNames.length];
    public DescriptiveStatistics[] heapStat = new DescriptiveStatistics[memTypesLength];
    public DescriptiveStatistics[] nativeStat = new DescriptiveStatistics[memTypesLength];

    MetricCollector()
    {
        for (int i = 0; i < Constants.MEM_TYPE_LENGTH; i++)
        {
            heapStat[i] = new DescriptiveStatistics();
            nativeStat[i] = new DescriptiveStatistics();
        }

        for (int i = 0; i < memDivisionNames.length; i++)
        {
            memDivisionNames[i] = memoryPoolMXBeans.get(i).getName();
            if (memDivisionNames[i].equals("nursery-allocate")) nurseryAllocatedIndex = i;

            if (memDivisionNames[i].equals("nursery-survivor")) nurserySurvivorIndex = i;

            if (memDivisionNames[i].equals("tenured-SOA")) tenuredSOAIndex = i;

            if (memDivisionNames[i].equals("tenured-LOA")) tenuredLOAIndex = i;
        }

        memoryMetrics = new MemoryMetricsImpl();
    }

    public DescriptiveStatistics[][] getMemDivisions()
    {
        return memDivisions;
    }

    public DescriptiveStatistics[] getHeapStat()
    {
        return heapStat;
    }

    public DescriptiveStatistics[] getNativeStat()
    {
        return nativeStat;
    }

    public DescriptiveStatistics getResidentMemoryStat()
    {
        return residentMemoryStat;
    }
}
