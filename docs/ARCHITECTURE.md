# Mimari

## Mimari yaklaşım

Başlangıç mimarisi modüler monolith olacaktır. Sistem tek uygulama olarak
dağıtılır ancak modül sınırları kod ve veri erişimi seviyesinde korunur.

Mikroservise ayrılma yalnızca bağımsız ölçek, farklı SLA, ayrı ekip sahipliği
veya hata izolasyonu gibi ölçülebilir bir ihtiyaç oluştuğunda değerlendirilir.

Bu bölüm hedef mimariyi anlatır; bütün bileşenlerin ilk günden kurulacağı
anlamına gelmez. Yol haritasındaki ilgili aşamaya gelmeden altyapı eklenmez.

## Önerilen modüller ve MVP sırası

```text
Backend Application
├── identity
├── content
├── media
├── quiz
├── gameplay
├── gamification
├── messaging
├── leaderboard
├── admin
└── shared
```

`admin`, yeni bir içerik domain'i üretmek yerine content ve quiz use case'lerine
yetkili giriş yüzeyi, yayınlama koordinasyonu ve audit sorumluluğu sağlar.
Modülün kapsamı yalnız gerçek yönetim use case'leri ortaya çıktıkça büyütülür.

### Kavramları basit okuma

- **Modül**, sistem içindeki bir iş departmanıdır. Örneğin `gameplay`, quiz
  çözme departmanıdır.
- **Domain**, o departmanın bildiği gerçek iş ve kurallardır. Örneğin “süresi
  dolan attempt cevap kabul etmez” bir gameplay domain kuralıdır.
- **Domain entity**, kendi durumunu ve geçiş kurallarını koruyan nesnedir.
  `Attempt.complete()` yalnız geçerli durumdayken tamamlamaya izin verir.
- **Katman**, modül içindeki teknik sorumluluk ayrımıdır. API isteği alır,
  application use case'i koordine eder, domain kuralları uygular,
  infrastructure PostgreSQL gibi araçlarla konuşur.

Modül ile katman aynı şey değildir. `gameplay` bir modüldür; `gameplay.domain`
ve `gameplay.api` bu modül içindeki farklı katmanlardır.

### identity

Kullanıcı, profil, rol ve izinleri yönetir.

### content

Dizi, film, sezon, bölüm ve içerik durumunu yönetir.

Aşama 2'de `Content`, sezon ve bölüm hiyerarşisinin aggregate root'u olarak
uygulandı. Domain sınıfları saf Java'dır; JPA entity'leri infrastructure
katmanında kalır. `DRAFT` içerik yalnız EDITOR/ADMIN yönetim yüzeyinden okunur,
normal kullanıcı sorguları yalnız `PUBLISHED` içeriği döndürür. İçerik
değişikliği ile kalıcı admin audit kaydı aynı transaction'da kesinleşir.

### quiz

Quiz, quiz sürümü, soru, seçenek ve yayınlama sürecini yönetir.

Aşama 3'te `Quiz`, sürüm/soru/seçenek hiyerarşisinin aggregate root'u olarak
uygulandı. İlk sürüm draft'tır; yayınlanan sürümün içeriği yerinde değişmez ve
yeni düzenleme yeni kimliklere sahip draft sürümü üretir. Yeni sürüm
yayımlandığında önceki aktif sürüm arşivlenir. Puanlama uygulaması gameplay
aşamasına ait olsa da `STANDARD_V1` politika kimliği sürümde sabitlenmiştir.
Kullanıcı quiz DTO'su doğru cevap alanını yapısal olarak içermez.

### media ve erişilebilirlik sınırı

`media`, değişmez medya varlığı kimliğini, güvenilir storage referansını,
MIME/boyut/bütünlük bilgisini ve erişilebilirlik metadatasını yönetir. Gerçek
dosya PostgreSQL'e iş verisi olarak gömülmez; storage sağlayıcısı bir port
arkasında kalır. Aşama 4.1'de geliştirme ve test için yerel dosya sistemi
adapter'ı eklendi. Kurumun production object storage/CDN standardı öğrenildiğinde
domain ve application sözleşmeleri değişmeden yeni adapter yazılır.

