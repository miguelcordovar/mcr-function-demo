package com.functions.decrypt;

import com.functions.helpers.CacheManager;
import com.functions.helpers.HttpClientSingleton;
import com.functions.helpers.KeyVaultClientSingleton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.apache.http.HttpHeaders;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class DecryptFunction {

    private final static String VTS_DECRYPT_URI = System.getenv("VTS_DECRYPT_URI");
    private final static String VTS_PREFIX_USERNAME = System.getenv("VTS_PREFIX_USERNAME");
    private final static String VTS_ENVIRONMENT = System.getenv("VTS_ENVIRONMENT");
    private final static String VTS_KEY_VAULT_URI = System.getenv("VTS_KEY_VAULT_URI");
    private final static String X_CODE_APP_HEADER = "x-code-app";

    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("decrypt")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<DecryptRequest>> request,
            final ExecutionContext context) {

        context.getLogger().info("Decrypt Function");

        //Get custom-header and request body
        //TODO define custom header name
        final String X_APP_CODE = request.getHeaders().getOrDefault(X_CODE_APP_HEADER, null);
        final String VTS_USERNAME = VTS_PREFIX_USERNAME.concat(X_APP_CODE).concat(VTS_ENVIRONMENT);

        //Get payload
        final DecryptRequest decryptRequest = request.getBody().get();

        //TODO: Validate payload
        Map<String, String> validation = validatePayload(decryptRequest);
        if (!validation.isEmpty()) {

            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body("ERROR EN PAYLOAD")
                .build();
        }

        try {
            //Create encrypt payload
            final Map<String, String> values = new HashMap<String, String>() {{
                put("ciphertext", decryptRequest.getCiphertext());
                put ("alg", decryptRequest.getAlg());
                put ("kid", decryptRequest.getKid());
            }};

            Gson gson = new Gson();
            final String payload = gson.toJson(values, new TypeToken<HashMap>(){}.getType());

            //Get VTS_Password from Key Vault or Cache Manager
            final String VTS_PASSWORD = getPassword(VTS_USERNAME);

            //Create encrypt request
            final HttpRequest httpRequest = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .uri(URI.create(VTS_DECRYPT_URI))
                .setHeader(HttpHeaders.CONTENT_TYPE,"application/json")
                .setHeader(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader(VTS_USERNAME, VTS_PASSWORD))
                .build();

            //Send encrypt request
            final HttpResponse<String> response = HttpClientSingleton.getInstance()
                .getHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());

            //Evaluate encrypt response
            if (response.statusCode() == HttpStatus.OK.value()) {
                final Map<String, Object> map = gson.fromJson(response.body(), Map.class);

                final DecryptResponse decryptResponse =
                    new DecryptResponse(map.get("plaintext").toString());

                //Return
                return request
                    .createResponseBuilder(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(decryptResponse)
                    .build();
            } else {
                //TODO: Evaluate status code


                //Return
                return request
                    .createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body("Response => code: " + response.statusCode() + " body: " + response.body())
                    .build();
            }
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);

            context.getLogger().info(sw.toString());

            //Return
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body("Message: " + ex.getMessage() + " ******* StackTrace: " + sw.toString())
                .build();
        }

    }

    private static final String getBasicAuthenticationHeader(String username, String password) {
        String valueToEncode = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }

    private static String getPassword(final String username) {
        CacheManager cacheManager = CacheManager.getInstance();
        String password = cacheManager.get(username);

        if (password == null) {
            password = KeyVaultClientSingleton.getInstance(VTS_KEY_VAULT_URI)
                .getSecretClient().getSecret(username).getValue();
            cacheManager.put(username, password);
        }

        return password;
    }

    public static Map<String, String> validatePayload(DecryptRequest request) {
        Map<String, String> values = new HashMap<String, String>();

        if (request.getCiphertext() == null) {
            values.put("HS0007", "Ciphertext field is null");
        } else if (request.getCiphertext().isEmpty()) {
            values.put("HS0008", "Ciphertext field is empty");
        }

        if (request.getAlg() == null) {
            values.put("HS0009", "Alg field is empty or null");
        } else if (request.getAlg().isEmpty()) {
            values.put("HS0010", "Alg field is empty or null");
        }

        if (request.getKid() == null) {
            values.put("HS0011", "Kid field is empty or null");
        }
        else if (request.getKid().isEmpty()) {
            values.put("HS0012", "Kid field is empty or null");
        }

        return values;
    }

}
