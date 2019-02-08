package com.ibm.cloudtools.agent;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
class RawData
{

    private static void addCpuGovernors(JSONObject cpuObject, ContainerAgent containerAgent)
    {
        String [] governors = containerAgent.metricCollector.cpuGovernors;
        int index = 0;
        Map<String, String> cpuMap = new HashMap<>();
        for(String governor : governors)
        {
            cpuMap.put("CPU" + index, governor);
            index++;
            if(governor.equals("powersave") && containerAgent.governorPowersaveFlag == 0)
            {
                containerAgent.governorPowersaveFlag = 1;
            }
        }
        cpuObject.put("Governors", cpuMap);
    }

    //adds load of the CPU during the run
    private static void addCpuLoad(JSONObject cpuObject, ContainerAgent containerAgent)
    {
        cpuObject.put("Load", Arrays.toString(containerAgent.metricCollector.cpuMetricsImpl.getCpuLoad().getValues()));
    }

    private static void addCpuCurrentFrequency(JSONObject cpuObject, ContainerAgent containerAgent)
    {
        int index = 0;
        DescriptiveStatistics[] cpuFrequencies = containerAgent.metricCollector.cpuMetricsImpl.getFreqStat();
        Map<String, String> cpuMap = new HashMap<>();
        for(DescriptiveStatistics frequency : cpuFrequencies)
        {
            cpuMap.put("CPU" + index, Arrays.toString(frequency.getValues()));
            index++;
        }

        cpuObject.put("CurrentFrequencies", cpuMap);
    }

    //adds Hyperthreading information by reading /proc/cpuinfo
    private static void addCpuHyperthreading(JSONObject cpuObject, ContainerAgent containerAgent)
    {
        int hyperthreading = containerAgent.metricCollector.hyperThreadingInfo;
        int index;
        Map<String, String> cpuMap = new HashMap<>();
        for(index = 0; index < Constants.NO_OF_CORES; index++)
        {
            cpuMap.put("CPU" + index, Integer.toString(hyperthreading));
        }

        cpuObject.put("Hyperthreading", cpuMap);
    }

    //apply statistical methods to memory data
    static void addMemoryToMonitor(JSONObject monitorObject, ContainerAgent containerAgent, MetricCollector metricCollector)
    {
        int outIndex = 0;
        DescriptiveStatistics[][] heapTypes = containerAgent.metricCollector.getMemDivisions();
        DescriptiveStatistics [] heapStat = containerAgent.metricCollector.getHeapStat();
        DescriptiveStatistics [] nativeStat = containerAgent.metricCollector.getNativeStat();

        Map<String, String> heapMap = new HashMap<>();
        Map<String, String> nativeMap = new HashMap<>();

        for(DescriptiveStatistics heapMem : heapStat)
        {
            heapMap.put(Constants.MEM_TYPES[outIndex], Arrays.toString(heapMem.getValues()));
            outIndex++;
        }


        outIndex = 0;
        for(DescriptiveStatistics nativeMem : nativeStat) {
            nativeMap.put(Constants.MEM_TYPES[outIndex], Arrays.toString(nativeMem.getValues()));
            outIndex++;
        }

        JSONObject heaps = new JSONObject();
        outIndex = 0;
        for(DescriptiveStatistics[] typeOfMemory : heapTypes)
        {
            String memory = Constants.MEM_TYPES[outIndex];
            JSONArray usageArray = new JSONArray();
            outIndex++;
            int index = 0;
            for(DescriptiveStatistics area : typeOfMemory)
            {
                String type = metricCollector.memDivisionNames[index];
                index++;
                Map<String, String> mymap = new HashMap<>();
                mymap.put(type, Arrays.toString(area.getValues()));
                usageArray.add(mymap);
            }
            heaps.put(memory, usageArray);
        }

        JSONObject heapObject = new JSONObject();
        heapObject.put("MemoryOccupied", heapMap);
        heapObject.put("HeapDivision", heaps);

        JSONObject nativeObject = new JSONObject();
        nativeObject.put("MemoryOccupied", nativeMap);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Heap", heapObject);
        jsonObject.put("Native", nativeObject);
        jsonObject.put("ResidentMemory", Arrays.toString(containerAgent.metricCollector.getResidentMemoryStat().getValues()));
        monitorObject.put("Memory", jsonObject);


    }

    static void addCpuToMonitor(JSONObject monitorObject, ContainerAgent containerAgent)
    {
        JSONObject cpuJsonObject = new JSONObject();
        addCpuCurrentFrequency(cpuJsonObject, containerAgent);
        addCpuGovernors(cpuJsonObject, containerAgent);
        addCpuHyperthreading(cpuJsonObject, containerAgent);
        addCpuLoad(cpuJsonObject, containerAgent);
        addCpuModels(cpuJsonObject);
        monitorObject.put("CPU", cpuJsonObject);
    }

    //adds CPU Models
    private static void addCpuModels(JSONObject cpuObject)
    {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();

        CentralProcessor centralProcessor = hardwareAbstractionLayer.getProcessor();
        Map<String, String> cpuMap = new HashMap<>();
        int index;
        for(index = 0; index < Constants.NO_OF_CORES; index++)
        {
            try
            {
                cpuMap.put("CPU" + index, centralProcessor.toString());
            }

            catch (Exception e) {
                return;
            }
        }
        cpuObject.put("Models", cpuMap);
    }
}
