package africa.jopen.controllers;

import africa.jopen.utils.ConnectionsManager;
import com.google.common.flogger.FluentLogger;
import jakarta.inject.Inject;

public class GStreamingController {
    @Inject
    private ConnectionsManager connectionsManager;

    private  String sessionId;
}
