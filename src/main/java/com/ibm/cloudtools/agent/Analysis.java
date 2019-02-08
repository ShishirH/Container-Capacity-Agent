package com.ibm.cloudtools.agent;

import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
class Analysis
{

    static void analyzeMemory(JSONObject memoryAnalysisObject, MetricCollector metricCollector)
    {
        JSONObject heapObject = new JSONObject();
        JSONObject nativeObject = new JSONObject();
        int index;
        for (index = 0; index < Constants.MEM_TYPE_LENGTH; index++)
        {
            Map<String, String> heapMap = new HashMap<>();
            String type = Constants.MEM_TYPES[index];

            double meanHeapSize = metricCollector.getHeapStat()[index].getMean() / 1000000.0;
            double maxHeapSize = metricCollector.getHeapStat()[index].getMax() / 1000000.0;

            heapMap.put("Max", Double.toString(maxHeapSize));
            heapMap.put("Min", Double.toString(metricCollector.getHeapStat()[index].getMin() / 1000000.0));
            heapMap.put("Median", Double.toString(metricCollector.getHeapStat()[index].getPercentile(50) / 1000000.0));
            heapMap.put("Mean", Double.toString(meanHeapSize));
            heapMap.put("StandardDeviation", Double.toString(metricCollector.getHeapStat()[index].getStandardDeviation() / 1000000.0));
            heapMap.put("Variance", Double.toString(metricCollector.getHeapStat()[index].getVariance()));

            /* not in Max */
            if(index != 2)
            {
                if(metricCollector.maxHeapSize < maxHeapSize)
                {
                    metricCollector.maxHeapSize = maxHeapSize;
                }

                if(metricCollector.meanHeapSize < meanHeapSize)
                {
                    metricCollector.meanHeapSize = meanHeapSize;
                }
            }
            heapObject.put(type, heapMap);
        }

        MetricCollector.heapSumValues += metricCollector.meanHeapSize;

        if(metricCollector.maxHeapOverIterations < metricCollector.maxHeapSize)
        {
            metricCollector.maxHeapOverIterations = metricCollector.maxHeapSize;
        }

        for (index = 0; index < Constants.MEM_TYPE_LENGTH; index++)
        {
            Map<String, String> nativeMap = new HashMap<>();
            String type = Constants.MEM_TYPES[index];
            double meanNativeMem = metricCollector.getNativeStat()[index].getMean() / 1000000.0;
            double maxNativeMem = metricCollector.getNativeStat()[index].getMax() / 1000000.0;

            nativeMap.put("Max", Double.toString(maxNativeMem));
            nativeMap.put("Min", Double.toString(metricCollector.getNativeStat()[index].getMin() / 1000000.0));
            nativeMap.put("Median", Double.toString(metricCollector.getNativeStat()[index].getPercentile(50) / 1000000.0));
            nativeMap.put("Mean", Double.toString(meanNativeMem));
            nativeMap.put("StandardDeviation", Double.toString(metricCollector.getNativeStat()[index].getStandardDeviation() / 1000000.0));
            nativeMap.put("Variance", Double.toString(metricCollector.getNativeStat()[index].getVariance()));

            if(index != 2)
            {
                if(metricCollector.maxNativeSize < maxNativeMem)
                {
                    metricCollector.maxNativeSize = maxNativeMem;
                }

                if(metricCollector.meanNativeSize < meanNativeMem)
                {
                    metricCollector.meanNativeSize = meanNativeMem;
                }
            }

            nativeObject.put(type, nativeMap);
        }

        MetricCollector.nativeSumValues += metricCollector.meanNativeSize;

        if(metricCollector.maxNativeOverIterations < metricCollector.maxNativeSize)
        {
            metricCollector.maxNativeOverIterations = metricCollector.maxNativeSize;
        }


        Map<String, String> residentMap = new HashMap<>();
        residentMap.put("Max", Double.toString(metricCollector.getResidentMemoryStat().getMax() / 1000000.0));
        residentMap.put("Min", Double.toString(metricCollector.getResidentMemoryStat().getMin() / 1000000.0));
        residentMap.put("Median", Double.toString(metricCollector.getResidentMemoryStat().getPercentile(50) / 1000000.0));
        residentMap.put("Mean", Double.toString(metricCollector.getResidentMemoryStat().getMean() / 1000000.0));
        residentMap.put("StandardDeviation", Double.toString(metricCollector.getResidentMemoryStat().getStandardDeviation()/ 1000000.0 ));
        residentMap.put("Variance", Double.toString(metricCollector.getResidentMemoryStat().getVariance()));


        if(metricCollector.maxResidentOverIterations < (metricCollector.getResidentMemoryStat().getMax() / 1000000.0))
        {
            metricCollector.maxResidentOverIterations = (metricCollector.getResidentMemoryStat().getMax() / 1000000.0);
        }

        memoryAnalysisObject.put("Heap", heapObject);
        memoryAnalysisObject.put("Native", nativeObject);
        memoryAnalysisObject.put("Resident", residentMap);
    }


    static void analyzeCPU(JSONObject cpuAnalysisObject, MetricCollector metricCollector)
    {
        int hyperthreading = metricCollector.hyperThreadingInfo;
        String [] governors = metricCollector.cpuGovernors;

        String model = metricCollector.cpuModel;

        for(int i = 0; i < Constants.NO_OF_CORES; i++)
        {
            JSONObject coreObject  = new JSONObject();
            coreObject.put("Max", metricCollector.cpuMetricsImpl.getFreqStat()[i].getMax());
            coreObject.put("Median", metricCollector.cpuMetricsImpl.getFreqStat()[i].getPercentile(50));
            coreObject.put("Min", metricCollector.cpuMetricsImpl.getFreqStat()[i].getMin());
            coreObject.put("Mean", metricCollector.cpuMetricsImpl.getFreqStat()[i].getMean());
            coreObject.put("StandardDeviation", metricCollector.cpuMetricsImpl.getFreqStat()[i].getStandardDeviation());
            coreObject.put("Variance", metricCollector.cpuMetricsImpl.getFreqStat()[i].getVariance());
            coreObject.put("Hyperthreading", hyperthreading);
            coreObject.put("Governor", governors[i]);
            coreObject.put("Model", model);
            cpuAnalysisObject.put("CPU" + i, coreObject);
        }

        JSONObject load = new JSONObject();
        load.put("Max", metricCollector.cpuMetricsImpl.getCpuLoad().getMax());
        load.put("Min", metricCollector.cpuMetricsImpl.getCpuLoad().getMin());
        load.put("Mean", metricCollector.cpuMetricsImpl.getCpuLoad().getMean());
        load.put("Median", metricCollector.cpuMetricsImpl.getCpuLoad().getPercentile(50));
        load.put("StandardDeviation", metricCollector.cpuMetricsImpl.getCpuLoad().getStandardDeviation());
        load.put("Variance", metricCollector.cpuMetricsImpl.getCpuLoad().getVariance());
        cpuAnalysisObject.put("CpuLoad", load);
    }
}
