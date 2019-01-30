package io.prometheus.jmx;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CpuMetricsImpl implements CpuMetrics
{
    private ConstPatterns constPatterns;
    DescriptiveStatistics loadStat;
    private DescriptiveStatistics [] freqStat;

    CpuMetricsImpl()
    {
        constPatterns = new ConstPatterns();
        loadStat = new DescriptiveStatistics();
        freqStat = new DescriptiveStatistics[Constants.NO_OF_CORES];

        for (int i = 0; i < Constants.NO_OF_CORES; i++)
        {
            freqStat[i] = new DescriptiveStatistics();
        }
    }

    DescriptiveStatistics[] getFreqStat()
    {
        return freqStat;
    }

    //checking hyperthreading. grep -E "cpu cores|siblings|physical id" /proc/cpuinfo. If siblings = 2 * cpu cores, then hyperthreading is enabled for that cpu
    public int[] getHyperthreadingInfo()
    {

        int [] hyperThreading = new int[Constants.NO_OF_CORES];
        String processorInfo;
        try
        {
            processorInfo = new Scanner(new File(Constants.PROC_CPU_INFO)).useDelimiter("\\Z").next();
        }

        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return hyperThreading;
        }

        String[] info = processorInfo.split("\n");

        Pattern siblingPattern = Pattern.compile(constPatterns.SIBLING);
        Pattern corePattern = Pattern.compile(constPatterns.CORE);
        Pattern siblingCPUPattern = Pattern.compile(constPatterns.SIBLING_CPU);

        int count = 0;
        int siblingCount = 0;
        int coreCount = 1;
        int flag = 0;
        for(String line : info)
        {
            Matcher siblingMatcher = siblingPattern.matcher(line);
            Matcher coreMatcher = corePattern.matcher(line);

            while(siblingMatcher.find())
            {
                Matcher matcher = siblingCPUPattern.matcher(line);
                if(matcher.find())
                {
                    flag++;
                    siblingCount = Integer.parseInt(line.substring(matcher.start(), matcher.end()));
                }
            }

            while(coreMatcher.find())
            {
                Matcher matcher = siblingCPUPattern.matcher(line);
                if(matcher.find())
                {
                    flag++;
                    coreCount = Integer.parseInt(line.substring(matcher.start(), matcher.end()));
                }
            }

            if(coreCount * 2 == siblingCount && flag == 2)
            {
                hyperThreading[count] = 1;
                count++;
                coreCount = 1;
                siblingCount = 0;
                flag = 0;
            }

            else if(coreCount * 2 != siblingCount && flag == 2)
            {
                hyperThreading[count] = 0;
                coreCount = 1;
                siblingCount = 0;
                flag = 0;

            }
        }

        return hyperThreading;
    }

    //Reads the governors for the CPUs
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
                    governors[i] = "WARNING: RUNNING ON VM!";
                }
            }

        return governors;
    }

    /*
        Records cpufrequency of all cores by reading from the system over a specified period of time(Constants.timeToSleep), until
        the number of values are reached.
    */

    public double[][] getCpuCurrentFrequency()
    {
        double[][] frequencies = new double[Constants.NO_OF_CORES][Constants.MAX_NUMBER_OF_VALUES];
        String curFrequency;


            for(int i = 0; i < Constants.MAX_NUMBER_OF_VALUES; i++) {
                for(int j = 0; j < Constants.NO_OF_CORES; j++)
                {
                    RandomAccessFile random;
                    try
                    {
                        String FREQ_CUR = "/sys/devices/system/cpu/cpu" + j + "/cpufreq/" + "cpuinfo_cur_freq";
                        random = new RandomAccessFile(FREQ_CUR, "r");
                        curFrequency = random.readLine();
                    }

                    catch (IOException e)
                    {
                        curFrequency = "-1";
                    }

                    frequencies[j][i] = Double.parseDouble(curFrequency);
                    freqStat[j].addValue(frequencies[j][i]);
                }

                try
                {
                    TimeUnit.SECONDS.sleep(Constants.TIME_TO_SLEEP);
                }

                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        return frequencies;
    }

    //reads and returns the model names of the CPU from proc/cpuinfo. Uses Regex.
    public String[] getCpuModels()
    {
        String[] modelNames = new String[Constants.NO_OF_CORES];
        String modelNamePattern = constPatterns.MODEL_NAME;
        Pattern modelPattern = Pattern.compile(modelNamePattern);
        String processorInfo;
        try
        {
            processorInfo = new Scanner(new File(Constants.PROC_CPU_INFO)).useDelimiter("\\Z").next();
        }

        catch (FileNotFoundException e)
        {
            System.err.println("UNABLE TO OBTAIN CPU MODELS");
            return modelNames;
        }

        String[] info = processorInfo.split("\n");

        int l = 0;
        for(String line : info)
        {
            Matcher m = modelPattern.matcher(line);
            while (m.find())
            {
                modelNames[l++] = line;
            }
        }
        return modelNames;
    }

    public DescriptiveStatistics getCpuLoad()
    {
        return loadStat;
    }
}
