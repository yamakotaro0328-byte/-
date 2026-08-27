package com.yamakotaro.discordsrvlink;

import com.yamakotaro.discordsrvlink.discord.LinkButtonListener;
import com.yamakotaro.discordsrvlink.discord.LinkButtonMessenger;
import com.yamakotaro.discordsrvlink.listener.DiscordReadySubscriber;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * DiscordSRVのアカウント連携コード({@code /discord link} で発行される4桁コード)を
 * Discord側の「ボタン」から入力できるようにするアドオン。
 * <p>
 * 独自のBotやJDAインスタンスは持たず、DiscordSRVが起動・接続する既存のJDAに
 * 相乗りしてボタン/メッセージのイベントを処理する。
 */
public class ButtonLinkAddon extends JavaPlugin {

    private DiscordReadySubscriber discordReadySubscriber;
    private LinkButtonListener linkButtonListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (Bukkit.getPluginManager().getPlugin("DiscordSRV") == null) {
            getLogger().severe("DiscordSRVが見つかりません。このプラグインはDiscordSRVのアドオンです。DiscordSRVを導入してください。");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.discordReadySubscriber = new DiscordReadySubscriber(this);
        DiscordSRV.api.subscribe(discordReadySubscriber);

        // /reload等で既にDiscordSRVのJDAが起動済みの場合は即座にセットアップする
        JDA jda = DiscordSRV.getPlugin().getJda();
        if (jda != null) {
            setupDiscordSide(jda);
        }
    }

    @Override
    public void onDisable() {
        if (discordReadySubscriber != null) {
            DiscordSRV.api.unsubscribe(discordReadySubscriber);
        }
        JDA jda = DiscordSRV.getPlugin() != null ? DiscordSRV.getPlugin().getJda() : null;
        if (jda != null && linkButtonListener != null) {
            jda.removeEventListener(linkButtonListener);
        }
    }

    /**
     * DiscordSRVのJDAが利用可能になったタイミングで呼ばれる。
     */
    public void setupDiscordSide(JDA jda) {
        if (linkButtonListener == null) {
            linkButtonListener = new LinkButtonListener(this);
            jda.addEventListener(linkButtonListener);
        }
        new LinkButtonMessenger(this, jda).ensureButtonMessage();
    }
}
