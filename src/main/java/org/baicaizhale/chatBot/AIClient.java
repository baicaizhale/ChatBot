package org.baicaizhale.chatBot;

import okhttp3.*;
import com.google.gson.*;
import java.io.IOException;

public class AIClient {
    private final String apiUrl;
    private final String apiKey;

    public AIClient(String apiUrl, String accountId, String apiKey, String model) {
        this.apiUrl = apiUrl
                .replace("{account_id}", accountId)
                .replace("{model}", model); // model 作为局部变量使用
        this.apiKey = apiKey;
    }

    public String getAIReply(String context, boolean isNameGeneration) {
        try {
            JsonObject requestBody = buildRequestBody(context, isNameGeneration);
            Request request = buildRequest(requestBody);

            try (Response response = new OkHttpClient().newCall(request).execute()) {
                return parseResponse(response);
            }
        } catch (Exception e) {
            ChatBot.getInstance().getLogger().warning("AI请求失败: " + e.getMessage());
            return null;
        }
    }

    private JsonObject buildRequestBody(String context, boolean isNameGeneration) {
        JsonObject requestBody = new JsonObject();
        if (isNameGeneration) {
            requestBody.addProperty("prompt", context);
            requestBody.addProperty("max_tokens", 15);
        } else {
            JsonArray messages = new JsonArray();
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", context);
            messages.add(systemMsg);
            requestBody.add("messages", messages);
            requestBody.addProperty("temperature", 0.7);
        }
        return requestBody;
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

    private String parseResponse(Response response) throws IOException {
        if (!response.isSuccessful() || response.body() == null) return null;

        String responseBody = response.body().string();
        JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
        return jsonResponse.getAsJsonObject("result").get("response").getAsString();
    }
}