Content bir kapak medya kimliğine, quiz sorusu ise isteğe bağlı özel medya
kimliğine application portu üzerinden referans verir; modüller media tablolarını
doğrudan sorgulamaz. Soruya özel görsel yoksa yayın anındaki içerik kapağı quiz
sürümüne sabitlenir. Aynı medya kimliğinin dosyası yerinde değiştirilmez.

Erişilebilirlik tek bir modül değil, API ve domain sınırlarını etkileyen
yatay bir kalite niteliğidir. Backend; görsel rolü, alternatif metin, eşdeğer
erişilebilir soru metni, semantik geri bildirim kodu ve ayarlanabilir süre
politikasını sağlar. Frontend daha sonra bunları semantik HTML, klavye erişimi,
odak yönetimi, ekran okuyucu durum duyurusu, yeterli kontrast, yeniden akış ve
yeterli hedef boyutuyla sunar.

Kullanıcının engel veya sağlık türü saklanmaz. Standart ve en az on kat
uzun süre seçenekleri herkese açık bir gameplay tercihi olur; seçilen politika
attempt'te sabitlenir ve skor bonusu üretmez.

### gameplay

Attempt, cevap, süre, durum geçişi ve puanlamayı yönetir.

Aşama 4'te `QuizAttempt` aggregate'i ACTIVE, COMPLETED ve EXPIRED durumlarını;
sunucu `Clock` deadline'ını, sıralı tek cevap kuralını ve `STANDARD_V1`
puanlamasını uygular. Attempt belirli quiz/politika sürümünü sabitler. Cevap
kalıcılaştırıldıktan sonra doğru seçenek açıklanır ve sonraki soru açılır.
PostgreSQL aynı soru, idempotency key ve aktif attempt yarışlarını unique
constraint'lerle korur. `QuizAttemptCompleted` process-içi domain event'tir;
güvenilir broker teslimatı değildir.

### gamification

XP işlem defteri ve kullanıcı XP özetini yönetir. `SCORE_MATCH_V1`, tamamlanan
attempt'in sunucu skorunu XP'ye birebir çevirir. `QUIZ_COMPLETED` kaydı kaynak
attempt ve deterministik referansla tekildir; sıfır skor da işlenmiş kaynak
olarak kaydedilir. Düzeltmeler geçmiş satırı değiştirmez, özgün kazanca bağlı
imzalı yeni `ADMIN_ADJUSTMENT` kayıtlarıdır.

Gameplay tamamlama ve XP ekleme Aşama 5'te aynı PostgreSQL transaction'ında
kanıtlandı. Aşama 6'da bu senkron çağrı kaldırıldı: gameplay attempt ile Outbox
olayını birlikte kesinleştirir, gamification ise olayı idempotent consumer
üzerinden işler. Modüller birbirlerinin tablolarına doğrudan erişmez.

### messaging

Sürümlü integration event sözleşmesini, Transactional Outbox publisher'ını,
Inbox deduplication'ı ve RabbitMQ adapter'larını yönetir. `quiz.completed` v1
olayı attempt kimliğinden deterministik event ID üretir. Publisher confirm
alınmadan Outbox satırı yayımlandı sayılmaz; consumer Inbox claim'i ile XP
yazımını aynı PostgreSQL transaction'ında gerçekleştirir.

### leaderboard

Global, içerik ve dönem bazlı sıralamayı yönetir.

Aşama 7'de ilk dönem `ALL_TIME`, kalıcı doğru kaynak XP ledger olarak
uygulandı. Global sonuç bütün ledger toplamını, içerik sonucu XP satırına
sabitlenmiş `contentId` toplamını kullanır. `LeaderboardQueryService` current
actor ve yayınlanmış içerik sınırını koordine eder; ledger SQL'i gamification
modülünün yayınlanmış `XpLeaderboardQuery` sözleşmesinin arkasında kalır.

PostgreSQL toplamı kullanıcı bazında gruplar ve `ROW_NUMBER` ile toplam XP
azalan, ilk XP zamanı artan, UUID artan sırasını üretir. Tek sorgu Top N ile
mevcut kullanıcı satırını döndürür. Aşama 8 Redis adapter'ı bu davranışı
değiştirmeden yeniden üretilebilir read model olarak uygulandı. PostgreSQL
snapshot'ı kısa bir read-only transaction içinde alınır; Redis yazımı bu
transaction kapandıktan sonra yapılır.

