# TRT İçerik Etkileşim Backend Platformu

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

### Gereksinimler

- Java 21
- Docker Desktop ve Docker Compose
- Node.js 20.19 veya üzeri

### 1. Altyapıyı başlat

Proje kök dizininde:

```powershell
docker compose up -d --wait
```

Bu komut PostgreSQL, RabbitMQ, Redis ve `8082` portundaki leaderboard servisini
başlatır.

### 2. Backend'i başlat

Aynı dizinde yeni bir PowerShell penceresi aç:

```powershell
$env:LEADERBOARD_REMOTE_ENABLED="true"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Backend `http://localhost:8081` adresinde çalışır. Sağlık kontrolü:
`http://localhost:8081/actuator/health`.

### 3. Web uygulamasını başlat

Başka bir terminalde:

```powershell
cd admin-web
npm ci
npm run dev
```

- Kullanıcı uygulaması: `http://localhost:5173/`
- Yönetim uygulaması: `http://localhost:5173/admin.html`
- RabbitMQ paneli: `http://localhost:15673/`

Yerel RabbitMQ kullanıcı adı `content_engagement`, parolası
`local_development_password` değeridir.

### İsteğe bağlı ayarlar

İlk yerel yönetici hesabını yalnız bir kez oluşturmak için backend'i başlatmadan
önce aşağıdaki değişkenleri tanımlayabilirsin:

```powershell
$env:INITIAL_ADMIN_ENABLED="true"
$env:INITIAL_ADMIN_EMAIL="admin@example.com"
$env:INITIAL_ADMIN_DISPLAY_NAME="İlk Yönetici"
$env:INITIAL_ADMIN_PASSWORD="uzun-ve-benzersiz-bir-parola"
```

İlk başarılı girişten sonra `INITIAL_ADMIN_ENABLED` değerini `false` yap ve parolayı
terminal ortamından kaldır.

Şifre sıfırlama e-postası denenecekse `MAILERSEND_SMTP_USERNAME`,
`MAILERSEND_SMTP_PASSWORD` ve `MAILERSEND_FROM_ADDRESS` değişkenleri gerekir. Bu
bilgileri repoya yazma; yalnız terminal ortamında tut.

### Test ve kapatma

```powershell
.\mvnw.cmd verify
.\mvnw.cmd -f leaderboard-service\pom.xml test
cd admin-web
npm test
```

Docker servislerini durdurmak için proje kökünde:

```powershell
docker compose down
```

Yerel portlar: monolith PostgreSQL `5433`, leaderboard PostgreSQL `5434`, RabbitMQ
`5673`, monolith Redis `6380` ve leaderboard Redis `6381`.

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
