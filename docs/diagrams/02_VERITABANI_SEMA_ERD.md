# 02 - Veritabanı Şeması ve ERD Modelleri (Şekil 2-a ve Şekil 2-b)

Bu doküman, **TRT tabii İçerik Etkileşim Platformu**'nun gerçek Flyway migration dosyalarıyla (`V1` - `V18`) ve kod tabanıyla %100 birebir örtüşen ilişkisel veritabanı şemasını iki parça halinde sunar.

---

## 🗄️ Şekil 2-a. Monolit Çekirdek Veritabanı Varlık-İlişki Modeli (Mermaid ERD)

```mermaid
erDiagram
    %% KİMLİK VE OTURUM
    IDENTITY_USER_ACCOUNTS ||--o{ PASSWORD_RESET_TOKENS : "sahiptir"
    IDENTITY_USER_ACCOUNTS ||--o{ GAMEPLAY_ATTEMPTS : "çözer"
    IDENTITY_USER_ACCOUNTS ||--o{ GAMEPLAY_QUIZ_REWARD_CLAIMS : "hak_kazanır"
    IDENTITY_USER_ACCOUNTS ||--o{ XP_TRANSACTIONS : "kaydına_sahiptir"

    IDENTITY_USER_ACCOUNTS {
        uuid id PK
        varchar_150 email UK "Tekil e-posta"
        varchar_100 display_name "Görünen ad / Takma ad"
        varchar_255 password_hash "Bcrypt parola özeti"
        varchar_30 role "USER, EDITOR, ADMIN"
        timestamptz created_at
    }

    PASSWORD_RESET_TOKENS {
        uuid id PK
        uuid user_id FK "identity_user_accounts.id"
        varchar_64 token_hash UK "SHA-256 token özeti"
        timestamptz expires_at "30 dk geçerli"
        timestamptz consumed_at "Tek kullanımlık kilit"
    }

    %% İÇERİK KATALOĞU
    CONTENT_CATALOG_ITEMS ||--o{ CONTENT_SEASONS : "içerir"
    CONTENT_SEASONS ||--o{ CONTENT_EPISODES : "içerir"
    CONTENT_CATALOG_ITEMS ||--o{ QUIZ_DEFINITIONS : "bağlanır"
    CONTENT_EPISODES ||--o{ QUIZ_DEFINITIONS : "bağlanabilir"

    CONTENT_CATALOG_ITEMS {
        uuid id PK
        varchar_200 title "Dizi / Film / Program"
        varchar_50 type "SERIES, MOVIE, SHOW"
        varchar_30 status "DRAFT, PUBLISHED, ARCHIVED"
    }

    CONTENT_SEASONS {
        uuid id PK
        uuid content_id FK "UK(content_id, season_number)"
        int season_number
    }

    CONTENT_EPISODES {
        uuid id PK
        uuid season_id FK "UK(season_id, episode_number)"
        int episode_number
        int duration_in_seconds
    }

    %% DEĞİŞMEZ QUIZ YAZARLIK SÜRÜMLERİ
    QUIZ_DEFINITIONS ||--o{ QUIZ_VERSIONS : "sürümlendirilir"
    QUIZ_VERSIONS ||--o{ QUIZ_QUESTIONS : "içerir"
    QUIZ_QUESTIONS ||--o{ QUESTION_OPTIONS : "seçenekleri"

    QUIZ_DEFINITIONS {
        uuid id PK
        uuid content_id FK "content_catalog_items.id"
        uuid episode_id FK "content_episodes.id"
        varchar_30 status "DRAFT, PUBLISHED, ARCHIVED"
    }

    QUIZ_VERSIONS {
        uuid id PK
        uuid quiz_id FK "UK(quiz_id, version_number)"
        int version_number
        varchar_30 status "DRAFT, PUBLISHED"
        timestamptz published_at "Yayınlanınca IMMUTABLE"
    }

    QUIZ_QUESTIONS {
        uuid id PK
        uuid quiz_version_id FK "UK(quiz_version_id, question_order)"
        uuid media_asset_id FK "media_assets.id"
        int question_order "1, 2, 3..."
        text prompt "Soru metni"
        text accessible_prompt "Erişilebilirlik metni"
        int time_limit_in_seconds "Varsayılan: 30 sn"
        int points "Varsayılan: 10 puan"
    }

    QUESTION_OPTIONS {
        uuid id PK
        uuid question_id FK "UK(question_id, option_key)"
        varchar_10 option_key "A, B, C, D"
        text option_text "Şık metni"
        boolean is_correct "Sunucu gizli cevabı"
    }

    %% GAMEPLAY VE YAŞAM DÖNGÜSÜ
    QUIZ_VERSIONS ||--o{ GAMEPLAY_ATTEMPTS : "çözülür"
    GAMEPLAY_ATTEMPTS ||--o{ ATTEMPT_ANSWERS : "içerir"

    GAMEPLAY_ATTEMPTS {
        uuid id PK
        uuid user_id FK "identity_user_accounts.id"
        uuid quiz_version_id FK "quiz_versions.id"
        varchar_30 status "IN_PROGRESS, AWAITING_NEXT_QUESTION, COMPLETED, ABANDONED"
        int current_question_order
        int score "Toplam puan"
        int earned_xp "Kalıcı kazanılan XP"
        timestamptz question_deadline "Sunucu bitiş zaman damgası"
        timestamptz completed_at
    }

    ATTEMPT_ANSWERS {
        uuid id PK
        uuid attempt_id FK "UK(attempt_id, question_id)"
        uuid question_id FK "quiz_questions.id"
        uuid selected_option_id FK "question_options.id"
        boolean is_correct
        int score_awarded "0 veya 10"
        varchar_30 outcome "CORRECT, WRONG, TIMEOUT"
    }

    %% ÖDÜL KİLİDİ VE APPEND-ONLY XP DEFTERİ
    GAMEPLAY_QUIZ_REWARD_CLAIMS {
        uuid user_id PK "PK(user_id, quiz_id)"
        uuid quiz_id PK "quiz_definitions.id"
        uuid attempt_id UK "gameplay_attempts.id"
        timestamptz claimed_at
    }

    XP_TRANSACTIONS {
        uuid id PK
        uuid user_id FK "identity_user_accounts.id"
        int amount "XP miktarı"
        varchar_40 reason "QUIZ_COMPLETED, ADMIN_ADJUSTMENT"
        varchar_40 policy_version "FIRST_COMPLETION_SCORE_V2"
        varchar_150 reference_key UK "Tekil işlem referansı"
        uuid source_attempt_id UK "gameplay_attempts.id"
        timestamptz occurred_at
    }

    %% TRANSACTIONAL OUTBOX & INBOX
    OUTBOX_EVENTS {
        uuid event_id PK
        varchar_100 event_type "quiz.completed.v1, xp.changed.v1"
        varchar_100 aggregate_type "GAMEPLAY_ATTEMPT, XP_TRANSACTION"
        uuid aggregate_id
        text payload "JSON Olay İçeriği"
        varchar_30 status "PENDING, PUBLISHED, FAILED"
        timestamptz created_at
    }

    INBOX_MESSAGES {
        uuid event_id PK "Tekil mesaj ID (Idempotency kalkanı)"
        varchar_100 event_type
        varchar_100 consumer_name
        timestamptz processed_at
    }
```

