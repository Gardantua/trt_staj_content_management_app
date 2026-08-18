# TRT tabii İçerik Etkileşim Platformu - Mimari ve Akış Diyagramları

Bu dizin, projenin IEEE formatındaki teknik raporunda ve sunum/savunma aşamalarında kullanılacak 5 temel **Mermaid** diyagramını ve açıklamalarını içerir.

---

## 📑 Diyagram İndeksi

| No | Dosya Adı | Diyagram Türü | Kapsam ve Açıklama |
| :--- | :--- | :--- | :--- |
| **1** | [`01_SISTEM_MIMARISI_VE_MODUL_SINIRLARI.md`](./01_SISTEM_MIMARISI_VE_MODUL_SINIRLARI.md) | Component / Layered Architecture | Modüler monolit yapısı, Clean/Hexagonal katman sınırları (`api`, `application`, `domain`, `ports`, `infrastructure`) ve Leaderboard mikroservis ayrımı. |
| **2** | [`02_VERITABANI_SEMA_ERD.md`](./02_VERITABANI_SEMA_ERD.md) | Entity-Relationship Diagram (ERD) | Monolith PostgreSQL ve Leaderboard PostgreSQL tabloları, Primary/Foreign Key ilişkileri, kısıtlamalar (Constraints) ve indeksler. |
| **3** | [`03_QUIZ_VE_GAMEPLAY_AKISI.md`](./03_QUIZ_VE_GAMEPLAY_AKISI.md) | Sequence Diagram | Server-Authoritative quiz akışı, sunucu saatine bağlı deadline üretimi, cevap doğrulama, `AWAITING_NEXT_QUESTION` durumu ve görsel önyükleme. |
| **4** | [`04_OLAY_GUDUMLU_MESAJLASMA_VE_OUTBOX.md`](./04_OLAY_GUDUMLU_MESAJLASMA_VE_OUTBOX.md) | Sequence / Event-Driven Flow | Dual-Write önleme, Transactional Outbox & Inbox deseni, RabbitMQ at-least-once dağıtımı, idempotent XP ve Redis sıralama güncellemesi. |
| **5** | [`05_CANLI_DAGITIM_VE_AG_TOPOLOJISI.md`](./05_CANLI_DAGITIM_VE_AG_TOPOLOJISI.md) | Deployment / Network Topology | Oracle Cloud Free Tier VM üzerindeki 8 izole Docker konteyneri, Caddy TLS reverse proxy, port izolasyonu ve kalıcı disk birimleri (Named Volumes). |

---

> [!NOTE]
> Tüm diyagramlar GitHub, IDE ve Markdown önizleyicilerinde doğrudan görsel olarak render edilecek şekilde saf Mermaid sözdizimiyle hazırlanmıştır.
