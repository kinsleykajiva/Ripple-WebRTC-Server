package africa.jopen.ripple;

import africa.jopen.ripple.utils.MessageQueue;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.apache.log4j.Logger;

public class MessageQueueService implements HttpService {
	static        org.apache.log4j.Logger log          = Logger.getLogger(MessageQueueService.class.getName());
	private final MessageQueue            messageQueue = MessageQueue.instance();
	
	
	
	@Override
	public void routing(HttpRules routingRules) {
		routingRules.post("/board", this::handlePost);
	}
	
	private void handlePost(ServerRequest request, ServerResponse response) {
		messageQueue.push(request.content().as(String.class));
		response.status(204).send();
	}
}