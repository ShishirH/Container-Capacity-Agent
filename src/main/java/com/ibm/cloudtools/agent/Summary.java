package com.ibm.cloudtools.agent;

import org.json.simple.JSONObject;

@SuppressWarnings("unchecked")
class Summary
{
    /* Get important memory data from the statistical analysis done */
    static void getSummaryMemory(JSONObject summaryObject, ContainerAgent containerAgent)
    {
        JSONObject heapObject = new JSONObject();
        JSONObject nativeObject = new JSONObject();
        JSONObject memoryObject = new JSONObject();

        double maxResidentMem = GenerateConfig.additionalBuffer(containerAgent.metricCollector.getResidentMemoryStat().getMax());
        double meanResidentMem = GenerateConfig.additionalBuffer(containerAgent.metricCollector.getResidentMemoryStat().getMean());

        containerAgent.metricCollector.maxHeapSize = GenerateConfig.additionalBuffer(containerAgent.metricCollector.maxHeapSize);
        containerAgent.metricCollector.maxNativeSize = GenerateConfig.additionalBuffer(containerAgent.metricCollector.maxNativeSize);
        containerAgent.metricCollector.meanHeapSize = GenerateConfig.additionalBuffer(containerAgent.metricCollector.meanHeapSize);
        containerAgent.metricCollector.meanNativeSize = GenerateConfig.additionalBuffer(containerAgent.metricCollector.meanNativeSize);

        /* converting to MB */
        maxResidentMem /= 1000000.0;
        meanResidentMem /= 1000000.0;

        heapObject.put("MaxSize", containerAgent.metricCollector.maxHeapSize);
        heapObject.put("MeanSize", containerAgent.metricCollector.meanHeapSize);
        nativeObject.put("MaxSize", containerAgent.metricCollector.maxNativeSize);
        nativeObject.put("MeanSize", containerAgent.metricCollector.meanNativeSize);

        memoryObject.put("MaxSize", containerAgent.metricCollector.maxHeapSize + containerAgent.metricCollector.maxNativeSize);
        memoryObject.put("MaxResident", maxResidentMem);
        memoryObject.put("MeanResident", meanResidentMem);
        memoryObject.put("Heap", heapObject);
        memoryObject.put("Native", nativeObject);
        summaryObject.put("Memory", memoryObject);
        containerAgent.maxMemSize = containerAgent.metricCollector.maxHeapSize + containerAgent.metricCollector.maxNativeSize;
        containerAgent.meanMemSize = containerAgent.metricCollector.meanHeapSize + containerAgent.metricCollector.meanNativeSize;
    }

    /* Get important CPU data from the statistical analysis done */
    static void getSummaryCPU(JSONObject summaryObject, ContainerAgent containerAgent)
    {
        JSONObject cpuObject = new JSONObject();
        for(int i = 0; i < Constants.NO_OF_CORES; i++)
        {
            JSONObject coreObject = new JSONObject();
            coreObject.put("MaxFreq", GenerateConfig.additionalBuffer(containerAgent.metricCollector.cpuMetricsImpl.getFreqStat()[i].getMax()));
            cpuObject.put("CPU" + i, coreObject);
        }

        cpuObject.put("Load", GenerateConfig.additionalBuffer(containerAgent.metricCollector.cpuMetricsImpl.getCpuLoad().getMax()));
        summaryObject.put("CPU", cpuObject);
    }
}