Redis sorted set skoru XP değil, PostgreSQL `ROW_NUMBER` sonucundaki benzersiz
pozisyondur. Böylece Redis'in eşit skorlu üyeleri sözlüksel sıralama davranışı
ürün tie-break kuralını değiştiremez. Top N sıra aralığından, mevcut kullanıcı
rank sorgusundan, katılımcı sayısı sorted set boyutundan; XP/zaman metadata'sı
ise toplu hash okumasından alınır. Yeni neslin bütün anahtarları hazırlandıktan
sonra tek `active` işaretçisi değiştirilir. Okuma eksik nesil veya bağlantı
hatası görürse PostgreSQL'e döner.

### admin

Editör işlemlerini, yayınlama yetkilerini ve audit kayıtlarını koordine eder;
content ve quiz iş kurallarını kendi içinde tekrar etmez.

Aşama 3'te audit portu ve JPA adapter'ı content paketinden `admin` modülüne
taşındı. Content ve quiz application servisleri aynı kalıcı audit sözleşmesini
kullanır; domain kuralları admin modülüne taşınmaz.

### admin-web

Aşama 10A'da aynı repo içinde ayrı React/TypeScript istemcisi olarak eklendi.
Frontend yalnız `/api/v1/admin/contents` sözleşmesini çağırır; content domain
kuralını veya authorization kararını tekrar uygulayan otorite değildir.
Tarayıcıdaki EDITOR/ADMIN kontrolü kullanıcı deneyimi sağlar, asıl rol kontrolü
Spring Security'de kalır.

Liste yolu aggregate hiyerarşisini yüklemeyen sayfalı admin özetleri döndürür;
sezon ve bölüm ağacı yalnız detay yolundan okunur. Yerel Vite proxy geliştirme
kolaylığıdır. Production OIDC, hosting, CORS ve CSRF topolojisi kurum sözleşmesi
geldiğinde ADR-0017 tetikleyicileriyle yeniden ele alınacaktır.

## Katmanlar

Her modül aşağıdaki sorumluluklara ayrılabilir:

```text
domain
application
ports
infrastructure
api
```

- `domain`: Entity, value object, durum geçişi ve iş kuralları
- `application`: Use case ve transaction sınırı
- `ports`: Repository ve dış sistem arayüzleri
- `infrastructure`: PostgreSQL, Redis, broker ve framework adapter'ları
- `api`: Controller ve request/response modelleri

Bu klasörlerin tamamı her modülde baştan açılmak zorunda değildir. Bir modül
yalnız ihtiyaç duyduğu katmanlarla başlar; domain bağımlılık yönü korunur.

Domain nesneleri yalnız getter/setter taşıyan veri torbaları olmamalıdır.
Durum geçişleri ve nesnenin kendi bütünlüğünü koruyan kurallar mümkün olduğunca
entity/value object içinde; değişebilir hesaplama politikaları ise saf domain
servislerinde bulunur. Application katmanı bu davranışları çağırır ve
transaction'ı koordine eder; controller iş kuralı yazmaz.

## Modüller arası iletişim

Her iletişimi event yapmak veya her iletişimi doğrudan çağrı yapmak doğru
değildir:

- Bir modülden belirli bir iş yapılması isteniyorsa yayınlanmış application API
  veya port kullanılır.
- Gerçekleşmiş bir iş gerçeği başka modüllere duyuruluyorsa domain event
  kullanılabilir.
- Başka process veya broker'a taşınacak, sürümlü ve kalıcı sözleşme integration
  event'tir.

`Spring ApplicationEventPublisher` varsayılan olarak process içi ve senkrondur;
tek başına hata izolasyonu veya kalıcı teslimat sağlamaz. `@Async` kullanmak da
uygulama çöktüğünde olayın kaybolmasını engellemez. Güvenilir dış olay teslimatı
gerektiğinde PostgreSQL Outbox ve idempotent consumer kullanılır.

