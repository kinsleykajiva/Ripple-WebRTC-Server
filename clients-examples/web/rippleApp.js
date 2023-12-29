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
    },
    utils             : {
        log               : console.log.bind(console),
        error             : console.error.bind(console),
        warn              : console.warn.bind(console),
        debug             : console.debug.bind(console),
        info              : console.info.bind(console),
        trace             : console.trace.bind(console),
        assert            : console.assert.bind(console),
    },
    init              : {},
    notificationsTypes: {},

};


// RippleSDK.log('RippleSDK loaded');
RippleSDK.utils.log('This is a custom log message');























