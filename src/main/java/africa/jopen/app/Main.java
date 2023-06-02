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
        logger.atInfo().log("Server Started");
        Quarkus.waitForExit();
        return 0;
    }
}
