package test;

import com.softwaremill.jox.Channel;
import com.softwaremill.jox.ChannelDone;
import com.softwaremill.jox.ChannelError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Demo1 {
    private static final Logger logger = LoggerFactory.getLogger(Demo1.class);

    private static void wsToChannel(TickerWebSocket ws, Channel<String> ch) throws InterruptedException {
        try {
            while (true) {
                ch.send(ws.receive());
            }
        } catch (IOException e) {
            ch.error(e);
        } catch (WebSocketClosedException e) {
            ch.done();
        }
    }

    private static void consume(Channel<String> ch) throws InterruptedException {
        var run = true;
        while (run) {
            // switch over ChannelClosed
            switch (ch.receiveOrClosed()) {
                case ChannelDone() -> {
                    logger.info("channel done");
                    run = false;
                }
                case ChannelError(Throwable cause) -> {
                    logger.error("channel error", cause);
                    run = false;
                }
                case String s -> {
                    logger.info("current price: {}", s);
                    Thread.sleep(1000);
                }
                default -> throw new IllegalStateException();
            };
        }
    }

    public static void main(String[] args) throws InterruptedException {
        var ws = TickerWebSocket.connect("CDR");
        var ch = new Channel<String>(16);
        Thread.startVirtualThread(() -> {
            try {
                wsToChannel(ws, ch);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        consume(ch);
    }
}
