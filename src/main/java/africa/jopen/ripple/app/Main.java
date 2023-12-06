
package africa.jopen.ripple.app;


import africa.jopen.ripple.MessageBoardEndpoint;
import africa.jopen.ripple.MessageQueueService;
import africa.jopen.ripple.services.GreetService;
import africa.jopen.ripple.utils.LoggerConfig;
import io.helidon.logging.common.LogConfig;
import io.helidon.config.Config;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.websocket.WsRouting;

import java.util.logging.Logger;


/**
 * The application main class.
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
   
    /**
     * Cannot be instantiated.
     */
    private Main() {
    }


    /**
     * Application main entry point.
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        LoggerConfig.setupLogger(LOGGER);
        // load logging configuration
        LogConfig.configureRuntime();

        // initialize global config from default configuration
        Config config = Config.create();
        Config.global(config);
        
        MessageQueueService messageQueueService = new MessageQueueService();
        WebServer server = WebServer.builder()
                .config(config.get("server"))
                .routing(Main::routing)
                .addRouting(WsRouting.builder()
                        .endpoint("/websocket/client", new MessageBoardEndpoint())
                        
                )
                .build()
                .start();
        
        LOGGER.info("WEB server is up! http://localhost:" + server.port() + "/simple-greet");
        
    }


    /**
     * Updates HTTP Routing.
     */
    static void routing(HttpRouting.Builder routing) {
        MessageQueueService messageQueueService = new MessageQueueService();
        
        
        routing
               .register("/greet", new GreetService())
               .register("/rest", messageQueueService)
               
               .get("/simple-greet", (req, res) -> res.send("Hello World!")
               );
    }
}