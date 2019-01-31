package io.prometheus.jmx;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
class RawData
{
    private static void addCpuGovernors(JSONObject cpuObject, ContainerAgent containerAgent)
    {
        String [] governors = containerAgent.metricCollector.cpuMetricsImpl.getCpuGovernors();
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
        double [][] cpuFrequencies = containerAgent.metricCollector.cpuMetricsImpl.getCpuCurrentFrequency();
        Map<String, String> cpuMap = new HashMap<>();
        for(double[] frequency : cpuFrequencies)
        {
            cpuMap.put("CPU" + index, Arrays.toString(frequency));
            index++;
        }

        cpuObject.put("CurrentFrequencies", cpuMap);
    }

    //adds Hyperthreading information by reading /proc/cpuinfo
    private static void addCpuHyperthreading(JSONObject cpuObject, ContainerAgent containerAgent)
    {
        int [] hyperthreading = containerAgent.metricCollector.cpuMetricsImpl.getHyperthreadingInfo();
        int index = 0;
        Map<String, String> cpuMap = new HashMap<>();
        for(int threading : hyperthreading)
        {
            cpuMap.put("CPU" + index, Integer.toString(threading));
            index++;
        }

        cpuObject.put("Hyperthreading", cpuMap);
    }

    //apply statistical methods to memory data
    static void addMemoryToMonitor(JSONObject monitorObject, ContainerAgent containerAgent)
    {
        int outIndex = 0;
        double [][][] heapTypes = containerAgent.metricCollector.getHeapTypes();
        double [][] heapMem = containerAgent.metricCollector.getHeapMemory();
        double [][] nativeMem = containerAgent.metricCollector.getNativeMemory();

        Map<String, String> heapMap = new HashMap<>();
        Map<String, String> nativeMap = new HashMap<>();

        for(double [] memType : heapMem)
        {
            heapMap.put(Constants.MEM_TYPES[outIndex], Arrays.toString(memType));
            outIndex++;
        }


        outIndex = 0;
        for(double [] memType : nativeMem) {
            nativeMap.put(Constants.MEM_TYPES[outIndex], Arrays.toString(memType));
            outIndex++;
        }

        JSONObject heaps = new JSONObject();
        outIndex = 0;
        for(double[][] typeOfMemory : heapTypes)
        {
            String memory = Constants.MEM_TYPES[outIndex];
            JSONArray usageArray = new JSONArray();
            outIndex++;
            int index = 0;
            for(double[] typeOfHeap : typeOfMemory)
            {
                String type = Constants.HEAP_TYPES[index];
                index++;
                Map<String, String> mymap = new HashMap<>();
                mymap.put(type, Arrays.toString(typeOfHeap));
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
        addCpuModels(cpuJsonObject, containerAgent);
        monitorObject.put("CPU", cpuJsonObject);
    }

    //adds CPU Models
    private static void addCpuModels(JSONObject cpuObject, ContainerAgent containerAgent)
    {
        String [] models = containerAgent.metricCollector.cpuMetricsImpl.getCpuModels();

        Map<String, String> cpuMap = new HashMap<>();
        int index = 0;
        for(String model : models)
        {
            cpuMap.put("CPU" + index, model.substring(13));
            index++;
        }
        cpuObject.put("Models", cpuMap);
    }
}
