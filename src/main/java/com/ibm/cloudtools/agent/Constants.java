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

public class Constants
{
    public static final int MAX_NUMBER_OF_VALUES =
            60; // Maximum number of samples to be stored before dumping.
    public static final int NO_OF_CORES =
            Runtime.getRuntime().availableProcessors(); // No of cores in the system
    public static final int TIME_TO_SLEEP = 1; // Period between recording values

    enum MEM_TYPES
    {
        Committed, Used, Max, Init
    }

    public static final double ONE_GB = 1073741824.0;
    public static final double ONE_MB = 1048576.0;
}
