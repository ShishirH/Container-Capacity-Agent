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
import com.ibm.cloudtools.system.SystemDump;
import com.sun.jna.Platform;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class AbstractCpuMetricsImpl implements CpuMetrics
{
    public DescriptiveStatistics loadStat;

    public static AbstractCpuMetricsImpl getCpuMetrics()
    {
        if(Platform.isLinux())
            return new LinuxCpuMetricsImpl();

        /*TODO Write implementations for other operating systems*/
        return new AbstractCpuMetricsImpl();
    }

    public void getCpuCurrentFrequency()
    {
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

    public String getCpuModels()
    {
        return SystemDump.centralProcessor.toString();
    }

    public String[] getCpuGovernors()
    {
        String[] governors = new String[Constants.NO_OF_CORES];

        for (int i = 0; i < Constants.NO_OF_CORES; i++)
        {
            governors[i] = "NOT FOUND!";

        }

        return governors;
    }

    public DescriptiveStatistics[] getFreqStat()
    {
        return new DescriptiveStatistics[Constants.NO_OF_CORES];
    }




}
