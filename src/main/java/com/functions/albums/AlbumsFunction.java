package com.functions.albums;

import com.functions.encrypt.EncryptRequest;
import com.functions.encrypt.EncryptResponse;
import com.functions.utils.HttpClientSingleton;
import com.functions.utils.RedisCacheClientSingleton;
import com.functions.utils.KeyVaultClientSingleton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class AlbumsFunction {

    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("albums")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<EncryptRequest>> request,
            final ExecutionContext context) throws IOException, InterruptedException {

        context.getLogger().info("Albums Function");

        final String VTS_PREFIX_USERNAME = System.getenv("VTS_PREFIX_USERNAME");
        final String VTS_ENVIRONMENT = System.getenv("VTS_ENVIRONMENT");
        final String ALBUMS_URI = System.getenv("ALBUMS_URI");
        final String VTS_KEY_VAULT_URI = System.getenv("VTS_KEY_VAULT_URI");

        final String APP_CODE = request.getHeaders().getOrDefault("x-code-app", "DEFAULT");
        final String VTS_USERNAME = VTS_PREFIX_USERNAME.concat(APP_CODE).concat(VTS_ENVIRONMENT);

        RedisCacheClientSingleton redisCacheClientSingleton = RedisCacheClientSingleton.getInstance();
        String VTS_PASSWORD = redisCacheClientSingleton.getJedis().get(VTS_USERNAME);

        if (VTS_PASSWORD == null) {
            VTS_PASSWORD = KeyVaultClientSingleton.getInstance(VTS_KEY_VAULT_URI).getSecretClient().getSecret(VTS_USERNAME).getValue();
            redisCacheClientSingleton.getJedis().set(VTS_USERNAME, VTS_PASSWORD);
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(ALBUMS_URI))
            .setHeader("User-Agent", "Java 11 HttpClient Bot") // add request header
            .setHeader("Content-Type","application/json")
            .setHeader("Authorization", getBasicAuthenticationHeader(VTS_USERNAME, VTS_PASSWORD))
            .build();

        HttpResponse<String> response = HttpClientSingleton.getInstance().getHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(response.body())
            .build();
    }

    private static final String getBasicAuthenticationHeader(String username, String password) {
        String valueToEncode = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }

}
