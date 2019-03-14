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

package com.ibm.cloudtools.exportMetrics;

import com.ibm.cloudtools.agent.Constants;
import com.ibm.cloudtools.agent.MetricCollector;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class Analysis
{
    public static void analyzeMemory(JSONObject memoryAnalysisObject, MetricCollector metricCollector)
    {
        JSONObject heapObject = new JSONObject();
        JSONObject nativeObject = new JSONObject();
        int index;

        for (index = 0; index < Constants.MEM_TYPE_LENGTH; index++)
        {
            Map<String, String> heapMap = new HashMap<>();
            Map<String, String> nativeMap = new HashMap<>();
            String type = Constants.MEM_TYPES[index];

            /* adding heap */
            addStats(true, heapMap, metricCollector.getHeapStat()[index]);
            /* adding native */
            addStats(true, nativeMap, metricCollector.getNativeStat()[index]);

            heapObject.put(type, heapMap);
            nativeObject.put(type, nativeMap);
        }

        /* adding resident memory */
        Map<String, String> residentMap = new HashMap<>();
        addStats(true, residentMap, metricCollector.getResidentMemoryStat());

        memoryAnalysisObject.put("Heap", heapObject);
        memoryAnalysisObject.put("Native", nativeObject);
        memoryAnalysisObject.put("Resident", residentMap);
    }

    public static void analyzeCPU(JSONObject cpuAnalysisObject, MetricCollector metricCollector)
    {
        for (int i = 0; i < Constants.NO_OF_CORES; i++)
        {
            Map<String, String> map = new HashMap<>();
            map.put("Hyperthreading", (metricCollector.hyperThreadingInfo == 1) ? "Enabled" : "Disabled");
            map.put("Governor", metricCollector.cpuGovernors[i]);
            map.put("Model", metricCollector.cpuModel);
            addStats(false, map, metricCollector.cpuMetricsImpl.getFreqStat()[i]);
            cpuAnalysisObject.put("CPU" + i, map);
        }

        /* adding cpu load */
        Map<String, String> loadMap = new HashMap<>();
        addStats(false, loadMap, metricCollector.cpuMetricsImpl.getCpuLoad());
        cpuAnalysisObject.put("CpuLoad", loadMap);

        MetricCollector.chartCpuLoadStat.addValue(metricCollector.cpuMetricsImpl.getCpuLoad().getPercentile(50));
    }

    private static void addStats(boolean isMemory, Map<String, String> map, DescriptiveStatistics statistics)
    {
        double div = (isMemory) ? Constants.ONE_MB : 1;

        double mean = (statistics.getMean());
        double max = (statistics.getMax());

        map.put("Max", Double.toString(max / div));
        map.put("Min", Double.toString((statistics.getMin() / div)));
        map.put("Median",
                Double.toString((statistics.getPercentile(50) / div)));
        map.put("Mean", Double.toString(mean / div));
        map.put("StandardDeviation",
                Double.toString((statistics.getStandardDeviation() / div)));
        map.put("Variance", Double.toString((statistics.getVariance() / div)));
    }
}
