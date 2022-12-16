package ex.undertow.exmaple;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author glqdlt
 */
public class App {

    private static Integer LISTEN_SERVER_PORT = 18080;
    private static String FORWARD_TARGET_SERVER_URL = "http://127.0.0.1:18081";

    public static void main(String[] args) {

        LoadBalancingProxyClient dd = new LoadBalancingProxyClient();
        dd.addHost(URI.create(FORWARD_TARGET_SERVER_URL));
        dd.setConnectionsPerThread(20);

        ExecutorService ee = Executors.newFixedThreadPool(1);
        ee.submit(() -> {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(30));
                dd.addHost(URI.create("http://127.0.0.1:18082"));
                Thread.sleep(TimeUnit.SECONDS.toMillis(30));
                dd.removeHost(URI.create(FORWARD_TARGET_SERVER_URL));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        final HttpHandler handler = new ProxyHandler(dd, 30000, ResponseCodeHandler.HANDLE_404);
        Undertow server = Undertow.builder()
                .addHttpListener(LISTEN_SERVER_PORT, "localhost")
                .setHandler(new HttpHandler() {
                    @Override
                    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
                        httpServerExchange.setRelativePath(httpServerExchange.getRequestPath());
                        handler.handleRequest(httpServerExchange);
                    }
                })
                .build();
        server.start();

    }
}