Bir modül başka modülün repository/infrastructure paketine veya tablosuna
doğrudan erişmez. Gerçek modül sınırları oluştuğunda bu bağımlılık yönü az
sayıda ArchUnit testiyle korunur. Henüz davranışı bulunmayan bütün modül ve
katman klasörleri peşinen oluşturulmaz.

## Başlangıç veri altyapısı

### PostgreSQL

Kullanıcılar, içerikler, quiz sürümleri, attempt'ler, cevaplar ve XP işlemleri
için kalıcı doğru kaynaktır.

### Redis

Redis, öğrenme hedefi nedeniyle Aşama 8'de eklendi. Leaderboard kuralları ve
PostgreSQL tabanlı doğru sonuç önce kanıtlandı. Redis'in ilk uygulaması:

- Leaderboard sorted set
- PostgreSQL'den yeniden oluşturulabilir leaderboard read model
- PostgreSQL ve Redis sonucunu karşılaştıran tutarlılık testi
- Beş saniyelik kontrollü yenileme ve ADMIN rebuild yolu
- Atomik nesil işaretçisi, PostgreSQL fallback ve kaynak metrikleri

ile sınırlıdır. Sık okunan quiz cache'i Redis öğrenme kapsamını büyütmek için
otomatik eklenmez. Aşama 9 abuse kontrolü, Redis kesintisinden bağımsız kalması
için uzak IP anahtarlı ve belleği sınırlı local token bucket olarak eklendi.
Global çok-instance kota gerçek bir gereksinim olursa Redis veya gateway
limiter ayrı kararla değerlendirilir. Redis kaybı kalıcı iş verisi kaybına
neden olmamalı; leaderboard PostgreSQL'den yeniden kurulabilmelidir.

`leaderboard.redis.hit` ve `leaderboard.postgresql.fallback` sayaçları okuma
kaynağını izler. API ayrıca `dataSource` ve Redis üretim zamanını döndürür.
Projeksiyon yenilemeleri snapshot tabanlıdır; bu nedenle duplicate olay Redis'te
ikinci kez artırma yapmaz. Bedeli, varsayılan yenileme aralığı boyunca stale
sonuç görülebilmesidir.

### RabbitMQ

Çekirdek gameplay ve PostgreSQL tabanlı idempotent XP ledger senkron olarak
kanıtlandıktan sonra Aşama 6'da `quiz.completed` v1 integration event'inin XP
yan etkisini ayırmak için RabbitMQ eklendi. İleride leaderboard veya başka
tüketiciler aynı sürümlü olaydan bağımsız kuyruklarla beslenebilir:

```text
QuizCompleted
├── XP oluştur
├── Leaderboard güncelle
└── İleride rozet/analitik işlemlerini ayır
```

Gameplay sonucu ve Outbox kaydı aynı PostgreSQL transaction'ında oluşturulur.
Publisher `FOR UPDATE SKIP LOCKED`, kalıcı mesaj ve publisher confirm kullanır.
XP consumer'ı Inbox claim'i ile ledger kaydını aynı transaction'da yazar;
at-least-once tekrar teslimatı ikinci XP üretmez. Üç deneme sonrasında kalıcı
hatalar dead-letter queue'ya taşınır.

RabbitMQ taşıma mekanizmasıdır, kalıcı doğru kaynak değildir. Broker kapalıyken
attempt tamamlanır ve Outbox olayı PostgreSQL'de bekler. Son-answer/complete
yanıtındaki `earnedXp` kesin skorla hemen hesaplanır; XP özeti consumer çalışana
kadar kısa süreli eski değer döndürebilir.

## Attempt zamanı ve yaşam döngüsü

- Attempt, kimliği doğrulanmış kullanıcı ve sabit `quizVersionId` ile başlar.
- Başlangıç, deadline ve cevap kabul zamanı sunucu tarafından belirlenir ve UTC
  saklanır.
- Frontend sayacı yalnız kullanıcı deneyimidir; karar kaynağı değildir.
- Testlerde gerçek saate bağımlı kalmamak için uygulama/domain'e bir `Clock`
  sağlanır.
- `ACTIVE`, `COMPLETED` ve `EXPIRED` geçişleri açık ve tek yönlüdür.
- Complete göndermeyen veya bağlantısı kopan attempt'in nasıl expire edileceği
  gameplay aşamasında açıkça kararlaştırılır.
