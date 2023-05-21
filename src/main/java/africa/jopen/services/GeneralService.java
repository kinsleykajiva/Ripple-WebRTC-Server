package africa.jopen.services;

import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

public class GeneralService implements Service {


    @Override
    public void update(Routing.Rules rules) {
        rules
                .get("/clients/all", this::getAllClient)
        ;
    }
    
    
    
    private void getAllClient(ServerRequest request, ServerResponse response){
    
    }
}
