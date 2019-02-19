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
import com.ibm.cloudtools.agent.MetricCollector;
import com.ibm.cloudtools.agent.SystemDump;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class RawData
{

    private static void addCpuGovernors(JSONObject cpuObject, ContainerAgent containerAgent)
    {
        String[] governors = containerAgent.metricCollector.cpuGovernors;
        int index = 0;
        Map<String, String> cpuMap = new HashMap<>();
        for (String governor : governors)
        {
            cpuMap.put("CPU" + index, governor);
            index++;
            if (governor.equals("powersave") && ContainerAgent.governorPowersaveFlag == 0)
            {
                ContainerAgent.governorPowersaveFlag = 1;
            }
        }
        cpuObject.put("Governors", cpuMap);
    }

    /* adds load of the CPU during the run */
    private static void addCpuLoad(JSONObject cpuObject, ContainerAgent containerAgent)
    {
        cpuObject.put(
                "Load",
                Arrays.toString(containerAgent.metricCollector.linuxCpuMetricsImpl.getCpuLoad().getValues()));
    }

    private static void addCpuCurrentFrequency(JSONObject cpuObject, ContainerAgent containerAgent)
    {
        int index = 0;
        DescriptiveStatistics[] cpuFrequencies =
                containerAgent.metricCollector.linuxCpuMetricsImpl.getFreqStat();
        Map<String, String> cpuMap = new HashMap<>();
        for (DescriptiveStatistics frequency : cpuFrequencies)
        {
            cpuMap.put("CPU" + index, Arrays.toString(frequency.getValues()));
            index++;
        }

        cpuObject.put("CurrentFrequencies", cpuMap);
    }

    private static void addCpuHyperthreading(JSONObject cpuObject, ContainerAgent containerAgent)
    {
        String hyperthreading =
                (containerAgent.metricCollector.hyperThreadingInfo == 1) ? "Enabled" : "Disabled";
        int index;
        Map<String, String> cpuMap = new HashMap<>();
        for (index = 0; index < Constants.NO_OF_CORES; index++)
        {
            cpuMap.put("CPU" + index, hyperthreading);
        }

        cpuObject.put("Hyperthreading", cpuMap);
    }

    /* apply statistical methods to memory data */
    public static void addMemoryToMonitor(
            JSONObject monitorObject, ContainerAgent containerAgent, MetricCollector metricCollector)
    {
        int outIndex = 0;

        DescriptiveStatistics[][] heapTypes = metricCollector.getMemDivisions();
        DescriptiveStatistics[] heapStat = metricCollector.getHeapStat();
        DescriptiveStatistics[] nativeStat = metricCollector.getNativeStat();

        Map<String, String> heapMap = new HashMap<>();
        Map<String, String> nativeMap = new HashMap<>();

        for (DescriptiveStatistics heapMem : heapStat)
        {
            heapMap.put(Constants.MEM_TYPES[outIndex], Arrays.toString(heapMem.getValues()));
            outIndex++;
        }

        outIndex = 0;
        for (DescriptiveStatistics nativeMem : nativeStat)
        {
            nativeMap.put(Constants.MEM_TYPES[outIndex], Arrays.toString(nativeMem.getValues()));
            outIndex++;
        }

        JSONObject heaps = new JSONObject();
        outIndex = 0;
        for (DescriptiveStatistics[] typeOfMemory : heapTypes)
        {
            String memory = Constants.MEM_TYPES[outIndex];
            JSONArray usageArray = new JSONArray();
            outIndex++;
            int index = 0;
            for (DescriptiveStatistics area : typeOfMemory)
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
        jsonObject.put(
                "ResidentMemory",
                Arrays.toString(containerAgent.metricCollector.getResidentMemoryStat().getValues()));
        monitorObject.put("Memory", jsonObject);
    }

    public static void addCpuToMonitor(JSONObject monitorObject, ContainerAgent containerAgent)
    {
        JSONObject cpuJsonObject = new JSONObject();
        addCpuCurrentFrequency(cpuJsonObject, containerAgent);
        addCpuGovernors(cpuJsonObject, containerAgent);
        addCpuHyperthreading(cpuJsonObject, containerAgent);
        addCpuLoad(cpuJsonObject, containerAgent);
        addCpuModels(cpuJsonObject);
        monitorObject.put("CPU", cpuJsonObject);
    }

    /* adds CPU Models */
    private static void addCpuModels(JSONObject cpuObject)
    {
        Map<String, String> cpuMap = new HashMap<>();
        int index;
        for (index = 0; index < Constants.NO_OF_CORES; index++)
        {
            try
            {
                cpuMap.put("CPU" + index, SystemDump.centralProcessor.toString());
            } catch (Exception e)
            {
                return;
            }
        }
        cpuObject.put("Models", cpuMap);
    }
}
