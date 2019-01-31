package io.prometheus.jmx;

class ConstPatterns
{
    final String SIBLING_CPU = "\\d+";
    final String SIBLING = "siblings*";
    final String CORE = "cpu cores*";
    final String DOUBLE ="\\d+.\\d+";
    final String SCIENTIFIC_DOUBLE = "\\d+.\\d+E?\\d+";
    final String MODEL_NAME = "(?=model name)";
}

class Constants
{
    final static int MAX_NUMBER_OF_VALUES = 10; //Maximum number of samples to be calculated.
    final static int NO_OF_CORES = Runtime.getRuntime().availableProcessors(); //No of cores in the system
    final static String PROC_CPU_INFO = "/proc/cpuinfo";
    final static String URL = "http://localhost:9999";
    final static int TIME_TO_SLEEP = 1; //Period between recording values
    final static String PROCESS_CPU_SECONDS = "process_cpu_seconds_total";
    final static String PROCESS_RESIDENT_MEMORY = "process_resident_memory_bytes";
    final static String[] HEAP_TYPES = {"Code Cache", "Metaspace", "Compressed Class Space", "PS Eden Space", "PS Survivor Space", "PS Old Gen"};
    final static String[] MEM_TYPES = {"Committed", "Used", "Max", "Init"};
    final static int MEM_TYPE_LENGTH = MEM_TYPES.length;

    ConstPatterns constPatterns = new ConstPatterns();

}
