# Container Capacity Agent

In a Kubernetes environment, where the limits and reserved resources of a container can be specified, a tool that can gauge with some accuracy the parameters can help in preventing the container being killed, and resources being wasted. This tool can also be used as a guideline for IT Admins to arrive at the right limits.

## Prerequisites
Maven to build the project.

To build, run the command
```bash
mvn package
```

## Usage
Copy the jar file with dependencies in target folder to your application, and run the application normally, with the tool as a javaagent. Make sure sufficient stress is provided to the application, either by simulating real world scenarios through a test suite.

To run, use
```bash
java -javaagent:ContainerAgent-1.0.0-jar-with-dependencies.jar your-application
```

After the run of the application completes, an Output YAML file will be generated that will suggest the limits and resources to reserve, while also recommending OpenJ9 specific options.

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.



## License
[Apache License 2.0](https://choosealicense.com/licenses/apache-2.0/)