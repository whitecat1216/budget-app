# 予算管理SaaS 開発用仕様書

- 文書バージョン：1.4
- 更新日：2026-10-26
- ステータス：既存Javaアプリへの適用版（会計サービス製品・一部運用要件は未確定）
- 想定構成：Spring MVCサーバーサイドWebアプリ + PostgreSQL。会計同期はアダプター追加で拡張する。

## 1. システム概要

複数企業が利用するマルチテナント型の予算管理SaaS。企業はセルフサービス登録し、部門・プロジェクト別に予算を管理する。会計ソフトとはAPI連携し、実績データを同期・集計する。

### 1.1 確定要件

- 企業によるセルフサービス登録
- 部門とプロジェクトを予算管理ディメンションとして扱う
- 会計ソフトAPI連携。CSVのみの実装に限定しない
- テナント間の完全な論理データ分離
- 承認前の予算変更を実効予算に反映しない
- 自己承認禁止
- 同期・承認の再試行に対する冪等性

## 2. 技術要件（現行リポジトリに合わせた構成）

| 層 | 採用技術 | 要件・補足 |
|---|---|---|
| 言語・実行環境 | Java 17 | Mavenで依存関係・ビルドを管理 |
| Web | Spring Boot 3.3.4 / Spring MVC | Controller → Service → MyBatis Mapper。HTML画面をサーバーで描画 |
| 画面 | Thymeleaf + Bootstrap 5.3 + 必要箇所のブラウザーJavaScript | JavaScriptは表示補助・入力補助・非同期API呼び出しに限定。認可・業務検証は必ずSpring側で実施 |
| 認証・認可 | Spring Security 6、HTTPセッション、BCrypt | メール＋パスワード認証、CSRF保護、ロール判定。本番ではHTTPSとSecure Cookieを使用 |
| メール | Spring Boot Mail / SMTP | 登録・招待の確認リンク送信。SMTP資格情報は環境変数またはSecret Managerから渡す |
| DB・永続化 | PostgreSQL 16、MyBatis 3 | 金額はNUMERIC、トランザクションとDB制約を使用。業務テーブルはtenant_idで分離 |
| 実行・配布 | Docker Compose | アプリとPostgreSQLを起動。本番のDB/SMTP秘密情報をComposeファイルに直書きしない |
| 会計連携 | JavaコネクタSPI（接続先未確定） | 製品ごとのOAuth/API仕様を実装し、共通同期処理から呼び出す |
| 非同期処理 | Spring TaskExecutor / Schedulerを候補 | オンラインAPIとは別ワーカーで実行し、オンラインと同一PostgreSQLを使用する。同期頻度・キュー製品は未確定 |
| テスト | JUnit 5、Spring Boot Test、Testcontainersを候補 | PostgreSQL結合試験、テナント分離・承認競合の試験を必須化 |

React、FastAPI、Pythonの別サービスは現行構成では採用しない。画面はThymeleaf、業務処理はSpring Bootにまとめる。新たなAPIが必要になった場合はSpring MVCにREST Controllerを追加し、同じService層と認可処理を共有する。会計コネクタの実装には対象製品のAPI仕様、OAuthクライアント情報、利用可能なスコープが必要である。

JavaScriptは画面の入力補助、表示更新、グラフ、Spring MVC APIの呼び出しに使う。ブラウザーからDBや会計サービスへ直接接続せず、テナントID・権限・金額などの業務ルールをJavaScriptだけで確定しない。画面で行う検査は操作性向上のための補助とし、同じ検査をサーバー側でも実施する。現在の実装例は予実レポートの部門選択に連動したプロジェクト絞り込みである。

オンライン処理とバッチ処理はアプリケーション実行経路を分けるが、初期構成ではDBを分けない。両方が同一PostgreSQLを利用し、バッチは専用Executorで実行する。DB接続プールを共有するため、ワーカー数・ページサイズ・API同時実行数を制限する。負荷計測でオンライン処理への影響が確認された場合に限り、接続プール調整、読み取りレプリカ、分析用DB等を検討する。

