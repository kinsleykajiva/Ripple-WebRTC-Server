
package africa.jopen.ripple.app;


import africa.jopen.ripple.MessageQueueService;
import africa.jopen.ripple.models.MainConfigModel;
import africa.jopen.ripple.plugins.SipUserAgentPlugin;
import africa.jopen.ripple.services.GreetService;
import africa.jopen.ripple.sockets.WebsocketEndpoint;
import africa.jopen.ripple.utils.ConnectionsManager;
import africa.jopen.ripple.utils.XUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.config.Config;
import io.helidon.logging.common.LogConfig;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.websocket.WsRouting;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.mjsip.config.OptionParser;
import org.mjsip.pool.PortConfig;
import org.mjsip.sip.address.SipURI;
import org.mjsip.sip.provider.SipConfig;
import org.mjsip.sip.provider.SipProvider;
import org.mjsip.time.ConfiguredScheduler;
import org.mjsip.time.SchedulerConfig;
import org.mjsip.ua.MediaConfig;
import org.mjsip.ua.UAConfig;
import org.mjsip.ua.UIConfig;
import org.zoolu.net.IpAddress;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZonedDateTime;

import static africa.jopen.ripple.utils.XUtils.copyConfigFilesTemplates;
import static africa.jopen.ripple.utils.XUtils.generateJwtKeys;


/**
 * The application main class.
 */
public class Main {
	
	static Logger log = Logger.getLogger(Main.class.getName());
	
