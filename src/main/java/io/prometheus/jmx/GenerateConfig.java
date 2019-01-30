package io.prometheus.jmx;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;

import java.math.RoundingMode;
import java.text.DecimalFormat;

class GenerateConfig
{
    static YamlMapping createYamlConfig(ContainerAgent containerAgent)
    {
        /* to round upto 3 decimal places */
        DecimalFormat decimalFormat = new DecimalFormat("#.###");
        decimalFormat.setRoundingMode(RoundingMode.CEILING);

        return Yaml.createYamlMappingBuilder()
                    .add("apiVersion", "v1")
                    .add("spec", Yaml.createYamlMappingBuilder()
                    .add("containers", Yaml.createYamlMappingBuilder()
                        .add("- name", "java")
                        .add("resources", Yaml.createYamlMappingBuilder()
                            .add("requests", Yaml.createYamlMappingBuilder()
                                .add("memory", containerAgent.meanMemSize + "MB")
                                .add("cpu", decimalFormat.format(containerAgent.metricCollector.cpuMetricsImpl.getCpuLoad().getMean() * containerAgent.targetMultiplier))
                                .build()
                            )
                                .add("limits", Yaml.createYamlMappingBuilder()
                                .add("memory", containerAgent.maxMemSize + "MB")
                                .add("cpu", decimalFormat.format(containerAgent.metricCollector.cpuMetricsImpl.getCpuLoad().getMax() * containerAgent.targetMultiplier))
                                .build())
                                .build())
                    .build())
                    .build())
                    .build();
    }

}
