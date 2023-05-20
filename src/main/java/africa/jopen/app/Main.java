package africa.jopen.app;


import com.google.common.flogger.FluentLogger;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.runtime.annotations.QuarkusMain;

import java.util.logging.Level;



public class Main /* implements QuarkusApplication*/ {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();


  


   // @Override
    public int run(String... args) throws Exception {

        logger.atInfo().log("xxxxxxxxxxxxxxxxxxxxxxxxxxRun(String... args)  ");
        logger.at(Level.ALL).log("ccccccccccccccccccccccccccccccccccccccccRun.ALL (String... args)  ");

        return 0;
    }
}
