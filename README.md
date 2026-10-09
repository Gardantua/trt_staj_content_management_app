# Hikaye İzi — İçerik Etkileşim ve Quiz Platformu

TRT Yeni Medya Kanal Koordinatörlüğü Yazılım Geliştirme Departmanındaki
27 Temmuz–21 Ağustos 2026 stajım kapsamında geliştirdiğim içerik etkileşim
platformudur. TRT ve tabii içerikleri üzerinde quiz çözme, XP kazanma ve
sıralama akışlarını uygular.

**[Canlı demo](https://hikayeizi.duckdns.org/) · [Mimari](docs/ARCHITECTURE.md) · [Teknik kararlar](docs/DECISIONS/README.md)**

## Projede yaptığım çalışmalar

- Java 21 ve Spring Boot ile içerik yönetimi, quiz sürümleme ve REST API akışları.
- Süre, cevap kabulü ve skorun sunucu tarafından belirlendiği quiz çözme modeli.
- PostgreSQL constraint'leri ve tekrarlı işlemlerde mükerrer XP üretimini önleyen kurallar.
- RabbitMQ ve Transactional Outbox/Inbox ile güvenilir asenkron mesajlaşma.
- Kendi PostgreSQL ve Redis verisine sahip ayrı leaderboard servisi.
- React ve TypeScript ile kullanıcı ve yönetim arayüzleri; Türkçe–İngilizce sunum.
- Domain, API ve gerçek altyapıyla Testcontainers entegrasyon testleri.
- Docker Compose, Caddy ve Oracle Cloud üzerinde HTTPS demo dağıtımı.

Bu depo bir staj projesini belgeler; TRT/tabii'nin resmî üretim sistemi veya
kurumsal kimlik entegrasyonu olduğu anlamına gelmez.

## Backend'e genel bakış

TRT ve tabii içeriklerini quiz ve oyunlaştırma yoluyla etkileşimli hale getiren,
API-first bir backend projesidir. Ana kullanıcı akışı:

```text
İçeriği yayınla → Quiz başlat → Cevapla → Tamamla → XP ver → Sırala
```

## Güncel kısa durum

- Çekirdek sistem modüler monolith'tir; leaderboard Aşama 11'de ayrı servise
  çıkarılmıştır.
- Java 21, Spring Boot, PostgreSQL, Flyway, RabbitMQ, Redis ve Testcontainers kullanılır.
- İçerik, quiz, gameplay, XP, mesajlaşma, leaderboard, medya ve yerel kullanıcı hesabı akışları uygulanmıştır.
- Admin web ve kullanıcı webi aynı backend'i kullanır; frontend güvenlik otoritesi değildir.
- Doğru cevap, süre, skor, attempt sahipliği ve XP hakkı sunucu tarafından belirlenir.
- PostgreSQL kalıcı doğru kaynaktır; Redis yeniden üretilebilir leaderboard read modelidir.
- İlk canlı demo için tek sunucu Compose, aynı-origin HTTPS, CSRF ve gerçek admin
  oturumu hazırlanmıştır; kurumsal OIDC sözleşmesi gelecekte ayrıca kesinleştirilecektir.

## Belgeler

- [Proje özeti](docs/PROJECT_BRIEF.md)
- [Kısa mimari](docs/ARCHITECTURE.md)
- [Kısa yol haritası](docs/ROADMAP.md)
- [Güncel durum özeti](docs/CURRENT_STATE.md)
- [Doküman dizini](docs/README.md)
- [Mimari kararlar](docs/DECISIONS/README.md)
- [Operasyon rehberi](docs/OPERATIONS.md)

## Yerel çalıştırma

Windows:

```powershell
docker compose up -d --wait
$env:LEADERBOARD_REMOTE_ENABLED="true"
$env:MAILERSEND_SMTP_USERNAME="MailerSend SMTP username"
$env:MAILERSEND_SMTP_PASSWORD="MailerSend SMTP password"
$env:MAILERSEND_FROM_ADDRESS="Doğrulanmış domain altındaki gönderen adresi"
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Şifre sıfırlama e-postaları MailerSend SMTP üzerinden gerçek alıcıya gider. Bilgileri
repoya veya sohbet mesajına yazma; yalnız çalıştırdığın terminalin ortam değişkeni
olarak tanımla. SMTP username/password, MailerSend panelindeki doğrulanmış domainin
`SMTP` bölümünden üretilir. `MAILERSEND_FROM_ADDRESS` aynı doğrulanmış domain altında
olmalıdır.

Monolith `8081`; leaderboard servisi `8082` portunu kullanır. Monolith PostgreSQL
`5433`, leaderboard PostgreSQL `5434`, RabbitMQ `5673`, monolith Redis `6380` ve
leaderboard Redis `6381` portundadır. Health kontrolleri:
`http://localhost:8081/actuator/health` ve
`http://localhost:8082/actuator/health`.

İlk mikroservis geçişinde mevcut XP satırları ADMIN yetkili
`POST /api/v1/admin/leaderboards/replay-xp-events` çağrısıyla idempotent biçimde
olaylara dönüştürülür. Ayrıntılar [leaderboard servis rehberinde](leaderboard-service/README.md)
ve [ADR-0031'de](docs/DECISIONS/ADR-0031-leaderboard-microservice-extraction.md)
bulunur.

## Production paketi

Oracle Always Free gibi tek Linux sunucusu için `compose.production.yaml` hazırdır.
Yalnız web katmanı internete açılır; kullanıcı uygulaması `/`, yönetim uygulaması
`/admin` adresindedir. Örnek secret dosyası ve ilk kurulum sırası
[operasyon rehberinde](docs/OPERATIONS.md) açıklanır. Repository hazırlığı gerçek
sunucu dağıtımı, domain doğrulaması ve yedek/restore provası yerine geçmez. 
Proje 15/08/2026 itibari ile https://hikayeizi.duckdns.org/ adresinde yayındadır.

## Çalışma kuralları

- Her görevde yalnızca roadmap'teki tek aşama ele alınır.
- Domain kuralları controller içine yazılmaz.
- Şema yalnız Flyway migration'larıyla değiştirilir.
- Idempotency, transaction, veri bütünlüğü, güvenlik ve hata davranışı testlerle korunur.
- Bir aşama tamamlanmadan sonraki aşamaya geçilmez.
- Her anlamlı görev sonunda `docs/CURRENT_STATE.md` güncellenir.

## Arşiv

Bu dosyanın önceki uzun sürümü ve diğer uzun çalışma belgeleri şu klasörde korunur:

- [Eski README](docs/archive/2026-08-11/README.md)
- [Eski mimari](docs/archive/2026-08-11/ARCHITECTURE.md)
- [Eski roadmap](docs/archive/2026-08-11/ROADMAP.md)
- [Eski current state](docs/archive/2026-08-11/CURRENT_STATE.md)

Arşiv dosyaları geçmiş kayıt ve ayrıntılı referanstır; günlük görevlerde önce kısa
belgeler okunmalıdır.