## 3. アーキテクチャとテナント分離

### 3.1 リクエスト処理

1. Spring SecurityがセッションCookieを検証する。
2. UserDetailsServiceがユーザーのtenant_idとロールをサーバー側で解決する。
3. ControllerとServiceはクライアント送信tenant_idを採用せず、認証Principalから取得する。
4. MyBatisのSELECT/UPDATE/DELETEにtenant_id条件を付け、INSERTにはPrincipal由来tenant_idを設定する。
5. 重要操作を業務更新と同じDBトランザクション内で監査ログへ記録する。

クライアントから送られた `tenant_id` を無条件に信頼してはならない。所属確認済みのテナントのみを許可する。複数テナント所属を許可する場合は、アクティブテナント切替時にも毎回検証する。

### 3.2 DB分離ルール

- 原則、業務テーブルに `tenant_id` を持たせる。
- 一意制約は必要に応じて `(tenant_id, business_key)` の複合制約とする。
- テナント配下の外部キーは、異なるテナントのレコードを参照できない制約を検討する。
- 現行構成はMyBatis SQLの明示的tenant_id条件を分離境界とする。PostgreSQL RLSは未設定であり、本番公開前に採用要否を判断し、DBロールと接続プールを含めて試験する。
- キャッシュキー、ファイルパス、検索インデックス、エクスポート、一時ファイル、ジョブ引数にもテナントスコープを含める。
- SaaS運用者の特権アクセスは最小化し、理由・操作者・対象・時刻を監査記録する。

## 4. 認証・セルフサービス登録

### 4.1 Web登録フロー

1. `POST /register` で企業・初期管理者情報を受け付ける。
2. 入力値を検証し、レート制限・bot対策・重複検査を行う。
3. 検証トークンのハッシュを保存し、認証メールを送る。
4. `GET /verify-email?token=...` で期限・一回性を検証する（DBにはトークンハッシュのみ保存）。
5. 登録時に作成した保留テナントと初期管理者を、同一DBトランザクション内で有効化する。
6. 初期管理者は `/onboarding` で企業名・年度開始月・初期部門を設定し、テナントのセットアップ状態を完了にする。

実装方式として、未認証の仮登録レコードを先に作る方式、認証後に本テナントを作成する方式のいずれも可能。認証前に本番業務データを利用できないこと、期限切れデータを安全に清掃できることを満たす。

### 4.2 登録セキュリティ

- メールアドレスを正規化し、重複判定ルールを明確化する。
- 現行実装はSpring SecurityのBCryptPasswordEncoder（cost 12）でパスワードをハッシュ化する。Argon2idへの移行は依存関係・運用コストを評価した後続判断とする。
- 検証トークンは十分なエントロピーを持たせ、DBにはハッシュで保存する。
- トークンは一回使用後に無効化し、有効期限を設ける。
- 認証メール再送と登録APIにレート制限を設ける。
- アカウントの存在を過度に推測できないレスポンスを検討する。
- 企業名のみを一意キーにしない。法人識別番号を使う場合は、対応国・地域と法的根拠を確定する。
- 初期管理者の復旧手順を設ける。管理者が1名だけの状態を許容する場合、ロックアウト対策を必須とする。
- プラン・試用期間・ユーザー上限等は `tenant_subscription` 等で拡張可能にする。

### 4.3 初期セットアップ

現在の `/onboarding` 画面で次を設定する。完了状態はテナント単位で保存し、初期管理者の初回ログイン時に案内する。

- 企業表示名
- 会計年度の開始月（1〜12月）
- 任意の初期部門コード・部門名
- 現行は円表示と日本の日時運用のため、基準通貨JPY・タイムゾーンAsia/Tokyoに固定する
- 勘定科目、プロジェクト設定、ユーザー招待、会計連携は各機能・後続フェーズで設定する。現行カテゴリを勘定科目として扱わない

複数通貨・複数タイムゾーンを有効にする前に、画面の金額・日付表示、計算、DB精度、帳票、同期データを一貫して対応させる。

