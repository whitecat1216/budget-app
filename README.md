# 予算管理アプリ (Budget Management App)

Java + Spring Boot + MyBatis + PostgreSQL で作ったシンプルな予算管理Webアプリです。

## 機能

- **ダッシュボード**: 月次の収入・支出・収支サマリー、カテゴリ別支出、予算達成状況
- **取引管理**: 収入・支出の登録・一覧・削除
- **予算管理**: カテゴリごとの月次予算設定と進捗確認
- **予実レポート**: 月ごとのカテゴリ別予算・支出実績・残額・執行率
- **企業登録・認証**: メール認証後に企業テナントを作成し、ログインして利用
- **部門・プロジェクト**: 予算・取引・申請を部門・プロジェクトに関連付け
- **支出申請・承認**: 下書き、提出、承認、差戻し、却下と履歴
- **ユーザー・ロール**: メール招待、ロールに基づく画面操作の制御
- **監査ログ**: ログイン、予算・取引・マスタ・申請の主要操作を記録
- **カテゴリ管理**: 収入/支出カテゴリの追加・削除（初期データ付き）

## 技術スタック

| 項目 | 技術 |
|------|------|
| 言語 | Java 17 |
| フレームワーク | Spring Boot 3.3 |
| 永続化 | MyBatis 3 |
| DB | PostgreSQL 16 |
| UI | Thymeleaf + Bootstrap 5 |
| 認証 | Spring Security 6、セッション、BCrypt |
| メール | Spring Boot Mail / SMTP |
| コンテナ | Docker / Docker Compose |

## 起動方法（Docker推奨）

### 前提
- Docker と Docker Compose がインストールされていること

### 手順

```bash
# プロジェクトを展開したディレクトリで
docker compose up --build
```

起動後、ブラウザで以下にアクセスしてください。

**http://localhost:8080**

初回起動時にDBスキーマと初期カテゴリが自動作成されます。

企業登録にはSMTP設定が必要です。Docker Compose起動前に`.env`を作成し、メール事業者の設定を記入してください。

```dotenv
SPRING_MAIL_HOST=smtp.example.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-smtp-user
SPRING_MAIL_PASSWORD=your-smtp-password
APP_MAIL_FROM=no-reply@example.com
APP_PUBLIC_URL=http://localhost:8080
```

SMTP未設定の状態ではメール確認リンクを送れないため、企業登録は完了しません。`APP_PUBLIC_URL`はメールに載せる公開URLに合わせてください。既存の初期テナントのデータにログインする場合は、初回起動時に一度だけ管理者を作れるよう、`BOOTSTRAP_ADMIN_EMAIL`と12文字以上の`BOOTSTRAP_ADMIN_PASSWORD`を設定します。認証後はユーザー・権限画面からメンバーを招待できます。

停止する場合:

```bash
docker compose down
```

データも削除したい場合:

```bash
docker compose down -v
```

## ローカル開発（Dockerなし）

1. PostgreSQL を起動し、以下のDBを作成

```sql
CREATE USER budgetuser WITH PASSWORD 'budgetpass';
CREATE DATABASE budgetdb OWNER budgetuser;
```

2. アプリ起動

```bash
# Maven が必要（このリポジトリにはMaven Wrapperを含めていません）
mvn spring-boot:run
```

環境変数で接続先を変更できます:

```bash
DB_HOST=localhost DB_PORT=5432 DB_NAME=budgetdb DB_USER=budgetuser DB_PASSWORD=budgetpass mvn spring-boot:run
```

## 画面構成

| URL | 内容 |
|-----|------|
| `/` | ダッシュボード（月次サマリー） |
| `/transactions` | 取引の登録・一覧 |
| `/budgets` | 月次予算の設定・進捗 |
| `/reports/budget-vs-actual` | 月次の予算実績対比 |
| `/categories` | カテゴリ管理 |

## プロジェクト構成

```
budget-app/
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/example/budget/
    │   ├── BudgetAppApplication.java
    │   ├── controller/     # Webコントローラー
    │   ├── service/        # ビジネスロジック
    │   ├── mapper/         # MyBatis Mapper
    │   └── model/          # エンティティ
    └── resources/
        ├── application.yml
        ├── schema.sql      # テーブル定義
        ├── data.sql        # 初期カテゴリ
        └── templates/      # Thymeleaf画面
```

## カスタマイズのヒント

- 初期カテゴリを変更したい → `src/main/resources/data.sql`
- ポートを変更したい → `docker-compose.yml` の `ports` や `application.yml` の `server.port`
- メール設定を変更したい → `SPRING_MAIL_*`環境変数を設定

---

作成日: 2026年

## SaaS仕様との対応状況

`dogs/`の仕様書に合わせ、企業登録・メール確認・ログイン、ユーザーロール、テナント単位の基本的なSQL分離、部門・プロジェクト、支出申請・承認、主要操作の監査ログを追加しています。既存の家計簿データは初期テナントID 1へ移行されます。

会計APIの実同期とMFAは未実装です。会計サービス製品と認証方式が未指定のため、接続成功を装う処理は置いていません。予算変更申請は承認後に反映され、部門・プロジェクト範囲、アカウント停止・ロール変更、パスワード再設定、認証系エンドポイントのプロセス内レート制限を実装しています。テナント分離はアプリケーションのtenant_id条件とサービス検証に依存し、DB RLS・全Mapperの境界試験は未完了です。マルチインスタンス運用では共有ストア型レート制限に置き換えてください。メール送信にはSMTP設定が必要です。
