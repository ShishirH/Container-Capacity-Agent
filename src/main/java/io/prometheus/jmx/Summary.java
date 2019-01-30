package io.prometheus.jmx;

import org.json.simple.JSONObject;

@SuppressWarnings("unchecked")
public class Summary
{
    //Get important memory data from the statistical analysis done
    static void getSummaryMemory(JSONObject summaryObject, ContainerAgent containerAgent)
    {
        JSONObject heapObject = new JSONObject();
        JSONObject nativeObject = new JSONObject();
        JSONObject memoryObject = new JSONObject();

        double maxHeapMem = 0;
        double maxNativeMem = 0;
        double meanHeapMem = 0;
        double meanNativeMem = 0;
        double maxResidentMem = containerAgent.metricCollector.getResidentMemoryStat().getMax() * (100.0 + containerAgent.buffer)/100.0;
        double meanResidentMem = containerAgent.metricCollector.getResidentMemoryStat().getMean() * (100.0 + containerAgent.buffer)/100.0;

        for(int i = 0; i < Constants.MEM_TYPE_LENGTH; i++)
        {
            //not in max type(gives wrong result of -1 for native, and gives value of GB order for heap)
            if (i != 2)
            {
                double heapMem = containerAgent.metricCollector.getHeapStat()[i].getMax() * (100.0 + containerAgent.buffer)/100.0;
                double nativeMem = containerAgent.metricCollector.getNativeStat()[i].getMax() * (100.0 + containerAgent.buffer) / 100.0;
                double heapMean = containerAgent.metricCollector.getHeapStat()[i].getMean() * (100.0 + containerAgent.buffer)/100.0;
                double nativeMean = containerAgent.metricCollector.getNativeStat()[i].getMean() * (100.0 + containerAgent.buffer)/100.0;

                if(heapMem > maxHeapMem)
                {
                    maxHeapMem = heapMem;
                }

                if(nativeMem > maxNativeMem)
                {
                    maxNativeMem = nativeMem;
                }

                if(heapMean > meanHeapMem)
                {
                    meanHeapMem = heapMean;
                }

                if(nativeMean > meanNativeMem)
                {
                    meanNativeMem = nativeMem;
                }
            }
        }

        //converting to MB
        maxHeapMem /= 1000000;
        maxNativeMem /= 1000000;

        meanNativeMem /= 1000000;
        meanHeapMem /= 1000000;

        maxResidentMem /= 1000000;
        meanResidentMem /= 1000000;

        heapObject.put("MaxSize", maxHeapMem);
        nativeObject.put("MaxSize", maxNativeMem);

        memoryObject.put("MaxSize", maxHeapMem + maxNativeMem);
        memoryObject.put("MaxResident", maxResidentMem);
        memoryObject.put("MeanResident", meanResidentMem);
        memoryObject.put("Heap", heapObject);
        memoryObject.put("Native", nativeObject);
        summaryObject.put("Memory", memoryObject);
        containerAgent.maxMemSize = maxHeapMem + maxNativeMem;
        containerAgent.meanMemSize = meanHeapMem + meanNativeMem;
    }

    //Get important CPU data from the statistical analysis done
    static void getSummaryCPU(JSONObject summaryObject, ContainerAgent containerAgent)
    {
        JSONObject cpuObject = new JSONObject();
        for(int i = 0; i < Constants.NO_OF_CORES; i++)
        {
            JSONObject coreObject = new JSONObject();
            coreObject.put("MaxFreq", (containerAgent.metricCollector.cpuMetricsImpl.getFreqStat()[i].getMax()) * (100.0 + containerAgent.buffer)/100.0);
            cpuObject.put("CPU" + i, coreObject);
        }

        cpuObject.put("Load", (containerAgent.metricCollector.cpuMetricsImpl.getCpuLoad().getMax()) * (100.0 + containerAgent.buffer)/100.0);
        summaryObject.put("CPU", cpuObject);
    }

}
