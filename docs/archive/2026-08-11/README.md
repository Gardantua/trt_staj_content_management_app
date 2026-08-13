# TRT İçerik Etkileşim Backend Platformu

TRT ve tabii içerikleri için geliştirilecek, API-first bir içerik etkileşim
platformudur. İlk kullanım senaryosu; kullanıcıların dizi ve bölüm bazlı
quizleri çözmesi, sunucu tarafında puanlanması, XP kazanması ve sıralamasını
görmesidir.

Bu repo öncelikle backend mühendisliği çalışmasıdır. Frontend yalnızca API'leri
gösterecek kadar geliştirilebilir.

GitHub deposu:
[Gardantua/trt_staj_content_management_app](https://github.com/Gardantua/trt_staj_content_management_app)

## Projenin öğrenme amacı

Bu proje yalnızca çalışan endpoint'ler üretmek için değil, bir backend
sisteminin neden bu şekilde tasarlandığını uçtan uca öğrenmek için
geliştirilecektir. Her aşamada:

1. Çözülen iş problemi ve ilgili domain kavramları açıklanır.
2. Teknoloji veya desen seçilmeden önce alternatifler ve trade-off'lar konuşulur.
3. Kod küçük adımlarla yazılır; önemli sınıfların ve transaction sınırlarının
   görevi açıklanır.
4. Değişken, metot ve sınıf adları yaptığı işi anlatacak şekilde seçilir.
5. Yeni aşamaya geçmeden önce yazılan kodun akışı, iş kuralları, hata davranışı
   ve testlerinin neyi kanıtladığı kullanıcıya açıklanır.
6. Önce risk ve beklenen davranış tanımlanır, sonra uygun test yazılır.
7. Çalıştırma ve hata ayıklama adımları kullanıcı tarafından tekrar edilebilir
   biçimde belgelenir.
8. Aşama sonunda öğrenilenler, alınan kararlar ve açık sorular kaydedilir.

Amaç, kodu yalnızca üretmek değil; veri modeli, API, transaction, eşzamanlılık,
güvenlik, test ve operasyon davranışlarına birlikte hâkim olmaktır.

## İlk sürüm

İlk sürümde:

- İçerik, sezon ve bölüm yönetimi
- Yayımlanmış içerik metadata/kapak bakımı ve oyun geçmişini koruyan kalıcı silme
- Quiz, soru ve cevap seçeneği yönetimi
- Quiz attempt başlatma
- Tek seferlik ve süre kontrollü cevap gönderme
- Sunucu taraflı puanlama
- Kullanıcı ve yönetici yetkilendirmesi
- Görselli sorular ve erişilebilir metin alternatifleri
- Ayarlanabilir quiz süresi
- XP işlemleri
- Global ve içerik bazlı leaderboard
- OpenAPI dokümantasyonu
- Otomatik testler

bulunacaktır.

İlk sürümde canlı TV, İngilizce öğrenme, arkadaş sistemi, yapay zekâyla soru
üretme, Kubernetes ve mikroservis ayrıştırması yapılmayacaktır.

## Önerilen başlangıç teknolojileri

Teknoloji seçimi kurum standardı öğrenilene kadar tavsiye niteliğindedir:

- Java 21
- Spring Boot
- PostgreSQL
- Flyway
- Docker Compose
- OpenAPI
- Testcontainers

RabbitMQ, stajdan sorumlu mühendisin bildirdiği kurum/proje gereksinimi olarak
kullanılacaktır. Ancak çekirdek quiz sistemi PostgreSQL üzerinde güvenilir
biçimde tamamlanmadan eklenmeyecektir. Redis de öğrenme hedefi nedeniyle
leaderboard aşamasında kullanılacaktır:

- XP önce PostgreSQL üzerinde idempotent bir işlem defteri olarak doğru
  çalıştırılır. RabbitMQ daha sonra Outbox ile quiz tamamlama yan etkilerini
  asenkronlaştırmak için eklenir.
- Leaderboard kuralları ve doğru sonuç önce PostgreSQL üzerinde kanıtlanır.
  Redis daha sonra PostgreSQL'den yeniden oluşturulabilir bir read model olarak
  eklenir ve iki yaklaşımın sonucu ile performansı karşılaştırılır.

## Sadeleştirilmiş çekirdek akış

İlk sürümün omurgası şudur:

```text
Yönetici içeriği ve quizi yayınlar
        ↓
Kullanıcı yayınlanmış quizi başlatır
        ↓
Sunucu süreyi ve cevapları doğrular
        ↓
Attempt tek kez tamamlanır ve skor kesinleşir
        ↓
XP oluşur ve sıralama okunabilir
```

Bu akışı kanıtlamayan teknoloji veya özellik MVP'ye eklenmez.

## Dokümantasyon

- [Proje özeti](docs/PROJECT_BRIEF.md)
- [Dokümantasyon ve diyagram dizini](docs/README.md)
- [Resmî proje tanımı ve teknik isterler](docs/PROJECT_SPECIFICATION.md)
- [PDF proje dokümanı](output/pdf/TRT_Icerik_Etkilesim_Backend_Proje_Dokumani.pdf)
- [Mimari](docs/ARCHITECTURE.md)
- [PlantUML sistem diyagramı](docs/diagrams/system-architecture.puml)
- [Yol haritası](docs/ROADMAP.md)
- [Güncel durum](docs/CURRENT_STATE.md)
- [Geliştirme zorlukları ve çözüm günlüğü](docs/DEVELOPMENT_CHALLENGES.md)
- [Mimari kararlar](docs/DECISIONS/README.md)

Uzun teknik referans:
`TRT_Etkilesimli_Quiz_Platformu_Backend_Mimari_Raporu.docx`

## Çalışma yöntemi

Her geliştirme görevinin başında `AGENTS.md` ve `docs/` altındaki belgeler
okunmalıdır. Bir görevde yalnızca yol haritasındaki tek aşama ele alınmalıdır.

Her aşamanın sonunda:

1. İlgili testler çalıştırılır.
2. Alınan kalıcı kararlar ADR olarak kaydedilir.
3. `docs/CURRENT_STATE.md` güncellenir.
4. Kullanıcıya değişikliklerin nedeni, sistemdeki yeri ve test sonucu anlatılır.
5. Bir sonraki aşamaya kullanıcı onayı olmadan geçilmez.

Eksik veya taşınmış dosyalar kullanıcı izni olmadan yeniden oluşturulmaz. PDF
sayfalarını görsele dönüştürme, OCR veya görsel kalite incelemesi gibi image
processing işlemleri de ayrıca kullanıcı onayı gerektirir.

## Yerel geliştirme

### Gereksinimler

- Java 21
- Çalışan Docker Desktop
- Git

Kurulumdan sonra yeni bir terminal aç ve sürümleri doğrula:

```powershell
java -version
docker version
docker compose version
```

`java -version` çıktısı Java 21 göstermelidir.

### PostgreSQL, RabbitMQ ve Redis'i başlatma

```powershell
docker compose up -d --wait
docker compose ps
```

Proje PostgreSQL'i host üzerinde `5433`, container içinde `5432`; RabbitMQ'yu
host üzerinde `5673`, container içinde `5672` portunda kullanır. RabbitMQ
yönetim arayüzü `http://localhost:15673` adresindedir. Redis host üzerinde
`6380`, container içinde `6379` portundadır. Farklı host portları,
geliştirme bilgisayarındaki mevcut servislerle çakışmayı önler.

Servisleri durdurmak için:

```powershell
docker compose stop
```

`docker compose down` container'ı kaldırır ancak isimlendirilmiş veritabanı
volume'unu silmez. `down -v` veriyi de sileceği için bilinçli karar olmadan
kullanılmamalıdır.

### Testleri çalıştırma

Windows:

```powershell
.\mvnw.cmd verify
```

Linux, macOS veya CI:

```text
bash ./mvnw verify
```

Bu komut kodu derler, Testcontainers ile geçici gerçek PostgreSQL, RabbitMQ ve Redis
başlatır, Flyway migration'larını uygular, API/mesajlaşma testlerini çalıştırır
ve çalıştırılabilir JAR üretir.

### Uygulamayı çalıştırma

Önce PostgreSQL, RabbitMQ ve Redis açık olmalıdır:

```powershell
docker compose up -d --wait
.\mvnw.cmd spring-boot:run
```

Health adresi:

```text
http://localhost:8081/actuator/health
```

Beklenen cevap:

```json
{"status":"UP"}
```

Yerel varsayılanlar `application.yml` içindedir. Gerçek ortamlarda PostgreSQL
bağlantısı `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`; RabbitMQ
bağlantısı `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME` ve
`RABBITMQ_PASSWORD`; Redis bağlantısı `REDIS_HOST` ve `REDIS_PORT` environment
değişkenleriyle dışarıdan verilmelidir.

### Yerel kimlikle çalıştırma

Kurum kimlik sağlayıcısı henüz belli olmadığı için Aşama 1, yalnız `local` ve
`test` profillerinde etkin olan geçici header kimliği içerir. Bu mekanizma
production authentication değildir. Varsayılan profilde test header'ları
kimlik oluşturmaz ve korunan API yolları güvenli biçimde `401` döner.

Yerel profili aç:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

`local` profilinde geliştirme sırasında çok sayıda kapak ve admin isteğinin 429
üretmemesi için HTTP rate limit kapalıdır. Diğer profillerde koruma varsayılan olarak
açıktır ve production ortamında yalnız bilinçli `RATE_LIMIT_ENABLED=false` ayarıyla
kapatılabilir.

Başka bir PowerShell terminalinde doğrulanmış aktör bağlamını oku:

```powershell
$headers = @{
    "X-Test-Actor-Id" = "11111111-1111-1111-1111-111111111111"
    "X-Test-Actor-Roles" = "USER"
}
Invoke-RestMethod -Uri "http://localhost:8081/api/v1/identity/me" -Headers $headers
```

Desteklenen geçici roller `USER`, `EDITOR` ve `ADMIN` değerleridir. Header
değerleri loglanmaz. Gerçek kimlik sağlayıcısı belirlendiğinde bu adapter
OIDC/JWT adapter'ıyla değiştirilecek; application use case'leri
`CurrentActorProvider` sözleşmesini kullanmaya devam edecektir.

Kullanıcı webi için header veya UUID girişi gerekmez. Backend `local` profilde,
frontend `npm run dev` ile çalışırken `http://localhost:5173/user.html` adresini
açın ve **Hesap oluştur** seçeneğini kullanın. E-posta/şifre hesabı PostgreSQL'de,
oturum ise HttpOnly çerezde tutulur. Bu geliştirme hesabı production tabii kimliği
değildir.

### Yerel demo kullanıcı ve skor verisi

Backend `local` profiliyle çalışırken sekiz sabit demo UUID için gerçek attempt,
XP ve leaderboard verisi üretilebilir:

```powershell
.\ops\seed-demo-gameplay.ps1
```

Script en fazla ilk üç yayımlanmış quizi API üzerinden çözer; SQL ile tabloya
doğrudan yazmaz. Daha önce XP üretilmiş demo kullanıcıları varsayılan olarak
atlar. Kullanıcı kimlikleri ve hedef doğrulukları
`docs/demo-data/demo-users.json` içindedir. Aynı kullanıcılarla yeniden alıştırma
attempt'i üretmek gerekirse bilinçli olarak `-Force` verilebilir.

### İçerik kataloğu API'leri

Aşama 2 ile içerik, sezon ve bölüm yönetimi eklendi. Yönetim yolları `EDITOR`
veya `ADMIN` rolü ister:

```text
POST   /api/v1/admin/contents
GET    /api/v1/admin/contents?query=ertugrul&page=0&size=20
GET    /api/v1/admin/contents/{contentId}
PUT    /api/v1/admin/contents/{contentId}
PUT    /api/v1/admin/contents/{contentId}/cover
DELETE /api/v1/admin/contents/{contentId}
POST   /api/v1/admin/contents/{contentId}/publish
POST   /api/v1/admin/contents/{contentId}/seasons
POST   /api/v1/admin/contents/{contentId}/season-plan
PUT    /api/v1/admin/contents/{contentId}/seasons/{seasonId}
DELETE /api/v1/admin/contents/{contentId}/seasons/{seasonId}
POST   /api/v1/admin/contents/{contentId}/seasons/{seasonId}/episodes
PUT    /api/v1/admin/contents/{contentId}/seasons/{seasonId}/episodes/{episodeId}
DELETE /api/v1/admin/contents/{contentId}/seasons/{seasonId}/episodes/{episodeId}
```

Kimliği doğrulanmış normal kullanıcı yalnız yayınlanmış kataloğu okuyabilir:

```text
GET /api/v1/contents?page=0&size=20
GET /api/v1/contents/{contentId}
```

`size` değeri 1–100 arasındadır. Sezon numarası içerik içinde, bölüm numarası
sezon içinde tektir. Bu kurallar hem domain modelinde hem PostgreSQL
constraint'lerinde korunur. Bir dizi, en az bir sezon ve her sezonda en az bir
bölüm bulunmadan yayınlanamaz. Yayımlanmış dizinin mevcut sezon ve bölümleri
değiştirilemez veya silinemez; yalnız son numaradan sonra yeni kayıt eklenebilir.
Yayın öncesinde doğrulanmış bir kapak görseli ve bu görselin alternatif metni
atanmalıdır.

Admin listesindeki isteğe bağlı `query`, başlığın herhangi bir bölümünde büyük/küçük
harf duyarsız eşleşir. Arama PostgreSQL üzerinde, aynı `page`/`size` sözleşmesiyle
çalışır; boş veya yalnız boşluk içeren sorgu bütün admin kataloğunu listeler.

`season-plan` komutu sezon numarası ve her sezonun bölüm sayısını tek istekte
alır. Sunucu `1. Sezon`, `1. Bölüm` gibi varsayılan başlıkları üretir ve bütün
hiyerarşiyi aynı transaction içinde kaydeder; herhangi bir tanım geçersizse
hiçbir sezon veya bölüm kalıcılaşmaz. Tekil güncelleme endpoint'leri otomatik
başlık ve açıklamaları sonradan özelleştirmek için korunur.

### Medya ve erişilebilir görsel sözleşmesi

Aşama 4.1 ile EDITOR/ADMIN için doğrulanan görsel yükleme ve kimliği doğrulanmış
kullanıcı için görsel okuma yolları eklendi:

```text
POST /api/v1/admin/media/images
GET  /api/v1/admin/media/images?page=0&size=24
GET  /api/v1/media/{mediaAssetId}/content
```

Yükleme `multipart/form-data` içindeki `file` alanıyla yapılır. Yalnız JPEG/PNG,
en fazla 5 MB ve en fazla 4096×4096 görseller kabul edilir; bildirilen MIME türü
dosya imzası ve çözülebilir içerikle doğrulanır. PostgreSQL değişmez medya
kimliği ve SHA-256 bütünlük bilgisini saklar. Yerel geliştirmede dosyalar
`MEDIA_STORAGE_DIRECTORY` ile değiştirilebilen klasöre yazılır; production
storage sağlayıcısı aynı portun farklı adapter'ı olacaktır.

### Quiz authoring ve sürümleme API'leri

Aşama 3 ile quiz, sürüm, soru ve cevap seçeneği yönetimi eklendi. Yönetim
yolları `EDITOR` veya `ADMIN` rolü ister:

```text
POST   /api/v1/admin/quizzes
DELETE /api/v1/admin/quizzes/{quizId}
POST   /api/v1/admin/quizzes/{quizId}/retire
GET    /api/v1/admin/quizzes?contentId={contentId}
GET    /api/v1/admin/quizzes/{quizId}
POST   /api/v1/admin/quizzes/{quizId}/versions
PUT    /api/v1/admin/quizzes/{quizId}/versions/{versionId}
POST   /api/v1/admin/quizzes/{quizId}/versions/{versionId}/questions
PUT    /api/v1/admin/quizzes/{quizId}/versions/{versionId}/questions/{questionId}
DELETE /api/v1/admin/quizzes/{quizId}/versions/{versionId}/questions/{questionId}
POST   /api/v1/admin/quizzes/{quizId}/versions/{versionId}/publish
POST   /api/v1/admin/quizzes/{quizId}/versions/{versionId}/archive
```

Kimliği doğrulanmış kullanıcı yalnız aktif yayın sürümünü okuyabilir:

```text
GET /api/v1/quizzes/{quizId}
GET /api/v1/quizzes
GET /api/v1/contents/{contentId}/quizzes
```

Genel quiz listesi yalnız keşif için quiz adı, kapsamı ve soru sayısı gibi özet
alanları döndürür; soru, seçenek veya doğru cevap bilgisi taşımaz.

Draft quizler kullanıcıya görünmez. Yayın için en az bir soru, her soruda en az
iki seçenek ve tam bir doğru seçenek gerekir. Kullanıcı response modeli doğru
cevap alanını içermez. Yayındaki sürüm yerinde değiştirilemez; düzenleme için
yeni draft sürümü oluşturulur. Puanlama politikası bu aşamada `STANDARD_V1`
kimliğiyle sürüme sabitlenmiştir; puan hesabı Aşama 4 gameplay kapsamında
sunucu tarafından uygulanır.

Soruya özel görsel `INFORMATIVE` veya `DECORATIVE` rolüyle tanımlanabilir.
Bilgilendirici görsel, alternatif metin ve cevabı sızdırmayan eşdeğer
`accessiblePrompt` olmadan yayınlanamaz. Özel görsel yoksa içerik kapağı yayın
anında quiz sürümüne dekoratif fallback olarak sabitlenir.

Quiz oluşturma isteği `scopeType` alanında `CONTENT`, `SEASON` veya `EPISODE`
taşır. Sezon kapsamı `seasonId`, bölüm kapsamı hem `seasonId` hem `episodeId`
ister. Aidiyet application katmanında doğrulanır ve Flyway V10 constraint'leri
çapraz içerik/sezon/bölüm ilişkisini veri tabanında da engeller. Alan verilmezse
eski istemciler için `CONTENT` varsayılır. Sayfalı medya listeleme API'si geriye
uyumluluk için korunur; admin web kapak ve soru görsellerini artık doğrudan dosya
yükleyerek bağlar ve bağımsız görsel kütüphanesi göstermez.

### Gameplay API'leri

Aşama 4 ile kimliği doğrulanmış kullanıcı için attempt akışı eklendi:

```text
POST /api/v1/quizzes/{quizId}/attempts
GET  /api/v1/attempts/{attemptId}
POST /api/v1/attempts/{attemptId}/answers
POST /api/v1/attempts/{attemptId}/timeouts
POST /api/v1/attempts/{attemptId}/complete
```

Answer, timeout ve complete istekleri en fazla 100 karakterlik `Idempotency-Key`
header'ı ister. Toplam quiz süresi veya başlangıçta süre seçimi yoktur. Her güncel
soru için sunucu 30 saniyelik `questionDeadline` üretir; erken cevapta sonraki soru
hemen açılır ve yeni 30 saniye başlar. Süre ve skor sunucu tarafından belirlenir.
Sorular sunucu sırasıyla cevaplanır ve aynı soru ikinci kez kabul edilmez.
Cevap kaydedildikten sonra response `correct` ve `correctOptionId` alanlarının
yanında `CORRECT`/`INCORRECT` semantik durumunu gösterir. Timeout yanıtı
`TIMED_OUT` durumunu taşır; ardından sıradaki
soruyu çözümlenmiş görsel ve erişilebilir metinle döndürür. Son cevap attempt'i otomatik
tamamlar. Kullanıcı yalnız kendi attempt'ini okuyabilir.

`STANDARD_V1` her doğru cevap için 10, yanlış veya zaman aşımı için 0 puan verir.
Sonuç response'undaki `submittedAnswers` ve `totalQuestionCount`, istemcinin
`8 / 10 doğru` gibi bir başarı özeti göstermesini sağlar.

### XP ve güvenilir mesajlaşma

Aşama 6 ile tamamlanan attempt ve sürümlü `quiz.completed` Outbox olayı aynı
PostgreSQL transaction'ında kesinleşir. RabbitMQ publisher olayı daha sonra
yayımlar; idempotent consumer Inbox kaydıyla birlikte XP ledger işlemini
oluşturur. Broker geçici olarak kapalı olsa bile attempt ve Outbox olayı
kaybolmaz, tekrar teslimat ikinci XP üretmez.

`FIRST_COMPLETION_SCORE_V2` politikasında aynı kullanıcı aynı quizden yalnız ilk
tamamlamasında XP kazanır. İlk tamamlamada XP kesin sunucu skoruna eşittir;
sonraki çözümler skor üretse de `earnedXp: 0` döndüren alıştırmalardır.
`GET /api/v1/me/xp` asenkron consumer çalışana kadar kısa süreli eski toplamı
döndürebilir.

```text
GET  /api/v1/me/xp
POST /api/v1/admin/xp-transactions/{transactionId}/adjustments
```

XP geçmişi append-only işlem defteridir. Aynı attempt, tekrar gönderilen complete
isteği veya aynı quizin yeni attempt'i ikinci ödül üretmez. Geçmiş satır
değiştirilmez; yalnız `ADMIN`
rolü, özgün kazanca bağlı pozitif veya negatif yeni bir düzeltme kaydı ekleyebilir.

Mesajlaşma akışı kalıcı direct exchange, dayanıklı XP queue'su, üç denemeli
artan gecikme ve dead-letter queue kullanır. Publish/consumer başarıları,
hataları ve duplicate mesajlar Actuator'ın Micrometer metrikleri üzerinden
izlenebilir.

### Leaderboard API'leri

Aşama 7 ile global ve içerik bazlı PostgreSQL leaderboard doğruluğu, Aşama 8
ile bu sonuçtan yeniden oluşturulabilen Redis read model tamamlandı:

```text
GET /api/v1/leaderboards/global?limit=20
GET /api/v1/leaderboards/contents/{contentId}?limit=20
```

İlk dönem `ALL_TIME`'dır. Kullanıcının bütün geçerli XP ledger işlemleri
toplanır; içerik sıralamasında yalnız ilgili içeriğin kazanç ve düzeltmeleri
sayılır. Tekrar çözülen bütün tamamlanmış attempt'ler XP ürettikleri ölçüde
toplama katılır. Sıra toplam XP azalan, ilk XP zamanı artan ve son olarak
kullanıcı UUID'si artan biçimde deterministiktir.

`limit` 1–100 arasındadır. Response Top N listesinin yanında kimliği doğrulanmış
kullanıcının kendi sırasını Top N dışında olsa bile döndürür. Henüz kalıcı
profil bulunmadığından görünen ad/avatar yoktur; arkadaş sıralaması sosyal
özellik olarak MVP dışındadır. Küçük örneklemde yanıltıcı olacağı için yüzdelik
alanı üretilmez.

Redis sorted set içinde puan yerine PostgreSQL'in ürettiği benzersiz pozisyon
saklanır; böylece toplam XP, ilk XP zamanı ve UUID tie-break sırası değişmez.
Projeksiyon varsayılan olarak beş saniyede bir yenilenir ve yalnız `ADMIN`
rolünün çağırabildiği aşağıdaki yolla elle yeniden oluşturulabilir:

```text
POST /api/v1/admin/leaderboards/rebuild
```

Response içindeki `dataSource`, sonucun `REDIS` veya Redis boş/erişilemezken
`POSTGRESQL_FALLBACK` kaynağından geldiğini gösterir. Redis sonucunda
`projectionGeneratedAt` eventual consistency sınırını görünür kılar. Redis
silinse veya kesilse XP kaybolmaz; doğru kaynak PostgreSQL'dir.

### Operasyon ve gözlemlenebilirlik

Aşama 9 OpenTelemetry trace, Prometheus metric, hazır Grafana dashboard'u,
rate limiting, güvenlik taraması, k6 yük profilleri ve disaster/migration
runbook'unu tamamlar. Bu servisler normal backend geliştirmesinde kaynak
tüketmemesi için yalnız açık profille başlar:

```powershell
docker compose --profile observability up -d --wait
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local,observability"
```

Yerel adresler:

```text
Grafana:    http://localhost:3000
Prometheus: http://localhost:9090
Tempo API:  http://localhost:3200
Metrics:    http://localhost:8081/actuator/prometheus
```

Grafana'nın yerel varsayılan kullanıcısı `admin`, parolası
`local_development_password` değeridir; `GRAFANA_ADMIN_USER` ve
`GRAFANA_ADMIN_PASSWORD` ile değiştirilebilir. Bu parola ve Compose ortamı
production secret yönetimi değildir. `/actuator/prometheus` yalnız
`observability` Spring profilinde açılır. Production'da ayrıca ayrı management
ağı veya kimlik doğrulaması gerekir.

Yük profilleri, ilk p95/p99 sonuçları, backup/restore, RPO/RTO, kesinti,
migration ve veri saklama sınırları [operasyon rehberinde](docs/OPERATIONS.md)
toplanmıştır. `load-test` Maven profili gerçek uygulama ve PostgreSQL üzerinde
bir dakikalık k6 baseline'ını çalıştırır:

```powershell
.\mvnw.cmd -Pload-test "-Dtest=Stage9K6LoadTest" test
```

## Sıradaki çalışma

Aşama 0–9 çekirdek backend roadmap'i ile Aşama 10A–10F admin/kullanıcı web
akışları tamamlandı. Yönetim ve kullanıcı arayüzleri
[admin-web çalışma rehberinde](admin-web/README.md) açıklanan aynı
React/TypeScript uygulamasının ayrı giriş noktalarıdır.

Yerel olarak backend'i geçici kimlik adapter'ıyla başlat:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Başka bir terminalde admin arayüzünü başlat:

```powershell
Set-Location admin-web
npm ci
$env:VITE_LOCAL_ACTOR_ID = "22222222-2222-2222-2222-222222222222"
$env:VITE_LOCAL_ACTOR_ROLES = "EDITOR"
npm run dev
```

Kullanıcı deneyimi için environment kimliği tanımlamadan
`http://localhost:5173/user.html` adresini açın ve hesap oluşturun. Kullanıcı
burada içerik keşfedebilir, quiz çözebilir, XP ve leaderboard sonucunu okuyabilir.

`VITE_LOCAL_ACTOR_*` production authentication değildir. Quiz yayın kontrolü,
dört seçenekli soru yazarlığı, soru başına 30 saniye ve cevap anahtarlı PDF 10E'de
tamamlanmıştır. Sıradaki çalışma yalnız yeni bir ürün kararıyla seçilmelidir.
