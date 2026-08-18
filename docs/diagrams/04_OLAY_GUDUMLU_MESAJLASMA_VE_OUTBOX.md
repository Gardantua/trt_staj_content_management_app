# 04 - Olay Güdümlü Mesajlaşma ve Outbox/Inbox Akışı (Şekil 4)

Bu diyagram, **TRT tabii İçerik Etkileşim Platformu**'ndaki **Transactional Outbox & Inbox** deseni, **RabbitMQ** asenkron olay dağıtımı, `xp_transactions` defteri ve **Redis Generation Tabanlı Sıralama** akışını gösterir.

---

## 📬 Şekil 4. Transactional Outbox/Inbox ve Olay Akışı (Mermaid Sequence)

```mermaid
sequenceDiagram
    autonumber
    participant Gameplay as Gameplay Modülü
    participant MonolithDB as Monolith PostgreSQL
    participant OutboxWorker as OutboxPublisherService
    participant Rabbit as RabbitMQ (quiz.exchange)
    participant Gamification as Gamification Consumer
    participant LBConsumer as Leaderboard Consumer (Ayrı Servis)
    participant LB_DB as Leaderboard PostgreSQL
    participant Redis as Redis (Generation ZSET)

    %% 1. ATOMİK OUTBOX KAYDI
    rect rgb(245, 248, 255)
        note over Gameplay, MonolithDB: 1. Dual-Write Koruması (Atomik Transaction)
        Gameplay->>MonolithDB: BEGIN TX: UPDATE gameplay_attempts (COMPLETED) + INSERT outbox_events (quiz.completed.v1)
        MonolithDB-->>Gameplay: COMMIT (Veri ve mesaj aynı anda kesinleşir)
    end

    %% 2. OUTBOX WORKER & RABBITMQ
    rect rgb(255, 250, 245)
        note over OutboxWorker, Rabbit: 2. Güvenilir Mesaj Yayımlama (At-Least-Once)
        OutboxWorker->>MonolithDB: SELECT * FROM outbox_events WHERE status='PENDING'
        OutboxWorker->>Rabbit: basicPublish('quiz.completed.v1')
        Rabbit-->>OutboxWorker: Publisher Confirm (ACK)
        OutboxWorker->>MonolithDB: UPDATE outbox_events SET status='PUBLISHED'
    end

    %% 3. GAMIFICATION & XP DEFTERİ
    rect rgb(245, 255, 245)
        note over Rabbit, Gamification: 3. İdempotent XP Kaydı (Append-Only Ledger)
        Rabbit->>Gamification: Mesajı ilet: quiz.completed.v1 (event_id=E1)
        Gamification->>MonolithDB: BEGIN TX
        Gamification->>MonolithDB: INSERT INTO inbox_messages (event_id=E1)
        alt İlk Teslimat
            Gamification->>MonolithDB: INSERT INTO xp_transactions (amount=40) + INSERT outbox_events (xp.changed.v1)
            Gamification->>MonolithDB: COMMIT
        else Tekrar Teslimat (Duplicate)
            Gamification->>MonolithDB: ROLLBACK (Inbox tekilliği sayesinde ikinci XP önlenir)
        end
        Gamification-->>Rabbit: basicAck
    end

    %% 4. LEADERBOARD & REDIS GENERATION
    rect rgb(255, 245, 245)
        note over OutboxWorker, Redis: 4. Leaderboard Servisi & Generation ZSET Güncellemesi
        OutboxWorker->>Rabbit: basicPublish('xp.changed.v1', tx_id=T1)
        Rabbit->>LBConsumer: Mesajı ilet: xp.changed.v1
        LBConsumer->>LB_DB: INSERT INTO leaderboard_xp_entries (transaction_id=T1)
        LBConsumer->>LB_DB: Deterministik sıralamayı hesapla (Toplam XP + İlk Kazanım Zamanı Tie-break)
        LBConsumer->>Redis: ZADD leaderboard:global:{generation} {score} {userId}
        note right of Redis: Generation geçişi ile atomik ve tutarlı liderlik tablosu
        LBConsumer-->>Rabbit: basicAck
    end
```

---

## 🔑 Çözülen Temel Problemler

1. **Dual-Write Probleminin Ortadan Kaldırılması:** İş verisi ve Outbox mesajı aynı SQL transaction'ında commit edildiği için ağ veya broker çökse bile hiçbir olay kaybolmaz.
2. **At-Least-Once Mesaj Teslimatında Idempotency:** Tüketici servislerdeki `inbox_messages` ve `transaction_id` tekillikleri mükerrer işlem yapılmasını %100 engeller.
3. **Redis Generation Modeli:** Redis sıralaması generation bazlı (`leaderboard:global:{gen}`) güncellenerek okuma esnasında yarış durumları ve eksik veri aktarımı engellenir.
