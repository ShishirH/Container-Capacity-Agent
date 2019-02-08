package com.ibm.cloudtools.agent;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

import java.io.IOException;
import java.io.RandomAccessFile;

class CpuMetricsImpl implements CpuMetrics
{
    DescriptiveStatistics loadStat;
    DescriptiveStatistics cpuSecondsStat;
    private DescriptiveStatistics [] freqStat;

    CpuMetricsImpl()
    {
        loadStat = new DescriptiveStatistics();
        freqStat = new DescriptiveStatistics[Constants.NO_OF_CORES];

        for (int i = 0; i < Constants.NO_OF_CORES; i++)
        {
            freqStat[i] = new DescriptiveStatistics();
        }
        cpuSecondsStat = new DescriptiveStatistics();
    }

    DescriptiveStatistics[] getFreqStat()
    {
        return freqStat;
    }

    DescriptiveStatistics getCpuSecondsStat()
    {
        return cpuSecondsStat;
    }

    /* checking hyperthreading. grep -E "cpu cores|siblings|physical id" /proc/cpuinfo. If siblings = 2 * cpu cores, then hyperthreading is enabled for that cpu */
    public int getHyperthreadingInfo()
    {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();
        CentralProcessor centralProcessor = hardwareAbstractionLayer.getProcessor();

        return centralProcessor.getLogicalProcessorCount() == 2 * centralProcessor.getPhysicalProcessorCount() ? 1 : 0;
    }

    /* Reads the governors for the CPUs */
    public String[] getCpuGovernors()
    {
        String[] governors = new String[Constants.NO_OF_CORES];

        for(int i = 0; i < Constants.NO_OF_CORES; i++)
        {
            try
            {
                String GOVERNOR_CUR = "/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_governor";
                RandomAccessFile random = new RandomAccessFile(GOVERNOR_CUR, "r");
                governors[i] = random.readLine();
            }

            catch (IOException e)
            {
                governors[i] = "NOT FOUND!";
            }
        }

        return governors;
    }

    /*
        Records cpufrequency of all cores by reading from the system for that instance of time
    */

    public void getCpuCurrentFrequency()
    {
        String curFrequency;

        for(int core = 0; core < Constants.NO_OF_CORES; core++)
        {
            RandomAccessFile random;
            try
            {
                String FREQ_CUR = "/sys/devices/system/cpu/cpu" + core + "/cpufreq/" + "cpuinfo_cur_freq";
                random = new RandomAccessFile(FREQ_CUR, "r");
                curFrequency = random.readLine();
            }

            catch (IOException e)
            {
                curFrequency = "-1";
            }

            freqStat[core].addValue(Double.parseDouble(curFrequency));
        }
    }

    /* reads and returns the model names of the CPU from proc/cpuinfo. Uses Regex. */
    public String getCpuModels()
    {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();
        return hardwareAbstractionLayer.getProcessor().toString();
    }

    public DescriptiveStatistics getCpuLoad()
    {
        return loadStat;
    }
}
