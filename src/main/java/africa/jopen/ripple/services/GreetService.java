package africa.jopen.ripple.services;

import java.util.concurrent.atomic.AtomicReference;

import africa.jopen.ripple.Message;
import io.helidon.config.Config;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

public class GreetService implements HttpService {

    

    /**
     * The config value for the key {@code greeting}.
     */
    private final AtomicReference<String> greeting = new AtomicReference<>();

    public GreetService() {
        this(Config.global().get("app"));
    }

    GreetService(Config appConfig) {
        greeting.set(appConfig.get("greeting").asString().orElse("Ciao"));
    }

    /**
     * A service registers itself by updating the routing rules.
     *
     * @param rules the routing rules.
     */
    @Override
    public void routing(HttpRules rules) {
        rules
                .get("/", this::getDefaultMessageHandler)
                .get("/{name}", this::getMessageHandler)
                .put("/greeting", this::updateGreetingHandler);
    }

    /**
     * Return a worldly greeting message.
     *
     * @param request  the server request
     * @param response the server response
     */
    private void getDefaultMessageHandler(ServerRequest request,
                                          ServerResponse response) {
        sendResponse(response, "World");
    }

    /**
     * Return a greeting message using the name that was provided.
     *
     * @param request  the server request
     * @param response the server response
     */
    private void getMessageHandler(ServerRequest request,
                                   ServerResponse response) {
        String name = request.path().pathParameters().get("name");
        sendResponse(response, name);
    }

    private void sendResponse(ServerResponse response, String name) {
        String  msg     = String.format("%s %s!", greeting.get(), name);
        Message message = new Message(msg,null);
       
        response.send(message);
    }

    private void updateGreetingFromJson(Message message, ServerResponse response) {
        if (message.greeting() == null) {
            Message errorMessage = new Message(null,"No greeting provided");
           
            response.status(Status.BAD_REQUEST_400)
                    .send(errorMessage);
            return;
        }

        greeting.set(message.message());
        response.status(Status.NO_CONTENT_204).send();
    }
    
    

    /**
     * Set the greeting to use in future messages.
     *
     * @param request  the server request
     * @param response the server response
     */
    private void updateGreetingHandler(ServerRequest request,
                                       ServerResponse response) {
        updateGreetingFromJson(request.content().as(Message.class), response);
    }

}