## 5. データモデル

すべてのテナント業務テーブルは原則 `tenant_id` を持つ。主キーはUUID等の推測困難なIDを推奨する。以下は論理モデルであり、実DDLはDB方言に合わせて作成する。

### 5.1 主要テーブル

#### `tenants`
- `id`
- `name`
- `status` (`pending_verification`, `active`, `suspended`, `closed`)
- `timezone`
- `locale`
- `base_currency`
- `fiscal_year_start_month`
- `created_at`, `updated_at`

#### `tenant_registrations`
- `id`
- `normalized_email`
- `company_name`
- `verification_token_hash`
- `expires_at`
- `verified_at`
- `status`
- `created_at`
- 生パスワードや認証トークンは保存しない。初期管理者アカウントの作成方式に応じて必要項目を調整する。

#### `users`
- `id`
- `email_normalized`
- `password_hash` または外部IdP識別子
- `display_name`
- `status`
- `email_verified_at`
- `created_at`, `updated_at`

#### `tenant_memberships`
- `tenant_id`
- `user_id`
- `role`
- `status`
- `invited_by`
- `joined_at`
- 制約：`UNIQUE(tenant_id, user_id)`

#### `user_scopes`
- `tenant_id`
- `user_id`
- `scope_type` (`department`, `project`, `all`)
- `scope_id`（全社スコープはNULL等の規則を定義）
- `permission_set`

#### `departments`
- `id`, `tenant_id`
- `code`, `name`
- `parent_department_id`
- `manager_user_id`
- `active_from`, `active_to`
- `status`
- 制約：`UNIQUE(tenant_id, code)`

#### `projects`
- `id`, `tenant_id`
- `code`, `name`
- `department_id`
- `owner_user_id`
- `start_date`, `end_date`
- `status` (`draft`, `active`, `on_hold`, `completed`, `archived`)
- `external_code`（必要な場合）
- 制約：`UNIQUE(tenant_id, code)`

#### `account_codes`
- `id`, `tenant_id`
- `code`, `name`, `category`
- `active_from`, `active_to`
- `status`
- 制約：`UNIQUE(tenant_id, code)`

#### `fiscal_years`
- `id`, `tenant_id`
- `name`, `start_date`, `end_date`
- `status`
- 制約：期間重複を禁止するかは会計年度方針に従う。

#### `budgets`
- `id`, `tenant_id`
- `fiscal_year_id`
- `department_id`
- `project_id`（共通費を許容する場合はNULL可）
- `account_code_id`
- `period_start`, `period_end`
- `initial_amount`（固定小数または最小通貨単位の整数）
- `currency`
- `version`
- `status`
- `created_by`, `created_at`, `updated_at`

#### `budget_change_events`
- `id`, `tenant_id`, `budget_id`
- `change_amount`
- `reason`
- `status` (`pending`, `approved`, `rejected`, `cancelled`)
- `request_id`
- `approved_at`, `approved_by`
- `idempotency_key`
- 制約：同じ変更イベントの二重確定を防ぐ一意制約を設ける。

#### `expense_requests`
- `id`, `tenant_id`
- `request_number`
- `department_id`, `project_id`, `account_code_id`
- `budget_id`
- `title`, `purpose`
- `amount`, `currency`
- `requested_by`
- `status`
- `submitted_at`, `approved_at`
- `created_at`, `updated_at`

#### `approval_steps`
- `id`, `tenant_id`
- `request_type`, `request_id`
- `step_order`
- `approver_user_id` または approver role/rule
- `status`
- `comment`
- `acted_at`
- 自己承認を検証するため、申請者と承認者を比較できる情報を保持する。

#### `actuals`
- `id`, `tenant_id`
- `source_connection_id`
- `external_record_id`
- `external_line_id`（明細単位の識別子がある場合）
- `department_id`, `project_id`, `account_code_id`
- `posting_date`
- `amount`, `currency`
- `description`
- `source_updated_at`
- `sync_run_id`
- `status` (`active`, `reversed`, `excluded`)
- 制約：`UNIQUE(tenant_id, source_connection_id, external_record_id, external_line_id)` 等で重複を防ぐ。

