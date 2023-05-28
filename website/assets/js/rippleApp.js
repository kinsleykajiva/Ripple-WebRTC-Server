'use strict';
const RippleSDK_CONST={
    notificationsTypes: Object.freeze({VIDEO_CALL: 'videoCall',VIDEO_ROOM: 'videoRoom',AUDIO_ROOM: 'audioRoom',}),
    featuresAvailable: Object.freeze({VIDEO_ROOM: 'VIDEO_ROOM',AUDIO_ROOM: 'AUDIO_ROOM',VIDEO_CALL: 'VIDEO_CALL',G_STREAM:'G_STREAM'}),
};



const RippleSDK = {
    log  : function () {
        console.log("['😄'RippleSDK]", {...arguments});
    },
    error: function () {
        console.error("'😡'[RippleSDK]", {...arguments});
    },
    info : function () {
        console.info("''[RippleSDK]", {...arguments});
    },
    warn : function () {
        console.error("'😒'[RippleSDK]", {...arguments});
    },
    accessPassword              : '',
    isAudioAccessRequired       : false,
    isVideoAccessRequired       : false,
    hasAccessToVideoPermission  : false,
    hasAccessToAudioPermission  : false,
    serverUrl                   : '',
    timeZone                    : Intl.DateTimeFormat().resolvedOptions().timeZone,
    serverName                  : '',
    clientID                    : '',
    serverClientId              : '',
    serverClientLastSeen        : 0,
    clientTypesAllowed          : ['Firefox', 'Chrome'],
    clientTypeInUse             : '',
    isWebSocketAccess           : false,
    isDebugSession              : false,
    remindServerTimeoutInSeconds: 26 ,
    iceServerArray              : [],
    app: {
        featuresAvailable : ["VIDEO_ROOM", "AUDIO_ROOM", "VIDEO_CALL","G_STREAM"],
        featuresInUse     : '',
        notificationsTypes: Object.freeze({VIDEO_CALL: 'videoCall',VIDEO_ROOM: 'videoRoom',AUDIO_ROOM: 'audioRoom',}),
        notifications     : [{id: '', type: RippleSDK_CONST.notificationsTypes.VIDEO_CALL, data: null}],
        reminderInterval  : null,
        startToRemindServerOfMe: () => {
            RippleSDK.app.reminderInterval = setInterval(() => {
                const body =  {
                    clientID: RippleSDK.serverClientId,
                    requestType: '',
                };
                if (RippleSDK.isWebSocketAccess) {
                    body.requestType="remember";
                    RippleSDK.Utils.webSocketSendAction(body);
                }else{
                    // we are doing this remember that we are still need an id , as we first attempt to
                    // connect to the websocket server we already need a ID , currently this is the best way of doing it.
                    // trigger to start the remember-me calls
                RippleSDK.Utils.fetchWithTimeout('/app/client/remember', {
                    method: 'POST',
                    body
                }).then(async res => {
                    if (res.success) {
                        RippleSDK.info('startToRemindServerOfMe', res.data);
                        RippleSDK.log("the Server still knows about me ")
                        RippleSDK.serverClientLastSeen = res.data.client.lastSeen;

                        if (res.data.client.clientSDP) {
                            RippleSDK.app.feature.gStream.remoteOfferStringSDP = res.data.client.clientSDP;
                            RippleSDK.app.webRTC.wasOfferSentSuccessfully      = true;
                            RippleSDK.log(" Happy yey ,  finally got our answer from the remote sever ");
                            await RippleSDK.app.webRTC.createAnswer();
                            RippleSDK.Utils.onRemoteSDPReady();
                            // ice candidates could have been sent or flowing by now
                        }

                        if (RippleSDK.app.webRTC.peerConnection) {
                            if (res.data.client.iceCandidates) {
                                RippleSDK.app.webRTC.peerConnection.addIceCandidate(res.data.client.iceCandidates);
                            }
                        }

                    } else {
                        alert("Client not found please re-connect this session is now invalid!")
                        RippleSDK.error("Client not found please re-connect this session is now invalid!")
                    }
                });
            }
            }, RippleSDK.remindServerTimeoutInSeconds * 1_000);
        },
        stopReminderServer: () => clearInterval(RippleSDK.app.reminderInterval),
        maxRetries: 10,
        makeConnection: async () => {

            try {
                const result = await RippleSDK.Utils.fetchWithTimeout('app/connect', {
                    method: 'POST',
                    body: {clientAgentName: RippleSDK.clientID}
                });
                RippleSDK.log(result);
                RippleSDK.serverClientId = result.data.clientID;
                RippleSDK.serverClientLastSeen = result.data.lastSeen;
                RippleSDK.app.startToRemindServerOfMe();

                return true;
            } catch (e) {
                RippleSDK.error(e)
            }

            return false;

        },
        rootCallbacks: {
            websockets:{/*The reason of these call back is to mend the reaction for the client to react in a way more meaning full and give more info on the messages*/
                tellClientOnMessage:null,
                onMessage:async message => {
                    if (!message) {
                        return;
                    }
                    const clientMessage = {
                        isFatal: false,
                        message: "",
                    };
                    message= JSON.parse(message);// convert to object

                    if (message.code === 200) {

                        if (message.eventType === 'webrtc') {
                            RippleSDK.info(" WebRTC Server Response  ", message.message);
                            if (message.data.sdp && message.data.clientSDP) {
                                RippleSDK.app.feature.gStream.remoteOfferStringSDP = message.data.clientSDP;                               
                                RippleSDK.log(" Happy yey ,  finally got our answer from the remote sever ");
                                await RippleSDK.app.webRTC.createAnswer();
                                RippleSDK.Utils.onRemoteSDPReady();
                            }
                        }
                        if (message.eventType === 'iceCandidates') {
                            RippleSDK.info(" iceCandidates Server Response  ", message.message);
                            RippleSDK.app.webRTC.peerConnection.addIceCandidate(message.data.iceCandidates);
                            return;
                        }
                        if (message.eventType === 'notification') {
                            RippleSDK.info(" Server Response  ", message.message);
                            if (message.data.nextActions) {
                                if (message.data.nextActions.includes('createPeerConnection')) {
                                    RippleSDK.app.webRTC.createPeerconnection();
                                    if (RippleSDK.app.featuresInUse === 'G_STREAM') {
                                        RippleSDK.log("We will wait for an offer")
                                    }
                                }
                            }
                        }
                        if (message.eventType === 'remember') {
                            //? so far we can ignore any additional data as we don't need it.
                            RippleSDK.info(" client remembered ", message.message);
                            return;
                        }

                    } else if (message.code === 400) {
                        if (message.eventType === "validation") {
                            RippleSDK.warn("Request InValid ", message.message);
                            clientMessage.message = `Invalid Session/Request ,Please reconnect : ${message.message}`

                        }
                    } else if (message.code === 500) {

                        if (message.eventType === "Error") {
                            RippleSDK.warn("Request Error ", message.message);
                            clientMessage.message = `Fatal Server Error: ${message.message}`
                            clientMessage.isFatal = true;
                        }

                    } else {
                        //? this is an error at most
                    }
                    RippleSDK.app.rootCallbacks.websockets.tellClientOnMessage(clientMessage);
                },
                tellClientOnConnected:null,
                onConnected:()=>RippleSDK.app.rootCallbacks.websockets.tellClientOnConnected(),
                tellClientOnClosed:null,
                onClosed:ev=> RippleSDK.app.rootCallbacks.websockets.tellClientOnClosed(ev),
                tellClientOnFatalError:null,
                fatalError:error=>{
                    RippleSDK.error(error);
                    RippleSDK.app.rootCallbacks.websockets.tellClientOnFatalError(error);

                },
                tellClientOnConnecting:null,
                onConnecting:()=>{
                    RippleSDK.log("initiating connection...");
                    RippleSDK.app.rootCallbacks.websockets.tellClientOnConnecting();
                },
            },
            networkError: error => {
                RippleSDK.error(error);
                if (RippleSDK.isDebugSession) {
                   // alert(error)
                }

            },
            answer: answer => {
                RippleSDK.log('answer', answer)
            },
            devices: media => {
                RippleSDK.log('devices ', media)

                if (media && media.active) {
                    RippleSDK.hasAccessToVideoPermission = true;
                    RippleSDK.hasAccessToAudioPermission = true;
                    RippleSDK.Utils.myMedia              = media;

                    if (RippleSDK.app.featuresInUse.includes('VIDEO_ROOM')) {
                        if (!RippleSDK.app.feature.videoRoom.loadMyLocalVideoObjectID || RippleSDK.app.feature.videoRoom.loadMyLocalVideoObjectID.length === 0) {
                            RippleSDK.error("Failed to find Video Rendering html Element ID , Please set {RippleSDK.app.feature.videoRoom.loadMyLocalVideoObjectID}")
                            return;
                        }
                        try {
                            const localVideo = document.getElementById(RippleSDK.app.feature.videoRoom.loadMyLocalVideoObjectID);
                            localVideo.srcObject = media;
                            RippleSDK.Utils.onAccessMediaAllowedNotification(media, true, true)
                        } catch (e) {
                            RippleSDK.error(e)
                        }
                    }
                } else {
                    RippleSDK.hasAccessToVideoPermission = false;
                    RippleSDK.hasAccessToAudioPermission = false;
                    RippleSDK.error("Access To Media has been rejected !Vide & wont get captured")
                    RippleSDK.Utils.onAccessMediaAllowedNotification(null, false, false)
                }
            },
            icecandidate: data => {
                RippleSDK.log('data', data)
            }
        },
        feature: {
            gStream:{
                remoteOfferStringSDP:null,
                startStreaming: async () => {
                    const body = {clientID: RippleSDK.serverClientId,};
                    if (RippleSDK.isWebSocketAccess) {
                        body.requestType = 'start';
                        RippleSDK.Utils.webSocketSendAction(body);
                    } else {
                        const result = await RippleSDK.Utils.fetchWithTimeout('streams/start', {
                            method: 'POST',body
                        });
                        if (result.success) {
                            RippleSDK.app.webRTC.createPeerconnection();
                            RippleSDK.log("cone creating the answer");
                        }
                    }
                },
            },
            videoRoom: {
                room: {
                    pin             : '',
                    roomID          : '',
                    roomName        : '',
                    maximumCapacity : 0,
                    password        : '',
                    createdTimeStamp: 0,
                    roomDescription : '',
                    creatorClientID : ''
                },
                loadMyLocalVideoObjectID: '',
                createRoom: async (roomName, password, pin) => {
                    if(RippleSDK.app.featuresInUse ==='G_STREAM' || RippleSDK.app.featuresInUse ==='VIDEO_CALL'){
                        RippleSDK.error("This has nothing to do with the feature in use please read docs!");
                        alert("This has nothing to do with the feature in use please read docs!");
                        return ;
                    }

                    if (!roomName || roomName === '') {
                        RippleSDK.error("room name is required");
                        return;
                    }
                    if (!password || password === '') {
                        RippleSDK.error("room password is required");
                        return;
                    }
                    if (!pin || pin === '') {
                        RippleSDK.error("room pin is required");
                        return;
                    }
                    password = password.toString()
                    roomName = roomName.toString()
                    pin = pin.toString()

                    try {
                        const result = await RippleSDK.Utils.fetchWithTimeout('/video/create-room', {
                            method: 'POST',
                            body: {
                                roomName,
                                password,
                                pin,
                                creatorClientID: RippleSDK.serverClientId,
                            }
                        });

                        if (result.success) {
                            RippleSDK.app.feature.videoRoom.room = result.data;
                            RippleSDK.log('Room', RippleSDK.app.feature.videoRoom.room);
                            return true;
                        }
                        return false;
                    } catch (e) {
                        RippleSDK.error(e)
                    }

                    return false;
                },
                /*The client or user logic is depended on the initialization of  ${RippleSDK.app.feature.videoRoom.room} object */
                joinRoom: async () => {
                    if (RippleSDK.app.feature.videoRoom.room.roomID.length === 0) {
                        RippleSDK.error("Please get the room details first!");
                        return;
                    }
                    try {
                        const result = await RippleSDK.Utils.fetchWithTimeout('/video/join-room', {
                            method: 'POST',
                            body: {
                                roomID  : RippleSDK.app.feature.videoRoom.room.roomID,
                                password: RippleSDK.app.feature.videoRoom.room.password,
                                clientID: RippleSDK.serverClientId /*! this id has to exist , meaning the client has to first connect!*/,
                            }
                        });
                        RippleSDK.log(result);
                        return !!result.success;

                    } catch (e) {
                        RippleSDK.error(e)
                    }

                    return false;
                },

            },
        },
        webRTC: {
            peerConnection: null,
            wasOfferSentSuccessfully:false,
            offerOptions: {offerToReceiveVideo: true, offerToReceiveAudio: true},
            createAnswer:async () => {

                await RippleSDK.app.webRTC.peerConnection.setRemoteDescription({
                    sdp : RippleSDK.app.feature.gStream.remoteOfferStringSDP,
                    type: 'offer',
                });
                RippleSDK.app.webRTC.peerConnection.createAnswer(async _sdp => {

                    await RippleSDK.app.webRTC.peerConnection.setLocalDescription(_sdp);
                    const body             = {
                        clientID: RippleSDK.serverClientId,
                        answer  : RippleSDK.app.webRTC.peerConnection.localDescription
                    }
                        if (!RippleSDK.isWebSocketAccess) {
                            let featureResourceUrl = '';

                            if (RippleSDK.app.featuresInUse === 'G_STREAM') {
                                featureResourceUrl = 'streams/send-answer';
                            }
                            const post = await RippleSDK.Utils.fetchWithTimeout(featureResourceUrl, {
                                method: 'POST',
                                body
                            });
                            if (post.success) {
                                // dont do anything for now
                            }
                        }else{
                            //
                            body.requestType='send-answer';
                            RippleSDK.Utils.webSocketSendAction(body);
                        }

                });
            },
            createOffer: () => {
                RippleSDK.app.webRTC.peerConnection.createOffer(RippleSDK.app.webRTC.offerOptions)
                    .then(async _sdp => {
                        await RippleSDK.app.webRTC.peerConnection.setLocalDescription(_sdp);
                        if(RippleSDK.isDebugSession){
                            RippleSDK.log("createOffer sdp",_sdp);
                        }else{
                            RippleSDK.info("createOffer sdp",_sdp);
                        }
                        // ToDo this add a condition check on this part as to avoid repeatiton
                        let featureResourceUrl = '';
                        const body = {
                            clientID: RippleSDK.serverClientId,
                            offer:_sdp.sdp
                        };
                        if(RippleSDK.app.featuresInUse==='G_STREAM'){
                            featureResourceUrl = 'streams/send-offer';
                        }
                        if(RippleSDK.app.featuresInUse==='VIDEO_ROOM'){
                            featureResourceUrl = 'video/send-offer';
                            body.roomID = RippleSDK.app.feature.videoRoom.room.roomID;
                        }
                        const post = await RippleSDK.Utils.fetchWithTimeout(featureResourceUrl, {
                            method: 'POST',
                            body
                        });                       
                        if(post.success){

                            if(post.data.sdp){ // accommodates video room , video call

                                RippleSDK.app.webRTC.peerConnection.setRemoteDescription({
                                    sdp : post.data.sdp,
                                    type: 'answer',
                                });
                                RippleSDK.app.webRTC.wasOfferSentSuccessfully = true;
                            }
                        }
                    });
            },
            shutDownPeerConnection:()=>{
                if(RippleSDK.app.webRTC.peerConnection){
                    RippleSDK.app.webRTC.peerConnection.close();
                    RippleSDK.app.webRTC.peerConnection = null;
                }
            },
            runPeerConnectionDelayedIceJobPayloadsArray: []/*this is an array of promises of request or other functions*/,
            runPeerConnectionDelayedIceJobTimeoutId    : null,
            runPeerConnectionDelayedIceJobs            : () => { /*this is no longer required as much but will keep it in-case there is need for this technique*/
                async function delayedExecution() {
                    // Your code to be executed
                    RippleSDK.log('Delayed execution');
                    // Check the condition
                    if (RippleSDK.app.webRTC.wasOfferSentSuccessfully) {
                        // Cancel the entire attempt
                        clearTimeout(RippleSDK.app.webRTC.runPeerConnectionDelayedIceJobTimeoutId);
                        RippleSDK.log('Condition met. Attempt canceled. Jobs To be done', RippleSDK.app.webRTC.runPeerConnectionDelayedIceJobPayloadsArray.length);
                        await Promise.all(RippleSDK.app.webRTC.runPeerConnectionDelayedIceJobPayloadsArray);

                        RippleSDK.app.webRTC.runPeerConnectionDelayedIceJobPayloadsArray = [];
                    } else {
                        // Incremental delay in milliseconds (between 4 and 5 seconds)
                        const incrementalDelay = Math.floor(Math.random() * 2000) + 4000;

                        // Schedule the next execution with incremental delay
                        RippleSDK.app.webRTC.runPeerConnectionDelayedIceJobTimeoutId = setTimeout(delayedExecution, incrementalDelay);
                    }
                }

                // Start the initial execution after 2 seconds
                RippleSDK.app.webRTC.runPeerConnectionDelayedIceJobTimeoutId = setTimeout(delayedExecution, 2000);
                // Cancel the execution after 20 seconds
                setTimeout(() => {
                    clearTimeout(RippleSDK.app.webRTC.runPeerConnectionDelayedIceJobTimeoutId);
                    RippleSDK.log('Execution canceled');
                }, 20000);
            },
            createPeerconnection: () => {
                const configuration = {
                    /* insertableStreams:true,
                     forceEncodedAudioInsertableStreams:true,
                     forceEncodedVideoInsertableStreams:true,*/
                    iceServers: [
                        {urls: 'stun:stun.l.google.com:19302'},
                        {urls: "stun:stun.services.mozilla.com"}
                    ]
                };
                if(RippleSDK.app.featuresInUse === "G_STREAM"){
                    // Increasing the ICE candidate gathering timeout , allowing more time for connectivity checks
                    // this can be adjusted for based on your experience as much or needs after you have done some monitirng on the application
                    configuration.iceCandidatePoolSize= 10;
                    RippleSDK.app.rootCallbacks.websockets.tellClientOnMessage({
                        type:'background',isGettingStreams:false,showLoadingUI:true,
                    });
                }
                RippleSDK.log('make a peer connection ...');

                RippleSDK.app.webRTC.peerConnection = new RTCPeerConnection(configuration);
                RippleSDK.app.webRTC.peerConnection.onicecandidate = async ev => {
                    if (!ev.candidate || (ev.candidate.candidate && ev.candidate.candidate.indexOf('endOfCandidates') > 0)) {
                        RippleSDK.log('End of candidates.');
                    } else {

                        const payload = {
                            message      : 'icecandidate',
                            candidate    : ev.candidate.candidate,
                            sdpMid       : ev.candidate.sdpMid,
                            sdpMLineIndex: ev.candidate.sdpMLineIndex
                        }
                        if (!RippleSDK.isWebSocketAccess) {
                            let featureResourceUrl = '';
                            if (RippleSDK.isDebugSession) {
                                RippleSDK.log('onicecandidate  payload ', payload);
                            } else {
                                RippleSDK.info('onicecandidate  payload ', payload);
                            }
                            const body = {
                                clientID    : RippleSDK.serverClientId,
                                iceCandidate: payload,

                            };

                            if (RippleSDK.app.featuresInUse === 'G_STREAM') {
                                featureResourceUrl = 'streams/update-ice-candidate';
                            }
                            if (RippleSDK.app.featuresInUse === 'VIDEO_ROOM') {
                                body.roomID        = RippleSDK.app.feature.videoRoom.room.roomID;
                                featureResourceUrl = 'video/update-ice-candidate';
                            }

                            const reqst = RippleSDK.Utils.fetchWithTimeout(featureResourceUrl, {
                                method: 'POST',
                                body
                            });

                            if (!RippleSDK.app.webRTC.wasOfferSentSuccessfully) {
                                //! ToDo deprecated!
                                RippleSDK.app.webRTC.runPeerConnectionDelayedIceJobPayloadsArray.push(reqst);
                                RippleSDK.app.webRTC.runPeerConnectionDelayedIceJobs();
                            } else {

                                const post = await reqst;
                                RippleSDK.log("qqq", post)
                                if (RippleSDK.isDebugSession) {
                                    RippleSDK.log('fetchWithTimeout post  ', post);
                                } else {
                                    RippleSDK.info('fetchWithTimeout post  ', post);
                                }
                            }
                        }else{
                            RippleSDK.app.rootCallbacks.websockets.tellClientOnMessage({
                                type:'background',isGettingStreams:false,showLoadingUI:true,
                            });
                            payload.requestType='update-ice-candidate';
                            RippleSDK.Utils.webSocketSendAction(payload);
                        }

                    }

                };
                RippleSDK.app.webRTC.peerConnection.oniceconnectionstatechange = ev => RippleSDK.log('make a peer oniceconnectionstatechange ...');
                RippleSDK.app.webRTC.peerConnection.onnegotiationneeded = ev => RippleSDK.log('make a peer onnegotiationneeded ...');

                RippleSDK.app.webRTC.peerConnection.ontrack = ev => {
                    // this will be used to render remote peers track audio and video
                    RippleSDK.log('onTrack event ', ev);
                    if(RippleSDK.app.featuresInUse==='G_STREAM') {
                        RippleSDK.app.rootCallbacks.websockets.tellClientOnMessage({
                            type:'stream',isGettingStreams:true,showLoadingUI:false,
                        });
                        const localVideo     = document.getElementById(RippleSDK.app.feature.videoRoom.loadMyLocalVideoObjectID);
                       /* if (ev.track.kind === 'audio') {
                            localVideo.style = 'display: none;';
                        }*/
                        localVideo.srcObject = new MediaStream([ev.track]);
                    }
                };
            }

        },

    },
    Utils: {
        convertToWebSocketUrl:url=>{
            if (url.startsWith('https://')) {
                return url.replace('https://', 'wss://');
            } else if (url.startsWith('http://')) {
                return url.replace('http://', 'ws://');
            } else {
                return url;
            }
        },
        convertFromWebSocketUrl:url=>{
            if (url.startsWith('wss://')) {
                return url.replace('wss://', 'https://');
            } else if (url.startsWith('ws://')) {
                return url.replace('ws://', 'http://');
            } else {
                return url;
            }
        },
        webRTCAdapter: deps => (deps && deps.adapter) || adapter,
        webSocketObject: null,
        webSocketSendAction: messageObject=>{
            if(!RippleSDK.Utils.webSocketObject){
                RippleSDK.error("Failed to act websocket is null" , RippleSDK.Utils.webSocketObject);
                return;
            }
            if (RippleSDK.Utils.webSocketObject.readyState === WebSocket.OPEN) {
                RippleSDK.Utils.webSocketObject.send(JSON.stringify(messageObject));
            }else{
                RippleSDK.error("WebSocket connection is not open yet." , RippleSDK.Utils.webSocketObject);
            }
        },
        webSocket:()=>{
            RippleSDK.serverUrl = RippleSDK.Utils.convertToWebSocketUrl(RippleSDK.serverUrl);
            RippleSDK.app.rootCallbacks.websockets.onConnecting();
            let reconnectAttempts    = 0;
            let maxReconnectAttempts    = 200
            const reconnectDelays = [10, 20, 35, 45, 55]; // delays in seconds
            const socketObject    = new WebSocket(`${RippleSDK.serverUrl}/client-access/${RippleSDK.serverClientId}/${RippleSDK.app.featuresInUse}`);

            if (socketObject) {
                socketObject.onopen    = () => {
                    reconnectAttempts = 0; // reset the reconnect attempts counter
                    RippleSDK.log("Connected to the server via a websocket transport")
                    RippleSDK.app.rootCallbacks.websockets.onConnected();
                };
                socketObject.onerror   = ev => {
                RippleSDK.app.rootCallbacks.websockets.fatalError(ev);
                };
                socketObject.onmessage = ev => {
                    console.log(
                        ev.data
                    );
                    RippleSDK.app.rootCallbacks.websockets.onMessage(ev.data);
                };
                socketObject.onclose   = (ev) => {
                    RippleSDK.log(`WebSocket closed with code ${ev.code} and reason ${ev.reason}`);
                    if (reconnectAttempts < maxReconnectAttempts) {
                        const delay = reconnectDelays[Math.min(reconnectAttempts, reconnectDelays.length - 1)];
                        RippleSDK.log(`Attempting to reconnect in ${delay} seconds...`);
                        setTimeout(() => {
                            reconnectAttempts++;
                          RippleSDK.Utils.webSocket();
                        }, delay * 1000); // convert delay to milliseconds
                    } else {
                        RippleSDK.log(`Maximum reconnection attempts (${maxReconnectAttempts}) reached, giving up.`);
                    }
                    RippleSDK.app.rootCallbacks.websockets.onClosed();
                };
            }
            RippleSDK.Utils.webSocketObject=socketObject;
        },
        fetchWithTimeout: async (url, options = {}) => {
            const {timeout = 8000} = options;

            if (options.method === 'POST') {
                options.headers = {
                    'Content-Type': 'application/json',
                }
                if(!options.body.clientID){
                    options.body.clientID=RippleSDK.serverClientId
                }
                options.body.timeStamp  = new Date().getTime();
                options.body.timeZone   = RippleSDK.timeZone;
                options.body.serverName = RippleSDK.serverName;
                options.body            = JSON.stringify(options.body);
            }

            const controller = new AbortController();
            const id = setTimeout(() => controller.abort(), timeout);
            const _urlRoot = RippleSDK.isWebSocketAccess? RippleSDK.Utils.convertFromWebSocketUrl(RippleSDK.serverUrl) :RippleSDK.serverUrl ;

            try {
                const response = await fetch(`${_urlRoot}/${url}`, {
                    ...options,
                    signal: controller.signal,
                });

                clearTimeout(id);

                if (!response.ok) {
                    RippleSDK.warn("An error has occurred")
                    RippleSDK.app.rootCallbacks.networkError(`HTTP error! Status: ${response.status}`);
                    // throw new Error(`HTTP error! Status: ${response.status}`);
                }

                const json = await response.json();
                if (json.serverName) {
                    RippleSDK.serverName = json.serverName;
                }
                RippleSDK.log('json-ttp', json)
                // process the response
                if (json.success) {
                    if (json.data.type === 'icecandidate') {
                        RippleSDK.app.rootCallbacks.icecandidate(json.data.candidate)
                    }
                }
                return json;
            } catch (error) {
                RippleSDK.app.rootCallbacks.networkError(`Request failed: ${error.message}`);
                clearTimeout(id);
                // throw new Error(`Request failed: ${error.message}`);
            }
        },
        isChromeOrFirefox: () => {
            const userAgent = navigator.userAgent.toLowerCase();

            if (userAgent.includes("firefox")) {
                RippleSDK.clientTypeInUse = 'Firefox';
                return "Firefox";
            } else if (userAgent.includes("chrome") || userAgent.includes("chromium")) {
                RippleSDK.clientTypeInUse = 'Chrome';
                return "Chrome";
            }

            return "Unknown";
        },
        uniqueIDGenerator: (seed = '', maxSize = 22) => {
            const alphabet       = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
            const date           = new Date();
            const timeString    = `${date.getHours()}${date.getMinutes()}${date.getSeconds()}`.padStart(6, '0');
            const dateTimeString = `${seed}${timeString}${Math.random().toString(36).substr(2, 4)}`.slice(0, 12);
            let uniID            = '';
            for (let i = 0; i < maxSize; i++) {
                uniID += alphabet.charAt(Math.floor(Math.random() * alphabet.length));
            }
            for (let i = 0; i < dateTimeString.length; i++) {
                const index = parseInt(dateTimeString.charAt(i), 36);
                uniID = uniID.substr(0, index) + dateTimeString.charAt(i) + uniID.substr(index + 1);
            }
            return uniID;
        },
        getOnlyQueryParamFromUrl: (variable) => {
            const queryString = window.location.search;
            const urlParams   = new URLSearchParams(queryString);
            return urlParams.get(variable);
        },
        isSecureAccess                  : () => location.protocol === 'https:' || new URL(RippleSDK.serverUrl).protocol === 'wss:',
        isWebRTCSupported               : () => !!window.RTCPeerConnection,
        canAccessMedia                  : () => !!navigator.mediaDevices && !!navigator.mediaDevices.getUserMedia,
        onRemoteSDPReady                : () => {
        },
        onAccessMediaAllowedNotification: (mediaStream, wasAudioAllowed, wasVideoAllowed) => {
        },
        myMedia                         : null,
        stopMyLocalMediaAccess: () => {
            if (RippleSDK.Utils.myMedia) {
                RippleSDK.Utils.myMedia.getTracks().forEach(track => track.stop());
            }
            const localVideo = document.getElementById(RippleSDK.app.feature.videoRoom.loadMyLocalVideoObjectID);

            localVideo.srcObject = null;
            RippleSDK.log("Stopped capturing my media , at there is no more rendering")
        },
        testMediaAccess: () => {
            if(RippleSDK.app.featuresInUse !=='G_STREAM'){
                RippleSDK.error("Failed to test please set the feature in use first")
                if(RippleSDK.isDebugSession){
                    alert("Set feature in use first before using media")
                    return
                }
            }
            let config = {audio: false, video: false};
            if (RippleSDK.isAudioAccessRequired) {
                config.audio = true;
            }
            if (RippleSDK.isVideoAccessRequired) {
                config.video = true;
            }
            navigator.mediaDevices.getUserMedia(config)
                .then(devices => RippleSDK.app.rootCallbacks.devices(devices))
                .catch(e => {
                    RippleSDK.error(e);
                    RippleSDK.app.rootCallbacks.devices(null);
                })

        },
        getMediaDevices: async () => {
            
            try {
                const devices            = await navigator.mediaDevices.enumerateDevices();
                const audioOutputDevices = devices.filter(device => device.kind === 'audiooutput');
                const audioInputDevices  = devices.filter(device => device.kind === 'audioinput');
                const videoInputDevices  = devices.filter(device => device.kind === 'videoinput');

                return {
                    audioOutputDevices,
                    audioInputDevices,
                    videoInputDevices
                };
            } catch (error) {
                throw new Error(`Failed to retrieve media devices: ${error.message}`);
            }
        },
        makeRandomNumber: (minValue = 1, maxValue = 1000) => Math.floor(Math.random() * (maxValue - minValue + 1)) + minValue,
        replaceAll: (f, r) => this.split(f).join(r),
        capitaliseTextFirstLetter: word => word.charAt(0).toUpperCase() + word.slice(1),
        capitaliseTextFirstCaseForWords: (text) => {
            if (!text || text.length < 1) {
                return text;
            }
            let firstLtr = 0;
            for (let i = 0; i < text.length; i++) {
                if (i === 0 && /[a-zA-Z]/.test(text.charAt(i)))
                    firstLtr = 2;
                if (firstLtr === 0 && /[a-zA-Z]/.test(text.charAt(i)))
                    firstLtr = 2;
                if (firstLtr === 1 && /[^a-zA-Z]/.test(text.charAt(i))) {
                    if (text.charAt(i) === "'") {
                        if (i + 2 === text.length && /[a-zA-Z]/.test(text.charAt(i + 1))) firstLtr = 3;
                        else if (i + 2 < text.length && /[^a-zA-Z]/.test(text.charAt(i + 2))) firstLtr = 3;
                    }
                    if (firstLtr === 3) firstLtr = 1;
                    else firstLtr = 0;
                }
                if (firstLtr === 2) {
                    firstLtr = 1;
                    text = text.substr(0, i) + text.charAt(i).toUpperCase() + text.substr(i + 1);
                } else {
                    text = text.substr(0, i) + text.charAt(i).toLowerCase() + text.substr(i + 1);
                }
            }
            return text;
        }
    },
    init: (isDebugging) => {
        RippleSDK.app.notifications = [];// resets the array
        const protocol = new URL(RippleSDK.serverUrl).protocol;
        if (protocol === 'http:' || protocol === 'https:') {
            RippleSDK.isWebSocketAccess = false;
        } else if (protocol === 'ws:' || protocol === 'wss:') {
            RippleSDK.isWebSocketAccess = true;
        }
        RippleSDK.Utils.isChromeOrFirefox();
        if (isDebugging) {
            RippleSDK.isDebugSession = true
            if (!RippleSDK.Utils.isWebRTCSupported()) {
                alert("Webrtc is not supported,So this SDK is useless")
            }
        }
        RippleSDK.clientID = RippleSDK.Utils.uniqueIDGenerator()
    }
};
