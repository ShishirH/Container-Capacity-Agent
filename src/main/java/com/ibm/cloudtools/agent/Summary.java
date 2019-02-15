package com.ibm.cloudtools.agent;

import org.json.simple.JSONObject;
import oshi.util.FormatUtil;

@SuppressWarnings("unchecked")
class Summary
{
    /* Get important memory data from the statistical analysis done */
    static void getSummaryMemory(JSONObject summaryObject, ContainerAgent containerAgent)
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
        double meanNative = Util.additionalBuffer(containerAgent.metricCollector.nativeStat[0].getMean());

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
    static void getSummaryCPU(JSONObject summaryObject, ContainerAgent containerAgent)
    {
        JSONObject cpuObject = new JSONObject();
        for (int i = 0; i < Constants.NO_OF_CORES; i++)
        {
            JSONObject coreObject = new JSONObject();
            coreObject.put("MaxFreq",
                    Util.additionalBuffer(containerAgent.metricCollector.cpuMetricsImpl.getFreqStat()[i].getMax()));
            cpuObject.put("CPU" + i, coreObject);
        }

        cpuObject.put("Load",
                Util.additionalBuffer(containerAgent.metricCollector.cpuMetricsImpl.getCpuLoad().getMax()));
        summaryObject.put("CPU", cpuObject);
    }
}
