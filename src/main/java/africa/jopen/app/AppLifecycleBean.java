package africa.jopen.app;

import com.google.common.flogger.FluentLogger;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
@ApplicationScoped
public class AppLifecycleBean {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    void onStart(@Observes StartupEvent ev) {
        logger.atInfo().log("The application is starting...");
    }

    void onStop(@Observes ShutdownEvent ev) {
        logger.atInfo().log("The application is stopping...");
    }

}
