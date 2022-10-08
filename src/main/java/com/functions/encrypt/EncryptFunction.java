package com.functions.encrypt;

import com.functions.utils.HttpClientSingleton;
import com.functions.utils.KeyVaultClientSingleton;
import com.functions.utils.RedisCacheClientSingleton;
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class EncryptFunction {

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

        context.getLogger().info("Encrypt Function");

        //Load environment variables
        final String VTS_ENCRYPT_URI = System.getenv("VTS_ENCRYPT_URI");
        final String VTS_PREFIX_USERNAME = System.getenv("VTS_PREFIX_USERNAME");
        final String VTS_ENVIRONMENT = System.getenv("VTS_ENVIRONMENT");
        final String VTS_KEY_VAULT_URI = System.getenv("VTS_KEY_VAULT_URI");

        //Get custom-header and request body
        final String APP_CODE = request.getHeaders().getOrDefault("x-code-app", null);
        final EncryptRequest encryptRequest = request.getBody().get();

        //Set VTS_USERNAME
        final String VTS_USERNAME = VTS_PREFIX_USERNAME.concat(APP_CODE).concat(VTS_ENVIRONMENT);

        //Get password
        RedisCacheClientSingleton redisCacheClientSingleton = RedisCacheClientSingleton.getInstance();
        String VTS_PASSWORD = redisCacheClientSingleton.getJedis().get(VTS_USERNAME);

        if (VTS_PASSWORD == null) {
            VTS_PASSWORD = KeyVaultClientSingleton.getInstance(VTS_KEY_VAULT_URI).getSecretClient().getSecret(VTS_USERNAME).getValue();
            redisCacheClientSingleton.getJedis().set(VTS_USERNAME, VTS_PASSWORD);
        }

        try {
            //Create encrypt payload
            final Map<String, String> values = new HashMap<String, String>() {{
                put("plaintext", encryptRequest.getPlaintext());
                put ("alg", encryptRequest.getAlg());
                put ("kid", encryptRequest.getKid());
            }};

            Gson gson = new Gson();
            String jsonPayload = gson.toJson(values, new TypeToken<HashMap>(){}.getType());

            //Create encrypt request
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .uri(URI.create(VTS_ENCRYPT_URI))
                .setHeader("User-Agent", "Java 11 HttpClient Bot") // add request header
                .setHeader("Content-Type","application/json")
                .setHeader("Authorization", getBasicAuthenticationHeader(VTS_USERNAME, VTS_PASSWORD))
                .build();

            //Send encrypt request
            HttpResponse<String> response = HttpClientSingleton.getInstance().getHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());

            //Evaluate encrypt response
            if (response.statusCode() == HttpStatus.OK.value()) {
                Map<String, Object> map = gson.fromJson(response.body(), Map.class);

                EncryptResponse encryptResponse = new EncryptResponse();
                encryptResponse.setCiphertext(map.get("ciphertext").toString());
                encryptResponse.setTag(map.get("tag").toString());

                //Return
                return request
                    .createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(encryptResponse)
                    .build();

            } else {

                //Return
                return request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .body("Response => code: " + response.statusCode() + " body: " + response.body())
                    .build();

            }

        } catch (Exception ex) {

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);

            //Return
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .header("Content-Type", "application/json")
                .body("Message: " + ex.getMessage() + " ******* StackTrace: " + sw.toString())
                .build();

        }

    }

    private static final String getBasicAuthenticationHeader(String username, String password) {
        String valueToEncode = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }

}
