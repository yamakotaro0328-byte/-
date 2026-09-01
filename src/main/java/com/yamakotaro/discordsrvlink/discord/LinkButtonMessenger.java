package com.yamakotaro.discordsrvlink.discord;

import com.yamakotaro.discordsrvlink.ButtonLinkAddon;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * 連携ボタン付きのEmbedメッセージをDiscordチャンネルに送信/維持する。
 * 起動のたびに重複投稿しないよう、送信済みメッセージIDをファイルに記録する。
 */
public class LinkButtonMessenger {

    public static final String BUTTON_ID = "discordsrv-button-link:start";

    private final ButtonLinkAddon plugin;
    private final JDA jda;

    public LinkButtonMessenger(ButtonLinkAddon plugin, JDA jda) {
        this.plugin = plugin;
        this.jda = jda;
    }

    public void ensureButtonMessage() {
        TextChannel channel = resolveChannel();
        if (channel == null) {
            plugin.getLogger().warning("ボタンを送信するチャンネルが見つかりません。config.ymlのaddon.channel-id、"
                    + "またはDiscordSRVのメインチャンネル設定を確認してください。");
            return;
        }

        File stateFile = new File(plugin.getDataFolder(), "discord-message.properties");
        String existingMessageId = readMessageId(stateFile);
        if (existingMessageId != null) {
            channel.retrieveMessageById(existingMessageId).queue(
                    message -> { /* 既にボタンメッセージが存在するので何もしない */ },
                    error -> sendButtonMessage(channel, stateFile)
            );
            return;
        }
        sendButtonMessage(channel, stateFile);
    }

    private TextChannel resolveChannel() {
        String channelId = plugin.getConfig().getString("addon.channel-id", "0");
        if (channelId != null && !channelId.isBlank() && !channelId.equals("0")) {
            TextChannel channel = jda.getTextChannelById(channelId.trim());
            if (channel != null) {
                return channel;
            }
            plugin.getLogger().warning("addon.channel-idで指定されたチャンネルが見つかりません: " + channelId
                    + " -> DiscordSRVのメインチャンネルにフォールバックします。");
        }
        return DiscordSRV.getPlugin().getMainTextChannel();
    }

    private void sendButtonMessage(TextChannel channel, File stateFile) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(plugin.getConfig().getString("addon.embed-title", "Minecraftアカウント連携"))
                .setDescription(plugin.getConfig().getString("addon.embed-description", ""))
                .setColor(new Color(88, 101, 242));

        // JDA v4のMessageActionは addActionRow ではなく setActionRow(Component...) という名前
        Button button = Button.primary(BUTTON_ID, plugin.getConfig().getString("addon.button-label", "アカウント連携"));

        channel.sendMessage(embed.build())
                .setActionRow(button)
                .queue(message -> saveMessageId(stateFile, message.getId()));
    }

    private String readMessageId(File file) {
        if (!file.exists()) {
            return null;
        }
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
            String id = props.getProperty("messageId");
            return (id == null || id.isBlank()) ? null : id;
        } catch (IOException e) {
            return null;
        }
    }

    private void saveMessageId(File file, String messageId) {
        Properties props = new Properties();
        props.setProperty("messageId", messageId);
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileOutputStream out = new FileOutputStream(file)) {
                props.store(out, "DiscordSRV-ButtonLink managed file - do not edit manually");
            }
        } catch (IOException e) {
            plugin.getLogger().warning("discord-message.propertiesの保存に失敗しました: " + e.getMessage());
        }
    }
}
