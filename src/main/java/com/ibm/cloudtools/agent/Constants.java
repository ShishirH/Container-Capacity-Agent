package com.ibm.cloudtools.agent;

class Constants
{
    final static int MAX_NUMBER_OF_VALUES = 10; //Maximum number of samples to be calculated.
    final static int NO_OF_CORES = Runtime.getRuntime().availableProcessors(); //No of cores in the system
    final static int TIME_TO_SLEEP = 1; //Period between recording values
    final static String[] MEM_TYPES = {"Committed", "Used", "Max", "Init"};
    final static int MEM_TYPE_LENGTH = MEM_TYPES.length;
    final static double ONE_GB = 1073741824.0;
    final static double ONE_MB = 1048576.0;
}