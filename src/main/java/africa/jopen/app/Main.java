package africa.jopen.app;


import africa.jopen.events.EventService;
import africa.jopen.utils.BannerTxT;
import africa.jopen.services.GeneralService;
import africa.jopen.services.SimpleGreetService;
import africa.jopen.services.GreetService;
import africa.jopen.services.VideoRoomService;
import africa.jopen.utils.ConnectionsManager;
import com.google.common.flogger.FluentLogger;
import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.media.common.MessageBodyReader;
import io.helidon.media.common.MessageBodyReaderContext;
import io.helidon.media.jsonb.JsonbSupport;
import io.helidon.media.jsonp.JsonpSupport;
import io.helidon.metrics.MetricsSupport;
import io.helidon.health.HealthSupport;
import io.helidon.health.checks.HealthChecks;
import io.helidon.common.LogConfig;
import io.helidon.common.reactive.Single;
import io.helidon.config.Config;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;

import java.util.concurrent.Flow;


/**
 * The application main class.
 */
public final class Main {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    /**
     * Cannot be instantiated.
     */
    private Main() {
    }
   
    public static void main(final String[] args) {
        startServer();
    }

    /**
     * Start the server.
     */
     public static Single<WebServer> startServer() {

        // load logging configuration
        LogConfig.configureRuntime();

        // By default this will pick up application.yaml from the classpath
        Config config = Config.create();

        WebServer server = WebServer.builder(createRouting(config))
                .config(config.get("server"))
                .addMediaSupport(JsonpSupport.create())
                .addMediaSupport(JsonbSupport.create())
                .build();

        Single<WebServer> webserver = server.start();

        // Try to start the server. If successful, print some info and arrange to
        // print a message at shutdown. If unsuccessful, print the exception.
        webserver.forSingle(ws -> {
           //System.out.println("WEB server is up! http://localhost:" + ws.port() + "/greet");
           System.out.println(BannerTxT.BANNER_TEXT);
           logger.atInfo().log("WEB server is up! http://localhost:" + ws.port());
            ws.whenShutdown().thenRun(() -> System.out.println("WEB server is DOWN. Good bye!"));
        })
        .exceptionallyAccept(t -> {
            System.err.println("Startup failed: " + t.getMessage());
            t.printStackTrace(System.err);
        });
        return webserver;

    }

    /**
     * Creates new {@link Routing}.
     * @return routing configured with JSON support, a health check, and a service
     * @param config configuration of this server
     */
    private static Routing createRouting(Config config) {
        ConnectionsManager connectionsManager = ConnectionsManager.getInstance();
        EventService eventService = new EventService(); // Create an instance of EventService

        SimpleGreetService simpleGreetService = new SimpleGreetService(config);
        GreetService greetService = new GreetService(config);
      
        
        
        HealthSupport health = HealthSupport.builder()
                .addLiveness(HealthChecks.healthChecks()) // Adds a convenient set of checks
                .build();

        Routing.Builder builder = Routing.builder()
                .register(MetricsSupport.create()) // Metrics at "/metrics"
                .register(health) // Health at "/health"
                .register("/app", new GeneralService(connectionsManager, eventService))
                .register("/video", new VideoRoomService(connectionsManager, eventService))
                .register("/simple-greet", simpleGreetService)
                .register("/greet", greetService);


        return builder.build();
    }
}