#### `accounting_connections`
- `id`, `tenant_id`
- `provider_code`
- `display_name`
- `status`
- `auth_type`
- `encrypted_credentials` または秘密管理サービス参照
- `scopes`
- `last_successful_sync_at`
- `created_by`, `created_at`, `updated_at`

#### `accounting_mappings`
- `id`, `tenant_id`, `connection_id`
- `entity_type` (`department`, `project`, `account_code` 等)
- `local_entity_id`
- `external_code`
- `external_name`
- `mapping_status`
- 制約：外部コードの重複ルールはコネクタ仕様に合わせる。

#### `sync_runs`
- `id`, `tenant_id`, `connection_id`
- `job_type`
- `status`
- `started_at`, `finished_at`
- `cursor_before`, `cursor_after`
- `records_seen`, `records_created`, `records_updated`, `records_skipped`, `records_failed`
- `error_code`, `safe_error_summary`
- `correlation_id`

#### `audit_logs`
- `id`, `tenant_id`（SaaS全体イベントは別スコープを定義）
- `actor_user_id`
- `action`
- `resource_type`, `resource_id`
- `result`
- `occurred_at`
- `correlation_id`
- `metadata`（機密情報を含めない）

### 5.2 金額・通貨

- 浮動小数点型を金額保存に使わない。
- 通貨の小数桁と丸め規則を定義する。
- 複数通貨を許容する場合、原通貨金額、換算レート、換算後金額、レート日付を別々に保持する。
- 初期リリースで複数通貨をサポートするかは未確定。

## 6. 集計ロジック

### 6.1 定義

- `current_budget = initial_budget + SUM(approved budget_change_events.change_amount)`
- `actual_total = SUM(active actuals.amount within requested period)`
- `remaining = current_budget - actual_total`
- `execution_rate = actual_total / current_budget * 100`
- `current_budget = 0` のとき執行率はNULL等で返し、UIでは `—` と表示する。
- 予約額を採用する場合は `available = current_budget - actual_total - active_reservations` とし、実績化時に予約を解放する。

### 6.2 ディメンションと二重計上

- 部門別集計では部門に属する全対象データを集計する。
- プロジェクト別集計では対象プロジェクトのデータを集計する。
- 全社合計は基礎となる予算明細・実績明細から一度だけ集計する。部門合計とプロジェクト合計を加算して全社合計を作ってはならない。
- プロジェクトに紐づかない共通費の扱いを明示する。
- マスタの名称変更後も、履歴上のIDと適切なスナップショット等で過去レポートの解釈を保つ。

## 7. 承認・更新のトランザクション

- 提出時に入力値・権限・対象予算・状態を再検証する。
- 承認時は対象行をロックする等の方法で同時操作を直列化する。
- 状態遷移を条件付き更新で実施し、既に処理済みの申請を再承認しない。
- 承認処理と承認履歴・監査ログの作成を同一トランザクションで扱う。
- 予算変更イベントは承認完了時に一度だけ集計対象となる。
- 外部通知はトランザクション内で直接送らず、Transactional Outbox等を用いて確実に配信することを検討する。
- 承認者はロールとデータ範囲に合致し、申請者本人ではないことをサーバー側で確認する。

## 8. 会計ソフトAPI連携基盤

### 8.1 共通インターフェース

各会計ソフトに固有の処理をコネクタへ閉じ込め、共通サービスは以下のような操作を呼び出す。

```text
AccountingConnector
  - authorize() / refresh_credentials()
  - test_connection()
  - fetch_accounts(cursor, date_range)
  - fetch_departments(cursor, date_range)
  - fetch_projects(cursor, date_range)       # 対応ソフトのみ
  - fetch_actuals(cursor, date_range)
  - normalize_record(provider_record)
  - revoke_connection()                      # 対応する場合
```

