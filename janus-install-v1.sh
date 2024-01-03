#!/bin/bash

echo "#######################This is for ubuntu linux 18.04 and above ####################### " && \
sudo apt-get update && \
sudo apt-get -y install aptitude && \
clear && echo "############################--Start work--############################"  && \
sudo snap install cmake --classic && \
sudo apt-get update && \
sudo apt-get -y install build-essential && \
sudo apt-get -y install g++ && \
sudo apt-get -y install gengetopt && \
sudo aptitude -y install libmicrohttpd-dev && \
sudo aptitude -y install  libjansson-dev && \
clear && echo "############################-->> libssl-dev Installation --############################"  && \
sudo aptitude -y install 	libssl-dev || sudo apt-get -y install libssl-dev && \
sudo apt  -y install libsrtp2-dev && clear &&\
sudo aptitude -y install libsofia-sip-ua-dev && \
sudo aptitude -y install libglib2.0-dev && clear && \
clear && echo "############################-->> libopus-dev Installation --############################"  && \
sudo aptitude -y install 	libopus-dev || sudo apt-get install -y libopus-dev && clear && \
sudo aptitude -y install libogg-dev && clear && \
sudo aptitude -y install libcurl4-openssl-dev && \
sudo aptitude -y install liblua5.3-dev && \
clear && echo "############################-->> libconfig-dev Installation --############################"  && \
sudo aptitude -y install libconfig-dev  || sudo apt-get install -y libconfig-dev && \
sudo aptitude -y install pkg-config  && clear && \
sudo aptitude -y install libtool automake && \
sudo apt -y install libnice-dev && \
sudo apt-get -y install libsrtp2-dev && \
sudo apt install supervisor && \
clear && echo "#########################################################################"  && \
echo "##################### Done.Doing Repo Packages###########################"  && \
echo "#########################################################################"  && \
sudo apt-get -y install libusrsctp-dev  && \
git clone -b v4.3.0 https://github.com/warmcat/libwebsockets.git && cd libwebsockets && mkdir build && cd build && cmake -DLWS_MAX_SMP=1 -DLWS_WITHOUT_EXTENSIONS=0 -DCMAKE_INSTALL_PREFIX:PATH=/usr -DCMAKE_C_FLAGS="-fpic" ..  && \
make && sudo make install && \
cd .. && cd .. && \
clear && echo ">> Step:INSTALLING paho.mqtt.c"  && \
git clone https://github.com/eclipse/paho.mqtt.c.git && cd paho.mqtt.c && make && sudo make install && cd .. && \
clear && echo ">> Step:INSTALLING libnanomsg-dev"  && \
sudo aptitude -y install libnanomsg-dev && \
clear && echo ">> Step:INSTALLING rabbitmq-c"  && \
git clone https://github.com/alanxz/rabbitmq-c  && cd rabbitmq-c  && git submodule init  && git submodule update  && mkdir build && cd build  && cmake -DCMAKE_INSTALL_PREFIX=/usr ..  && make && sudo make install && cd .. && cd .. && \
clear && echo ">> Step:INSTALLING doxygen graphviz "  && \
sudo aptitude -y install doxygen graphviz && \
clear && echo ">> Step:INSTALLING janus-gateway FROM GITHUB "  && \
git clone -b v1.2.1 https://github.com/meetecho/janus-gateway.git && cd janus-gateway && sudo add-apt-repository ppa:git-core/ppa -y &&\
sudo git config --global --add safe.directory /home/ubuntu/janus-gateway  && \
sudo sh autogen.sh && \
sudo mkdir -p /opt/janus && sudo mkdir -p /opt/janus/bin && sudo ./configure --prefix=/opt/janus --enable-post-processing && sudo  make && sudo  make install && \
sudo make configs && \
sudo ./configure --enable-docs && cd .. && \
clear && echo ">> Step:SYSTEM CONFIG using supervisor "  && \
sudo mkdir -p /etc/supervisor/conf.d/ && \
FILE=/etc/supervisor/conf.d/janus.conf && sudo mkdir -p "$(dirname "$FILE")" && sudo touch "$FILE" && \
sudo sh -c 'printf "[program:janus]\n command=/opt/janus/bin/janus\n user=root\n autostart=true\n autorestart=true\n stderr_logfile=/var/log/janus.err.log\n stdout_logfile=/var/log/janus.out.log\n\n" >/etc/supervisor/conf.d/janus.conf' && \
sudo supervisorctl reread  && sudo supervisorctl update && \
sudo supervisorctl reload && \
cd ~ && sudo rm -rf janus-gateway/ rabbitmq-c/ libwebsockets/ paho.mqtt.c/ && \
clear && echo "###################################################################################################"  && \
echo "##################### Done.To test open http://localhost:8088/janus/info ##########################"  && \
echo "##################### Configs found here /opt/janus/etc/janus/ ##########################"  && \
echo "###################################################################################################"