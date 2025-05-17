package org.baicaizhale.chatBot;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public class ChatBot extends JavaPlugin implements Listener {
    private static ChatBot instance;
    private AIClient aiClient;
    private String botName;
    private final Map<UUID, List<String>> chatHistory = new HashMap<>();
    private String systemPrompt;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();
        // 加载提示词
        systemPrompt = getConfig().getString("prompt.system_prompt");
        // 生成AI名称
        generateBotName();
        // 初始化AI客户端
        aiClient = new AIClient(
                getConfig().getString("cloudflare.api_url"),
                getConfig().getString("cloudflare.account_id"),
                getConfig().getString("cloudflare.api_key"),
                getConfig().getString("cloudflare.model")
        );
        // 注册事件监听
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("插件已启用！AI玩家名称: " + botName);
    }

    // 通过AI生成玩家名
    private void generateBotName() {
        String prompt = "生成一个符合Minecraft玩家风格的英文名，要求：\n" +
                "1. 长度3-12字符\n" +
                "2. 可以包含数字但不要以数字开头\n" +
                "3. 示例：EnderDragon、Creeper42\n" +
                "只需返回名称，不要其他内容！";

        String name = aiClient.getAIReply(prompt, true);
        if (name != null && name.matches("[a-zA-Z][a-zA-Z0-9_]{2,11}")) {
            botName = name.trim();
        } else {
            // 备用名称生成逻辑
            String[] fallbackNames = {"ShadowEnder", "NetherCraft", "CreeperMC"};
            botName = fallbackNames[new Random().nextInt(fallbackNames.length)];
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        // 跳过AI自己的消息
        if (player.getName().equals(botName)) return;

        // 构建对话上下文
        List<String> history = chatHistory.getOrDefault(player.getUniqueId(), new ArrayList<>());
        history.add(player.getName() + ": " + message);
        if (history.size() > getConfig().getInt("context.max_length", 5)) {
            history.remove(0);
        }
        chatHistory.put(player.getUniqueId(), history);

        // 构建AI请求内容
        String context = systemPrompt + "\n当前聊天记录：\n" + String.join("\n", history);

        // 异步处理
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            String response = aiClient.getAIReply(context, false);
            if (response != null && !response.isEmpty()) {
                Bukkit.getScheduler().runTask(this, () ->
                        Bukkit.broadcastMessage("<" + botName + "> " + response)
                );
            }
        });
    }

    public static ChatBot getInstance() {
        return instance;
    }
}