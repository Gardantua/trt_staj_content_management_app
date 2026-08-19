# 01 - Sistem Mimarisi ve Modül Sınırları (Şekil 1)

Bu diyagram, **TRT tabii İçerik Etkileşim Platformu**'nun genel bileşen mimarisini, Clean Architecture katman sınırlarını, modüler monolit yapısını ve bağımsız **Leaderboard** mikroservisi ile olan ilişkisini kesişmeyen, temiz ve dikey hiyerarşik bir geometride gösterir.

---

## 🏛️ Şekil 1. Sistem Mimarisi ve Modül Sınırları (Mermaid Architecture Diagram)

```mermaid
flowchart TB
    %% 1. İSTEMCİ VE PROXY (ÜST KATMAN)
    subgraph Tier1["1. İstemci ve Kenar Yönlendirme Katmanı"]
        direction LR
        UserWeb["Kullanıcı Arayüzü (React SPA) [ / ]"]
        AdminWeb["Yönetim Paneli (React SPA) [ /admin ]"]
        Caddy["Caddy Ters Vekil Sunucusu\n• Otomatik TLS / Let's Encrypt\n• SPA Dağıtımı & /api Proxy"]
        UserWeb & AdminWeb -->|HTTPS:443| Caddy
    end

    %% 2. UYGULAMA SERVİSLERİ (ORTA KATMAN)
    subgraph Tier2["2. Uygulama Katmanı"]
        direction LR
        
        subgraph Monolith["Çekirdek Modüler Monolit (Spring Boot 8081)"]
            direction TB
            API["API Katmanı (Controllers & DTOs)"]
            AppDomain["Uygulama ve Saf Domain Modülleri\n(Gameplay, Quiz, Identity, Gamification)"]
            InfraPorts["Altyapı Adaptörleri\n(Spring Data JDBC, AMQP Publisher)"]
            API --> AppDomain --> InfraPorts
        end

        subgraph LeaderboardSvc["Leaderboard Mikroservisi (Spring Boot 8082)"]
            direction TB
            LB_In["RabbitMQ Olay Tüketicisi (Inbox)"]
            LB_Core["Sıralama Projeksiyon Motoru"]
            LB_API["Dahili Sıralama API'si"]
            LB_In --> LB_Core
            LB_API --> LB_Core
        end
    end

    %% 3. VERİ VE MESAJLAŞMA (ALT KATMAN)
    subgraph Tier3["3. Veri ve Mesajlaşma Altyapısı"]
        direction LR
        MonoDB[("Monolith PostgreSQL 17\n(Kalıcı Doğru Kaynak - SSOT)")]
        RabbitMQ[["RabbitMQ 4.1 Broker\n(quiz.exchange)"]]
        LB_DB[("Leaderboard PostgreSQL 17\n(Projeksiyon Verisi)")]
        LB_Redis[("Leaderboard Redis 8.2\n(Generation ZSET)")]
    end

    %% KESİŞMEYEN VE ANLAŞILIR GEOMETRİK BAĞLANTILAR
    Caddy -->|/api İstekleri| API
    InfraPorts -->|ACID Transaction| MonoDB
    InfraPorts -->|Outbox Olayları| RabbitMQ
    RabbitMQ -->|xp.changed.v1| LB_In
    API -.->|Dahili REST İstemcisi| LB_API
    LB_Core -->|Yazma ve Okuma| LB_DB
    LB_Core -->|ZSET Güncelleme| LB_Redis
```

---

## 📌 Mimari Açıklamalar

1. **Katmanlı Hiyerarşi (Top-Down):** İstekler en üstteki Caddy ters vekilinden girer, monolit API katmanına akar ve sırasıyla domain ile altyapı adaptörlerine iner.
2. **Çekirdek - Mikroservis Ayrımı:** Monolit ve Leaderboard mikroservisi yan yana konumlandırılmış; aralarındaki tek yönlü asenkron iletişim ortadaki RabbitMQ brokerı üzerinden kurulmuştur.
3. **Veri İzolasyonu:** Monolit doğrudan kendi PostgreSQL'ine, Leaderboard servisi ise kendi PostgreSQL ve Redis kaynaklarına erişir; veritabanı seviyesinde çapraz erişim kesinlikle engellenmiştir.
