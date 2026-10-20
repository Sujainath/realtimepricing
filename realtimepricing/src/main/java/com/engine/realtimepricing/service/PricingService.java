package com.engine.realtimepricing.service;

import com.engine.realtimepricing.model.OutputTick;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PricingService {

    public static class PriceRecord {
        public double price;
        public long timestamp;

        public PriceRecord() {}
        public PriceRecord(double price, long timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }
    }

    public static class SymbolData {
        public double openPrice;
        public List<PriceRecord> records = new ArrayList<>();
    }

    private final Map<String, SymbolData> symbolMap = new ConcurrentHashMap<>();
    private final File backupFile = new File("state.json");
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        if (backupFile.exists()) {
            try {
                Map<String, SymbolData> loaded = mapper.readValue(
                        backupFile,
                        mapper.getTypeFactory().constructMapType(Map.class, String.class, SymbolData.class)
                );
                symbolMap.putAll(loaded);
                System.out.println("Restored state from state.json");
            } catch (Exception e) {
                System.out.println("Could not load state: " + e.getMessage());
            }
        }
    }

    @PreDestroy
    public synchronized void cleanup() {
        try {
            mapper.writeValue(backupFile, symbolMap);
            System.out.println("State saved to state.json");
        } catch (Exception e) {
            System.out.println("Could not save state: " + e.getMessage());
        }
    }

    public synchronized OutputTick updateAndCalculate(String symbol, double buy, double sell) {
        long now = Instant.now().getEpochSecond();
        double currentPrice = (buy + sell) / 2.0;

        SymbolData data = symbolMap.computeIfAbsent(symbol, k -> {
            SymbolData sd = new SymbolData();
            sd.openPrice = currentPrice;
            return sd;
        });

        // 24 hours window cutoff (86400 seconds)
        long cutoff = now - 86400;
        List<PriceRecord> recentRecords = new ArrayList<>();
        double high = currentPrice;
        double low = currentPrice;

        for (PriceRecord r : data.records) {
            if (r.timestamp >= cutoff) {
                recentRecords.add(r);
                if (r.price > high) high = r.price;
                if (r.price < low) low = r.price;
            }
        }

        recentRecords.add(new PriceRecord(currentPrice, now));
        data.records = recentRecords;

        if (!recentRecords.isEmpty()) {
            data.openPrice = recentRecords.get(0).price;
        }

        double change = 0.0;
        if (data.openPrice > 0) {
            change = ((currentPrice - data.openPrice) / data.openPrice) * 100.0;
        }

        double roundedHigh = Math.round(high * 10000.0) / 10000.0;
        double roundedLow = Math.round(low * 10000.0) / 10000.0;
        double roundedChange = Math.round(change * 100.0) / 100.0;

        return new OutputTick(symbol, buy, sell, roundedHigh, roundedLow, roundedChange, now);
    }
}