	/**
	 * Cannot be instantiated.
	 */
	private Main() {
	}
	
	
	/**
	 * Application main entry point.
	 *
	 * @param args command line arguments.
	 */
	public static void main(String[] args) {
		log.info("Starting server...");
		var connectionsManager = ConnectionsManager.getInstance();
		connectionsManager.setBANNER();
		// load logging configuration
		LogConfig.configureRuntime();
		
		// initialize global config from default configuration
		Config config = Config.create();
		Config.global(config);
		final String configName = config.get("app.config.folderName").asString().orElse("config");
		XUtils.BASE_APP_LOCATION_PATH = Paths.get("").toAbsolutePath().toString();
		
		
		log.info("App running in folder path " + XUtils.BASE_APP_LOCATION_PATH);
		log.info("App running in config folder path " + XUtils.BASE_APP_LOCATION_PATH + File.separator + configName);
		
		File file;
		try {
			
			file = new File(XUtils.BASE_APP_LOCATION_PATH + File.separator + configName);
			if (!file.exists()) {
				log.log(Level.ERROR, "Configs not found");
				
				Path sourceFolder = Paths.get(XUtils.BASE_APP_LOCATION_PATH + File.separator + configName);
				if (!Files.exists(sourceFolder)) {
					Files.createDirectories(sourceFolder);
				}
				if (!file.exists()) {
					log.log(Level.ERROR, "Configs not found");
					System.exit(1);
				}
			}
		} catch (Exception e) {
			log.log(Level.ERROR, "Configs not found", e);
		}
		
		try {
			
			
			Path sourceFolder      = Paths.get("src/main/resources/configs/");
			Path destinationFolder = Paths.get(XUtils.BASE_APP_LOCATION_PATH + File.separator + configName);
			
			copyConfigFilesTemplates(sourceFolder, destinationFolder);
			
			file = new File(XUtils.BASE_APP_LOCATION_PATH + File.separator + configName + File.separator + "ripple.json");
			if (!file.exists()) {
				log.log(Level.ERROR, "ripple .json not found");
				log.log(Level.ERROR, "Exiting System");
				System.exit(1);
			}
			
			ObjectMapper objectMapper = new ObjectMapper();
			XUtils.MAIN_CONFIG_MODEL = objectMapper.readValue(file, MainConfigModel.class);
		} catch (Exception e) {
			log.log(Level.ERROR, "mainConfig .json not found", e);
		}
		
		if (XUtils.MAIN_CONFIG_MODEL.certificates().certFilePath().isEmpty()) {
			log.info("No certificates set , attempting to set this up !");
			final String privateKey = "privateKey.pem";
			final String publicKey  = "publicKey.pem";
			
			if (!new File(XUtils.BASE_APP_LOCATION_PATH + File.separator + configName + File.separator + "keys").exists()) {
				new File(XUtils.BASE_APP_LOCATION_PATH + File.separator + configName + File.separator + "keys").mkdir();
			}
			
			Path privateKeyPath = new File(XUtils.BASE_APP_LOCATION_PATH + File.separator + configName + File.separator + "keys" + File.pathSeparator + privateKey).toPath();
			Path publicKeyPath  = new File(XUtils.BASE_APP_LOCATION_PATH + File.separator + configName + File.separator + "keys" + File.pathSeparator + publicKey).toPath();
			
			if (Files.notExists(privateKeyPath)) {
				try {
					generateJwtKeys(privateKeyPath, publicKeyPath);
				} catch (Exception e) {
					log.log(Level.ERROR, "Error " + e.getMessage(), e);
				}
			} else {
				// create a new one every day
				Instant retentionPeriod = ZonedDateTime.now().minusDays(1).toInstant();
				try {
					if (Files.getLastModifiedTime(privateKeyPath).toInstant()
							.isBefore(retentionPeriod)) {
						generateJwtKeys(privateKeyPath, publicKeyPath);
					}
				} catch (Exception e) {
					log.log(Level.ERROR, "Error " + e.getMessage(), e);
				}
			}
			
			// restart the server
			// ToDo review how this is not better.
			//server.stop();
		}
        
  
		System.setProperty("server.port", String.valueOf(XUtils.MAIN_CONFIG_MODEL.serverPort()));
		
		
		config.get("server").asNode().ifPresent(node -> node.get("port").asInt().ifPresent(port -> System.setProperty("server.port", String.valueOf(XUtils.MAIN_CONFIG_MODEL.serverPort()))));
		
		MessageQueueService messageQueueService = new MessageQueueService();
		WebsocketEndpoint   websocketEndpoint   = new WebsocketEndpoint();
		
		WebServer server = WebServer.builder()
				.config(config.get("server"))
				.routing(Main::routing)
				.port(XUtils.MAIN_CONFIG_MODEL.serverPort())
				.addRouting(WsRouting.builder().endpoint("/websocket/client", websocketEndpoint))
				.build()
				.start();
		
		websocketEndpoint.startOrphansCron();
		
		log.info("WEB server is up! http://localhost:" + server.port());
		
		
		/*SipConfig    sipConfig = new SipConfig();
		sipConfig.setHostPort(9099);
		
		UAConfig        uaConfig        = new UAConfig();
		uaConfig.setAuthRealm("Asterisk");
		uaConfig.setUser("kinsl1ey-kaji1va1");
		uaConfig.setDisplayName("kinsley-kajiva");
		sipConfig.setViaAddr("vaef2ey.com2monreso2lve.com");
		uaConfig.setAuthPasswd("Ch1bhodhor0");
		uaConfig.setRegistrar(new SipURI("veaf2ey.com2monreso2lve.com",9099)); // Set the SIP server address
		SchedulerConfig schedulerConfig = new SchedulerConfig();
		MediaConfig     mediaConfig     = new MediaConfig();
		PortConfig      portConfig      = new PortConfig();
		UIConfig        uiConfig        = new UIConfig();
		sipConfig.normalize();
		uaConfig.normalize(sipConfig);
		ConfiguredScheduler configuredScheduler = new ConfiguredScheduler(schedulerConfig);
		var sipPr = new SipProvider(sipConfig,configuredScheduler);
	
		SipUserAgentPlugin sipUserAgentPlugin = new SipUserAgentPlugin(
				sipPr,portConfig.createPool(),uaConfig,uiConfig,mediaConfig
		);
		System.out.println(uaConfig.getProxy());
		System.out.println(uaConfig.getUserURI());*/
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