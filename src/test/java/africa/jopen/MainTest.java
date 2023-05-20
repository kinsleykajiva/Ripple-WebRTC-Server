
package africa.jopen;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.time.Duration;
import java.util.Collections;
import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;

import io.helidon.media.jsonp.JsonpSupport;
import io.helidon.common.http.Http;
import io.helidon.webclient.WebClient;
import io.helidon.webclient.WebClientResponse;
import io.helidon.webserver.WebServer;

import org.junit.jupiter.api.Order;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class MainTest {

    private static final JsonBuilderFactory JSON_BUILDER = Json.createBuilderFactory(Collections.emptyMap());
    private static final JsonObject TEST_JSON_OBJECT = JSON_BUILDER.createObjectBuilder()
                .add("greeting", "Hola")
                .build();

    private static WebServer webServer;
    private static WebClient webClient;

    @BeforeAll
    static void startTheServer() {
        webServer = Main.startServer().await(Duration.ofSeconds(10));

        webClient = WebClient.builder()
                .baseUri("http://localhost:" + webServer.port())
                .addMediaSupport(JsonpSupport.create())
                .build();
    }

    @AfterAll
    static void stopServer() {
        if (webServer != null) {
            webServer.shutdown().await(10, TimeUnit.SECONDS);
        }
    }


    @Test
    void testMicroprofileMetrics() {
        String get = webClient.get()
                .path("/simple-greet/greet-count")
                .request(String.class)
                .await(Duration.ofSeconds(5));

        assertThat(get, containsString("Hello World!"));

        String openMetricsOutput = webClient.get()
                .path("/metrics")
                .request(String.class)
                .await(Duration.ofSeconds(5));

        assertThat("Metrics output", openMetricsOutput, containsString("application_accessctr_total"));
    }

    @Test
    void testMetrics() {
        WebClientResponse response = webClient.get()
                .path("/metrics")
                .request()
                .await(Duration.ofSeconds(5));
        assertThat(response.status().code(), is(200));
    }

    @Test
    void testHealth() {
        WebClientResponse response = webClient.get()
                .path("health")
                .request()
                .await(Duration.ofSeconds(5));
        assertThat(response.status().code(), is(200));
    }

    @Test
    void testSimpleGreet() {
        JsonObject jsonObject = webClient.get()
                                         .path("/simple-greet")
                                         .request(JsonObject.class)
                                         .await(Duration.ofSeconds(5));
        assertThat(jsonObject.getString("message"), is("Hello World!"));
    }

    @Test
    void testGreetings() {
        JsonObject jsonObject;
        WebClientResponse response;

        jsonObject = webClient.get()
                .path("/greet/Joe")
                .request(JsonObject.class)
                .await(Duration.ofSeconds(5));
        assertThat(jsonObject.getString("message"), is("Hello Joe!"));

        response = webClient.put()
                .path("/greet/greeting")
                .submit(TEST_JSON_OBJECT)
                .await(Duration.ofSeconds(5));
        assertThat(response.status().code(), is(204));

        jsonObject = webClient.get()
                .path("/greet/Joe")
                .request(JsonObject.class)
                .await(Duration.ofSeconds(5));
        assertThat(jsonObject.getString("message"), is("Hola Joe!"));
    }

}
