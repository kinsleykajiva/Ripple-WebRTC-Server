#!/bin/bash

# Define variables
UBUNTU_VERSION="18.04"
REPO_URL="https://github.com"
PAHO_MQTT_C_REPO="$REPO_URL/eclipse/paho.mqtt.c.git"
RABBITMQ_C_REPO="$REPO_URL/alanxz/rabbitmq-c"
LIBWEBSOCKETS_REPO="$REPO_URL/warmcat/libwebsockets.git"
JANUS_GATEWAY_REPO="$REPO_URL/meetecho/janus-gateway.git"

# Define an array of packages to install
PACKAGES=("aptitude" "cmake" "build-essential" "g++" "gengetopt" "libmicrohttpd-dev" "libjansson-dev" "libssl-dev" "libsrtp2-dev" "libsofia-sip-ua-dev" "libglib2.0-dev" "libopus-dev" "libogg-dev" "libcurl4-openssl-dev" "liblua5.3-dev" "libconfig-dev" "pkg-config" "libtool" "automake" "libnice-dev" "libsrtp2-dev" "supervisor" "libusrsctp-dev" "doxygen" "graphviz")

# Update system
sudo apt-get update

# Install packages
for package in "${PACKAGES[@]}"; do
    sudo apt-get -y install $package
done

# Clone and install paho.mqtt.c
git clone -b v4.3.0 $PAHO_MQTT_C_REPO && cd paho.mqtt.c && make && sudo make install && cd ..

# Clone and install rabbitmq-c
git clone $RABBITMQ_C_REPO && cd rabbitmq-c && git submodule init && git submodule update && mkdir build && cd build && cmake -DCMAKE_INSTALL_PREFIX=/usr .. && make && sudo make install && cd .. && cd ..

# Clone and install libwebsockets
git clone -b v4.3.0 $LIBWEBSOCKETS_REPO && cd libwebsockets && mkdir build && cd build && cmake -DLWS_MAX_SMP=1 -DLWS_WITHOUT_EXTENSIONS=0 -DCMAKE_INSTALL_PREFIX:PATH=/usr -DCMAKE_C_FLAGS="-fpic" .. && make && sudo make install && cd .. && cd ..

# Clone and install janus-gateway
git clone -b v1.2.1 $JANUS_GATEWAY_REPO && cd janus-gateway && sudo add-apt-repository ppa:git-core/ppa -y && sudo git config --global --add safe.directory /home/ubuntu/janus-gateway && sudo sh autogen.sh && sudo mkdir -p /opt/janus && sudo mkdir -p /opt/janus/bin && sudo ./configure --prefix=/opt/janus --enable-post-processing && sudo make && sudo make install && sudo make configs && sudo ./configure --enable-docs && cd ..

# Configure supervisor
sudo mkdir -p /etc/supervisor/conf.d/
echo "[program:janus]
command=/opt/janus/bin/janus
user=root
autostart=true
autorestart=true
stderr_logfile=/var/log/janus.err.log
stdout_logfile=/var/log/janus.out.log" | sudo tee /etc/supervisor/conf.d/janus.conf

sudo supervisorctl reread
sudo supervisorctl update
sudo supervisorctl reload

# Clean up
cd ~ && sudo rm -rf janus-gateway/ rabbitmq-c/ libwebsockets/ paho.mqtt.c/

echo "Done. To test open http://localhost:8088/janus/info"
echo "Configs found here /opt/janus/etc/janus/"