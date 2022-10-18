package com.functions.helpers;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;

public class KeyVaultClientSingleton {
    private static KeyVaultClientSingleton instance;
    private static Object monitor = new Object();
    private SecretClient secretClient;

    private KeyVaultClientSingleton(final String URI) {
        this.secretClient = new SecretClientBuilder()
            .vaultUrl(URI)
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();
    }

    public static KeyVaultClientSingleton getInstance(final String URI) {
        if (instance == null) {
            synchronized (monitor) {
                if (instance == null) {
                    instance = new KeyVaultClientSingleton(URI);
                }
            }
        }
        return instance;
    }

    public SecretClient getSecretClient() {
        return this.secretClient;
    }
}

