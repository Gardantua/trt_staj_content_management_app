# 05 - Canlı Dağıtım, Ağ ve Konteyner Topolojisi (Şekil 5)

Bu diyagram, **TRT tabii İçerik Etkileşim Platformu**'nun **Oracle Cloud Free Tier VM** üzerindeki 8 izole Docker konteynerini, Caddy SSL/TLS ters vekilini ve ağ güvenlik sınırlarını kesişmeyen, net ve anlaşılır bir hiyerarşik düzende gösterir.

---

## 🌐 Şekil 5. Canlı Dağıtım ve Ağ Topolojisi (Mermaid Deployment Diagram)

```mermaid
flowchart TB
    %% 1. DIŞ İNTERNET (ÜST KATMAN)
    Clients["Kullanıcılar ve Yöneticiler (Web & Mobil Tarayıcılar)"]

    %% 2. ORACLE VM HOST ALTYAPISI
    subgraph HostVM["Oracle Cloud Infrastructure (Always Free VM - 12 GB RAM / Ubuntu Linux)"]
        direction TB

        %% Uç Güvenlik ve Proxy
        subgraph EdgeLayer["Uç Güvenlik ve Ters Vekil (Edge Proxy)"]
            direction LR
            Ports["Açık Dış Portlar: 80 (HTTP) ve 443 (HTTPS)"]
            Caddy["Caddy Web Server Konteyneri\n(https://hikayeizi.duckdns.org / TLS 1.3)"]
            Ports --> Caddy
        end

        %% Dahili Docker Köprü Ağı
        subgraph InternalNetwork["İzole Dahili Ağ (Docker Bridge: app-network - Dış Dünyaya Kapalı)"]
            direction TB
            
            %% Uygulama Konteynerleri Satırı
            subgraph AppTier["Uygulama Servisleri"]
                direction LR
                WebSPA["web Konteyneri\n(React SPA Statik Dağıtım)"]
                BackendApp["backend Konteyneri\n(Spring Boot Monolit - Port 8081)"]
                LBService["leaderboard-service Konteyneri\n(Spring Boot Mikroservis - Port 8082)"]
            end

            %% Veri ve Mesajlaşma Konteynerleri Satırı
            subgraph DataTier["Kalıcı Veri ve Mesajlaşma Katmanı"]
                direction LR
                MainPG[("postgres Konteyneri\n(Monolith PostgreSQL 17)")]
                Broker[["rabbitmq Konteyneri\n(RabbitMQ 4.1 Broker)"]]
                LBPG[("leaderboard-postgres Konteyneri\n(Projeksiyon PostgreSQL 17)")]
                LBRedis[("leaderboard-redis Konteyneri\n(Redis 8.2 ZSET)")]
            end
        end

        %% Kalıcı Disk Birimleri
        subgraph StorageTier["Kalıcı Disk Birimleri (Docker Named Volumes)"]
            Volumes[("caddy_data | postgres_data | leaderboard_postgres_data | media_data | rabbitmq_data")]
        end
    end

    %% DÜZ VE ANLAŞILIR AKIŞ ÇİZGİLERİ
    Clients -->|HTTPS:443| Ports
    Caddy -->|/ ve /admin Yolları| WebSPA
    Caddy -->|/api/* İstekleri| BackendApp
    
    BackendApp -->|JDBC Transaction| MainPG
    BackendApp -->|Outbox AMQP Publish| Broker
    BackendApp -.->|Dahili REST İstemcisi| LBService

    Broker -->|xp.changed.v1 Tüketimi| LBService
    LBService -->|Projeksiyon Yazımı| LBPG
    LBService -->|Sıralama Güncellemesi| LBRedis

    DataTier -.-> StorageTier
```

---

## 🔒 Güvenlik ve Ağ Mimarisi Özeti

1. **Sıfır Güven (Zero-Trust) Port İzolasyonu:** İnternete yalnızca Port 80 ve Port 443 açıktır. Veritabanları (`postgres`, `leaderboard-postgres`), kuyruk (`rabbitmq`) ve önbellek (`leaderboard-redis`) dış dünyaya tamamen kapalı olup yalnızca `app-network` içinde haberleşir.
2. **Kesişmeyen Dikey Akış:** İstekler Caddy'den uygulama servislerine (`AppTier`), oradan da kendi ilgili veritabanı veya mesajlaşma konteynerine (`DataTier`) doğrudan ve paralel bir geometride akar.
3. **Kalıcı Disk Güvencesi:** Konteynerler yeniden başlatılsa dahi tüm ilişkisel veriler, medya yüklemeleri ve SSL sertifikaları alttaki `StorageTier` Named Volume birimlerinde güvende kalır.
