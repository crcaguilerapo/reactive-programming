package com.cristian;

import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.Server;

public class Main {
    static Server newServer(int port) {
        return Server
                .builder()
                .http(port)
                .service("/", (ctx, req) -> HttpResponse.of("Hello, Armeria!"))
                .build();
    }
    public static void main(String[] args) throws Exception {
        Server server = newServer(8080);
        server.closeOnJvmShutdown();
        server.start().join();
    }
}