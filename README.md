

# Ripple-WebRTC-Server

<p align="center" width="100%">
<img src="logo.png">
</p>


Ripple-WebRTC-Server is a Java-based WebRTC media server built using the Quarkus framework. It provides support for video rooms and audio rooms. The server is still in development mode and aims to deliver a robust and efficient WebRTC experience.
The primary goal is to use this app as an Native Image created by [ GraalVM tools](https://github.com/graalvm)

Check the branch before cloning this . This app is developed based on two web-based frameworks : [Helidon](https://helidon.io/) and [Quarkus](https://quarkus.io/)

The goal of the project is to provide a new alternative  to WebRTC severs powered by java .Application will be easy to set up  and run . Will provide client SDKs to use .

We aim to keep the memory footprint down and Image size to a very small app still giving you the best performance to scale .

Deployment and configuration should take less than 30 mins to get you going.

# Target clients are :
- Browser base app (Chrome-based,FireFox). (coming Soon)
- Mobile Apps (Android & IOS) (coming Soon)
- Desktop Clients (JavaFX,TornadoFX,Flutter,DotNet,QT C++,Electron) (coming Soon)

There will be some form SDK/Library to help implement or utilise the server .

## For Documentation Please refer to:  ![Docs Page](/docs/index.md)

![](https://placehold.it/400x90/ff0000/000000?text=STILL_IN_DEVELOPMENT!)

The code don't make sense as much yet or not functional as required! There will be code example on how to set and run this.


## Build and run


With JDK17+
```bash
mvn package
java -jar target/ripple-webrtc-server.jar
```

## Exercise the application
```
curl -X GET http://localhost:8080/simple-greet
{"message":"Hello World!"}
```

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
curl -s -X GET http://localhost:8080/metrics
# TYPE base:gc_g1_young_generation_count gauge
. . .

# JSON Format
curl -H 'Accept: application/json' -X GET http://localhost:8080/metrics
{"base":...
. . .
```



## Try health

```
curl -s -X GET http://localhost:8080/health
{"outcome":"UP",...

```



## Building a Native Image

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
./target/ripple-webrtc-server
```

If you don't have GraalVM installed, you can use the following command to build the native executable in a container:


## Building the Docker Image

```
docker build -t ripple-webrtc-server .
```

## Running the Docker Image

```
docker run --rm -p 8080:8080 ripple-webrtc-server:latest
```

Exercise the application as described above.
                                

## Building a Custom Runtime Image

Build the custom runtime image using the jlink image profile:

```
mvn package -Pjlink-image
```

This uses the helidon-maven-plugin to perform the custom image generation.
After the build completes it will report some statistics about the build including the reduction in image size.

The target/ripple-webrtc-server-jri directory is a self contained custom image of your application. It contains your application,
its runtime dependencies and the JDK modules it depends on. You can start your application using the provide start script:

```
./target/ripple-webrtc-server-jri/bin/start
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

curl -X GET http://localhost:6060/greet/Joe

## Features

The Ripple-WebRTC-Server aims to provide the following main features:

- Video rooms: Support for creating and joining video rooms.
- Audio rooms: Support for creating and joining audio rooms.
- AppRTC example for video call
- Broadcast With G-Streamer transcoding


[Optional] JVM Memory managment has been : `vm.args="-XX:+UseG1GC -XX:+AggressiveHeap"` . 
This has been the main set up during development .  `-XX:+UseG1GC` enables the G1 garbage collector for memory optimization, `-XX:+AggressiveHeap` enables aggressive heap deallocation for better memory management 

Access transport that will be support to above features are :
- Rest HTTP 
- Websockets

Primary transport focus is  Rest HTTP for now  . Demo pages are so far  that are still in progress are : 

* ![Video-Room Page](/website/demos/video-room.html)
* ![Video-Call Page](/website/demos/video-call.html)
* ![G-Streamer Page](/website/demos/G-Streamer.html)

Client Javascript SDK/Lib -  ![Ripple JS](/website/assets/js/rippleApp.js)

Please note that this server is still under development, and additional features and improvements are planned for future releases.

Feel free to explore and contribute to the development of the Ripple-WebRTC-Server.

<a href="https://bmc.link/kinsleyKAJIVA" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 60px !important;width: 217px !important;" ></a>



###### Website Theme Credit found under the website folder