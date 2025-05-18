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

        // 初始化AI客户端
        aiClient = new AIClient(
                getConfig().getString("cloudflare.api_url"),
                getConfig().getString("cloudflare.account_id"),
                getConfig().getString("cloudflare.api_key"),
                getConfig().getString("cloudflare.model")
        );

        // 加载配置
        systemPrompt = getConfig().getString("prompt.system_prompt");
        generateBotName();

        // 注册事件
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("插件已启用！AI玩家名称: " + botName);
    }

    // 生成AI名称（优化逻辑）
    private void generateBotName() {
        String prompt = "生成一个Minecraft风格英文名，要求：\n" +
                "1. 长度3-12字符\n" +
                "2. 不要数字开头\n" +
                "3. 示例：EnderWarrior、Creeper42\n" +
                "只需返回名称，不要其他内容！";
        String name = aiClient.getAIReply(prompt, true);

        if (name != null && name.matches("[a-zA-Z][a-zA-Z0-9_]{2,11}")) {
            botName = name.trim();
        } else {
            String[] fallback = {"NetherScout", "EnderVoyager", "StoneCutter"};
            botName = fallback[new Random().nextInt(fallback.length)];
            getLogger().warning("使用备用名称: " + botName);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().trim();

        // 过滤不处理的消息
        if (shouldSkipMessage(player, message)) return;

        // 构建上下文并异步处理
        String context = buildAIRequestContext(player, message);
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> processAIReply(context));
    }

    // 判断是否跳过消息
    private boolean shouldSkipMessage(Player player, String message) {
        // 基础过滤
        if (player.getName().equals(botName)) return true;
        if (message.length() < getConfig().getInt("reply_rules.min_length")) return true;
        if (getConfig().getStringList("reply_rules.blacklist").stream().anyMatch(message::contains)) return true;

        // 高级过滤
        return message.matches(".*[\\s\\p{Punct}]{3,}.*") ||  // 无效符号
                message.matches(".*(重复|刚才说啥|上一句).*");   // 禁止回溯对话
    }

    // 构建AI请求上下文
    private String buildAIRequestContext(Player player, String message) {
        List<String> history = chatHistory.getOrDefault(player.getUniqueId(), new ArrayList<>());
        history.add(player.getName() + ": " + message);

        // 维护历史记录长度
        int maxHistory = getConfig().getInt("context.max_length");
        while (history.size() > maxHistory) history.remove(0);
        chatHistory.put(player.getUniqueId(), history);

        return systemPrompt + "\n当前对话：\n" + String.join("\n", history);
    }

    // 处理AI回复
    private void processAIReply(String context) {
        String rawResponse = aiClient.getAIReply(context, false);
        if (rawResponse == null) return;

        // 解析置信度和内容
        String[] parts = rawResponse.split("\\|", 2);
        if (parts.length != 2) return;

        try {
            double confidence = Double.parseDouble(parts[0]);
            String reply = parts[1].trim();

            if (confidence >= getConfig().getDouble("reply_rules.require_reply_threshold") &&
                    isValidReply(reply)) {
                Bukkit.getScheduler().runTask(this, () ->
                        Bukkit.broadcastMessage("<" + botName + "> " + reply)
                );
            }
        } catch (NumberFormatException e) {
            getLogger().warning("AI返回格式错误: " + rawResponse);
        }
    }

    // 验证回复合法性
    private boolean isValidReply(String reply) {
        if (reply.isEmpty() || reply.length() > 40) return false;

        // 过滤禁止话题和重复内容
        String bannedRegex = String.join("|", getConfig().getStringList("reply_rules.banned_topics"));
        return !reply.matches(".*(" + bannedRegex + ").{3,}.*") &&  // 禁止连续提及敏感词
                !reply.matches(".*([:：][)）]|>_<){2,}.*");           // 禁止多个表情
    }

    public static ChatBot getInstance() {
        return instance;
    }
}