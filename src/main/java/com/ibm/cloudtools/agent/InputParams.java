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

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;

public class InputParams
{
    public static String name;
    public static String apiVersion;
    public static int config;
    public static double cpuTargetMultiplier;

    static long buffer;
    static long enableDiagnostics;

    static void readInputJSON(String args)
    {
        try
        {
            JSONObject inputObject = (JSONObject) new JSONParser().parse(new FileReader(args));
            final String configuration = (String) inputObject.get("config");

            name =
                    inputObject.containsKey("name") ? (String) inputObject.get("name") : "java";

            apiVersion =
                    inputObject.containsKey("apiVersion") ? (String) inputObject.get("apiVersion") : "v1";

            buffer =
                    inputObject.containsKey("buffer") ? (Long) inputObject.get("buffer") : 10;

            cpuTargetMultiplier = (Double) inputObject.get("cpuTargetMultiplier");

            /*TODO Look at what ideal values for the differnet configurations would be*/
            config = (configuration.equals("performance")) ? 0 : 1;

            enableDiagnostics =
                    inputObject.containsKey("enableDiagnostics") ? (Long) inputObject.get("enableDiagnostics")
                            : 0;
        }

        catch (IOException | ParseException e)
        {
            System.err.println("Input JSON not found. Using reasonable defaults.");
            name = "java";
            apiVersion = "v1";
            buffer = 10;
            cpuTargetMultiplier = 1.0;
            config = 1;
            enableDiagnostics = 0;
        }
    }
}
