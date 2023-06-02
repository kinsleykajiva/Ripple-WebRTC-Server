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



public class Main  implements QuarkusApplication  {
    @ConfigProperty(name = "app.config.folderName")
    String configName;
    private  final FluentLogger logger = FluentLogger.forEnclosingClass();


   // @Override
    public int run(String... args) throws Exception {
        XUtils.BASE_APP_LOCATION_PATH =  Paths.get("").toAbsolutePath().toString();

        logger.atInfo().log("Server Started");
        logger.atInfo().log("App running in folder path " +  XUtils.BASE_APP_LOCATION_PATH );
        logger.atInfo().log("App running in config folder path " +  XUtils.BASE_APP_LOCATION_PATH +"/" + configName );

        File file ;
        try {
            // load the maincofig
            file = new File(XUtils.BASE_APP_LOCATION_PATH + "/" + configName + "/mainConfig.json");
            ObjectMapper objectMapper = new ObjectMapper();
            MainConfigModel mainConfig = objectMapper.readValue(file, MainConfigModel.class);

            // Access the loaded object
            System.out.println("appName: " + mainConfig.appName());
            System.out.println("serverTimeZone: " + mainConfig.serverTimeZone());
        } catch (Exception e) {
            e.printStackTrace();
        }


        Quarkus.waitForExit();
        return 0;
    }
}
