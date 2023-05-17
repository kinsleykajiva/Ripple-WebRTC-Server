package africa.jopen.app;


import com.google.common.flogger.FluentLogger;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

import java.util.logging.Level;

@QuarkusMain
public class Main  implements QuarkusApplication {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();


    public static void main(String... args) {
        Quarkus.run(Main.class, args);
    }


    @Override
    public int run(String... args) throws Exception {

        logger.atInfo().log("Run(String... args)  ");
        logger.at(Level.ALL).log("Run.ALL (String... args)  ");

        return 0;
    }
}
