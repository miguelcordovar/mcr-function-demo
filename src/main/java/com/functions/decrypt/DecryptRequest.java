package com.functions.decrypt;

public class DecryptRequest {
    private String ciphertext;
    private String alg;
    private String kid;

    public DecryptRequest() {
    }

    public DecryptRequest(String ciphertext, String alg, String kid) {
        this.ciphertext = ciphertext;
        this.alg = alg;
        this.kid = kid;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public void setCiphertext(String ciphertext) {
        this.ciphertext = ciphertext;
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
