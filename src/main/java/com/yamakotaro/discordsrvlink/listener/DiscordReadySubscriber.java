package com.yamakotaro.discordsrvlink.listener;

import com.yamakotaro.discordsrvlink.ButtonLinkAddon;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;

/**
 * DiscordSRV独自のイベントバス({@code DiscordSRV.api}) 経由で購読するリスナー。
 * DiscordSRVのJDAが接続完了したタイミングで通知される。
 */
public class DiscordReadySubscriber {

    private final ButtonLinkAddon plugin;

    public DiscordReadySubscriber(ButtonLinkAddon plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onDiscordReady(DiscordReadyEvent event) {
        plugin.setupDiscordSide(DiscordSRV.getPlugin().getJda());
    }
}
