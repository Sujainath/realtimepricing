package com.engine.realtimepricing.model;

import java.util.List;

public class SubscribeMessage {
    public String action = "subscribeMarketPrice";
    public List<String> symbols;

    public SubscribeMessage(List<String> symbols) {
        this.symbols = symbols;
    }
}