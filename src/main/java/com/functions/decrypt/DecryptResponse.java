package com.functions.decrypt;

public class DecryptResponse {
    private String plaintext;

    public DecryptResponse() {
    }

    public DecryptResponse(String plaintext) {
        this.plaintext = plaintext;
    }

    public String getPlaintext() {
        return plaintext;
    }

    public void setPlaintext(String plaintext) {
        this.plaintext = plaintext;
    }
}