各メソッドの実際の引数・戻り値は共通DTOで定義する。提供元APIにない機能は「非対応」として明示し、成功したように見せかけない。

### 8.2 認証情報

- OAuth 2.0、APIキー、サービスアカウント等、提供元の正式な方式に従う。
- トークン・APIキーはKMS等の鍵管理と連携した暗号化、または専用シークレット管理サービスで保管する。
- DB・ログ・エラーメッセージ・フロントエンドへ秘密情報を平文で露出しない。
- リフレッシュトークン更新の排他制御を行い、同時更新による失効を防ぐ。
- 接続解除時にローカル秘密情報を失効・削除し、提供元の失効APIがあれば呼び出す。
- 最小スコープを要求し、接続権限を監査可能にする。

### 8.3 同期ジョブ

会計サービス未選定の間は、接続先固有のAPI呼び出しを実装したように見せない。先行して実装できるのは、共通DTO・コネクタSPI・実行履歴・ジョブ受付・状態照会・排他・冪等性などの接続先非依存部分である。手動同期要求はSpring側で認証・認可・テナント配下の接続を検証して受付し、実行IDを返す。同期本体はブラウザーやWebリクエスト内ではなく、Javaワーカーで処理する。

1. テナントと接続の有効性を検証する。
2. `sync_runs` を作成し、相関IDを発行する。
3. 保存済みカーソル・前回同期時刻・対象期間から取得範囲を決める。
4. ページングしながら外部データを取得する。
5. 外部データを共通DTOへ正規化する。
6. コードマッピングを適用し、未対応レコードをエラーキューへ記録する。
7. 外部一意キーに基づくupsert等で冪等に保存する。
8. カーソルを進め、件数と状態を保存する。
9. 成功・部分成功・失敗を確定し、必要に応じて管理者へ通知する。

バッチ実装時は次の境界を守る。

- 同期履歴テーブルを実行状態の正本とし、最低限 `QUEUED`、`RUNNING`、`SUCCEEDED`、`PARTIAL_SUCCESS`、`FAILED` を管理する。試行回数、相関ID、件数、開始・終了時刻、再開用カーソルも保持する。
- ジョブ引数には検証済みの `tenantId`、`connectionId`、`runId`、対象範囲を含め、ワーカー内でも接続のテナント所属を検証する。HTTPセッションやThreadLocalの認証状態を引き継がない。
- 外部通信中はDBトランザクションを保持せず、ページ単位で取込とカーソル更新を確定する。再実行時は `(connection_id, external_id)` 等の一意キーでupsertし、確定済みデータを二重登録しない。
- 同一接続に対する同時実行を排他制御し、接続先別にワーカー数を制限する。一時障害のみ上限付き再試行し、認証失敗・入力不備は要対応状態にする。
- 画面のJavaScriptは受付API呼び出しと実行状態の表示のみを担当する。認可、同期、再試行の判断、データ保存はJava側で行う。

現在は同一DB上の実行履歴をキューとして使い、SpringのスケジューラーからJavaワーカーを起動する。PostgreSQLの `SKIP LOCKED` と接続単位の一意制約で複数アプリインスタンス間の二重実行を防ぎ、リース切れジョブをカーソル位置から再開する。処理量が増えた場合に永続キューまたはSpring Batchの導入を判断する。会計プロバイダーの実装前に同期完了扱いにしてはならない。

### 8.4 再試行とレート制限

- 429、ネットワークタイムアウト、5xx等の一時エラーを識別する。
- `Retry-After` があれば尊重し、指数バックオフとジッターを適用する。
- 無限再試行は禁止し、上限到達後は手動対応対象にする。
- 400系のデータ不備・権限不足等は原因別に処理し、無意味な再試行を避ける。
- ジョブの重複実行をロック・キュー制御・一意キーで防ぐ。
- 同期途中に失敗した場合、確定済みページと未確定ページを安全に再処理できるようにする。
- 接続先ごとのAPI制限に応じて同時実行数を制限する。

