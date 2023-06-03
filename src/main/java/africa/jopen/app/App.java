package africa.jopen.app;


import africa.jopen.models.configs.main.MainConfigModel;
import africa.jopen.utils.XUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.flogger.FluentLogger;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Base64;


@QuarkusMain
public class App {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    public static void main(String... args) {
        logger.atInfo().log("Server Starting");
        Config configNameConfig = ConfigProvider.getConfig();
        final String configName = configNameConfig.getValue("app.config.folderName", String.class);

        XUtils.BASE_APP_LOCATION_PATH = Paths.get("").toAbsolutePath().toString();

        logger.atInfo().log("App running in folder path " + XUtils.BASE_APP_LOCATION_PATH);
        logger.atInfo().log("App running in config folder path " + XUtils.BASE_APP_LOCATION_PATH + "/" + configName);

        File file;
        try {

            file = new File(XUtils.BASE_APP_LOCATION_PATH + "/" + configName);
            if (!file.exists()) {
                logger.atSevere().log("Configs not found");
                System.exit(1);
            }
        } catch (Exception e) {

            logger.atSevere().withCause(e).log("Configs not found");
        }

        try {
            file = new File(XUtils.BASE_APP_LOCATION_PATH + "/" + configName + "/mainConfig.json");
            if (!file.exists()) {
                logger.atSevere().log("mainConfig .json not found");
                System.exit(1);
            }
            ObjectMapper objectMapper = new ObjectMapper();
            XUtils.MAIN_CONFIG_MODEL = objectMapper.readValue(file, MainConfigModel.class);
        } catch (Exception e) {
            logger.atSevere().withCause(e).log("Failed to process");
        }



        if(XUtils.MAIN_CONFIG_MODEL.certificates().certFilePath().isEmpty()){
           logger.atInfo().log("No certificates set , attempting to set this up !");
           final String privateKey = "privateKey.pem";
           final String publicKey = "publicKey.pem";

           if(!new File(XUtils.BASE_APP_LOCATION_PATH +"/"+configName + "/keys").exists()) {
               new File(XUtils.BASE_APP_LOCATION_PATH + "/" + configName + "/keys").mkdir();
           }

            Path privateKeyPath = new File(XUtils.BASE_APP_LOCATION_PATH + "/" +configName + "/keys/"+privateKey).toPath();
            Path publicKeyPath =  new File(XUtils.BASE_APP_LOCATION_PATH  + "/" + configName + "/keys/"+publicKey).toPath();

            if (Files.notExists(privateKeyPath)) {
                try {
                    generateJwtKeys(privateKeyPath, publicKeyPath);
                }
                catch (Exception e) {
                    logger.atSevere().withCause(e).log("Error " + e.getMessage());
                }
            }else{
                // create a new one every day
                Instant retentionPeriod = ZonedDateTime.now().minusDays(1).toInstant();
                try {
                    if (Files.getLastModifiedTime(privateKeyPath).toInstant()
                            .isBefore(retentionPeriod)) {
                        generateJwtKeys(privateKeyPath, publicKeyPath);
                    }
                }
                catch (Exception e) {
                    logger.atSevere().withCause(e).log("Error " + e.getMessage());
                }
            }

            // restart the server
            // ToDo review how this is not better.
            logger.atInfo().log("Restart the sever if there were no key set foe sever as much!");



        }

        System.setProperty("quarkus.application.name", XUtils.MAIN_CONFIG_MODEL.serverName());
        System.setProperty("quarkus.http.port", String.valueOf(XUtils.MAIN_CONFIG_MODEL.serverPort()));
        System.setProperty("quarkus.http.ssl-port", String.valueOf(XUtils.MAIN_CONFIG_MODEL.serverSSLPort()));
        System.setProperty("quarkus.application.version", XUtils.MAIN_CONFIG_MODEL.serverVersion());
        logger.atInfo().log("Server configs loaded successfully !");
        Quarkus.run(Main.class, args);
    }
    private static void generateJwtKeys(Path privateKeyPath, Path publicKeyPath) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        if (Files.notExists(privateKeyPath.getParent())) {
            Files.createDirectories(privateKeyPath.getParent());
        }

        try (BufferedWriter writer = Files.newBufferedWriter(privateKeyPath,
                StandardCharsets.UTF_8)) {
            writer.write("-----BEGIN PRIVATE KEY-----");
            writer.write(System.lineSeparator());
            writer.write(Base64.getMimeEncoder().encodeToString(kp.getPrivate().getEncoded()));
            writer.write(System.lineSeparator());
            writer.write("-----END PRIVATE KEY-----");
        }

        try (BufferedWriter writer = Files.newBufferedWriter(publicKeyPath,
                StandardCharsets.UTF_8)) {
            writer.write("-----BEGIN PUBLIC KEY-----");
            writer.write(System.lineSeparator());
            writer.write(Base64.getMimeEncoder().encodeToString(kp.getPublic().getEncoded()));
            writer.write(System.lineSeparator());
            writer.write("-----END PUBLIC KEY-----");
        }
    }

}
