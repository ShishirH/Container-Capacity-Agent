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

package com.ibm.cloudtools.agent;

import java.io.File;

/* Class containing helper functions that are used throughout the project. */
public class Util
{
    /* Used to make the file path compatible between Windows and Linux/OSX */
    public static String separatorsToSystem(String res)
    {
        if (res == null) return null;
        if (File.separatorChar == '\\') {
            // From Windows to Linux/Mac
            return res.replace('/', File.separatorChar);
        } else {
            // From Linux/Mac to Windows
            return res.replace('\\', File.separatorChar);
        }
    }

    public static double additionalBuffer(double value)
    {
        value = (value * (InputParams.getBuffer() + 100.0)) / 100.0;
        return value;
    }
}
