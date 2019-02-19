/*
 * ******************************************************************************
 *  * Copyright (c) 2012, 2018 IBM Corp. and others
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
import com.ibm.cloudtools.agent.ContainerAgent;
import com.ibm.cloudtools.agent.Util;
import org.json.simple.JSONObject;
import oshi.util.FormatUtil;

@SuppressWarnings("unchecked")
public class Summary
{
    /* Get important memory data from the statistical analysis done */
    public static void getSummaryMemory(JSONObject summaryObject, ContainerAgent containerAgent)
    {
        JSONObject heapObject = new JSONObject();
        JSONObject nativeObject = new JSONObject();
        JSONObject memoryObject = new JSONObject();

        double maxResidentMem =
                Util.additionalBuffer(containerAgent.metricCollector.getResidentMemoryStat().getMax());
        double meanResidentMem =
                Util.additionalBuffer(containerAgent.metricCollector.getResidentMemoryStat().getMean());

        double maxHeap = Util.additionalBuffer(containerAgent.metricCollector.heapStat[0].getMax());
        double meanHeap = Util.additionalBuffer(containerAgent.metricCollector.heapStat[0].getMean());
        double maxNative = Util.additionalBuffer(containerAgent.metricCollector.nativeStat[0].getMax());
        double meanNative =
                Util.additionalBuffer(containerAgent.metricCollector.nativeStat[0].getMean());

        heapObject.put("MaxSize", FormatUtil.formatBytes((long) maxHeap));
        heapObject.put("MeanSize", FormatUtil.formatBytes((long) meanHeap));
        nativeObject.put("MaxSize", FormatUtil.formatBytes((long) maxNative));
        nativeObject.put("MeanSize", FormatUtil.formatBytes((long) meanNative));

        memoryObject.put("MaxSize", FormatUtil.formatBytes((long) (maxHeap + maxNative)));
        memoryObject.put("MaxResident", FormatUtil.formatBytes((long) maxResidentMem));
        memoryObject.put("MeanResident", FormatUtil.formatBytes((long) meanResidentMem));
        memoryObject.put("Heap", heapObject);
        memoryObject.put("Native", nativeObject);
        summaryObject.put("Memory", memoryObject);
    }

    /* Get important CPU data from the statistical analysis done */
    public static void getSummaryCPU(JSONObject summaryObject, ContainerAgent containerAgent)
    {
        JSONObject cpuObject = new JSONObject();
        for (int i = 0; i < Constants.NO_OF_CORES; i++)
        {
            JSONObject coreObject = new JSONObject();
            coreObject.put(
                    "MaxFreq",
                    Util.additionalBuffer(
                            containerAgent.metricCollector.linuxCpuMetricsImpl.getFreqStat()[i].getMax()));
            cpuObject.put("CPU" + i, coreObject);
        }

        cpuObject.put(
                "Load",
                Util.additionalBuffer(containerAgent.metricCollector.linuxCpuMetricsImpl.getCpuLoad().getMax()));
        summaryObject.put("CPU", cpuObject);
    }
}
