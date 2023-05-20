package africa.jopen.schedules;


import africa.jopen.utils.ConnectionsManager;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClientCrons {
	@Inject
	ConnectionsManager connectionsManager;
	
	
	@Scheduled(every = "60s")
	void executeTask() {
		connectionsManager.removeOrphanClients();
	}
	
	@Scheduled(every = "10s")
	void executeCallNotificationsTask() {
		connectionsManager.removeDeadCallNotifications();
	}
	
	
}