### 8.5 マッピングと訂正

- 外部勘定科目・部門・プロジェクトコードとローカルマスタを対応付ける。
- 未マッピングデータは、明示的な運用ルールなしに任意の科目・部門へ自動割当しない。
- 外部取引の更新・取消を検知し、既存実績の更新・取消状態へ反映する。
- 会計期間締め後の変更をどう扱うかは業務ルールとして確定する。
- 初期範囲は会計ソフトから実績を取得する一方向連携を基本とする。仕訳登録などの書戻しは対象ソフト・権限・承認フローを確定後に追加する。

### 8.6 CSVの位置付け

CSVはAPI連携の代替として必須ではないが、API非対応ソフト・初期移行・障害時の補助手段として採用するかは別途決定する。採用する場合もAPI取込と同じ検証・重複防止・監査ログを通す。

## 9. API設計案

現行アプリはThymeleafを使うWeb MVCであり、画面操作はHTMLフォームとセッションCookieを使用する。以下のRESTパスは外部連携・将来のAPIクライアント用の設計案で、現在すべてがAPIとして実装されていることを示さない。新しいREST Controllerを追加する場合もService層で同じtenant_idとロール検査を適用する。

### 9.0 現行Web画面の主なルート

| Method | Path | 用途 |
|---|---|---|
| GET/POST | `/register` | 企業・初期管理者の登録開始 |
| GET | `/verify-email` | 確認トークンによる登録完了 |
| GET/POST | `/login` | Spring Securityフォームログイン |
| POST | `/logout` | CSRF保護付きログアウト |
| GET/POST | `/team`, `/team/invite` | 管理者による招待・ロール付与 |
| GET/POST | `/requests` | 支出申請の作成と一覧 |
| POST | `/requests/{id}/submit` | 支出申請の提出 |
| POST | `/requests/{id}/review` | 承認・差戻し・却下 |
| GET | `/audit-logs` | テナント内監査ログ |

### 9.1 登録・認証

| Method | Path | 用途 |
|---|---|---|
| POST | `/registrations` | 企業登録開始 |
| POST | `/registrations/verify-email` | メール認証 |
| POST | `/registrations/resend-verification` | 認証メール再送 |
| POST | `/auth/login` | ログイン（認証基盤に応じて変更） |
| POST | `/auth/password-reset` | パスワード再設定開始 |
| POST | `/onboarding` | 初期セットアップ完了（実装済み） |

### 9.2 マスタ・予算・申請

| Method | Path | 用途 |
|---|---|---|
| GET/POST | `/departments` | 部門一覧・作成 |
| GET/PATCH | `/departments/{id}` | 部門詳細・更新 |
| GET/POST | `/projects` | プロジェクト一覧・作成 |
| GET/PATCH | `/projects/{id}` | プロジェクト詳細・更新 |
| GET/POST | `/account-codes` | 勘定科目一覧・作成 |
| GET/POST | `/fiscal-years` | 会計年度一覧・作成 |
| GET | `/dashboard` | ダッシュボード集計 |
| GET/POST | `/budgets` | 予算一覧・作成 |
| GET/PATCH | `/budgets/{id}` | 予算詳細・編集（状態・権限条件付き） |
| POST | `/budgets/{id}/submit` | 予算提出 |
| POST | `/budgets/{id}/changes` | 予算変更申請 |
| GET/POST | `/requests` | 支出申請一覧・作成 |
| POST | `/requests/{id}/submit` | 申請提出 |
| POST | `/requests/{id}/approve` | 承認 |
| POST | `/requests/{id}/return` | 差戻し |
| POST | `/requests/{id}/reject` | 却下 |
| GET | `/reports/budget-vs-actual` | 予算実績対比 |
| GET | `/audit-logs` | 監査ログ検索 |

### 9.3 会計連携

