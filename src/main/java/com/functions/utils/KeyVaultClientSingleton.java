package com.functions.utils;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;

public class KeyVaultClientSingleton {

    private static KeyVaultClientSingleton instance;

    private SecretClient secretClient;

    private KeyVaultClientSingleton(final String URI) {
        this.secretClient = new SecretClientBuilder()
            .vaultUrl(URI)
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();
    }

    public static KeyVaultClientSingleton getInstance(final String URI) {
        if (instance == null) {
            instance = new KeyVaultClientSingleton(URI);
        }
        return instance;
    }

    public SecretClient getSecretClient() {
        return this.secretClient;
    }
}

