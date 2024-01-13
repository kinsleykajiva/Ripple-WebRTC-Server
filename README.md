# Ripple-WebRTC-Server

<p align="center" width="100%">
<img src="logo.png">
</p>

Ripple-WebRTC-Server is a Java-based WebRTC media server built using the Helidon SE framework.  The server  aims to deliver a robust and efficient WebRTC experience.
The primary goal is to use this app as an Native Image created by [ GraalVM tools](https://github.com/graalvm)


The goal of the project is to provide a new alternative  to WebRTC severs powered by java .Application will be easy to set up  and run . Will provide client SDKs to use .

We aim to keep the memory footprint down and Image size to a very small app still giving you the best performance to scale .

Deployment and configuration should easy to get you going.

# Target clients are :
- Browser base app (Chrome-based,FireFox). (coming Soon)
- Mobile Apps (Android & IOS) (coming Soon)
- Desktop Clients (JavaFX,TornadoFX,Flutter,DotNet,QT C++,Electron) (coming Soon)


Client Javascript SDK/Lib -  ![Ripple JS](/website/assets/js/rippleApp.js)


## For Documentation Please refer to:  ![Docs Page](/docs/index.md)


## Features

The Ripple-WebRTC-Server aims to provide the following main features:
- Broadcast With G-Streamer transcoding
- Video rooms: Support for creating and joining video rooms (coming soon).
- Audio rooms: Support for creating and joining audio rooms (coming soon).
- AppRTC example for video call (coming soon)

[Optional] JVM Memory management has been : `"-XX:+UseG1GC -XX:+AggressiveHeap"` .
This has been the main set up during development .  `-XX:+UseG1GC` enables the G1 garbage collector for memory optimization, `-XX:+AggressiveHeap` enables aggressive heap deallocation for better memory management


Connection Transport is WebSockets

Feel free to explore and contribute to the development of the Ripple-WebRTC-Server.
















## Build and run


With JDK21
```bash
mvn package
java -jar target/ripple-webrtc.jar
```

## Exercise the application

Basic:
```
curl -X GET http://localhost:8080/simple-greet
Hello World!
```


JSON:
```
curl -X GET http://localhost:8080/greet
{"message":"Hello World!"}

curl -X GET http://localhost:8080/greet/Joe
{"message":"Hello Joe!"}

curl -X PUT -H "Content-Type: application/json" -d '{"greeting" : "Hola"}' http://localhost:8080/greet/greeting

curl -X GET http://localhost:8080/greet/Jose
{"message":"Hola Jose!"}
```



## Try metrics

```
# Prometheus Format
curl -s -X GET http://localhost:8080/observe/metrics
# TYPE base:gc_g1_young_generation_count gauge
. . .

# JSON Format
curl -H 'Accept: application/json' -X GET http://localhost:8080/observe/metrics
{"base":...
. . .
```


## Try health

This example shows the basics of using Helidon SE Health. It uses the
set of built-in health checks that Helidon provides plus defines a
custom health check.

Note the port number reported by the application.

Probe the health endpoints:

```bash
curl -X GET http://localhost:8080/observe/health
curl -X GET http://localhost:8080/observe/health/ready
```



## Building a Native Image

The generation of native binaries requires an installation of GraalVM 22.1.0+.

You can build a native binary using Maven as follows:

```
mvn -Pnative-image install -DskipTests
```

The generation of the executable binary may take a few minutes to complete depending on
your hardware and operating system. When completed, the executable file will be available
under the `target` directory and be named after the artifact ID you have chosen during the
project generation phase.

Make sure you have GraalVM locally installed:

```
$GRAALVM_HOME/bin/native-image --version
```

Build the native image using the native image profile:

```
mvn package -Pnative-image
```

This uses the helidon-maven-plugin to perform the native compilation using your installed copy of GraalVM. It might take a while to complete.
Once it completes start the application using the native executable (no JVM!):

```
./target/ripple-webrtc
```

Yep, it starts fast. You can exercise the application’s endpoints as before.


## Building the Docker Image

```
docker build -t ripple-webrtc .
```

## Running the Docker Image

```
docker run --rm -p 8080:8080 ripple-webrtc:latest
```

Exercise the application as described above.
                                

## Run the application in Kubernetes

If you don’t have access to a Kubernetes cluster, you can [install one](https://helidon.io/docs/latest/#/about/kubernetes) on your desktop.

### Verify connectivity to cluster

```
kubectl cluster-info                        # Verify which cluster
kubectl get pods                            # Verify connectivity to cluster
```

### Deploy the application to Kubernetes

```
kubectl create -f app.yaml                  # Deploy application
kubectl get pods                            # Wait for quickstart pod to be RUNNING
kubectl get service  ripple-webrtc         # Get service info
```

Note the PORTs. You can now exercise the application as you did before but use the second
port number (the NodePort) instead of 8080.

After you’re done, cleanup.

```
kubectl delete -f app.yaml
```
                                

## Building a Custom Runtime Image

Build the custom runtime image using the jlink image profile:

```
mvn package -Pjlink-image
```

This uses the helidon-maven-plugin to perform the custom image generation.
After the build completes it will report some statistics about the build including the reduction in image size.

The target/ripple-webrtc-jri directory is a self contained custom image of your application. It contains your application,
its runtime dependencies and the JDK modules it depends on. You can start your application using the provide start script:

```
./target/ripple-webrtc-jri/bin/start
```

Class Data Sharing (CDS) Archive
Also included in the custom image is a Class Data Sharing (CDS) archive that improves your application’s startup
performance and in-memory footprint. You can learn more about Class Data Sharing in the JDK documentation.

The CDS archive increases your image size to get these performance optimizations. It can be of significant size (tens of MB).
The size of the CDS archive is reported at the end of the build output.

If you’d rather have a smaller image size (with a slightly increased startup time) you can skip the creation of the CDS
archive by executing your build like this:

```
mvn package -Pjlink-image -Djlink.image.addClassDataSharingArchive=false
```

For more information on available configuration options see the helidon-maven-plugin documentation.
                                
