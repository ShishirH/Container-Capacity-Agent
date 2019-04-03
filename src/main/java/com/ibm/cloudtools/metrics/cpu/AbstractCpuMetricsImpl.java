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
import com.ibm.cloudtools.system.SystemDump;
import com.sun.jna.Platform;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class AbstractCpuMetricsImpl implements CpuMetrics
{

    public static AbstractCpuMetricsImpl getCpuMetrics()
    {
        if (Platform.isLinux()) return new LinuxCpuMetricsImpl();

        /*TODO Write implementations for other operating systems*/
        return new AbstractCpuMetricsImpl();
    }

    public void getCpuCurrentFrequency() {
        System.err.println("COULD NOT GET CURRENT FREQUENCY");
    }

    public DescriptiveStatistics getCpuLoad()
    {
        return null;
    }

    /* checking hyperthreading. If logical_processor_count = 2 * physical_count,
    then hyperthreading is enabled for that cpu */
    public int getHyperthreadingInfo()
    {
        return SystemDump.centralProcessor.getLogicalProcessorCount()
                == 2 * SystemDump.centralProcessor.getPhysicalProcessorCount()
                ? 1
                : 0;
    }

    public String getCpuModel() {
        return SystemDump.centralProcessor.toString();
    }

    public String[] getCpuGovernors()
    {
        String[] governors = new String[Constants.NO_OF_CORES];

        for (int i = 0; i < Constants.NO_OF_CORES; i++) {
            governors[i] = "NOT FOUND!";
        }

        MetricCollector.governorPowersaveFlag = 0;

        return governors;
    }

    public DescriptiveStatistics[] getFreqStat() {
        return new DescriptiveStatistics[Constants.NO_OF_CORES];
    }
}
