package com.paidy.forex.proxy;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Main class.
 *
 */

public class Main {
    // Base URI the Grizzly HTTP server will listen on
//    public static final String BASE_URI = "http://localhost:8081/";
    public static final String ONE_FRAME_URI = "http://localhost:8080/";

 // Base URI the Grizzly HTTP server will listen on
    public static final String BASE_URI;
    public static final String protocol;
    public static final Optional<String> host;
    public static final Optional<String> port;
    
    static{
      protocol = "http://";
      host = Optional.ofNullable(System.getenv("HOSTNAME"));
      port = Optional.ofNullable(System.getenv("PORT"));
      BASE_URI = protocol + host.orElse("localhost") + ":" + port.orElse("8081") + "/" ;
    }
    
    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer() {
        // create a resource config that scans for JAX-RS resources and providers
        // in com.example package
        final ResourceConfig rc = new ResourceConfig().packages("com.paidy.forex.proxy");
        
        rc.getClasses().forEach(c -> System.out.println(c.descriptorString()));
        	
        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    /**
     * Main method.
     * @param args
     * @throws IOException
     */
    
    public static void main(String[] args) throws IOException {

        final HttpServer server = startServer();
        System.out.println(String.format("Jersey app started with endpoints available at "
                + "%s%nHit Ctrl-C to stop it...", BASE_URI));
        System.in.read();
        server.shutdownNow();
    }
}

