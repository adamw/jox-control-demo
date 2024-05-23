package test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;

public class TickerWebSocket {
    private static final Logger logger = LoggerFactory.getLogger(TickerWebSocket.class);

    private final Random r = new Random();

    private final String tickerSymbol;

    private TickerWebSocket(String tickerSymbol) {
        this.tickerSymbol = tickerSymbol;
    }

    String getTickerSymbol() {
        return tickerSymbol;
    }

    String receive() throws IOException, WebSocketClosedException {
        var result = r.nextInt(20);
        if (result == 0) {
            logger.info("WebSocket done");
            throw new WebSocketClosedException();
        } else if (result == 1) {
            logger.info("WebSocket error");
            throw new IOException();
        } else {
            return Integer.toString(result);
        }
    }

    static TickerWebSocket connect(String tickerSymbol) {
        return new TickerWebSocket(tickerSymbol);
    }
}

class WebSocketClosedException extends Exception {}
