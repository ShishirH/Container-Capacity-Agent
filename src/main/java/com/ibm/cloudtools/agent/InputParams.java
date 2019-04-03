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

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;

public class InputParams
{
  public static String getName()
  {
    return name;
  }

  public static String getApiVersion()
  {
    return apiVersion;
  }

  public static int getConfig()
  {
    return config;
  }

  public static double getCpuTargetMultiplier()
  {
    return cpuTargetMultiplier;
  }

  static long getBuffer()
  {
    return buffer;
  }

  static long getEnableDiagnostics()
  {
    return enableDiagnostics;
  }

  private static String name;
  private static String apiVersion;
  private static int config;
  private static double cpuTargetMultiplier;

  private static long buffer;
  private static long enableDiagnostics;

  public static int PERFORMANCE_CONFIG = 0;
  public static int RESOURCE_SAVE_CONFIG = 1;

  /* Reads the input.json configuration file and sets the parameters. */
  static void readInputJSON(String args)
  {
    try {
      JSONObject inputObject = (JSONObject) new JSONParser().parse(new FileReader(args));
      final String configuration = (String) inputObject.get("config");

      name = inputObject.containsKey("name") ? (String) inputObject.get("name") : "java";

      apiVersion =
          inputObject.containsKey("apiVersion") ? (String) inputObject.get("apiVersion") : "v1";

      buffer = inputObject.containsKey("buffer") ? (Long) inputObject.get("buffer") : 10;

      cpuTargetMultiplier = (Double) inputObject.get("cpuTargetMultiplier");

      /*TODO Look at what ideal values for the differnet configurations would be*/
      config = (configuration.equals("performance")) ? PERFORMANCE_CONFIG : RESOURCE_SAVE_CONFIG;

      enableDiagnostics =
          inputObject.containsKey("enableDiagnostics")
              ? (Long) inputObject.get("enableDiagnostics")
              : 0;
    } catch (IOException | ParseException e) {
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
