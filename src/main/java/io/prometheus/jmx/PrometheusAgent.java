package io.prometheus.jmx;

import com.ibm.lang.management.internal.ExtendedOperatingSystemMXBeanImpl;

import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

public class PrometheusAgent
{

    void getMetrics(MetricCollector metricCollector)
    {
        ExtendedOperatingSystemMXBeanImpl extendedOperatingSystemMXBean =  ExtendedOperatingSystemMXBeanImpl.getInstance();
        extendedOperatingSystemMXBean.getProcessCpuLoad();
        try
        {
            TimeUnit.SECONDS.sleep(1);
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }

        for(int i = 0; i < Constants.MAX_NUMBER_OF_VALUES; i++)
        {
            metricCollector.cpuMetricsImpl.loadStat.addValue(extendedOperatingSystemMXBean.getProcessCpuLoad() * Constants.NO_OF_CORES);
            metricCollector.prometheusAgent.refreshURL(metricCollector);

            for (String line : metricCollector.lines)
            {
                String[] wordOfLine = line.split(" ");

                for (String word : wordOfLine)
                {
                    if (word.equals(Constants.PROCESS_CPU_SECONDS))
                    {
                        Matcher m = metricCollector.doublePattern.matcher(line);
                        while (m.find())
                        {
                            metricCollector.cpuSecondsStat.addValue(Double.parseDouble(line.substring(m.start(), m.end())));
                        }
                    }

                    else if(word.equals(Constants.PROCESS_RESIDENT_MEMORY))
                    {
                        Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                        while(m.find())
                        {
                            metricCollector.residentMemoryStat.addValue(Double.parseDouble(line.substring(m.start(), m.end())));
                        }
                    }

                    else if(!metricCollector.prometheusAgent.getHeapAndNative(word, line, i, metricCollector))
                    {
                        metricCollector.prometheusAgent.getHeapTypes(word, line, i, metricCollector);

                    }
                }
            }

            try
            {
                metricCollector.time[i] = i * Constants.TIME_TO_SLEEP;
                TimeUnit.SECONDS.sleep(Constants.TIME_TO_SLEEP);
            }

            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //Refreshes the contents of the web page, allowing for the metrics to be updated.
    void refreshURL(MetricCollector metricCollector)
    {
        //wait until prometheus server has started before starting to scrape.
        try
        {
            metricCollector.out = new Scanner(new URL(Constants.URL).openStream(), "UTF-8").useDelimiter("\\A").next();
        }
        catch (Exception e)
        {
            refreshURL(metricCollector);
        }
        metricCollector.lines = metricCollector.out.split("\n");
    }

    /*
        Get size of the total heap and native memory. Obtains the data from Prometheus, by parsing the webpage using regex.
        Gives bytes committed, bytes used, maximum, and the initial size.
    */

    private boolean getHeapAndNative(String word, String line, int iteration, MetricCollector metricCollector)
    {
        switch (word) {
            case "jvm_memory_bytes_committed{area=\"heap\",}": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                while (m.find()) {
                    metricCollector.heapMemory[0][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                    metricCollector.heapStat[0].addValue(metricCollector.heapMemory[0][iteration]);
                }
                return true;
            }
            case "jvm_memory_bytes_committed{area=\"nonheap\",}": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                while (m.find()) {
                    metricCollector.nativeMemory[0][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                    metricCollector.nativeStat[0].addValue(metricCollector.nativeMemory[0][iteration]);
                }
                return true;
            }
            case "jvm_memory_bytes_used{area=\"heap\",}": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                while (m.find()) {
                    metricCollector.heapMemory[1][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                    metricCollector.heapStat[1].addValue(metricCollector.heapMemory[1][iteration]);
                }
                return true;
            }
            case "jvm_memory_bytes_used{area=\"nonheap\",}": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                while (m.find()) {
                    metricCollector.nativeMemory[1][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                    metricCollector.nativeStat[1].addValue(metricCollector.nativeMemory[1][iteration]);

                }
                return true;
            }
            case "jvm_memory_bytes_max{area=\"heap\",}": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                while (m.find()) {
                    metricCollector.heapMemory[2][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                    metricCollector.heapStat[2].addValue(metricCollector.heapMemory[2][iteration]);
                }
                return true;
            }
            case "jvm_memory_bytes_max{area=\"nonheap\",}": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                while (m.find()) {
                    metricCollector.nativeMemory[2][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                    metricCollector.nativeStat[2].addValue(metricCollector.nativeMemory[2][iteration]);
                }
                return true;
            }
            case "jvm_memory_bytes_init{area=\"heap\",}": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                while (m.find()) {
                    metricCollector.heapMemory[3][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                    metricCollector.heapStat[3].addValue(metricCollector.heapMemory[3][iteration]);
                }
                return true;
            }
            case "jvm_memory_bytes_init{area=\"nonheap\",}": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                while (m.find()) {
                    metricCollector.nativeMemory[3][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                    metricCollector.nativeStat[3].addValue(metricCollector.nativeMemory[3][iteration]);
                }

                return true;
            }
        }

        return false;

    }

    /*
        Obtains divisions of heap
    */

    private void getHeapTypes(String word, String line, int iteration, MetricCollector metricCollector)
    {

        switch (word)
        {
            case "jvm_memory_pool_bytes_committed{pool=\"Code": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                while (m.find()) {
                    metricCollector.heapTypes[0][0][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                m = metricCollector.doublePattern.matcher(line);

                while (m.find()) {
                    metricCollector.heapTypes[0][0][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }

                break;
            }
            case "jvm_memory_pool_bytes_committed{pool=\"Metaspace\",}": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                while (m.find()) {
                    metricCollector.heapTypes[0][1][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                m = metricCollector.doublePattern.matcher(line);

                while (m.find()) {
                    metricCollector.heapTypes[0][1][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                break;
            }
            case "jvm_memory_pool_bytes_committed{pool=\"Compressed": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                while (m.find()) {
                    metricCollector.heapTypes[0][2][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                m = metricCollector.doublePattern.matcher(line);

                while (m.find()) {
                    metricCollector.heapTypes[0][2][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                break;
            }
            case "jvm_memory_pool_bytes_committed{pool=\"PS": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                int type;
                if(line.contains("Eden Space"))
                    type = 3;
                else if(line.contains("Survivor Space"))
                    type = 4;
                else
                    type = 5;

                while (m.find()) {
                    metricCollector.heapTypes[0][type][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                m = metricCollector.doublePattern.matcher(line);

                while (m.find()) {
                    metricCollector.heapTypes[0][type][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                break;
            }

            case "jvm_memory_pool_bytes_used{pool=\"Code": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                while (m.find()) {
                    metricCollector.heapTypes[1][0][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                m = metricCollector.doublePattern.matcher(line);

                while (m.find()) {
                    metricCollector.heapTypes[1][0][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }

                break;
            }
            case "jvm_memory_pool_bytes_used{pool=\"Metaspace\",}": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                while (m.find()) {
                    metricCollector.heapTypes[1][1][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                m = metricCollector.doublePattern.matcher(line);

                while (m.find()) {
                    metricCollector.heapTypes[1][1][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                break;
            }
            case "jvm_memory_pool_bytes_used{pool=\"Compressed": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                while (m.find()) {
                    metricCollector.heapTypes[1][2][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                m = metricCollector.doublePattern.matcher(line);

                while (m.find()) {
                    metricCollector.heapTypes[1][2][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                break;
            }
            case "jvm_memory_pool_bytes_used{pool=\"PS": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                int type;
                if(line.contains("Eden Space"))
                    type = 3;
                else if(line.contains("Survivor Space"))
                    type = 4;
                else
                    type = 5;

                while (m.find()) {
                    metricCollector.heapTypes[1][type][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                m = metricCollector.doublePattern.matcher(line);

                while (m.find()) {
                    metricCollector.heapTypes[1][type][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                break;
            }

            case "jvm_memory_pool_bytes_max{pool=\"Code": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                while (m.find()) {
                    metricCollector.heapTypes[2][0][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                m = metricCollector.doublePattern.matcher(line);

                while (m.find()) {
                    metricCollector.heapTypes[2][0][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }

                break;
            }
            case "jvm_memory_pool_bytes_max{pool=\"Metaspace\",}": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                while (m.find()) {
                    metricCollector.heapTypes[2][1][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                m = metricCollector.doublePattern.matcher(line);

                while (m.find()) {
                    metricCollector.heapTypes[2][1][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                break;
            }
            case "jvm_memory_pool_bytes_max{pool=\"Compressed": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                while (m.find()) {
                    metricCollector.heapTypes[2][2][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                m = metricCollector.doublePattern.matcher(line);

                while (m.find()) {
                    metricCollector.heapTypes[2][2][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                break;
            }
            case "jvm_memory_pool_bytes_max{pool=\"PS": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                int type;
                if(line.contains("Eden Space"))
                    type = 3;
                else if(line.contains("Survivor Space"))
                    type = 4;
                else
                    type = 5;

                while (m.find()) {
                    metricCollector.heapTypes[2][type][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                m = metricCollector.doublePattern.matcher(line);

                while (m.find()) {
                    metricCollector.heapTypes[2][type][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                break;
            }


            case "jvm_memory_pool_bytes_init{pool=\"Code": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                while (m.find()) {
                    metricCollector.heapTypes[3][0][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                m = metricCollector.doublePattern.matcher(line);

                while (m.find()) {
                    metricCollector.heapTypes[3][0][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }

                break;
            }
            case "jvm_memory_pool_bytes_init{pool=\"Metaspace\",}": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                while (m.find()) {
                    metricCollector.heapTypes[3][1][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                m = metricCollector.doublePattern.matcher(line);

                while (m.find()) {
                    metricCollector.heapTypes[3][1][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                break;
            }
            case "jvm_memory_pool_bytes_init{pool=\"Compressed": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                while (m.find()) {
                    metricCollector.heapTypes[3][2][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                m = metricCollector.doublePattern.matcher(line);

                while (m.find()) {
                    metricCollector.heapTypes[3][2][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                break;
            }
            case "jvm_memory_pool_bytes_init{pool=\"PS": {
                Matcher m = metricCollector.scientificDoublePattern.matcher(line);
                int type;
                if(line.contains("Eden Space"))
                    type = 3;
                else if(line.contains("Survivor Space"))
                    type = 4;
                else
                    type = 5;

                while (m.find()) {
                    metricCollector.heapTypes[3][type][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                m = metricCollector.doublePattern.matcher(line);

                while (m.find()) {
                    metricCollector.heapTypes[3][type][iteration] = Double.parseDouble(line.substring(m.start(), m.end()));
                }
                break;

            }
        }

    }
}
