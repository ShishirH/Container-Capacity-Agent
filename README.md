# Container Capacity Agent

In a Kubernetes environment, where the limits and reserved resources of a container can be specified, a tool that can gauge with some accuracy the parameters can help in preventing the container being killed, and resources being wasted. This tool can also be used as a guideline for IT Admins to arrive at the right limits.

## Prerequisites
Maven to build the project.

To build, run the command
```bash
mvn package
```

## Input Parameters
Create an input.json configuration file. A sample is included in the repository.
```json
{
  "config": "performance", 
  "cpuTargetMultiplier": 1.0,
  "buffer": 10,
  "name": "SpringApplication",
  "apiVersion": "nightly",
  "enableDiagnostics": 1
}
```

config: Can be performance or resourceEfficiency. Performance gives settings for maximum throughput. 
resourceEfficiency tries to find a nice balance between throughput and resources used. 

cpuTargetMultiplier: If the CPU of the target machine where the container is going to run is different than the CPU on which the test is
curently running, you can scale the CPU metrics used if the ratio of performance of the CPUs is known.

buffer: The additional buffer on top of the calculated values during the run.

name: Name of the application

apiVersion: The version of the application.

enableDiagnostics: If you want to look at the data collected throughout the run, pass 1.

## Usage
Copy the jar file with dependencies in the target folder of the tool and run the application normally, with the tool as a javaagent. Make sure sufficient stress is provided to the application, either by simulating real world scenarios through a test suite.

To run, use
```bash
java -javaagent:ContainerAgent-1.0.0-jar-with-dependencies.jar your-application
```

After the run of the application completes, an Output YAML file will be generated that will suggest the limits and resources to reserve, while also recommending OpenJ9 specific options.

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.



## License
[Apache License 2.0](https://choosealicense.com/licenses/apache-2.0/)