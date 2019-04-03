/*
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      https://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 */

package com.ibm.cloudtools.metrics.cpu;

import com.ibm.cloudtools.agent.Constants;
import com.ibm.cloudtools.agent.MetricCollector;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.IOException;
import java.io.RandomAccessFile;

public class LinuxCpuMetricsImpl extends AbstractCpuMetricsImpl
{
    private DescriptiveStatistics[] freqStat;

    LinuxCpuMetricsImpl()
    {
        freqStat = new DescriptiveStatistics[Constants.NO_OF_CORES];

        for (int i = 0; i < Constants.NO_OF_CORES; i++) {
            freqStat[i] = new DescriptiveStatistics();
        }
    }

    public DescriptiveStatistics[] getFreqStat() {
        return freqStat;
    }

    /* Reads the governors for the CPUs */
    public String[] getCpuGovernors()
    {
        String[] governors = new String[Constants.NO_OF_CORES];

        for (int i = 0; i < Constants.NO_OF_CORES; i++) {
            try {
                String GOVERNOR_CUR = "/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_governor";
                RandomAccessFile random = new RandomAccessFile(GOVERNOR_CUR, "r");
                governors[i] = random.readLine();

                if (governors[i].equals("powersave")) MetricCollector.governorPowersaveFlag = 1;

                random.close();
            } catch (IOException e) {
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

        for (int core = 0; core < Constants.NO_OF_CORES; core++) {
            RandomAccessFile random;
            try {
                String FREQ_CUR = "/sys/devices/system/cpu/cpu" + core + "/cpufreq/" + "cpuinfo_cur_freq";
                random = new RandomAccessFile(FREQ_CUR, "r");
                curFrequency = random.readLine();
                random.close();
            } catch (IOException e) {
                curFrequency = "-1";
            }

            freqStat[core].addValue(Double.parseDouble(curFrequency));
        }
    }
}
