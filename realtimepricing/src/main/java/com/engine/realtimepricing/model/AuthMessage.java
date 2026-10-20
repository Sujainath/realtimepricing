package com.engine.realtimepricing.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthMessage {
    public String action = "auth";

    @JsonProperty("api_key")
    public String apiKey;

    public String secret;

    public AuthMessage(String apiKey, String secret) {
        this.apiKey = apiKey;
        this.secret = secret;
    }
}