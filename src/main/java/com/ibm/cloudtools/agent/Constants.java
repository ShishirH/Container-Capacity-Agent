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

package com.ibm.cloudtools.agent;

public class Constants
{
    public static final int MAX_NUMBER_OF_VALUES = 10; // Maximum number of samples to be calculated.
    public static final int NO_OF_CORES =
            Runtime.getRuntime().availableProcessors(); // No of cores in the system
    public static final int TIME_TO_SLEEP = 1; // Period between recording values
    public static final String[] MEM_TYPES = {"Committed", "Used", "Max", "Init"};
    public static final int MEM_TYPE_LENGTH = MEM_TYPES.length;
    public static final double ONE_GB = 1073741824.0;
    public static final double ONE_MB = 1048576.0;
}
