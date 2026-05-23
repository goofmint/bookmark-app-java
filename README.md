# Bookmark App for Java

Spring BootとSQLiteで動くローカル用ブックマーク管理アプリです。

## 起動

```sh
./gradlew run
```

ブラウザで http://localhost:8080 を開きます。

SQLiteのデータベースファイルは、初回起動時にプロジェクト直下の `bookmarks.db` として作成されます。

## 機能

- URLだけでブックマークの追加
- サーバーサイドでHTMLタイトルを取得
- 保存済みブックマークの一覧表示
- `title`, `url`, `ogp_image_url` の保存
