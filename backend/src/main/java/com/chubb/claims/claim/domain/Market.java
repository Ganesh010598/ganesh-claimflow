package com.chubb.claims.claim.domain;

/** Illustrative six-market demo; actual market list was not supplied in the brief. */
public enum Market {
    MY("MYR"), SG("SGD"), HK("HKD"), TH("THB"), VN("VND"), ID("IDR");
    private final String currency;
    Market(String currency) { this.currency = currency; }
    public String currency() { return currency; }
}