- Aynı kullanıcının eşzamanlı aktif attempt politikası gameplay başlamadan
  belirlenir.
- Attempt, kullanılan quiz ve puanlama politikası sürümünü sabitler.

## Kritik kurallar

- Attempt başladığında `quizVersionId` sabitlenir.
- Attempt kimliği doğrulanmış kullanıcıya aittir; sahiplik her erişimde kontrol
  edilir.
- Yayınlanmış quiz sürümü immutable kabul edilir.
- `user_answers` üzerinde attempt ve question için unique constraint bulunur.
- Puanlama ve süre sunucu tarafından hesaplanır.
- Bilgi taşıyan medya metin alternatifsiz; görsele dayalı soru cevabı
  sızdırmayan eşdeğer sunum olmadan yayınlanamaz.
- Her yayınlanmış soru için kullanılan medya quiz sürümüne sabitlenir.
- Doğru/yanlış geri bildirimi yalnız renge bağlı bir sözleşme olmaz.
- XP append-only işlem defteri olarak saklanır.
- Aynı kaynak için iki XP işlemi unique constraint ile engellenir.
- `SCORE_MATCH_V1` XP tutarı kesinleşmiş sunucu skoruna eşittir.
- Düzeltme, özgün kazancı değiştirmek yerine yalnız ADMIN'in oluşturabildiği
  imzalı yeni kayıtla yapılır.
- Redis leaderboard PostgreSQL verilerinden yeniden kurulabilir.

## Leaderboard iş kuralları

- İlk sürüm yalnız `ALL_TIME` dönemidir; sıfırlama yoktur.
- En iyi attempt seçilmez; ledger'a giren bütün completion XP'leri ve onların
  yönetici düzeltmeleri toplamı belirler.
- Global kapsam bütün XP'yi, içerik kapsamı yalnız ilgili içerik aidiyetini
  toplar.
- Tie-break toplam XP azalan, ilk XP zamanı artan, kullanıcı UUID'si artandır.
- Sıfır XP'li completion katılım sayılır; adjustment nedeniyle negatif toplam
  mümkündür.
- Top N yanında mevcut kullanıcının sırası ayrıca döner.
- Yüzdelik, sezon, hile/diskalifiye, profil adı ve arkadaş kapsamı ilk sürümde
  yoktur.

XP içeriği yalnız uygulama koduna güvenmez. Flyway V8 migration'ı eski satırları
geri doldurur; PostgreSQL trigger'ı completion satırını kaynak attempt, adjustment
satırını özgün completion ile kullanıcı ve içerik bakımından doğrular.

## Migration ve kapasite yaklaşımı

- Şema yalnız Flyway ile değiştirilir.
- Küçük/boş geliştirme tablolarında sade migration tercih edilir.
- Büyük production tablolarında index, backfill, constraint doğrulama ve veri
  tipi değişiklikleri kademeli expand-contract yaklaşımını izler. V9'un nullable
  W3C context kolonları ve eski Outbox satırı testi ilk uyumluluk provasıdır.
- Transaction dışı çalışması gereken PostgreSQL DDL komutları Flyway davranışı
  dikkate alınarak ayrı doğrulanır.
- Redis, connection pool veya ölçek kararı vermeden önce örnek kapasite modeli
  tanımlanır: eşzamanlı attempt, saniyedeki cevap, leaderboard büyüklüğü ve
  hedef p95 gecikme.

## Başlangıç API yüzeyi

```text
GET  /api/v1/contents
GET  /api/v1/episodes/{episodeId}/quizzes
POST /api/v1/attempts
POST /api/v1/attempts/{attemptId}/answers
POST /api/v1/attempts/{attemptId}/complete
GET  /api/v1/me/xp
POST /api/v1/admin/xp-transactions/{transactionId}/adjustments
GET  /api/v1/leaderboards/{scope}
```

Admin endpoint'leri ayrı yetki gerektirir.

## Güvenlik

- Kimlik doğrulama için kurum standardı varsa ona uyulur.
- Admin API'lerinde RBAC uygulanır.
- Doğru cevap istemci DTO'suna eklenmez.
- İstemci tarafından bildirilen süre güvenilir kabul edilmez.
- Token, secret, doğru cevap ve kişisel veri loglanmaz.
- Her kaynağa erişimde sahiplik/yetki kontrol edilir.

