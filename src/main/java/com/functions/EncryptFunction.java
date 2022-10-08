package com.functions;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.fasterxml.jackson.core.type.TypeReference;
import com.functions.encrypt.EncryptRequest;
import com.functions.encrypt.EncryptResponse;
import com.functions.encrypt.SecretClientSingleton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class EncryptFunction {

    static SSLContext insecureContext() {
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

    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("encrypt")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<EncryptRequest>> request,
            final ExecutionContext context) throws IOException, InterruptedException {

        context.getLogger().info("Encrypt Request");

        Properties props = System.getProperties();
        props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());


        HttpClient httpClient = HttpClient.newBuilder()
            .sslContext(insecureContext())
            .build();

        final String APP_CODE = request.getHeaders().getOrDefault("x-code-app", "DEFAULT");

        EncryptRequest encryptRequest = request.getBody().get();

        final String VTS_PREFIX_USERNAME = System.getenv("VTS_PREFIX_USERNAME");
        final String VTS_ENVIRONMENT = System.getenv("VTS_ENVIRONMENT");
        final String VTS_ENCRYPT_URI = System.getenv("VTS_ENCRYPT_URI");

        final String VTS_USERNAME = VTS_PREFIX_USERNAME.concat(APP_CODE).concat(VTS_ENVIRONMENT);

        String keyVaultUri = System.getenv("VTS_KEY_VAULT_URI");

        SecretClientSingleton secretClientSingleton = SecretClientSingleton.getInstance(keyVaultUri);

        KeyVaultSecret retrievedSecret = secretClientSingleton.getSecretClient().getSecret(VTS_USERNAME);

        String VTS_PASSWORD = retrievedSecret.getValue();

        var values = new HashMap<String, String>() {{
            put("plaintext", encryptRequest.getPlaintext());
            put ("alg", encryptRequest.getAlg());
            put ("kid", encryptRequest.getKid());
        }};

        Gson gson = new Gson();
        Type gsonType = new TypeToken<HashMap>(){}.getType();
        String jsonPayload = gson.toJson(values,gsonType);

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .uri(URI.create(VTS_ENCRYPT_URI))
            .setHeader("User-Agent", "Java 11 HttpClient Bot") // add request header
            .setHeader("Content-Type","application/json")
            .setHeader("Authorization", getBasicAuthenticationHeader(VTS_USERNAME, VTS_PASSWORD))
            .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        Map<String, Object> map = gson.fromJson(response.body(), Map.class);

        EncryptResponse encryptResponse = new EncryptResponse();
        encryptResponse.setCiphertext(map.get("ciphertext").toString());
        encryptResponse.setTag(map.get("tag").toString());

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(encryptResponse)
            .build();
    }

    private static final String getBasicAuthenticationHeader(String username, String password) {
        String valueToEncode = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }

}
