package test;

import com.softwaremill.jox.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Scanner;

import static com.softwaremill.jox.Select.select;

public class Demo2 {
    private static final Logger logger = LoggerFactory.getLogger(Demo2.class);

    /*
        ┌──────┐
        │ CDR  ├───┐
        └──────┘   │
                   │
        ┌──────┐   │     ┌─────────────────────────────┐
        │ AAPL ├───┼────►│      Prices channel         ├────┬─────► consumer
        └──────┘   │     └─────────────────────────────┘    │
                   │                                        │
        ┌──────┐   │                                        │
        │ NVDA ├───┘                                        │
        └──────┘         ┌─────────────────────────────┐    │
                         │      Control channel        ├────┘
                         └─────────────────────────────┘
     */

    sealed interface TickerWebSocketEvent permits TickerWebSocketError, TickerWebSocketDone {}
    record TickerWebSocketError(String symbol) implements TickerWebSocketEvent {}
    record TickerWebSocketDone(String symbol) implements TickerWebSocketEvent {}

    private static void wsToChannel(TickerWebSocket ws, Channel<String> prices, Channel<TickerWebSocketEvent> control) throws InterruptedException {
        try {
            while (true) {
                prices.send(ws.getTickerSymbol() + ": " + ws.receive());
            }
        } catch (IOException e) {
            control.send(new TickerWebSocketError(ws.getTickerSymbol()));
        } catch (WebSocketClosedException e) {
            control.send(new TickerWebSocketDone(ws.getTickerSymbol()));
        }
    }

    private static void consume(Channel<String> prices, Channel<TickerWebSocketEvent> control) throws InterruptedException {
        while (true) {
            switch (select(control.receiveClause(), prices.receiveClause())) {
                case String p -> {
                    logger.info("current price: {}", p);
                    Thread.sleep(1000);
                }
                case TickerWebSocketDone d -> {
                    logger.info("ticker done: {}, not restarting", d.symbol());
                }
                case TickerWebSocketError e -> {
                    logger.error("ticker error: {}, restarting", e.symbol());
                    startTicker(e.symbol(), prices, control);
                }
                default -> throw new IllegalStateException();
            }
        }
    }

    private static void startTicker(String symbol, Channel<String> prices, Channel<TickerWebSocketEvent> control) {
        var ws = TickerWebSocket.connect(symbol);
        Thread.startVirtualThread(() -> {
            try {
                wsToChannel(ws, prices, control);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void main(String[] args) {
        var prices = new Channel<String>(4);
        var control = Channel.<TickerWebSocketEvent>newUnlimitedChannel();

        Thread.startVirtualThread(() -> {
            try {
                consume(prices, control);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        Scanner input = new Scanner(System.in);
        while (input.hasNextLine()) {
            var cmd = input.nextLine();
            if (cmd.startsWith("sub")) {
                var symbol = cmd.substring(4);
                startTicker(symbol, prices, control);
            }
        }
    }
}
