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

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        // 生成AI名称
        botName = generateBotName();
        getLogger().info("AI玩家名称已生成: " + botName);
        // 初始化AI客户端
        aiClient = new AIClient(
                getConfig().getString("cloudflare.api_url"),
                getConfig().getString("cloudflare.account_id"),
                getConfig().getString("cloudflare.api_key"),
                getConfig().getString("cloudflare.model")
        );
        // 注册事件监听
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    // 生成随机玩家名
    private String generateBotName() {
        String[] prefixes = {"Shadow", "Ender", "Nether", "Creeper"};
        String[] suffixes = {"123", "X", "_", "MC"};
        return prefixes[new Random().nextInt(prefixes.length)] + suffixes[new Random().nextInt(suffixes.length)];
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        // 获取玩家对话历史
        List<String> history = chatHistory.getOrDefault(player.getUniqueId(), new ArrayList<>());
        history.add(player.getName() + ": " + message);
        // 限制上下文长度
        int maxHistory = getConfig().getInt("context.max_length", 5);
        if (history.size() > maxHistory) {
            history.remove(0);
        }
        chatHistory.put(player.getUniqueId(), history);
        // 异步调用AI判断
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            String response = aiClient.getAIReply(String.join("\n", history));
            if (response != null && !response.isEmpty()) {
                // 模拟玩家发送消息
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