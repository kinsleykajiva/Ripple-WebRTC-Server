'use strict';
const RippleSDK = {
    accessPassword: '',
    isAudioAccessRequired: false,
    isVideoAccessRequired: false,
    hasAccessToVideoPermission: false,
    hasAccessToAudioPermission: false,
    serverUrl: '',
    timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    serverName: '',
    clientID: '',
    serverClientId: '',
    serverClientLastSeen: 0,
    clientTypesAllowed: ['Firefox', 'Chrome'],
    clientTypeInUse: '',
    isWebSocketAccess: false,
    isDebugSession: false,
    remindServerTimeoutInSeconds: 26,
    iceServerArray: [],
    messageTypes: ["ack", "request", "message", "symmetricKey"],
    app: {
        featuresAvailable: ["VIDEO_ROOM", "AUDIO_ROOM"],
        featuresInUse: [],
        reminderInterval: null,
        startToRemindServerOfMe: () => {
            RippleSDK.app.reminderInterval = setInterval(() => {
                RippleSDK.Utils.fetchWithTimeout('/app/client/remember', {
                    method: 'POST',
                    body: {clientID: RippleSDK.serverClientId}
                }).then(res => {

                    if (res.success) {
                        console.log("the Server still knows about me ")
                        RippleSDK.serverClientLastSeen = res.data.client.lastSeen;
                        console.log('ice - ',res.data.client.iceCandidates)
                        if(res.data.client.iceCandidates){
                            if(RippleSDK.app.webRTC.peerConnection){
                                RippleSDK.app.webRTC.peerConnection.addIceCandidate(res.data.client.iceCandidates);
                            }

                        }
                    } else {
                        alert("Client not found please re-connect this session is now invalid!")
                        console.error("Client not found please re-connect this session is now invalid!")
                    }
                });
            }, RippleSDK.remindServerTimeoutInSeconds * 1_000);
        },
        stopReminderServer: () => {
            clearInterval(RippleSDK.app.reminderInterval)
        },
        maxRetries: 10,
        rootCallbacks: {
            networkError:error=>{
                console.error(error);
                if(RippleSDK.isDebugSession){
                    alert(error)
                }

            },
            answer: answer => {
                console.log('answer', answer)
            },
            devices: media => {
                console.log('devices ', media)

                if (media && media.active) {
                    RippleSDK.hasAccessToVideoPermission = true;
                    RippleSDK.hasAccessToAudioPermission = true;
                    RippleSDK.Utils.myMedia = media;

                    if (RippleSDK.app.featuresInUse.includes('VIDEO_ROOM')) {
                        if (!RippleSDK.app.feature.videoRoom.loadMyLocalVideoObjectID || RippleSDK.app.feature.videoRoom.loadMyLocalVideoObjectID.length === 0) {
                            console.error("Failed to find Video Rendering html Element ID , Please set {RippleSDK.app.feature.videoRoom.loadMyLocalVideoObjectID}")
                            return;
                        }
                        try {
                            const localVideo = document.getElementById(RippleSDK.app.feature.videoRoom.loadMyLocalVideoObjectID);
                            localVideo.srcObject = media;
                            RippleSDK.Utils.onAccessMediaAllowedNotification(media, true, true)
                        } catch (e) {
                            console.error(e)
                        }

                    }
                } else {
                    RippleSDK.hasAccessToVideoPermission = false;
                    RippleSDK.hasAccessToAudioPermission = false;
                    console.error("Access To Media has been rejected !Vide & wont get captured")
                    RippleSDK.Utils.onAccessMediaAllowedNotification(null, false, false)
                }


            },
            icecandidate: data => {
                console.log('data', data)
            }
        },
        feature: {
            videoRoom: {
                room: {
                    pin: '',
                    roomID: '',
                    roomName: '',
                    maximumCapacity: 0,
                    password: '',
                    createdTimeStamp: 0,
                    roomDescription: '',
                    creatorClientID: ''
                },
                loadMyLocalVideoObjectID: '',
                createRoom: async (roomName, password, pin) => {

                    if (!roomName || roomName === '') {
                        console.error("room name is required");
                        return;
                    }
                    if (!password || password === '') {
                        console.error("room password is required");
                        return;
                    }
                    if (!pin || pin === '') {
                        console.error("room pin is required");
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
                        console.log(result);
                        if (result.success) {
                            RippleSDK.app.feature.videoRoom.room = result.data;
                            console.log('Room', RippleSDK.app.feature.videoRoom.room);
                            return true;
                        }
                        return false;
                    } catch (e) {
                        console.error(e)
                    }

                    return false;
                },
                /*The client or user logic is depended on the initialization of  ${RippleSDK.app.feature.videoRoom.room} object */
                joinRoom: async () => {
                    if (RippleSDK.app.feature.videoRoom.room.roomID.length === 0) {
                        console.error("Please get the room details first!")
                        return
                    }
                    try {
                        const result = await RippleSDK.Utils.fetchWithTimeout('/video/join-room', {
                            method: 'POST',
                            body: {
                                roomID: RippleSDK.app.feature.videoRoom.room.roomID,
                                password: RippleSDK.app.feature.videoRoom.room.password,
                                clientID: RippleSDK.serverClientId /*! this id has to exist , meaning the client has to first connect!*/,
                            }
                        });
                        console.log(result);
                        if (result.success) {

                            return true;
                        }
                        return false;
                    } catch (e) {
                        console.error(e)
                    }

                    return false;
                },
                makeConnection: async () => {

                    try {
                        const result = await RippleSDK.Utils.fetchWithTimeout('/video/connect', {
                            method: 'POST',
                            body: {clientAgentName: RippleSDK.clientID}
                        });
                        console.log(result);
                        RippleSDK.serverClientId = result.data.clientID;
                        RippleSDK.serverClientLastSeen = result.data.lastSeen;
                        // trigger to start the remember-me calls
                        RippleSDK.app.startToRemindServerOfMe();
                        return true;
                    } catch (e) {
                        console.error(e)
                    }

                    return false;

                },
            },
        },
        webRTC: {
            peerConnection: null,
            offerOptions: {offerToReceiveVideo: true, offerToReceiveAudio: true},
            createOffer: () => {
                RippleSDK.app.webRTC.peerConnection.createOffer(RippleSDK.app.webRTC.offerOptions)
                    .then(async _sdp => {
                        await RippleSDK.app.webRTC.peerConnection.setLocalDescription(_sdp);
                        const payload = {

                            sdp: _sdp
                        };
                       // console.log("local peer sdp set")
                       // console.log('2 post payload ', _sdp.sdp);
                        //ToDo this add a condition check on this part as to avoid repeatiton
                        const post = await RippleSDK.Utils.fetchWithTimeout('video/send-offer', {
                            method: 'POST',
                            body: {roomID: RippleSDK.app.feature.videoRoom.room.roomID ,offer : _sdp.sdp,
                                clientID: RippleSDK.serverClientId}
                        });
                        console.log('XXXX post post ', post);
                        RippleSDK.app.webRTC.peerConnection.setRemoteDescription({
                            sdp: post.data.sdp,
                            type: 'answer',
                        });

                    });
            },
            createPeerconnection: () => {
                const configuration = {
                    /* insertableStreams:true,
                     forceEncodedAudioInsertableStreams:true,
                     forceEncodedVideoInsertableStreams:true,*/
                    iceServers: [
                        {urls: 'stun:stun.l.google.com:19302'}
                    ]
                };
                console.log('make a peer connection ...');

                RippleSDK.app.webRTC.peerConnection = new RTCPeerConnection(configuration);
                RippleSDK.app.webRTC.peerConnection.onicecandidate = async ev => {
                    if (!ev.candidate || (ev.candidate.candidate && ev.candidate.candidate.indexOf('endOfCandidates') > 0)) {
                        console.log('End of candidates.');
                    } else {

                        const payload = {
                            message: 'icecandidate',
                            candidate: ev.candidate.candidate,
                            sdpMid: ev.candidate.sdpMid,
                            sdpMLineIndex: ev.candidate.sdpMLineIndex
                        };
                      //  console.log('1 post payload ', payload);
                        console.log('1 ev.candidate', ev.candidate);

                        const post = await RippleSDK.Utils.fetchWithTimeout('video/update-ice-candidate', {
                            method: 'POST',
                            body: {
                                roomID: RippleSDK.app.feature.videoRoom.room.roomID, iceCandidate:payload,
                                clientID: RippleSDK.serverClientId
                            }
                        });
                        console.log('XXXX post post ', post);
                    }

                };
                RippleSDK.app.webRTC.peerConnection.oniceconnectionstatechange = ev => {
                    console.log('make a peer oniceconnectionstatechange ...');

                };
                RippleSDK.app.webRTC.peerConnection.onnegotiationneeded = ev => {
                    console.log('make a peer onnegotiationneeded ...');

                };

                RippleSDK.app.webRTC.peerConnection.ontrack = ev => {
                    // this will be used to render remote peers track audio and video
                    console.log('onTrack event ', ev)

                };

                RippleSDK.app.webRTC.createOffer();


            }

        },

    },
    Utils: {
        webRTCAdapter: deps => (deps && deps.adapter) || adapter,
        fetchWithTimeout: async (url, options = {}) => {
            const {timeout = 8000} = options;

            if (options.method === 'POST') {
                options.headers = {
                    'Content-Type': 'application/json',
                }
                options.body.timeStamp = new Date().getTime();
                options.body.timeZone = RippleSDK.timeZone;
                options.body.serverName =RippleSDK.serverName;
                options.body = JSON.stringify(options.body);
            }

            const controller = new AbortController();
            const id = setTimeout(() => controller.abort(), timeout);

            try {
                const response = await fetch(`${RippleSDK.serverUrl}/${url}`, {
                    ...options,
                    signal: controller.signal,
                });

                clearTimeout(id);

                if (!response.ok) {
                    console.warn("An error has occurred")
                    RippleSDK.app.rootCallbacks.networkError(`HTTP error! Status: ${response.status}`);
                   // throw new Error(`HTTP error! Status: ${response.status}`);
                }

                const json = await response.json();
                console.log('json-ttp', json)
                // process the response
                if (json.success) {
                    if (json.data.type === 'icecandidate') {
                        RippleSDK.app.rootCallbacks.icecandidate(json.data.candidate)
                    }


                }
                return json;
            } catch (error) {
                clearTimeout(id);
                RippleSDK.app.rootCallbacks.networkError(`Request failed: ${error.message}`);
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
            const alphabet = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
            const date = new Date();
            const timeString = `${date.getHours()}${date.getMinutes()}${date.getSeconds()}`.padStart(6, '0');
            const dateTimeString = `${seed}${timeString}${Math.random().toString(36).substr(2, 4)}`.slice(0, 12);
            let uniID = '';
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
            const urlParams = new URLSearchParams(queryString);
            return urlParams.get(variable);
        },
        isSecureAccess: () => location.protocol === 'https:' || new URL(RippleSDK.serverUrl).protocol === 'wss:',
        isWebRTCSupported: () => !!window.RTCPeerConnection,
        canAccessMedia: () => !!navigator.mediaDevices && !!navigator.mediaDevices.getUserMedia,
        onAccessMediaAllowedNotification: (mediaStream, wasAudioAllowed, wasVideoAllowed) => {
        },
        myMedia: null,
        stopMyLocalMediaAccess: () => {
            if (RippleSDK.Utils.myMedia) {
                RippleSDK.Utils.myMedia.getTracks().forEach(track => track.stop());
            }
            const localVideo = document.getElementById(RippleSDK.app.feature.videoRoom.loadMyLocalVideoObjectID);

            localVideo.srcObject = null;
            console.log("Stopped capturing my media , at there is no more rendering")

            //  this.localStream.getTracks().forEach(track => track.stop());
        },
        testMediaAccess: () => {
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
                    console.error(e);
                    RippleSDK.app.rootCallbacks.devices(null);
                })

        },
        getMediaDevices: async () => {
            /*to call this function call it by
                getMediaDevices()
              .then(({ audioOutputDevices, audioInputDevices, videoInputDevices }) => {
                console.log('Audio Output Devices:', audioOutputDevices);
                console.log('Audio Input Devices:', audioInputDevices);
                console.log('Video Input Devices:', videoInputDevices);
              })
              .catch(error => {
                console.error('Error retrieving media devices:', error);
              });

            * */
            try {
                const devices = await navigator.mediaDevices.enumerateDevices();
                const audioOutputDevices = devices.filter(device => device.kind === 'audiooutput');
                const audioInputDevices = devices.filter(device => device.kind === 'audioinput');
                const videoInputDevices = devices.filter(device => device.kind === 'videoinput');

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
        capitaliseTextFirstLetter: (word) => {
            return word.charAt(0).toUpperCase() + word.slice(1);
        },
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
    init: (isDebuging) => {

        const protocol = new URL(RippleSDK.serverUrl).protocol;
        if (protocol === 'http:' || protocol === 'https:') {
            RippleSDK.isWebSocketAccess = false;
        } else if (protocol === 'ws:' || protocol === 'wss:') {
            RippleSDK.isWebSocketAccess = true;
        }
        RippleSDK.Utils.isChromeOrFirefox();
        if (isDebuging) {
            RippleSDK.isDebugSession = true
            if (!RippleSDK.Utils.isWebRTCSupported()) {
                alert("Webrtc is not supported,So this SDK is useless")
            }
        }
        RippleSDK.clientID = RippleSDK.Utils.uniqueIDGenerator()
    }
};
