package com.ibm.cloudtools.agent;

import com.ibm.lang.management.internal.ExtendedOperatingSystemMXBeanImpl;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
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
            Map<String, String> nativeMap = new HashMap<>();
            String type = Constants.MEM_TYPES[index];

            /* adding heap */
            addStats(true, heapMap, metricCollector.getHeapStat()[index]);
            /* adding native */
            addStats(true, nativeMap, metricCollector.getNativeStat()[index]);

            if (index == 0)
            {
                MetricCollector.heapSumValues += metricCollector.getHeapStat()[index].getMean();
                MetricCollector.nativeSumValues += metricCollector.getNativeStat()[index].getMean();
            }

            heapObject.put(type, heapMap);
            nativeObject.put(type, nativeMap);
        }

        /* adding resident memory */
        Map<String, String> residentMap = new HashMap<>();
        addStats(true, residentMap, metricCollector.getResidentMemoryStat());
        MetricCollector.chartResidentStat.addValue(Util.convertToMB(metricCollector.getResidentMemoryStat().getPercentile(50)));

        memoryAnalysisObject.put("Heap", heapObject);
        memoryAnalysisObject.put("Native", nativeObject);
        memoryAnalysisObject.put("Resident", residentMap);
    }

    static void analyzeCPU(JSONObject cpuAnalysisObject, MetricCollector metricCollector)
    {
        int hyperthreading = metricCollector.hyperThreadingInfo;
        String[] governors = metricCollector.cpuGovernors;
        ExtendedOperatingSystemMXBeanImpl extendedOperatingSystemMXBean =
                ExtendedOperatingSystemMXBeanImpl.getInstance();

        String model = metricCollector.cpuModel;

        for (int i = 0; i < Constants.NO_OF_CORES; i++)
        {
            Map<String, String> map = new HashMap<>();
            map.put("Hyperthreading", (hyperthreading == 1) ? "Enabled" : "Disabled");
            map.put("Governor", governors[i]);
            map.put("Model", model);
            addStats(false, map, metricCollector.cpuMetricsImpl.getFreqStat()[i]);
            cpuAnalysisObject.put("CPU" + i, map);
        }

        /* adding cpu load */
        Map<String, String> loadMap = new HashMap<>();
        addStats(false, loadMap, metricCollector.cpuMetricsImpl.getCpuLoad());
        cpuAnalysisObject.put("CpuLoad", loadMap);

        MetricCollector.chartCpuLoadStat.addValue(metricCollector.cpuMetricsImpl.getCpuLoad().getPercentile(50));
        CpuMetricsImpl.cpuSecondsStat.addValue(extendedOperatingSystemMXBean.getProcessCpuTime());
    }

    private static void addStats(boolean isMemory, Map<String, String> map, DescriptiveStatistics statistics)
    {
        double div = (1024.0 * 1024.0);
        if(!isMemory)
        {
            div = 1;
        }
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
