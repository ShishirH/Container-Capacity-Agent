package com.ibm.cloudtools.agent;

class Constants {
  static final int MAX_NUMBER_OF_VALUES = 10; // Maximum number of samples to be calculated.
  static final int NO_OF_CORES =
      Runtime.getRuntime().availableProcessors(); // No of cores in the system
  static final int TIME_TO_SLEEP = 1; // Period between recording values
  static final String[] MEM_TYPES = {"Committed", "Used", "Max", "Init"};
  static final int MEM_TYPE_LENGTH = MEM_TYPES.length;
  static final double ONE_GB = 1073741824.0;
  static final double ONE_MB = 1048576.0;
}
