# Hikaye İzi — İçerik Etkileşim ve Quiz Platformu

Hikaye İzi'ni, TRT Yeni Medya Kanal Koordinatörlüğü Yazılım Geliştirme
Departmanında yaptığım 27 Temmuz–21 Ağustos 2026 stajı sırasında geliştirdim.
TRT ve tabii içerikleri için quizlerin hazırlanabildiği, kullanıcıların quiz
çözerek XP kazandığı ve sıralamalarını görebildiği bir web uygulaması.

Bu projede ağırlıklı olarak Java ve Spring Boot ile backend geliştirmeye
odaklandım. Quiz akışının yanında, aynı isteğin tekrar gönderilmesi, iki cevabın
aynı anda gelmesi veya mesajlaşma servisinin kesilmesi gibi durumları da ele aldım.

**[Canlı demo](https://hikayeizi.duckdns.org/) · [Mimari](docs/ARCHITECTURE.md) · [Teknik kararlar](docs/DECISIONS/README.md)**

**Teknolojiler:** Java 21 · Spring Boot · PostgreSQL · Flyway · RabbitMQ · Redis ·
React · TypeScript · Docker Compose · Testcontainers

## Uygulamada neler yapılabiliyor?

- Kullanıcı hesap oluşturur, içerik ve quiz keşfeder, süreli soruları cevaplar.
- İlk quiz tamamlamasında skoruna göre XP kazanır; aynı quizi tekrar çözmek
  alıştırmadır ve ek XP üretmez.
- Sonuçlarını, toplam XP'sini ve global/içerik bazlı sıralamasını görür.
- Editör içerik, sezon, bölüm, medya ve quiz taslaklarını yönetir; quiz sürümünü
  yayınlar. Yayınlanmış sürümde değişiklik için yeni bir taslak oluşturulur.
- Kullanıcı ve yönetim arayüzleri Türkçe ve İngilizce sunumu destekler.

## Projede yaptığım çalışmalar

- İçerik yönetimi, quiz sürümleme, kullanıcı hesabı ve yetkilendirme API'lerini yazdım.
- Quiz süresini, cevap kabulünü ve skoru sunucunun belirlediği bir akış kurdum.
- Tekrar isteklerin ve eşzamanlı işlemlerin ikinci cevap veya XP kaydı üretmesini
  transaction ve PostgreSQL tekillik kurallarıyla engelledim.
- Quiz sonucundan XP üreten akışı RabbitMQ ve Outbox/Inbox ile asenkron hale getirdim.
- Leaderboard'u kendi PostgreSQL ve Redis verisine sahip ayrı bir servise çıkardım.
- Kullanıcı ve yönetim arayüzlerini React ve TypeScript ile geliştirdim;
  Türkçe ve İngilizce dil desteği ekledim.
- İş kuralları ve entegrasyonlar için testler yazdım. Altyapı testlerinde
  Testcontainers kullandım.
- Uygulamayı Docker Compose ve Caddy ile Oracle Cloud üzerinde HTTPS üzerinden yayımladım.

Bu benim staj projem; TRT/tabii'nin resmî uygulaması değil.

## Mimari ve veri akışı

```text
React kullanıcı / yönetim arayüzü
  → Spring Boot API ve modüler monolith
  → Quiz sonucu + Outbox kaydı (aynı PostgreSQL transaction'ı)
  → RabbitMQ → Inbox korumalı XP consumer'ı
  → XP ledger + xp.changed.v1 Outbox olayı
  → RabbitMQ → Ayrı leaderboard servisi
  → Servise ait PostgreSQL projeksiyonu ve Redis okuma modeli
```

İlk aşamada içerik, quiz, gameplay, kimlik ve XP modüllerini tek bir Spring Boot
uygulamasında tuttum. Bu yapıya modüler monolith deniyor. Daha sonra leaderboard'u
ayrı bir servise çıkardım. Bu servis kendi veritabanını kullanıyor; ana uygulamanın
tablolarını doğrudan okumuyor.

| Problem | Uygulanan yaklaşım |
| --- | --- |
| İstemcinin skor veya süreyi değiştirmesi | Süre, cevap kabulü, kaynak sahipliği ve skor sunucuda doğrulanır. |
| Aynı soruya eşzamanlı cevap gönderilmesi | Transaction ve veritabanı tekillik kurallarıyla yalnız bir cevap kalıcılaşır. |
| Tekrar çözüm veya tekrar isteğin ek XP üretmesi | İlk tamamlama hakkı ve XP kayıtları veritabanı tekilliğiyle korunur; tekrar işlemler idempotenttir. |
| Veritabanı kaydı başarılıyken mesajın gönderilememesi | Sonuç ve gönderilecek olay aynı transaction'da Outbox'a yazılır; publisher daha sonra gönderir. |
| RabbitMQ'nun aynı mesajı tekrar teslim etmesi | Inbox ve XP/projeksiyon tekillikleri aynı olayın ikinci etkiyi üretmesini önler. |
| Redis verisinin silinmesi veya erişilememesi | PostgreSQL kalıcı kaynaktır; Redis yeniden oluşturulur, okuma PostgreSQL'e düşebilir. |

**Idempotency**, aynı işlemin tekrarının ikinci bir iş etkisi üretmemesidir.
**Outbox**, gönderilecek olayların uygulama verisiyle birlikte veritabanına
kaydedilmesidir; **Inbox** ise tüketicinin işlediği olayları takip eder.
XP ve leaderboard asenkron güncellendiği için sonuçların görünmesi kısa süre
gecikebilir.

XP'yi aynı işlemde doğrudan kaydetmek daha basit bir seçenekti. Asenkron yapıya
geçerek mesaj kesintisi ve tekrar teslimat durumları üzerinde çalıştım; bunun
karşılığında yönetilecek bileşen sayısı arttı. Bu kararları ve alternatifleri
[Outbox ADR'sinde](docs/DECISIONS/ADR-0012-transactional-outbox-rabbitmq.md) ve
[leaderboard ADR'sinde](docs/DECISIONS/ADR-0031-leaderboard-microservice-extraction.md) anlattım.

## Belgeler

- [Proje özeti](docs/PROJECT_BRIEF.md)
- [Kısa mimari](docs/ARCHITECTURE.md)
- [Kısa yol haritası](docs/ROADMAP.md)
- [Güncel durum özeti](docs/CURRENT_STATE.md)
- [Doküman dizini](docs/README.md)
- [Mimari kararlar](docs/DECISIONS/README.md)
- [Operasyon rehberi](docs/OPERATIONS.md)

## Test kapsamı

Testlerde başarılı akışların yanında tekrar istekleri, eşzamanlı işlemleri ve
altyapı kesintilerini kontrol ettim. Entegrasyon testlerini Testcontainers ile
gerçek PostgreSQL, RabbitMQ ve Redis container'ları üzerinde çalıştırdım.

| Davranış | İlgili test dosyası |
| --- | --- |
| Eşzamanlı cevaplarda tek kayıt; tekrar complete işleminde tek olay ve XP; ilk tamamlamaya ödül | [GameplayIntegrationTest](src/test/java/com/trt/contentengagement/gameplay/GameplayIntegrationTest.java) |
| Yayınlanmış quiz sürümünün değişmemesi ve kullanıcı sözleşmesinde doğru cevap bayrağının gizlenmesi | [QuizAuthoringIntegrationTest](src/test/java/com/trt/contentengagement/quiz/QuizAuthoringIntegrationTest.java) |
| Tekrar mesajda tek XP; broker kesintisinde olayın beklemesi ve toparlanınca gönderilmesi | [MessagingIntegrationTest](src/test/java/com/trt/contentengagement/messaging/MessagingIntegrationTest.java) |
| Ayrı serviste tekrar RabbitMQ teslimatının tek projeksiyon kaydı oluşturması | [LeaderboardRabbitIntegrationTest](leaderboard-service/src/test/java/com/trt/contentengagement/leaderboardservice/LeaderboardRabbitIntegrationTest.java) |
| Deterministik sıralama ve Redis modelinin servis PostgreSQL'inden yeniden kurulması | [LeaderboardProjectionIntegrationTest](leaderboard-service/src/test/java/com/trt/contentengagement/leaderboardservice/LeaderboardProjectionIntegrationTest.java) |
| CSRF token'ı olmayan yazma isteğinin reddedilmesi | [CsrfProtectionIntegrationTest](src/test/java/com/trt/contentengagement/identity/CsrfProtectionIntegrationTest.java) |

Testleri çalıştırmak için aşağıdaki komutları kullanabilirsin. Önceki test
sonuçlarını [güncel durum belgesinde](docs/CURRENT_STATE.md) tuttum.

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

Entegrasyon testleri için Docker çalışıyor olmalıdır. Proje kökünden:

```powershell
.\mvnw.cmd verify
.\mvnw.cmd -f leaderboard-service\pom.xml test
cd admin-web
npm test
npm run build
```

İlk komut ana backend'i test edip paketler; ikinci komut ayrı leaderboard
servisini test eder. Frontend komutları Vitest testlerini, TypeScript kontrolünü
ve production build'i çalıştırır. Yük testleri ayrı operasyon senaryolarıdır;
standart backend doğrulamasına dahil değildir.

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

## Kapsam ve açık konular

- Kimlik doğrulama yerel hesap ve sunucu oturumuyla sağlanır. Kurumsal TRT/tabii
  SSO/OIDC entegrasyonu bu depoda uygulanmış bir özellik değildir.
- Tek sunucu dağıtımı sade bir demo ortamı sağlar; sunucu kaybı tüm bileşenleri
  etkileyebilir. Ayrı konuma yedekleme ve geri yükleme provası operasyon işidir.
- Backend yeniden başladığında bellek içindeki oturumlar kaybolur.
- Ayrı leaderboard servisinin internal API kimlik doğrulaması ve büyük veri için
  batch/cursor replay yaklaşımı ayrıca ele alınmalıdır.
- Canlı TV, sosyal ağ ve yapay zekâ ile otomatik soru üretimi mevcut kapsamın
  dışındadır. Demo veya yerel test sonuçları gerçek trafik kapasitesi garantisi vermez.

Geliştirme kuralları [AGENTS.md](AGENTS.md) dosyasında, eski çalışma belgeleri
[doküman arşivinde](docs/archive/2026-08-11/) bulunuyor.
