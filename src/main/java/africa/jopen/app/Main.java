package africa.jopen.app;


import com.google.common.flogger.FluentLogger;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import org.eclipse.microprofile.config.inject.ConfigProperty;


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
