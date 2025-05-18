package org.baicaizhale.chatBot;

import okhttp3.*;
import com.google.gson.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AIClient {
    private final OkHttpClient client;
    private final String apiUrl;
    private final String apiKey;

    public AIClient(String apiUrl, String accountId, String apiKey, String model) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        this.apiUrl = apiUrl
                .replace("{account_id}", accountId)
                .replace("{model}", model);
        this.apiKey = apiKey;
    }

    public String getAIReply(String context, boolean isNameGeneration) {
        try {
            JsonObject requestBody = buildRequestBody(context, isNameGeneration);
            Request request = buildRequest(requestBody);
            return executeRequest(request);
        } catch (Exception e) {
            ChatBot.getInstance().getLogger().warning("AI请求失败: " + e.getMessage());
            return null;
        }
    }

    private JsonObject buildRequestBody(String context, boolean isNameGeneration) {
        JsonObject body = new JsonObject();
        if (isNameGeneration) {
            body.addProperty("prompt", context);
            body.addProperty("max_tokens", 15);
        } else {
            JsonArray messages = new JsonArray();
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", context);
            messages.add(systemMsg);
            body.add("messages", messages);
            body.addProperty("temperature", 0.7);
        }
        return body;
    }

    private Request buildRequest(JsonObject requestBody) {
        return new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(
                        new Gson().toJson(requestBody),
                        MediaType.get("application/json")
                ))
                .build();
    }

    private String executeRequest(Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return null;

            String responseBody = response.body().string();
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            return json.getAsJsonObject("result").get("response").getAsString();
        }
    }
}