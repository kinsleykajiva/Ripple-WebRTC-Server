package africa.jopen.ripple.models;

public record MainConfigModel(String appName, boolean isDebugMode, int serverPort,
                              int serverSSLPort, String serverName, String serverVersion,
                              String serverTimeZone, BasicAuth basicAuth, Session session, Nat nat, Logs logs,
                              Certificates certificates , AdminMonitor  adminMonitor , StoragePath storagePath


) {
}
