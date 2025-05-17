package org.baicaizhale.chatBot;

import okhttp3.*;
import java.io.IOException;

public class AIClient {
    private final String apiUrl;
    private final String apiKey;
    private final String model;

    public AIClient(String apiUrl, String accountId, String apiKey, String model) {
        this.apiUrl = apiUrl.replace("{account_id}", accountId);
        this.apiKey = apiKey;
        this.model = model;
    }

    public String getAIReply(String context) {
        OkHttpClient client = new OkHttpClient();
        // 构建请求体（符合Cloudflare Workers AI格式）
        String json = String.format(
                "{ \"model\": \"%s\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}] }",
                model, context.replace("\"", "\\\"")
        );
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                // 直接返回原始响应内容（需根据实际API调整解析逻辑）
                return response.body().string();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}