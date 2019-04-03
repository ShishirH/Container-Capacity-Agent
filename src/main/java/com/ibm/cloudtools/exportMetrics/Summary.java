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

package com.ibm.cloudtools.exportMetrics;

import com.ibm.cloudtools.agent.Constants;
import com.ibm.cloudtools.agent.ContainerAgent;
import com.ibm.cloudtools.agent.Util;
import org.json.simple.JSONObject;
import oshi.util.FormatUtil;

import java.util.Arrays;

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
                Util.additionalBuffer(containerAgent.metricCollector.residentMemoryStat.getMax());
        double meanResidentMem =
                Util.additionalBuffer(containerAgent.metricCollector.residentMemoryStat.getMean());

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
        for (int i = 0; i < Constants.NO_OF_CORES; i++) {
            JSONObject coreObject = new JSONObject();
            coreObject.put(
                    "MaxFreq",
                    Util.additionalBuffer(
                            containerAgent.metricCollector.cpuMetricsImpl.getFreqStat()[i].getMax()));
            cpuObject.put("CPU" + i, coreObject);
        }

        cpuObject.put(
                "Load",
                Util.additionalBuffer(containerAgent.metricCollector.cpuMetricsImpl.getCpuLoad().getMax()));

        cpuObject.put(
                "Hyperthreading", containerAgent.metricCollector.cpuMetricsImpl.getHyperthreadingInfo());

        cpuObject.put(
                "Governors",
                Arrays.toString(containerAgent.metricCollector.cpuMetricsImpl.getCpuGovernors()));

        cpuObject.put("Model", containerAgent.metricCollector.cpuMetricsImpl.getCpuModel());

        summaryObject.put("CPU", cpuObject);
    }
}
