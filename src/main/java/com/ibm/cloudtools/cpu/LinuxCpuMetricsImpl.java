/*
 * ******************************************************************************
 *  * Copyright (c) 2012, 2019 IBM Corp. and others
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

package com.ibm.cloudtools.cpu;

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
        loadStat = new DescriptiveStatistics();
        freqStat = new DescriptiveStatistics[Constants.NO_OF_CORES];

        for (int i = 0; i < Constants.NO_OF_CORES; i++)
        {
            freqStat[i] = new DescriptiveStatistics();
        }
    }

    public DescriptiveStatistics[] getFreqStat()
    {
        return freqStat;
    }

    /* Reads the governors for the CPUs */
    public String[] getCpuGovernors()
    {
        String[] governors = new String[Constants.NO_OF_CORES];

        for (int i = 0; i < Constants.NO_OF_CORES; i++)
        {
            try
            {
                String GOVERNOR_CUR = "/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_governor";
                RandomAccessFile random = new RandomAccessFile(GOVERNOR_CUR, "r");
                governors[i] = random.readLine();

                if(governors[i].equals("powersave"))
                    MetricCollector.governorPowersaveFlag = 1;

                random.close();
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

        for (int core = 0; core < Constants.NO_OF_CORES; core++)
        {
            RandomAccessFile random;
            try
            {
                String FREQ_CUR = "/sys/devices/system/cpu/cpu" + core + "/cpufreq/" + "cpuinfo_cur_freq";
                random = new RandomAccessFile(FREQ_CUR, "r");
                curFrequency = random.readLine();
                random.close();
            } catch (IOException e)
            {
                curFrequency = "-1";
            }

            freqStat[core].addValue(Double.parseDouble(curFrequency));
        }
    }

    public DescriptiveStatistics getCpuLoad()
    {
        return loadStat;
    }
}
