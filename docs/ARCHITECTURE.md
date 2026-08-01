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
├── quiz
├── gameplay
├── gamification
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

### quiz

Quiz, quiz sürümü, soru, seçenek ve yayınlama sürecini yönetir.

### gameplay

Attempt, cevap, süre, durum geçişi ve puanlamayı yönetir.

### gamification

XP işlem defteri, XP özeti ve gelecekte rozetleri yönetir.

### leaderboard

Global, içerik ve dönem bazlı sıralamayı yönetir.

### admin

Editör işlemlerini, yayınlama yetkilerini ve audit kayıtlarını koordine eder;
content ve quiz iş kurallarını kendi içinde tekrar etmez.

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

Redis, öğrenme hedefi nedeniyle Aşama 8'de kullanılacaktır. Ancak leaderboard
kuralları ve PostgreSQL tabanlı doğru sonuç önce kanıtlanacaktır. Redis'in ilk
uygulaması:

- Leaderboard sorted set
- PostgreSQL'den yeniden oluşturulabilir leaderboard read model
- PostgreSQL ve Redis sonucunu karşılaştıran tutarlılık kontrolü

ile sınırlıdır. Sık okunan quiz cache'i ve rate limiting, Redis öğrenme
kapsamını büyütmek için otomatik olarak eklenmez; bunlar ayrıca ölçülmüş okuma
yükü veya abuse ihtiyacı gerektirir. Redis kaybı kalıcı iş verisi kaybına neden
olmamalı; leaderboard PostgreSQL'den yeniden kurulabilmelidir.

### RabbitMQ

Çekirdek gameplay ve PostgreSQL tabanlı idempotent XP ledger senkron ve
güvenilir biçimde tamamlandıktan sonra `QuizCompleted` integration event'inin
aşağıdaki yan etkilerini ayırmak için kullanılacaktır. RabbitMQ kullanımı
stajdan sorumlu mühendis tarafından proje gereksinimi olarak bildirilmiştir;
uygulama sırası yine iş davranışını altyapıdan önce kanıtlama kararını korur:

```text
QuizCompleted
├── XP oluştur
├── Leaderboard güncelle
└── İleride rozet/analitik işlemlerini ayır
```

Gameplay sonucu ve Outbox kaydı aynı PostgreSQL transaction'ında oluşturulur.
Consumer'lar Inbox/idempotency yaklaşımıyla tekrar teslimata dayanıklı olur.

RabbitMQ hedef mimarinin gerekli bir bileşenidir fakat XP iş kuralının doğruluk
şartı değildir. Önce aynı davranış PostgreSQL üzerinde doğru çalışır; broker
aşamasında işlem sınırı değiştirilirken sonuçların değişmediği testlerle
kanıtlanır.

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
- XP append-only işlem defteri olarak saklanır.
- Aynı kaynak için iki XP işlemi unique constraint ile engellenir.
- Redis leaderboard PostgreSQL verilerinden yeniden kurulabilir.

## Leaderboard iş kuralları

Redis tasarımından önce aşağıdaki ürün kararları verilir:

- Kullanıcının en iyi, ilk veya son attempt'lerinden hangisinin sayıldığı
- Tekrar çözme sınırı
- Eşit puanda deterministik tie-break sırası
- Global ve içerik bazlı skorun hangi veriden üretildiği
- Dönem/sıfırlama davranışı
- İptal edilmiş veya hileli attempt'in sıralamadan çıkarılma yöntemi

Bu kurallar PostgreSQL üzerinde test edilmeden Redis veri yapısı seçilmez.

## Migration ve kapasite yaklaşımı

- Şema yalnız Flyway ile değiştirilir.
- Küçük/boş geliştirme tablolarında sade migration tercih edilir.
- Büyük production tablolarında index, backfill, constraint doğrulama ve veri
  tipi değişiklikleri için kademeli expand-contract yaklaşımı değerlendirilir.
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
GET  /api/v1/me/stats
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

İlk sürümden itibaren:

- Correlation/trace ID
- Structured log
- Merkezi hata modeli
- HTTP ve veritabanı süre metrikleri

bulunmalıdır. RabbitMQ eklendiğinde queue depth, retry, DLQ ve consumer işlem
süreleri de izlenir.

## Açık kararlar

- Java/Spring Boot ile kurumun kullandığı backend teknolojisinin uyumu
- Maven veya Gradle
- Kimlik doğrulama yöntemi
- Medya dosyalarının gerçek saklama/erişim yaklaşımı
- İlk leaderboard kapsamı ve sıfırlama dönemi
- Attempt süre modeli, otomatik expire ve tekrar çözme politikası
- Puanlama ve XP politikalarının sürümleme yöntemi
- Öğrenme/demo ve hedef ortam için kapasite varsayımları

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
