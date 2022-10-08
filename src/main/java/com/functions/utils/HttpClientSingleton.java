package com.functions.utils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class HttpClientSingleton {

    private SSLContext insecureContext() {
        TrustManager[] noopTrustManager = new TrustManager[]{
            new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] xcs, String string) {}
                public void checkServerTrusted(X509Certificate[] xcs, String string) {}
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            }
        };

        try {
            SSLContext sc = SSLContext.getInstance("ssl");
            sc.init(null, noopTrustManager, null);
            return sc;
        } catch (KeyManagementException | NoSuchAlgorithmException ex) {
            return null;
        }
    }

    private static HttpClientSingleton instance;

    private HttpClient httpClient;

    private HttpClientSingleton() {
        this.httpClient = HttpClient.newBuilder()
            .sslContext(insecureContext())
            .build();
    }

    public static HttpClientSingleton getInstance() {
        if (instance == null) {
            instance = new HttpClientSingleton();
        }
        return instance;
    }

    public HttpClient getHttpClient() {
        return this.httpClient;
    }
}

