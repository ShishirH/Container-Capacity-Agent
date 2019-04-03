## Workflow
ContainerAgent.java is the main agent class from which execution starts.
It creates a new thread, and starts collecting metrics through CpuMetricsImpl.java and
MemoryMetricsImpl.java in com.ibm.cloudtools.metrics package.

The metric collection is done through a daemon thread. Once the main program stops,
or an interrupt is called, the shutdownhook exports the required YAML file, and if specified,
also additional files that can be used for diagnosis.
 
All the metrics are stored in MetricCollector.java for the run. 