package com.functions.encrypt;

public class EncryptRequest {
    private String plaintext;
    private String alg;
    private String kid;

    public EncryptRequest() {
    }

    public EncryptRequest(String plaintext, String alg, String kid) {
        this.plaintext = plaintext;
        this.alg = alg;
        this.kid = kid;
    }

    public String getPlaintext() {
        return plaintext;
    }

    public void setPlaintext(String plaintext) {
        this.plaintext = plaintext;
    }

    public String getAlg() {
        return alg;
    }

    public void setAlg(String alg) {
        this.alg = alg;
    }

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }
}
