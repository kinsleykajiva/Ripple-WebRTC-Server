'use strict';
const RippleSDK = {
    serverUrl                   : '',
    serverName                   : '',
    timeZone                    : Intl.DateTimeFormat().resolvedOptions().timeZone,
    clientID                    : '',
    isDebugSession              : false,
    clientTypesAllowed          : ['Firefox', 'Chrome'],
    featuresAvailable           : Object.freeze({VIDEO_ROOM: 'VIDEO_ROOM',AUDIO_ROOM: 'AUDIO_ROOM',VIDEO_CALL: 'VIDEO_CALL',G_STREAM:'G_STREAM',G_STREAM_BROADCAST:'G_STREAM_BROADCASTER',G_STREAM_BROADCAST_CONSUMER:'G_STREAM_BROADCAST_CONSUMER'}),
    app               : {
        iceServerArray              : [{urls: 'stun:stun.l.google.com:19302'},{urls: "stun:stun.services.mozilla.com"}],
        isAudioAccessRequired       : false,
        isVideoAccessRequired       : false,
        hasAccessToVideoPermission  : false,
        hasAccessToAudioPermission  : false,
        maxRetries: 10,
        remindServerTimeoutInSeconds: 50 ,
        reminderInterval  : null,
        mediaUI:{
          renderGroupParentId:""
        },
        webRTC:{
            EVENT_NAMES : {
                ICE_CANDIDATE: 'icecandidate',
                ICE_CONNECTION_STATE_CHANGE: 'iceconnectionstatechange',
                ICE_GATHERING_STATE_CHANGE: 'icegatheringstatechange',
                NEGOTIATION_NEEDED: 'negotiationneeded',
                SIGNALING_STATE_CHANGE: 'signalingstatechange',
                TRACK: 'track',
                DATA_CHANNEL: 'datachannel',
                CONNECTION_STATE_CHANGE: 'connectionstatechange',
                REMOVE_STREAM: 'removestream',
                ADD_STREAM: 'addstream',
                ICE_CANDIDATE_ERROR: 'icecandidateerror',
                IDENTITY_RESULT: 'identityresult',
                IDP_ASSERTION_ERROR: 'idpassertionerror',
                IDP_VALIDATION_ERROR: 'idpvalidationerror',
                PEER_IDENTITY: 'peeridentity',
                STATS_ENDED: 'statsended',
            },

            peerConnectionsMap:new Map(),
            localStream:null,
            remoteStreamsMap:new Map(),
            peerConnectionConfig:{
                iceServers: [],
                iceTransportPolicy: 'all',
                bundlePolicy: 'balanced',
                rtcpMuxPolicy: 'require',
                iceCandidatePoolSize: 10,
            },
            peerConnectionOptions:{
                optional: [
                    {DtlsSrtpKeyAgreement: true},
                    {RtpDataChannels: false}
                ]
            },
            peerConnectionConstraints:{
                optional: [
                    {DtlsSrtpKeyAgreement: true},
                    {RtpDataChannels: false}
                ]
            },
            peerConnectionConstraintsForOffer:{
                offerToReceiveAudio: true,
                offerToReceiveVideo: true,
            },
            peerConnectionConstraintsForAnswer:{
                offerToReceiveAudio: true,
                offerToReceiveVideo: true,
            },
            peerConnectionConstraintsForDataChannel:{
                ordered: true,
                maxRetransmits: 0,
            },
            peerConnectionConstraintsForDataChannelForChat:{
                ordered: true,
                maxRetransmits: 0,
            },
            peerConnectionConstraintsForDataChannelForFile:{
                ordered: true,
                maxRetransmits: 0,
            },
            peerConnectionConstraintsForDataChannelForScreen:{
                ordered: true,
                maxRetransmits: 0,
            },
            peerConnectionConstraintsForDataChannelForStream:{
                ordered: true,
                maxRetransmits: 0,
            },
            peerConnectionConstraintsForDataChannelForStreamForScreen:{
                ordered: true,
                maxRetransmits: 0,
            },
            peerConnectionConstraintsForDataChannelForStreamForFile:{
                ordered: true,
                maxRetransmits: 0,
            },
            peerConnectionConstraintsForDataChannelForStreamForChat:{
                ordered: true,
                maxRetransmits: 0,
            },
            peerConnectionConstraintsForDataChannelForFileForScreen:{
                ordered: true,
                maxRetransmits: 0,
            },
            peerConnectionConstraintsForDataChannelForFileForChat:{
                ordered: true,
                maxRetransmits: 0,
            },
            peerConnectionConstraintsForDataChannelForScreenForChat:{
                ordered: true,
                maxRetransmits: 0,
            },
            peerConnectionConstraintsForDataChannelForStreamForFileForScreen:{
                ordered: true
            },
            createPeerConnection:(threadRef)=>{

                if(!threadRef){
                    RippleSDK.utils.error('createPeerConnection', 'no threadRef');
                    return null;
                }

                if(RippleSDK.app.webRTC.peerConnectionsMap.has(threadRef)){
                    RippleSDK.utils.warn('createPeerConnection', `peer connection already exists for threadRef : ${threadRef}`);
                    return RippleSDK.app.webRTC.peerConnectionsMap.get(threadRef);
                }
                const peerCon = new RTCPeerConnection(RippleSDK.app.webRTC.peerConnectionConfig);
                const eventHandlers = {
                    [RippleSDK.app.webRTC.EVENT_NAMES.ICE_CANDIDATE]: (ev) => {
                        if (!ev.candidate || (ev.candidate.candidate && ev.candidate.candidate.indexOf('endOfCandidates') > 0)) {
                            RippleSDK.utils.log('End of candidates.');
                        }else{
                            if (ev.candidate) {
                                const payload        = {
                                    clientID     : RippleSDK.clientID,
                                    requestType  : 'iceCandidate',
                                    transaction: RippleSDK.utils.uniqueIDGenerator("transaction",12),
                                    threadRef    : threadRef,
                                    candidate    : ev.candidate.candidate,
                                    sdpMid       : ev.candidate.sdpMid,
                                    sdpMLineIndex: ev.candidate.sdpMLineIndex
                                };
                                RippleSDK.utils.log('iceCandidate', payload);
                                RippleSDK.transports.websocket.webSocketSendAction(payload);
                            }
                        }

                    },
                    [RippleSDK.app.webRTC.EVENT_NAMES.ICE_CONNECTION_STATE_CHANGE]: (ev) => {
                        switch (peerCon.iceConnectionState) {
                            case 'closed':
                            case 'failed':
                            case 'disconnected':
                                RippleSDK.utils.log('iceConnectionState', peerCon.iceConnectionState);
                                break;
                        }
                    },
                    [RippleSDK.app.webRTC.EVENT_NAMES.NEGOTIATION_NEEDED]: (ev) => {
                        RippleSDK.utils.log('negotiationneeded', ev);
                    },
                    [RippleSDK.app.webRTC.EVENT_NAMES.SIGNALING_STATE_CHANGE]: (ev) => {
                        RippleSDK.utils.log('signalingstatechange', ev);
                    },
                    [RippleSDK.app.webRTC.EVENT_NAMES.TRACK]: (ev) => {
                        RippleSDK.utils.log('track', ev);
                    },
                    [RippleSDK.app.webRTC.EVENT_NAMES.DATA_CHANNEL]: (ev) => {
                        RippleSDK.utils.log('datachannel', ev);
                    },
                    [RippleSDK.app.webRTC.EVENT_NAMES.CONNECTION_STATE_CHANGE]: (ev) => {
                        RippleSDK.utils.log('connectionstatechange', ev);
                    },
                    [RippleSDK.app.webRTC.EVENT_NAMES.REMOVE_STREAM]: (ev) => {
                        RippleSDK.utils.log('removestream', ev);
                    },
                    [RippleSDK.app.webRTC.EVENT_NAMES.ADD_STREAM]: (ev) => {
                        RippleSDK.utils.log('addstream', ev);
                    },
                    [RippleSDK.app.webRTC.EVENT_NAMES.ICE_CANDIDATE_ERROR]: (ev) => {
                        RippleSDK.utils.log('icecandidateerror', ev);
                    },
                    [RippleSDK.app.webRTC.EVENT_NAMES.IDENTITY_RESULT]: (ev) => {
                        RippleSDK.utils.log('identityresult', ev);
                    },
                    [RippleSDK.app.webRTC.EVENT_NAMES.IDP_ASSERTION_ERROR]: (ev) => {
                        RippleSDK.utils.log('idpassertionerror', ev);
                    },
                };
                for (const eventName in eventHandlers) {
                    peerCon.addEventListener(eventName, eventHandlers[eventName]);
                }

                return peerCon;
                // called like this: RippleSDK.app.webRTC.peerConnectionsMap.set(threadRef,RippleSDK.app.webRTC.createPeerConnection(threadRef));


            }
        },
        startToRemindServerOfMe:()=>{
            RippleSDK.utils.log('startToRemindServerOfMe');
            RippleSDK.app.reminderInterval = setInterval(()=>{
                const body =  {
                    clientID: RippleSDK.clientID,
                    requestType: 'remember',

                };
                RippleSDK.transports.websocket.webSocketSendAction(body);

            }, RippleSDK.app.remindServerTimeoutInSeconds *1000);


        },
        features:{
            streaming:{
                threads:[],
                functions:{
                    renderThreadUI: (threadRef) => {
                        if (RippleSDK.app.features.streaming.threads.length === 0) {
                            RippleSDK.utils.error('renderThreadUI', 'no threads found');
                            return;
                        }
                        if (!threadRef) {
                            RippleSDK.utils.error('renderThreadUI', 'no threadRef');
                            return;
                        }
                        if (!RippleSDK.app.features.streaming.threads[threadRef]) {
                            RippleSDK.utils.error('renderThreadUI', `no thread found with ref : ${threadRef}`);
                            return;
                        }
                        const groupParentElement = document.getElementById(RippleSDK.app.mediaUI.renderGroupParentId);
                        if(!groupParentElement){
                            RippleSDK.utils.error('renderThreadUI', `no groupParentElement found with id : ${RippleSDK.app.mediaUI.renderGroupParentId}`);
                            return;
                        }

                        const ui = ` 
                                              <div class="card-body">
                                                <h5 class="card-title">Video Stream</h5>
                                                <video id="localVideo_${threadRef}" class="video-stream" autoplay playsinline muted></video>
                                                <div class="video-controls">
                                                  <!-- Controls go here -->
                                                    <div class="video-controls">
                                                      <button id="playPauseButton_${threadRef}"><i class="fas fa-play"></i></button>
                                                      <button id="fastRewindButton_${threadRef}"><i class="fas fa-backward"></i></button>
                                                      <button id="fastForwardButton_${threadRef}"><i class="fas fa-forward"></i></button>
                                                      <input type="range" id="volumeControl_${threadRef}" min="0" max="1" step="0.1">
                                                      <button style="margin-left: 10%" id="fullscreenButton_${threadRef}"><i class="fas fa-expand"></i></button>
                                                    </div>
                                                  <!-- Controls go here -->
                                                </div>
                                              </div>
                                                `;
                        // append the threadRef to the groupParentElement
                        const newElement = document.createElement('div');
                        newElement.setAttribute('id', `streamThread_${threadRef}`);
                        // set class as card
                        newElement.classList.add('card');

                        newElement.innerHTML = ui;
                        groupParentElement.appendChild(newElement);
                        // add event listeners
                        const playPauseButton = document.getElementById(`playPauseButton_${threadRef}`);
                        const fastRewindButton = document.getElementById(`fastRewindButton_${threadRef}`);
                        const fastForwardButton = document.getElementById(`fastForwardButton_${threadRef}`);
                        const volumeControl = document.getElementById(`volumeControl_${threadRef}`);
                        const fullscreenButton = document.getElementById(`fullscreenButton_${threadRef}`);
                        const localVideo = document.getElementById(`localVideo_${threadRef}`);

                        playPauseButton.addEventListener('click', () => {
                            if (localVideo.paused) {
                                localVideo.play();
                                playPauseButton.innerHTML = '<i class="fas fa-pause"></i>';
                            } else {
                                localVideo.pause();
                                playPauseButton.innerHTML = '<i class="fas fa-play"></i>';
                            }
                        });

                        fastRewindButton.addEventListener('click', () => {
                            localVideo.currentTime -= 10;
                        });

                        fastForwardButton.addEventListener('click', () => {
                            localVideo.currentTime += 10;
                        });

                        volumeControl.addEventListener('input', () => {
                            localVideo.volume = volumeControl.value;

                        });

                        fullscreenButton.addEventListener('click', () => {
                            if (localVideo.requestFullscreen) {
                                localVideo.requestFullscreen();
                            } else if (localVideo.mozRequestFullScreen) {
                                localVideo.mozRequestFullScreen(); // Firefox
                            } else if (localVideo.webkitRequestFullscreen) {
                                localVideo.webkitRequestFullscreen(); // Chrome and Safari
                            }
                        })

                    },
                    startBroadCast:(threadRef)=>{
                        if(RippleSDK.app.features.streaming.threads.length === 0){
                            RippleSDK.utils.error('startBroadCast', 'no threads found');
                            return;
                        }
                        if(!threadRef){
                            RippleSDK.utils.error('startBroadCast', 'no threadRef');
                            return;
                        }
                        if(!RippleSDK.app.features.streaming.threads[threadRef]){
                            RippleSDK.utils.error('startBroadCast', `no thread found with ref : ${threadRef}`);
                            return;
                        }

                        RippleSDK.transports.websocket.webSocketSendAction({
                            clientID: RippleSDK.clientID,
                            requestType: 'startBroadCast',
                            threadRef: threadRef,
                        });
                    },
                    requestToResumeTransmission:(threadRef)=>{
                        if(RippleSDK.app.features.streaming.threads.length === 0){
                            RippleSDK.utils.error('requestToResumeTransmission', 'no threads found');
                            return;
                        }
                        if(!threadRef){
                            RippleSDK.utils.error('requestToResumeTransmission', 'no threadRef');
                            return;
                        }
                        if(!RippleSDK.app.features.streaming.threads[threadRef]){
                            RippleSDK.utils.error('requestToResumeTransmission', `no thread found with ref : ${threadRef}`);
                            return;
                        }
                    },
                    requestToPauseTransmission:(threadRef)=>{
                        if(RippleSDK.app.features.streaming.threads.length === 0){
                            RippleSDK.utils.error('requestToPauseTransmission', 'no threads found');
                            return;
                        }
                        if(!threadRef){
                            RippleSDK.utils.error('requestToPauseTransmission', 'no threadRef');
                            return;
                        }
                        if(!RippleSDK.app.features.streaming.threads[threadRef]){
                            RippleSDK.utils.error('requestToPauseTransmission', `no thread found with ref : ${threadRef}`);
                            return;
                        }
                    }
                }
            }
        },
        callbacks:{
            onMessage:messageObject=>{
                RippleSDK.utils.log('onMessage', messageObject);
                if(!messageObject){
                    RippleSDK.utils.log('onMessage', 'no messageObject');
                    return;
                }
                // test if the message is a string or an object
                if(typeof messageObject === 'string'){
                    messageObject = JSON.parse(messageObject);
                }
                const eventType = messageObject.eventType;
                if(eventType === "register"){
                    RippleSDK.utils.log('onMessage', 'register');
                    RippleSDK.app.startToRemindServerOfMe();
                }

            },
            tellClientOnConnected:null,
            onConnected:()=>{
                RippleSDK.utils.log('onConnected');
                if(RippleSDK.app.callbacks.tellClientOnConnected) {
                    RippleSDK.app.callbacks.tellClientOnConnected();
                }else{
                    RippleSDK.utils.log('onConnected', 'no callback');
                }
            },
            tellClientOnClosed:null,
            onClosed:()=>{
                RippleSDK.utils.log('onClosed');
                if(RippleSDK.app.callbacks.tellClientOnClosed) {
                    RippleSDK.app.callbacks.tellClientOnClosed();
                }else{
                    RippleSDK.utils.log('onClosed', 'no callback');
                }
            },
            onConnecting:()=>{
                RippleSDK.utils.log('onConnecting');
                //Todo should be able to tell client this is connecting to the server
            },
            tellClientOnFatalError:null,
            networkError:err=>{
                RippleSDK.utils.log('networkError', err);
                if(RippleSDK.app.callbacks.tellClientOnFatalError) {
                    RippleSDK.app.callbacks.tellClientOnFatalError(err);
                }else{
                    RippleSDK.utils.log('networkError', 'no callback');
                }
            }
        }
    },
    transports:{
        websocket: {
            socket: null,
            isConnected: false,
            webSocketSendAction: (messageObject) => {
                if(!RippleSDK.transports.websocket.isConnected){
                    RippleSDK.utils.log('webSocketSendAction', 'not connected');
                    return;
                }
                if(!messageObject){
                    RippleSDK.utils.log('webSocketSendAction', 'no messageObject');
                    return;
                }
                if(RippleSDK.transports.websocket.socket.readyState !== 1){
                    RippleSDK.utils.log('webSocketSendAction', 'not ready');
                    return;
                }else{
                    messageObject. isDebugSession= RippleSDK.isDebugSession,
                    messageObject.transaction = RippleSDK.utils.uniqueIDGenerator("transaction",12);
                    RippleSDK.utils.log('webSocketSendAction', 'ready');
                    RippleSDK.transports.websocket.socket.send(JSON.stringify(messageObject));
                }

            },
            connect: () => {
                let reconnectAttempts                 = 0;
                const maxReconnectAttempts            = 200;
                const reconnectDelays               = [10, 20, 35, 45, 55]; // delays in seconds
                let url = RippleSDK.utils.convertToWebSocketUrl(RippleSDK.serverUrl);
                if (url.endsWith('/')) {
                    url = url+ 'websocket/client';
                }else{
                    url = url+ '/websocket/client';
                }
                RippleSDK.transports.websocket.socket           = new WebSocket(url);
                RippleSDK.transports.websocket.socket.onopen = () => {
                    RippleSDK.transports.websocket.isConnected = true;
                    RippleSDK.app.callbacks.onConnected();
                    RippleSDK.transports.websocket.webSocketSendAction({
                        clientID: RippleSDK.clientID,
                        requestType: 'register',
                    });

                };
                RippleSDK.transports.websocket.socket.onmessage = (event) => {
                    const data = JSON.parse(event.data);
                    RippleSDK.app.callbacks.onMessage(data);
                };
                RippleSDK.transports.websocket.socket.onclose = (ev) => {
                    RippleSDK.transports.websocket.isConnected = false;
                    RippleSDK.utils.log(`WebSocket closed with code ${ev.code} and reason ${ev.reason}`);

                    if(reconnectAttempts < maxReconnectAttempts){

                        const reconnectDelay = reconnectDelays[Math.floor(Math.random() * reconnectDelays.length)];
                        RippleSDK.utils.log(`WebSocket reconnecting in ${reconnectDelay} seconds`);
                        setTimeout(() => {
                            reconnectAttempts++;
                            RippleSDK.transports.websocket.connect();
                        }, reconnectDelay * 1000);
                    }else{
                        RippleSDK.utils.log(`WebSocket reconnecting failed after ${maxReconnectAttempts} attempts,giving up`);
                        RippleSDK.app.callbacks.onClosed();
                    }


                };
                RippleSDK.transports.websocket.socket.onerror = (error) => {
                    RippleSDK.utils.log('onerror', error);
                    RippleSDK.app.callbacks.networkError(error);
                };
            },
        }
    },
    utils             : {
        log               : console.log.bind(console),
        error             : console.error.bind(console),
        warn              : console.warn.bind(console),
        debug             : console.debug.bind(console),
        info              : console.info.bind(console),
        trace             : console.trace.bind(console),
        assert            : console.assert.bind(console),
        convertToWebSocketUrl: (url) => {
            if (url.startsWith('https://')) {
                return url.replace('https://', 'wss://');
            } else if (url.startsWith('http://')) {
                return url.replace('http://', 'ws://');
            } else {
                return url;
            }
        },
        convertFromWebSocketUrl: (url) => {
            if (url.startsWith('wss://')) {
                return url.replace('wss://', 'https://');
            } else if (url.startsWith('ws://')) {
                return url.replace('ws://', 'http://');
            } else {
                return url;
            }
        },
        isChromeOrFirefox: () => {
            try{
                const isChrome = !!window.chrome && (!!window.chrome.webstore || !!window.chrome.runtime);
                const isFirefox = typeof InstallTrigger !== 'undefined';
                return isChrome || isFirefox;
            }catch (e) {
                RippleSDK.utils.log('isChromeOrFirefox', e);
                const userAgent = navigator.userAgent.toLowerCase();
                return userAgent.indexOf('chrome') > -1 || userAgent.indexOf('firefox') > -1;
            }
        },
        fetchWithTimeout: async (url, options) => {
            const {timeout = 8000} = options;
            if (options.method === 'POST') {
                options.headers = {
                    'Content-Type': 'application/json',
                };
                if (!options.body.clientID) {
                    options.body.clientID = RippleSDK.clientID;

                }
                options.body.timeStamp = new Date().getTime();
                options.body = JSON.stringify(options.body);

            }
            const controller = new AbortController();
            const id = setTimeout(() => controller.abort(), timeout);
            const _urlRoot = RippleSDK.utils.convertToWebSocketUrl(url);
            try {
                const response = await fetch(`${_urlRoot}/${url}`, {
                    ...options,
                    signal: controller.signal,
                });

                clearTimeout(id);
                if (!response.ok) {
                    RippleSDK.utils.warn('fetchWithTimeout', response);
                    RippleSDK.app.callbacks.networkError(`HTTP error! Status: ${response.status}`);
                   // throw new Error(`${response.status} ${response.statusText}`);
                }
                const json = await response.json();
                if(json.serverName){
                    RippleSDK.serverName = json.serverName;
                }
                if(json.success){

                }
                return json;
            }catch (e) {
                RippleSDK.utils.error('fetchWithTimeout', e);
                RippleSDK.app.callbacks.networkError(`Request failed: ${e.message}`);
                clearTimeout(id);
            }

        },
        isWebRTCSupported               : () => !!window.RTCPeerConnection,
        replaceAll: (f, r) => this.split(f).join(r),
        uniqueIDGenerator: (seed = '', maxSize = 22) => {
            const alphabet       = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
            const date            = new Date();
            const timeString     = `${date.getHours()}${date.getMinutes()}${date.getSeconds()}`.padStart(6, '0');
            const dateTimeString = `${seed}${timeString}${Math.random().toString(36).substr(2, 4)}`.slice(0, 12);
            let uniID            = '';
            for (let i = 0; i < maxSize; i++) {
                uniID += alphabet.charAt(Math.floor(Math.random() * alphabet.length));
            }
            for (let i = 0; i < dateTimeString.length; i++) {
                const index = parseInt(dateTimeString.charAt(i), 36);
                uniID = uniID.substr(0, index) + dateTimeString.charAt(i) + uniID.substr(index + 1);
            }
            return uniID.replace(/[^a-z0-9]/gi, '') /*remove non-alphanumeric characters */;
        },
    },
    init: (config) => {
    RippleSDK.serverUrl = config.url;
    RippleSDK.isDebugSession = config.isDebugging;
    RippleSDK.utils.log('init url ', RippleSDK.utils.convertToWebSocketUrl(RippleSDK.serverUrl));
    RippleSDK.clientID = RippleSDK.utils.uniqueIDGenerator("client",12)+RippleSDK.utils.uniqueIDGenerator(RippleSDK.timeZone);
    RippleSDK.utils.log('clientID ', RippleSDK.clientID);
    RippleSDK.transports.websocket.connect();
    RippleSDK.app.iceServerArray = !config.iceCandidates ? RippleSDK.app.iceServerArray : config.iceCandidates;
    RippleSDK.app.webRTC.peerConnectionConfig.iceServers = RippleSDK.app.iceServerArray;
    RippleSDK.app.mediaUI.renderGroupParentId = config.renderGroupParentId;
},
    notificationsTypes: {},

};


























