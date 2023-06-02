package africa.jopen.app;


import africa.jopen.models.configs.main.MainConfigModel;
import africa.jopen.utils.XUtils;
import com.google.common.flogger.FluentLogger;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;
import java.nio.file.Paths;
import java.util.logging.Level;


public class Main implements QuarkusApplication {
    @ConfigProperty(name = "app.config.folderName")
    String configName;
    private final FluentLogger logger = FluentLogger.forEnclosingClass();


    // @Override
    public int run(String... args) throws Exception {
        XUtils.BASE_APP_LOCATION_PATH = Paths.get("").toAbsolutePath().toString();

        logger.atInfo().log("Server Started");
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


        Quarkus.waitForExit();
        return 0;
    }
}
