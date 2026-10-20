package com.engine.realtimepricing.model;

public class OutputTick {
    public String s; // Symbol
    public double b; // Buy price
    public double a; // Sell/Ask price
    public double h; // Self-computed 24h High
    public double l; // Self-computed 24h Low
    public double c; // Self-computed 24h Change %
    public long t;   // Timestamp (seconds)

    public OutputTick(String s, double b, double a, double h, double l, double c, long t) {
        this.s = s;
        this.b = b;
        this.a = a;
        this.h = h;
        this.l = l;
        this.c = c;
        this.t = t;
    }
}