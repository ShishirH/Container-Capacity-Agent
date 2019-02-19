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

package com.ibm.cloudtools.agent;

import java.io.File;

public class Util
{
    static String separatorsToSystem(String res)
    {
        if (res == null) return null;
        if (File.separatorChar == '\\')
        {
            // From Windows to Linux/Mac
            return res.replace('/', File.separatorChar);
        }
        else
        {
            // From Linux/Mac to Windows
            return res.replace('\\', File.separatorChar);
        }
    }

    public static double additionalBuffer(double value)
    {
        value = (value * (ContainerAgent.buffer + 100.0)) / 100.0;
        return value;
    }

    public static double convertToMB(double value)
    {
        return value / (1024.0 * 1024.0);
    }
}
