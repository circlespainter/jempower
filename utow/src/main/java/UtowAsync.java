import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.nio.ByteBuffer;

import io.undertow.util.SameThreadExecutor;

import java.util.Timer;
import java.util.TimerTask;

// 9000

public final class UtowAsync implements HttpHandler {
    public static void main(String[] args) throws Exception {
        Undertow.builder()
            .addHttpListener(9000, "0.0.0.0")
            .setHandler(Handlers.path().addPrefixPath("/hello", new UtowAsync()))
            .build()
            .start();
    }

    int num = 0;
    final HttpServerExchange acv[] = new HttpServerExchange[1000000];

    synchronized final void store(HttpServerExchange async) {
        if (async == null) while (num > 0) {
            reply(acv[--num]);
            acv[num] = null;
        }
        else acv[num++] = async;
    }

    final byte[] bytes = "Hello, world!".getBytes();
    final ByteBuffer buf = ByteBuffer.allocate(bytes.length).put(bytes);

    {
        buf.flip();
    }

    final void reply(HttpServerExchange exchange) {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseHeaders().put(Headers.SERVER, "undertow-async");
        exchange.getResponseSender().send(buf.duplicate());
    }

    final public void handleRequest(final HttpServerExchange exchange) throws Exception {
        exchange.dispatch(SameThreadExecutor.INSTANCE, () -> store(exchange));
    }

    {
        new Timer().schedule(new TimerTask() {
            public final void run() {
                UtowAsync.this.store(null);
            }
        }, 10, 10);
    }
}
