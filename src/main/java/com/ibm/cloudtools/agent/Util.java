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

    static double additionalBuffer(double value)
    {
        value = (value * (ContainerAgent.buffer + 100.0)) / 100.0;
        return value;
    }

    static double convertToMB(double value)
    {
        return value / (1024.0 * 1024.0);
    }
}