| Method | Path | 用途 |
|---|---|---|
| GET/POST | `/accounting/connections` | 接続一覧・接続開始 |
| GET | `/accounting/connections/{id}` | 接続状態 |
| POST | `/accounting/connections/{id}/test` | 接続テスト |
| POST | `/accounting/connections/{id}/sync` | 手動同期の受付 |
| POST | `/accounting/connections/{id}/disconnect` | 接続解除 |
| GET | `/accounting/connections/{id}/mappings` | マッピング取得 |
| PUT | `/accounting/connections/{id}/mappings` | マッピング更新 |
| GET | `/accounting/sync-runs` | 同期履歴 |
| POST | `/accounting/sync-runs/{id}/retry` | 再試行可能な失敗の再実行 |

同期受付・履歴照会・失敗実行の再試行APIと、同一PostgreSQL上のジョブキュー/Javaワーカーを実装済み。`AccountingSyncProcessor` が登録されていない接続先は受付を拒否する。OAuthコールバック等の公開エンドポイントはCSRF・state・redirect URI等を厳格に検証する。

### 9.4 API共通要件

- 入力検証と明示的な許可リストを使用する。
- リストAPIはページング、ソート許可リスト、絞込上限を持つ。
- 更新系APIは必要に応じて `Idempotency-Key` を受け付ける。
- 楽観ロック用の版番号またはETagを利用し、更新競合を検出する。
- エラーは安定したコード、利用者向けメッセージ、相関IDを返す。内部スタックトレースや秘密情報は返さない。
- 権限外のリソースIDに対する応答は、情報漏えいを避ける一貫した方針とする。

## 10. セキュリティ要件

- TLSを使用し、保存データ・バックアップ・シークレットを適切に暗号化する。
- 認証・認可はAPI側で必ず実施する。
- HTMLフォームはSpring Security CSRF保護を有効にする。セッションCookieはHTTPS環境でSecure、HttpOnly、SameSiteを設定する。
- 企業登録と招待のメールトークンは十分な乱数、期限、一回利用を必須とし、DBにはハッシュのみ保存する。
- ログイン・登録・パスワード再設定・招待は現行実装でプロセス内IP単位のレート制限を適用する。複数インスタンスでは共有ストアに移行し、アカウント単位の制限・bot対策も追加する。
- tenant_idは認証Principalから得て、クライアント入力を信用しない。全MapperのSELECT/UPDATE/DELETE/INSERTをテナント境界テストで監査する。
- ロールに加え、部門・プロジェクト責任者に割り当てたデータスコープをサーバー側で確認する。権限外の一覧・集計を返さず、ロール・範囲変更時には既存セッションを失効する。
- CSRF、XSS、SQLインジェクション、SSRF、IDOR、ファイルアップロード攻撃を考慮する。
- 添付ファイルの拡張子・MIME・サイズ制限、マルウェア検査、署名付きURL期限を設ける。
- セッション失効、パスワード再設定、メール変更、管理者権限変更を安全に実装する。
- 個人情報・会計情報のログ出力を制限する。
- 依存ライブラリ・コンテナイメージを定期スキャンし、脆弱性対応手順を定義する。
- 監査ログの改ざん防止、保存期間、参照権限を設定する。

## 11. テスト計画

### 11.1 単体テスト

- 集計式、ゼロ予算、マイナス残額、通貨丸め
- 予算・申請の状態遷移
- 自己承認禁止
- 承認済み変更イベントの一度限りの反映
- メール認証トークンの期限・一回性
- コードマッピング・外部データ正規化
- リトライ分類と冪等キー

### 11.2 結合・セキュリティテスト

- テナントAからテナントBのレコード・添付・エクスポートを取得できない
- API、検索、キャッシュ、ジョブ、監査ログでテナント分離が維持される
- 登録APIのレート制限、メール認証、重複登録制御
- 部門・プロジェクト範囲による認可
- 同時承認、二重クリック、再送信時の二重反映防止
- 外部APIの認証切れ、429、5xx、タイムアウト、ページング、差分取得
- 同期途中失敗後の再実行と重複防止
- マッピング不備データの隔離・再処理
- 認証情報がログ・APIレスポンス・フロントエンドに漏れないこと

