# DiscordSRV-ButtonLink

DiscordSRVの「Discordアカウント連携」機能を、Discord側の**ボタン**から使えるようにするアドオンプラグインです。

## 仕組み

DiscordSRVには元々アカウント連携機能があります。

1. Minecraft内で `/discord link` を実行すると4桁の認証コードが発行される
2. そのコードをDiscord Botに送る(通常はDMや特定チャンネルに直接コードを打つだけ)と連携完了

このアドオンは (2) の部分を「ボタンを押す→案内に従ってコードを送る」という、より分かりやすいUIに変えるものです。

- 指定したDiscordチャンネルに「アカウント連携」ボタン付きのメッセージを自動投稿します
- ボタンを押すと、そのユーザーだけに見えるメッセージで「〇〇秒以内にコードを送ってください」と案内します
- 案内後にそのチャンネルへ送られたメッセージを認証コードとして扱い、DiscordSRV本体の連携処理 (`AccountLinkManager#process`) にそのまま渡します
- 送信されたコードのメッセージは自動削除され、結果メッセージも一定時間後に自動削除されます

**注意:** DiscordSRVが内部で使用しているJDA (Discordライブラリ) は v4系であり、Discordの「モーダル(入力ダイアログ)」機能が存在しません。そのため「ボタン→ポップアップ入力欄」という完全なモーダルUIは実現できず、「ボタン→チャンネルにメッセージとしてコード送信」という形になっています。

このアドオン自体は独自のDiscord Botトークンを持ちません。DiscordSRV本体が接続しているBotにそのまま相乗りします。

## 必要環境

- Paper (または Spigot) 1.21.x
- [DiscordSRV](https://modrinth.com/plugin/discordsrv) が導入・設定済みで、Discordアカウント連携が有効になっていること

## ビルド方法

```bash
mvn package
```

`target/discordsrv-button-link-1.0.0.jar` が生成されます。

GitHub Actions (`.github/workflows/build.yml`) がpushのたびに実際にPaperMC/DiscordSRVの配布リポジトリへアクセスできる環境で `mvn package` を走らせ、jarをビルド成果物としてアップロードします。「Actions」タブの該当ワークフロー実行から `discordsrv-button-link` アーティファクトとしてダウンロードできます。

※ 開発時のサンドボックス環境ではネットワークポリシーにより PaperMC のMavenリポジトリ (`repo.papermc.io`) と外部のパッケージ配布サービス全般へ到達できなかったため、その場での `mvn package` 実行はできていません。代わりに、実際のDiscordSRV/JDA v4のソースコードから使用APIのメソッドシグネチャをすべて確認したうえで、それを再現したスタブ(スタブ自体はリポジトリには含めていません)に対して `javac` で本体コードを実コンパイルし、型・シグネチャレベルでの誤りがないことを確認済みです。ただしこの方法では**依存関係の座標(groupId/artifactId/バージョン/配布元リポジトリ)**までは検証できません。実際、最初のCI実行は依存関係解決の段階で2回失敗しました:

1. DiscordSRVをJitPack (`com.github.DiscordSRV:DiscordSRV`) 経由で取得しようとしたが、そもそもDiscordSRVはJitPackではなく公式のNexusリポジトリ (`nexus.scarsz.me`) で配布されており、座標も `com.discordsrv:discordsrv` が正しかった
2. その前段階で、JitPackのタグ命名 (`v1.30.5`) に関する思い込みで `v` の有無を間違えていた(これは(1)の誤りに気づく前の修正で、根本的な誤りではなかった)

現在は `com.discordsrv:discordsrv` + Nexusリポジトリという、DiscordSRV公式のサンプル (`DiscordSRV-ApiTest`) や実際に運用されている他のDiscordSRVアドオンのpom.xmlと同じ構成に修正済みです。

## 導入方法

1. `discordsrv-button-link-1.0.0.jar` を `plugins/` フォルダに配置
2. サーバーを起動すると `plugins/DiscordSRV-ButtonLink/config.yml` が生成される
3. `config.yml` の `addon.channel-id` にボタンを投稿したいDiscordチャンネルのIDを設定(未設定ならDiscordSRVのメインチャンネルが使われる)
4. サーバーを再起動

## config.yml

```yaml
addon:
  channel-id: "0"          # 0の場合はDiscordSRVのメインチャンネルを使用
  code-wait-seconds: 90    # ボタンを押してからコード送信までの待ち時間(秒)
  button-label: "アカウント連携"
  embed-title: "Minecraftアカウント連携"
  embed-description: |
    下のボタンを押してください。
    ゲーム内で `/discord link` を実行すると4桁の認証コードが発行されるので、
    ボタンを押した後の案内に従って、そのコードをこのチャンネルに送信してください。
```
