package io.prometheus.jmx;

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
            heapMap.put("Max", Double.toString(metricCollector.getHeapStat()[index].getMax()));
            heapMap.put("Min", Double.toString(metricCollector.getHeapStat()[index].getMin()));
            heapMap.put("Median", Double.toString(metricCollector.getHeapStat()[index].getPercentile(50)));
            heapMap.put("Mean", Double.toString(metricCollector.getHeapStat()[index].getMean()));
            heapMap.put("StandardDeviation", Double.toString(metricCollector.getHeapStat()[index].getStandardDeviation()));
            heapMap.put("Variance", Double.toString(metricCollector.getHeapStat()[index].getVariance()));
            heapObject.put(type, heapMap);
        }

        for (index = 0; index < Constants.MEM_TYPE_LENGTH; index++)
        {
            Map<String, String> nativeMap = new HashMap<>();
            String type = Constants.MEM_TYPES[index];
            nativeMap.put("Max", Double.toString(metricCollector.getNativeStat()[index].getMax()));
            nativeMap.put("Min", Double.toString(metricCollector.getNativeStat()[index].getMin()));
            nativeMap.put("Median", Double.toString(metricCollector.getNativeStat()[index].getPercentile(50)));
            nativeMap.put("Mean", Double.toString(metricCollector.getNativeStat()[index].getMean()));
            nativeMap.put("StandardDeviation", Double.toString(metricCollector.getNativeStat()[index].getStandardDeviation()));
            nativeMap.put("Variance", Double.toString(metricCollector.getNativeStat()[index].getVariance()));
            nativeObject.put(type, nativeMap);
        }

        Map<String, String> residentMap = new HashMap<>();
        residentMap.put("Max", Double.toString(metricCollector.getResidentMemoryStat().getMax()));
        residentMap.put("Min", Double.toString(metricCollector.getResidentMemoryStat().getMin()));
        residentMap.put("Median", Double.toString(metricCollector.getResidentMemoryStat().getPercentile(50)));
        residentMap.put("Mean", Double.toString(metricCollector.getResidentMemoryStat().getMean()));
        residentMap.put("StandardDeviation", Double.toString(metricCollector.getResidentMemoryStat().getStandardDeviation()));
        residentMap.put("Variance", Double.toString(metricCollector.getResidentMemoryStat().getVariance()));

        memoryAnalysisObject.put("Heap", heapObject);
        memoryAnalysisObject.put("Native", nativeObject);
        memoryAnalysisObject.put("Resident", residentMap);
    }


    static void analyzeCPU(JSONObject cpuAnalysisObject, MetricCollector metricCollector)
    {
        int [] hyperthreading = metricCollector.cpuMetricsImpl.getHyperthreadingInfo();
        String [] governors = metricCollector.cpuMetricsImpl.getCpuGovernors();
        String [] models = metricCollector.cpuMetricsImpl.getCpuModels();

        for(int i = 0; i < Constants.NO_OF_CORES; i++)
        {
            JSONObject coreObject  = new JSONObject();
            coreObject.put("Max", metricCollector.cpuMetricsImpl.getFreqStat()[i].getMax());
            coreObject.put("Median", metricCollector.cpuMetricsImpl.getFreqStat()[i].getPercentile(50));
            coreObject.put("Min", metricCollector.cpuMetricsImpl.getFreqStat()[i].getMin());
            coreObject.put("Mean", metricCollector.cpuMetricsImpl.getFreqStat()[i].getMean());
            coreObject.put("StandardDeviation", metricCollector.cpuMetricsImpl.getFreqStat()[i].getStandardDeviation());
            coreObject.put("Variance", metricCollector.cpuMetricsImpl.getFreqStat()[i].getVariance());
            coreObject.put("Hyperthreading", hyperthreading[i]);
            coreObject.put("Governor", governors[i]);
            coreObject.put("Model", models[i].substring(13));
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
