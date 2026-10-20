package com.engine.realtimepricing.client;

import com.engine.realtimepricing.model.AuthMessage;
import com.engine.realtimepricing.model.OutputTick;
import com.engine.realtimepricing.model.SubscribeMessage;
import com.engine.realtimepricing.service.PricingService;
import com.engine.realtimepricing.ws.FrontendSocketHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Arrays;
import java.util.List;

@Component
public class FeedListener {

    private static final String WS_URL = "wss://v3-prod.livefxhub.com/ws/algo";
    private static final String API_KEY = "50558cc472f332b399c2760009728e70f39562a4399693b5";
    private static final String SECRET_KEY = "2451125110c1a9216219b0aca76eb1d25380dba220d80e741100386c0dc1d31d";

    private final PricingService pricingService;
    private final FrontendSocketHandler socketHandler;
    private final ObjectMapper mapper = new ObjectMapper();

    // Document-ல் உள்ள அதே அத்தனை Symbols[cite: 10]
    private final List<String> symbols = Arrays.asList(
            "AUDUSD", "AUDCAD", "AUDCHF", "AUDJPY", "AUDNZD",
            "AUDSGD", "CADCHF", "CADJPY", "CHFJPY", "CHFSGD",
            "EURAUD", "EURCAD", "EURCHF", "EURGBP", "EURJPY",
            "EURNZD", "EURUSD", "GBPAUD", "GBPCAD", "GBPCHF",
            "GBPJPY", "GBPNZD", "GBPUSD", "NZDCAD", "NZDCHF",
            "NZDJPY", "NZDUSD", "USDCAD", "USDCHF", "USDCNH",
            "USDJPY", "USDMXN", "USDNOK", "USDPLN", "USDSEK",
            "USDSGD", "USDTRY", "USDZAR", "EURTRY", "GBPTRY",
            "NOKJPY", "SEKJPY", "SGDJPY", "ZARJPY", "XAUUSD",
            "XAGUSD", "BTCUSD", "ETHUSD", "BNBUSD", "SOLUSD",
            "XRPUSD", "ADAUSD", "DOGUSD", "DOTUSD", "LTCUSD",
            "BCHUSD", "XLMUSD", "TRXUSD", "UNIUSD", "FILUSD",
            "AVXUSD"
    );

    public FeedListener(PricingService pricingService, FrontendSocketHandler socketHandler) {
        this.pricingService = pricingService;
        this.socketHandler = socketHandler;
    }

    @PostConstruct
    public void start() {
        new Thread(this::connect).start();
    }

    private void connect() {
        while (true) {
            try {
                System.out.println("Connecting to LiveFXHub feed...");
                StandardWebSocketClient client = new StandardWebSocketClient();

                WebSocketSession session = client.execute(new TextWebSocketHandler() {
                    private boolean subscribed = false;

                    @Override
                    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                        System.out.println("Connected to feed. Authenticating...");
                        // Step 1: Send only Auth message and WAIT[cite: 9, 10]
                        String authJson = "{\"action\":\"auth\",\"api_key\":\"" + API_KEY.trim() + "\",\"secret\":\"" + SECRET_KEY.trim() + "\"}";
                        System.out.println("Sent Auth: " + authJson);
                        session.sendMessage(new TextMessage(authJson));
                    }

                    @Override
                    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                        String payload = message.getPayload();
                        System.out.println("FEED INCOMING: " + payload);

                        JsonNode node = mapper.readTree(payload);

                        // Step 2 & 3: Check for successful auth before subscribing[cite: 10]
                        if (!subscribed) {
                            // Any success acknowledgment from server
                            if ((node.has("status") && "success".equalsIgnoreCase(node.get("status").asText()))
                                    || (node.has("event") && "authenticated".equalsIgnoreCase(node.get("event").asText()))
                                    || (node.has("message") && node.get("message").asText().toLowerCase().contains("success"))
                                    || (node.has("action") && "auth".equalsIgnoreCase(node.get("action").asText()))) {

                                System.out.println("Authentication confirmed! Subscribing to symbols now...");
                                SubscribeMessage sub = new SubscribeMessage(symbols);
                                session.sendMessage(new TextMessage(mapper.writeValueAsString(sub)));
                                subscribed = true;
                                return;
                            }
                        }

                        // Step 4 & 5: Ingest Price updates and process[cite: 9, 10]
                        if (node.has("symbol") && node.has("buy") && node.has("sell")) {
                            String symbol = node.get("symbol").asText();
                            double buy = node.get("buy").asDouble();
                            double sell = node.get("sell").asDouble();

                            OutputTick out = pricingService.updateAndCalculate(symbol, buy, sell);
                            socketHandler.broadcast(mapper.writeValueAsString(out));
                        }
                    }

                    @Override
                    public void handleTransportError(WebSocketSession session, Throwable exception) {
                        System.out.println("Transport error: " + exception.getMessage());
                    }

                    @Override
                    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                        System.out.println("CLOSED REASON: " + status.getReason() + " | CODE: " + status.getCode());
                    }
                }, WS_URL).get();

                while (session.isOpen()) {
                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                System.out.println("Feed error: " + e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
            }
        }
    }
}