### 11.3 受け入れテスト

1. 企業が自ら登録し、認証後に初期管理者として利用開始できる。
2. 部門・プロジェクト別に予算と実績を表示できる。
3. 全社合計が部門・プロジェクト内訳を二重計上しない。
4. 会計API同期後、対象期間の実績が正しく反映される。
5. 同じ同期を複数回実行しても実績が重複しない。
6. 同期失敗の理由・再試行状態を管理者が確認できる。
7. 重要操作を監査ログから追跡できる。

## 12. 運用・監視

- 登録成功率、メール送信失敗率、認証失敗、APIエラー率を監視する。
- 会計同期の成功率、最終成功時刻、遅延、失敗件数、レート制限回数を監視する。
- ジョブ滞留、キュー深度、DB接続数、CPU・メモリ、ストレージ使用量を監視する。
- 失敗ジョブの再試行・隔離・手動再実行の手順を整備する。
- バックアップとリストア訓練を行い、RPO/RTOは事業要件として確定する。
- 退会・契約終了時のデータエクスポート、保持、削除の手順を定義する。
- 外部会計APIの変更通知を監視し、コネクタ別に契約テストを行う。

## 13. 実装フェーズ案

### Phase 1：基盤・登録
- テナント、ユーザー、認証、メール検証
- テナント分離、ロール・データ範囲
- 初期セットアップ（企業名、年度開始月、任意の初期部門）、監査ログ基盤

### Phase 2：マスタ・予算
- 部門・プロジェクト・勘定科目・会計年度
- 予算作成・提出・変更・承認
- ダッシュボードと予算実績レポート（実績の初期投入方法は別途決定）

### Phase 3：支出申請・通知
- 申請、承認経路、差戻し・却下
- 通知、添付ファイル
- 承認競合と冪等性のテスト

### Phase 4：会計API連携
- 対象会計ソフトを1製品選定
- OAuth等の認証、接続設定、マッピング
- 差分同期、冪等取込、同期履歴、エラー監視
- 外部APIの契約テスト

### Phase 5：本番運用準備
- 負荷・セキュリティ試験
- バックアップ・復旧・監視・アラート
- 料金プラン・利用制限、運用管理画面
- データ保持・退会手順

## 14. 未確定事項

| 項目 | 実装前に確定する内容 |
|---|---|
| セルフ登録 | 法人識別番号、企業ドメイン検証、手動審査、試用期間 |
| ユーザー所属 | 1ユーザーが複数テナントに所属できるか |
| 会計年度 | 開始月、期間粒度、締め処理 |
| 予算構造 | 部門予算とプロジェクト予算の親子・配賦ルール、共通費 |
| 通貨 | 現行はJPY固定。将来の複数通貨、為替・税の扱い |
| 承認 | 金額帯、代理承認、合議、期限、超過申請 |
| 予約額 | 支出申請時点で予算残額から予約額を差し引くか |
| 会計ソフト | 初期対応製品、契約・API権限、取得可能データ、レート制限 |
| 双方向連携 | 実績取得のみか、仕訳登録・予算書戻しも行うか |
| 認証 | MFA、SSO、パスワードポリシー |
| 保存・運用 | データリージョン、監査ログ保存期間、RPO/RTO、退会時の削除方針 |
| 課金 | プラン、ユーザー数、プロジェクト数、同期頻度、容量制限 |

---

## 付録A：主要な不変条件

1. テナント配下のデータは、同一テナントの認可済みユーザーのみ参照できる。
2. 申請者本人は当該申請を承認できない。
3. 承認前の予算変更は現在予算に含めない。
4. 予算変更イベントは一度だけ実効予算に反映する。
5. 同一外部会計明細を複数回同期しても実績を重複登録しない。
6. 全社合計は基礎明細から計算し、部門・プロジェクト集計の合算で二重計上しない。
7. 外部認証情報はログ・画面・APIレスポンスに平文で出さない。
8. 重要操作は監査ログで追跡できる。
