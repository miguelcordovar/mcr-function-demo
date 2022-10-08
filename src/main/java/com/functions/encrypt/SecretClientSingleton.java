package com.functions.encrypt;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;

public class SecretClientSingleton {

    private static SecretClientSingleton instance;

    private String URI;

    private SecretClient secretClient;

    private SecretClientSingleton(String URI) {
        this.URI = URI;
        this.secretClient = new SecretClientBuilder()
            .vaultUrl(URI)
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();
    }

    public static SecretClientSingleton getInstance(String URI) {
        if (instance == null) {
            instance = new SecretClientSingleton(URI);
        }
        return instance;
    }

    public SecretClient getSecretClient() {
        return secretClient;
    }
}