## Gözlemlenebilirlik

İlk sürümde:

- Destek amaçlı `requestTraceId` ile OpenTelemetry `traceId/spanId` ayrımı
- HTTP → Outbox → RabbitMQ boyunca W3C `traceparent`/`tracestate`
- Structured log ve merkezi hata modeli
- Prometheus HTTP/gecikme ve mesajlaşma metrikleri
- Tempo trace deposu ve provision edilmiş Grafana dashboard'u

bulunur. Event/actor UUID gibi sınırsız değerler metric label'ı yapılmaz.
RabbitMQ publish/consume süresi, başarı/hata, retry ve DLQ operasyon sinyalleri
izlenir. Production scraper/exporter ağı, sampling, retention ve alarm kanalı
kurum altyapısıyla kesinleştirilmelidir.

## Açık kararlar

- Java/Spring Boot ile kurumun kullandığı backend teknolojisinin uyumu
- Maven veya Gradle
- Kimlik doğrulama yöntemi
- Medya dosyalarının gerçek saklama/erişim yaklaşımı
- Leaderboard dönem/sıfırlama ve hile/diskalifiye operasyon politikası
- Attempt otomatik expire ve tekrar çözme politikası
- Gelecekte XP bonusu/çarpanı gerekip gerekmediği
- Gerçek production trafik hedefi ve ilk ölçümlerin SLO/alarm kalibrasyonu

Bu kararlar uygulamaya başlamadan veya ilgili aşamaya gelindiğinde ADR olarak
kaydedilmelidir.

## Teknoloji kararlarının değerlendirme biçimi

| Öneri | Çözdüğü problem | Neden başlangıç adayı? | Ne zaman yeniden değerlendirilir? |
|---|---|---|---|
| Java 21 + Spring Boot | Web API, transaction ve ekosistem | Güçlü tip sistemi, olgun Spring ekosistemi ve öğrenme değeri | Kurum standardı veya mentor ekibi farklıysa |
| PostgreSQL | Kalıcı iş verisi ve veri bütünlüğü | Transaction, constraint, indeks ve locking yetenekleri | Ölçülmüş ve belirli bir iş yükünde yetersizse |
| Flyway | Şema değişikliklerini sürümlemek | Tekrarlanabilir ortam ve inceleme geçmişi | Kurumun eşdeğer migration standardı varsa |
| Docker Compose | Yerel bağımlılıkları tekrarlanabilir çalıştırmak | PostgreSQL'i makineye özel kurulumdan ayırır | Kurumun Podman/dev-container standardı varsa |
| OpenAPI | İstemci-sunucu sözleşmesini görünür kılmak | API-first geliştirme ve test edilebilir sözleşme | Özel iç servis ihtiyacı gRPC gibi başka protokol gerektirirse |
| Testcontainers | Gerçek altyapı davranışını test etmek | PostgreSQL constraint/migration davranışını sahte DB olmadan doğrular | CI altyapısı container çalıştıramıyorsa eşdeğer ephemeral ortam aranır |
| RabbitMQ | Tamamlama sonrası bağımsız yan etkiler | Kurum gereksinimi; routing, retry ve DLQ ile güvenilir asenkron işlemeyi sağlar | Kurum standardı değişir veya replay/stream hacmi Kafka gibi başka bir broker gerektirirse |
| Redis | Hızlı leaderboard ve geçici veri | Sorted set ve düşük gecikmeli sıralama | PostgreSQL çözümü yeterliyse eklenmez; kalıcı doğru kaynak yapılmaz |
| OpenTelemetry + Prometheus | İstek/mesaj akışını ve eğilimleri izlemek | Standart W3C context, düşük cardinality metrik ve mevcut Spring entegrasyonu | Kurumun farklı telemetry backend'i veya sampling standardı varsa |
| Yerel token bucket | Tek-instance MVP abuse kontrolü | Redis'ten bağımsız, düşük maliyetli ve bounded bellek | Çok instance'ta global kullanıcı/API-key kotası gerekirse gateway veya dağıtık limiter seçilir |
