package com.functions.encrypt;

public class EncryptResponse {
    private String ciphertext;
    private String tag;

    public EncryptResponse() {
    }

    public EncryptResponse(String ciphertext, String tag) {
        this.ciphertext = ciphertext;
        this.tag = tag;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public void setCiphertext(String ciphertext) {
        this.ciphertext = ciphertext;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
