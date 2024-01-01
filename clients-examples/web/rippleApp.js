'use strict';
const RippleSDK = {
    serverUrl                   : '',
    serverName                   : '',
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
                    clientID: RippleSDK.clientID,
                    requestType: 'remember',
                    isDebugSession: RippleSDK.isDebugSession,
                };
                RippleSDK.transports.websocket.webSocketSendAction(body);

            }, RippleSDK.app.remindServerTimeoutInSeconds *1000);


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
                const messageType = messageObject.messageType;

            },
            tellClientOnConnected:null,
            onConnected:()=>{
                RippleSDK.utils.log('onConnected');
                RippleSDK.app.callbacks.tellClientOnConnected();
            },
            tellClientOnClosed:null,
            onClosed:()=>{
                RippleSDK.utils.log('onClosed');
                RippleSDK.app.callbacks.tellClientOnClosed();
            },
            onConnecting:()=>{
                RippleSDK.utils.log('onConnecting');
                //Todo should be able to tell client this is connecting to the server
            },
            tellClientOnFatalError:null,
            networkError:err=>{
                RippleSDK.utils.log('networkError', err);
                RippleSDK.app.callbacks.tellClientOnFatalError(err);
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
                    RippleSDK.utils.log('webSocketSendAction', 'ready');
                    RippleSDK.transports.websocket.socket.send(JSON.stringify(messageObject));
                }

            },
            connect: () => {
                let reconnectAttempts       = 0;
                const maxReconnectAttempts = 200;
                const reconnectDelays       = [10, 20, 35, 45, 55]; // delays in seconds
                RippleSDK.transports.websocket.socket = new WebSocket(RippleSDK.serverUrl);
                RippleSDK.transports.websocket.socket.onopen = () => {
                    RippleSDK.transports.websocket.isConnected = true;
                    RippleSDK.transports.websocket.socket.send(JSON.stringify({
                        clientID: RippleSDK.clientID,
                        requestType: 'connect',
                    }));
                    RippleSDK.app.callbacks.onConnected();
                };
                RippleSDK.transports.websocket.socket.onmessage = (event) => {
                    const data = JSON.parse(event.data);
                    RippleSDK.utils.log('onmessage', data);
                    if (data.requestType === 'connect') {
                        RippleSDK.utils.log('onmessage', 'connected');
                        RippleSDK.app.startToRemindServerOfMe();
                    }
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
            const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
            return `${protocol}://${window.location.host}${url}`;
        },
        convertFromWebSocketUrl: (url) => {
            const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
            return url.replace(`${protocol}://${window.location.host}`, '');
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
            return uniID;
        },
    },
    init              : {},
    notificationsTypes: {},

};


// RippleSDK.log('RippleSDK loaded');
RippleSDK.utils.log('This is a custom log message');























