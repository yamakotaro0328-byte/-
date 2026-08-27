package com.yamakotaro.discordsrvlink.discord;

import com.yamakotaro.discordsrvlink.ButtonLinkAddon;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.message.guild.GuildMessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 「アカウント連携」ボタン押下 → チャンネルへのコード送信 → DiscordSRVの連携処理、
 * という一連の流れを処理する。
 * <p>
 * DiscordSRVが同梱しているJDAはv4系でモーダル(入力ダイアログ)が無いため、
 * ボタンを押した後は通常のメッセージとしてコードを送ってもらう形にしている。
 */
public class LinkButtonListener extends ListenerAdapter {

    /** key = channelId + ":" + userId, value = 受付期限(epoch millis) */
    private final ConcurrentHashMap<String, Long> awaitingCode = new ConcurrentHashMap<>();

    private final ButtonLinkAddon plugin;

    public LinkButtonListener(ButtonLinkAddon plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onButtonClick(ButtonClickEvent event) {
        if (!LinkButtonMessenger.BUTTON_ID.equals(event.getComponentId())) {
            return;
        }

        String discordId = event.getUser().getId();
        AccountLinkManager linkManager = DiscordSRV.getPlugin().getAccountLinkManager();

        UUID existing = linkManager.getUuid(discordId);
        if (existing != null) {
            event.reply("すでにMinecraftアカウントと連携済みです。")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        long waitSeconds = plugin.getConfig().getLong("addon.code-wait-seconds", 90);
        String key = key(event.getChannel().getId(), discordId);
        awaitingCode.put(key, System.currentTimeMillis() + waitSeconds * 1000L);

        event.reply("✅ " + waitSeconds + "秒以内に、ゲーム内で `/discord link` を実行した際に表示される認証コードを"
                        + "このチャンネルに送信してください。")
                .setEphemeral(true)
                .queue();
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        User author = event.getAuthor();
        if (author.isBot()) {
            return;
        }

        String key = key(event.getChannel().getId(), author.getId());
        Long expiresAt = awaitingCode.get(key);
        if (expiresAt == null) {
            return;
        }
        if (System.currentTimeMillis() > expiresAt) {
            awaitingCode.remove(key);
            return;
        }

        String code = event.getMessage().getContentRaw().trim();
        AccountLinkManager linkManager = DiscordSRV.getPlugin().getAccountLinkManager();
        String resultMessage = linkManager.process(code, author.getId());

        deleteQuietly(event.getMessage());
        sendTemporaryReply(event.getChannel(), author, resultMessage);

        // 連携に成功していれば待受を終了、失敗(コード間違い等)なら期限内は再入力を許可する
        if (linkManager.getUuid(author.getId()) != null) {
            awaitingCode.remove(key);
        }
    }

    private void deleteQuietly(Message message) {
        message.delete().queue(ignored -> {}, error -> {});
    }

    private void sendTemporaryReply(TextChannel channel, User author, String resultMessage) {
        channel.sendMessage(author.getAsMention() + " " + resultMessage)
                .queue(sent -> sent.delete().queueAfter(15, TimeUnit.SECONDS, ignored -> {}, error -> {}));
    }

    private String key(String channelId, String userId) {
        return channelId + ":" + userId;
    }

    /**
     * ボタンは押されたがコードが送られないまま期限切れになった待受状態を掃除する。
     * 呼び出さないまま放置すると、離脱者の分がメモリに残り続けてしまう。
     */
    public void purgeExpired() {
        long now = System.currentTimeMillis();
        awaitingCode.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}