---

## 🗄️ Şekil 2-b. Leaderboard Servisi PostgreSQL Projeksiyonu ve Veri Sahipliği

```mermaid
erDiagram
    LEADERBOARD_XP_ENTRIES {
        uuid event_id PK "Olay Tekil ID"
        uuid transaction_id UK "Monolith xp_transactions.id"
        uuid user_id "Kullanıcı Tekil Kimliği"
        uuid content_id "İçerik ID"
        int amount "XP Değeri"
        varchar_40 reason "QUIZ_COMPLETED, ADMIN_ADJUSTMENT"
        timestamptz occurred_at "İlk kazanım zamanı (Tie-break)"
        timestamptz consumed_at
    }
```

---

## 🛡️ Kritik Veri Bütünlüğü Kısıtlamaları (Constraints)

1. **`gameplay_quiz_reward_claims (PK: user_id, quiz_id)`:** Kullanıcının o quizden ilk tamamlama ödülünü yalnızca bir kez almasını garanti eder (ADR-0026).
2. **`attempt_answers (UK: attempt_id, question_id)`:** Aynı soruya yarış durumunda (race condition) ikinci bir cevabın kaydedilmesini veritabanı seviyesinde imkansız kılar.
3. **`xp_transactions (UK: reference_key, UK: source_attempt_id)`:** Append-only defterde aynı denemeden kaynaklanan mükerrer XP kaydı oluşmasını engeller.
4. **`leaderboard_xp_entries (UK: transaction_id)`:** RabbitMQ at-least-once tekrar iletiminde liderlik tablosu mikroservisinde çift kayıt oluşmasını önler.
