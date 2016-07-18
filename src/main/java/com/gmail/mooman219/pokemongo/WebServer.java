package com.gmail.mooman219.pokemongo;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author Joseph Cumbo (mooman219)
 */
public class WebServer {

    /**
     * The internal route that handles authentication.
     */
    public static final String DIR_AUTH = "/auth";
    /**
     * The port the web server will run on.
     */
    private final int port;
    /**
     * The base address of the web server.
     */
    private final String address;
    /**
     * The last successful authorization made, null if one hasn't been made yet.
     */
    private UserToken lastToken = null;

    /**
     * Creates a new WebServer.
     *
     * @param port the port the web server will run on.
     */
    public WebServer(int port) {
        this.port = port;
        this.address = "http://localhost:" + port;
    }

    /**
     * Gets the port the server is running on.
     *
     * @return the port the server is running on.
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the address the server is running on.
     *
     * @return the address the server is running on.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Gets the last successful authorization made on the web server. This may
     * be null if a successful authorization hasn't been made yet.
     *
     * @return the last successful authorization made, null if one hasn't been
     * made yet.
     */
    public UserToken getLastToken() {
        return lastToken;
    }

    /**
     * Starts the WebServer.
     */
    public void start() {
        Vertx vertx = Vertx.vertx();
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);

        router.route(DIR_AUTH).handler(this::handleAuth);

        server.requestHandler(router::accept).listen(this.port);
    }

    /**
     * Handles authentication for the server.
     *
     * @param context the routing context.
     */
    private void handleAuth(RoutingContext context) {
        HttpServerResponse res = context.response();
        HttpServerRequest req = context.request();
        if (req.method() == HttpMethod.POST) {
            /**
             * When the token request is made, the redirect-uri needs to be
             * valid or else Google will respond with a 400. Therefore, we
             * handle the post case differently.
             */
            res.putHeader("content-type", "text/plain");
            res.end("Post Received");
        } else if (req.params().contains("code")) {
            /**
             * Try to parse the one time use code to create the UserToken.
             */
            UserToken auth = UserToken.createUserToken(req.params().get("code"), this.getAddress() + DIR_AUTH);
            res.putHeader("content-type", "text/html");
            if (auth != null) {
                this.lastToken = auth;
                res.end("<b>Success</b>, authenticated with the server.\nYou may close this window now.");
            } else {
                res.end("<b>Error</b>, unable to authenticate with the server. <a href=" + this.getAddress() + DIR_AUTH + ">Click here to try again.</a>");
            }
        } else {
            /**
             * Redirect to the google auth service to generate the one time use
             * code needed for the UserToken.
             */
            res.setStatusCode(302);
            res.putHeader("Location", UserToken.URL_GOOGLE_AUTH
                    + "client_id=" + UserToken.CLIENT_ID
                    + "&redirect_uri=" + WebServer.encodeUrl(this.getAddress() + DIR_AUTH) // We need to encode because this is a get parameter.
                    + "&response_type=code"
                    + "&scope=openid%20email%20https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.email");
            res.end();
        }
    }

    /**
     * Encodes the given value to be used in as a query parameter.
     *
     * @param value the value to encode.
     * @return the encoded value, empty string if UTF-8 isn't support on the
     * platform.
     */
    public static String encodeUrl(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            System.out.println("Error: UTF-8 encoding is not supported on this platform.");
            return "";
        }
    }

}
