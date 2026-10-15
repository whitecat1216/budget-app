# 予算管理アプリ (Budget Management App)

Java + Spring Boot + MyBatis + PostgreSQL で作ったシンプルな予算管理Webアプリです。

## 機能

- **ダッシュボード**: 月次の収入・支出・収支サマリー、カテゴリ別支出、予算達成状況
- **取引管理**: 収入・支出の登録・一覧・削除
- **予算管理**: カテゴリごとの月次予算設定と進捗確認
- **カテゴリ管理**: 収入/支出カテゴリの追加・削除（初期データ付き）

## 技術スタック

| 項目 | 技術 |
|------|------|
| 言語 | Java 17 |
| フレームワーク | Spring Boot 3.3 |
| ORM | MyBatis |
| DB | PostgreSQL 16 |
| UI | Thymeleaf + Bootstrap 5 |
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
# Maven が必要
./mvnw spring-boot:run
# または
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
- 認証を追加したい → Spring Security を導入

---

作成日: 2026年
