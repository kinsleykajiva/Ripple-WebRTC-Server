'use strict';
const RippleSDK = {
    serverUrl                   : '',
    timeZone                    : Intl.DateTimeFormat().resolvedOptions().timeZone,
    clientID                    : '',
    isDebugSession              : false,
    app               : {
        iceServerArray              : [{urls: 'stun:stun.l.google.com:19302'},{urls: "stun:stun.services.mozilla.com"}],
        isAudioAccessRequired       : false,
        isVideoAccessRequired       : false,
        hasAccessToVideoPermission  : false,
        hasAccessToAudioPermission  : false,
        maxRetries: 10,
        remindServerTimeoutInSeconds: 26 ,
        reminderInterval  : null,
        startToRemindServerOfMe:()=>{
            RippleSDK.utils.log('startToRemindServerOfMe');
            RippleSDK.app.reminderInterval = setInterval(()=>{
                const body =  {
                    clientID: RippleSDK.serverClientId,
                    requestType: '',
                };
            }, RippleSDK.app.remindServerTimeoutInSeconds *1000);

        },
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
                    RippleSDK.utils.log('webSocketSendAction', 'ready');
                    RippleSDK.transports.websocket.socket.send(JSON.stringify(messageObject));
                }

            },
            connect: () => {
                RippleSDK.transports.websocket.socket = new WebSocket(RippleSDK.serverUrl);
                RippleSDK.transports.websocket.socket.onopen = () => {
                    RippleSDK.transports.websocket.isConnected = true;
                    RippleSDK.transports.websocket.socket.send(JSON.stringify({
                        clientID: RippleSDK.clientID,
                        requestType: 'connect',
                    }));
                };
                RippleSDK.transports.websocket.socket.onmessage = (event) => {
                    const data = JSON.parse(event.data);
                    RippleSDK.utils.log('onmessage', data);
                    if (data.requestType === 'connect') {
                        RippleSDK.utils.log('onmessage', 'connected');
                        RippleSDK.app.startToRemindServerOfMe();
                    }
                };
                RippleSDK.transports.websocket.socket.onclose = () => {
                    RippleSDK.transports.websocket.isConnected = false;
                    RippleSDK.utils.log('onclose');
                };
                RippleSDK.transports.websocket.socket.onerror = (error) => {
                    RippleSDK.utils.log('onerror', error);
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
            return uniID;
        },
    },
    init              : {},
    notificationsTypes: {},

};


// RippleSDK.log('RippleSDK loaded');
RippleSDK.utils.log('This is a custom log message');























