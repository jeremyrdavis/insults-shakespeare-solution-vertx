package com.redhat.summit2019;

import com.redhat.summit2019.model.Insult;
import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.StaticHandler;

import java.util.concurrent.TimeUnit;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

public class HttpApplication extends AbstractVerticle {

  static final String template = "Hello, %s!";

  WebClient webClient;

  @Override
  public void start(Future<Void> future) {

    webClient = WebClient.create(vertx);
    
    // Create a router object.
    Router router = Router.router(vertx);

    router.get("/api/greeting").handler(this::greeting);
    router.get("/api/insult").handler(BodyHandler.create());
    router.get("/api/insult").handler(this::insultHandler);
    router.get("/*").handler(StaticHandler.create());

    // Create the HTTP server and pass the "accept" method to the request handler.
    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(
            // Retrieve the port from the configuration, default to 8080.
            config().getInteger("http.port", 8080), ar -> {
              if (ar.succeeded()) {
                System.out.println("Server started on port " + ar.result().actualPort());
                System.out.println("Config startup message " + config().getString("startup.message", "none"));
              }else{
                System.out.println("Server failed " + ar.cause());
                future.fail(ar.cause());
              }
                future.handle(ar.mapEmpty());
            });

  }

  private void insultHandler(RoutingContext rc) {

    System.out.println("insultHandler called");
    System.out.println("noun.port: " + config().getInteger("noun.port"));
    System.out.println("noun.url: " + config().getString("noun.url"));
    System.out.println("adjective.port: " + config().getInteger("adjective.port"));
    System.out.println("adjective.url:" + config().getString("adjective.url"));

    Single<JsonObject> noun = webClient
            .get(config().getInteger("noun.port", 8080), config().getString("noun.url", "insult-nouns"),"/api/noun")
            .rxSend()
            .doOnSuccess(r -> System.out.println("noun" + r.bodyAsString()))
            .map(HttpResponse::bodyAsJsonObject);

    Single<JsonObject> adj1 = webClient
            .get(config().getInteger("adjective.port", 8080), config().getString("adjective.url", "insult-adjectives"), "/api/adjective")
            .rxSend()
            .doOnSuccess(r -> System.out.println("adj1" + r.bodyAsString()))
            .map(HttpResponse::bodyAsJsonObject);

    Single<JsonObject> adj2 = webClient
            .get(config().getInteger("adjective.port", 8080), config().getString("adjective.url", "insult-adjectives"), "/api/adjective")
            .rxSend()
            .doOnSuccess(r -> System.out.println("adj2" + r.bodyAsString()))
            .map(HttpResponse::bodyAsJsonObject);

    Single.zip(
            adj1.doOnError(error -> System.out.println(error.getMessage())),
            adj2.doOnError(error -> System.out.println(error.getMessage())),
            noun.doOnError(error -> System.out.println(error.getMessage())),
            Insult::new)
            .subscribe(r ->
                    rc.response()
                            .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
                            .end(r.toString()));
  }

  private void error(RoutingContext rc, Throwable error){
    rc.response()
            .setStatusCode(500)
            .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
            .end(new JsonObject().put("error", error.getMessage()).encodePrettily());
  }



  private void greeting(RoutingContext rc) {
    String name = rc.request().getParam("name");
    if (name == null) {
      name = "World";
    }

    JsonObject response = new JsonObject()
        .put("content", String.format(template, name));

    rc.response()
        .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
        .end(response.encodePrettily());
  }
}
