package com.redhat.summit2019;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertTrue;

@RunWith(VertxUnitRunner.class)
public class InsultEndpointTest {

    private static final int PORT = 8081;

    private Vertx vertx;
    private WebClient client;

    @Before
    public void before(TestContext context) {
        vertx = Vertx.vertx();
        vertx.exceptionHandler(context.exceptionHandler());
        vertx.deployVerticle(HttpApplication.class.getName(),
        new DeploymentOptions().setConfig(
                new JsonObject()
                        .put("http.port", PORT)
                        .put("noun.port", 80)
                        .put("adjective.port", 80)
                        .put("adjective.url", "insult-adjectives-insults-workshop.b9ad.pro-us-east-1.openshiftapps.com")
                        .put("noun.url", "insult-nouns-insults-workshop.b9ad.pro-us-east-1.openshiftapps.com")
        ),
        context.asyncAssertSuccess());
        client = WebClient.create(vertx);
    }

    @After
    public void after(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void callInsultEndpoint(TestContext context) {
        // Send a request and get a response
        Async async = context.async();
        client.get(PORT, "localhost", "/api/insult")
            .send(resp -> {
                context.assertTrue(resp.succeeded());
                context.assertEquals(resp.result().statusCode(), 200);
                String insult = resp.result().bodyAsJsonObject().getString("insult");
                context.assertNotNull(insult);
                async.complete();
            });
    }
}