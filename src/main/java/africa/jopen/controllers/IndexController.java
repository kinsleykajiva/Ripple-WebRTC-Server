package africa.jopen.controllers;


import africa.jopen.utils.XUtils;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("/")
public class IndexController {

    @GET
    @Path("/about")
    public Response getAllClient() {

        return XUtils.buildSuccessResponse(true, 200, "About Server", Map.of("details", XUtils.MAIN_CONFIG_MODEL , "features", List.of("Video Call" ,"Video Room" ,"G Stream")));
    }
    @GET
    @Path("/")
    public Response index() {

        return XUtils.buildSuccessResponse(true, 200, "Index Server", Map.of());
    }
}
