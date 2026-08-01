# Geliştirme Yol Haritası

Her görevde yalnızca bir aşama ele alınmalıdır. Bir sonraki aşamaya kullanıcı
onayı olmadan geçilmez.

Her aşama başlamadan önce önceki aşamanın kabul kriterleri ve açık sorunları
gözden geçirilir. Her aşama sonunda sistemin ilgili parçası kullanıcıya
anlatılır; testlerin hangi riski kanıtladığı ve açık kalan kararlar
`docs/CURRENT_STATE.md` içinde kaydedilir.

Bu yol haritasında iş davranışı ile altyapı ayrılmıştır. XP, RabbitMQ olmadan;
doğru leaderboard da Redis olmadan önce çalıştırılır. Böylece yeni teknoloji
eklenmeden önce korunacak davranış bilinir.

RabbitMQ kullanımı stajdan sorumlu mühendis tarafından proje gereksinimi olarak
bildirilmiştir. Bu zorunluluk Aşama 6'nın uygulanacağını kesinleştirir; RabbitMQ
yine çekirdek gameplay ve PostgreSQL XP davranışı kanıtlandıktan sonra eklenir.

## Aşama 0 - Proje temeli

Durum: Tamamlandı; yerel doğrulama ve hosted CI başarılı

### İşler

- Kurum/mentor teknoloji standardını öğren
- Java 21 ile uyumlu ve desteklenen Spring Boot sürümünü doğrula
- Maven veya Gradle seç
- Git deposunu başlat
- Minimal uygulama iskeleti oluştur
- Yalnız PostgreSQL ve Docker Compose ekle
- Flyway migration altyapısı ve boş/baseline migration ekle
- Health endpoint ve temel hata modeli oluştur
- Testcontainers smoke testi yaz
- CI için başlangıç pipeline'ı ekle

### Kabul kriterleri

- Uygulama yerel ortamda başlar.
- PostgreSQL bağlantısı çalışır.
- Migration otomatik uygulanır.
- Health endpoint başarılı cevap verir.
- En az bir integration smoke testi geçer.
- README'de gerçek çalıştırma komutları bulunur.
- Redis ve RabbitMQ henüz çalışma ortamına eklenmemiştir.

### Test yaklaşımı

- Uygulama context'inin açıldığını doğrulayan smoke test
- Testcontainers ile gerçek PostgreSQL bağlantısı ve Flyway migration testi
- Health ve temel hata zarfı için API testi
- CI'da aynı test komutunun temiz ortamda çalıştığının doğrulanması

### Öğrenme çıktısı

Kullanıcı build lifecycle, dependency injection, configuration profile,
migration ve gerçek veritabanıyla integration test farkını açıklayabilir.

## Aşama 1 - Kimlik ve erişim temeli

Durum: Tamamlandı; geçici kimlik yalnız local/test profillerinde

Bu aşama content ve gameplay kaynaklarının gerçek bir kullanıcıya ait
olabilmesi için öne alınmıştır. Kurum kimlik sağlayıcısı bilinmiyorsa geçici test
kimliği kullanılabilir; bu yaklaşım production authentication gibi
sunulmamalıdır.

### İşler

- Authentication yaklaşımını kurum standardıyla doğrula ve ADR kaydet
- User kimliği ile USER, EDITOR ve ADMIN rollerini modelle
- Request içinden güvenilir current-user/actor bağlamı üret
- Rol ve kaynak sahipliği kontrollerinin ortak uygulama şeklini belirle
- Hassas veri ve parola saklama sınırını belirle

### Kabul kriterleri

- Kimliği olmayan istek korunan endpoint'e erişemez.
- USER, EDITOR ve ADMIN test kimlikleri birbirinden ayrılır.
- Application use case'i kullanıcı kimliğini frontend payload'ından değil,
  güvenilir actor bağlamından alır.
- Geçici authentication kullanılıyorsa production sınırlaması belgelenir.

### Test yaklaşımı

- Authentication ve rol eşleme integration testleri
- Eksik, geçersiz ve yanlış rollü istekler için API testleri
- Kullanıcı kimliğinin request body ile taklit edilemediğini doğrulayan test
- Security loglarının token/parola taşımadığını doğrulayan test

### Öğrenme çıktısı

Kullanıcı authentication, authorization, RBAC ve kaynak sahipliği arasındaki
farkı açıklayabilir.

## Aşama 2 - İçerik kataloğu

Durum: Tamamlandı; 21 test başarılı

### İşler

- Content, Season ve Episode domain modelleri
- Migration ve constraint'ler
- EDITOR/ADMIN yetkili CRUD API'leri
- Kullanıcı listeleme ve cursor/standart pagination
- Validation ve hata kodları
- Kritik yönetici işlemleri için audit kaydı
- İlk gerçek modül sınırları için küçük ArchUnit kural seti

### Kabul kriterleri

- Dizi, sezon ve bölüm hiyerarşisi oluşturulabilir.
- Aynı sezon/bölüm numarası tekrar edemez.
- Yayında olmayan içerik kullanıcı API'sinde görünmez.
- Normal USER içerik yönetimi yapamaz.
- Modül domain'i Spring/JPA ayrıntılarına ve başka modülün infrastructure
  paketine bağımlı değildir.

### Test yaklaşımı

- Content, Season ve Episode kuralları için saf domain unit testleri
- Unique ve foreign key constraint'leri için PostgreSQL integration testleri
- Admin authorization, validation ve yayın filtresi için API testleri
- Pagination sınırları ve boş sonuç davranışı testleri
- Bağımlılık yönü için ArchUnit testleri

### Öğrenme çıktısı

Kullanıcı modül ile katman farkını, rich domain modelini, entity/value object
ayrımını, migration görevini ve controller-application-domain-repository
akışını açıklayabilir.

## Aşama 3 - Quiz authoring ve sürümleme

Durum: Bekliyor

### İşler

- Quiz, QuizVersion, Question ve AnswerOption modelleri
- Draft, published ve archived durumları
- Doğru cevap ve zorluk tanımı
- Quiz yayınlama use case'i
- Yetki ve admin audit kaydı
- Puanlama politikasının sürümleme sınırını belirle

### Kabul kriterleri

- Draft quiz oluşturulabilir ve yayınlanabilir.
- Eksik veya geçersiz quiz yayınlanamaz.
- Yayındaki sürüm yerinde değiştirilemez.
- Yeni düzenleme yeni draft sürümü oluşturur.
- Normal USER doğru cevap ve yönetim API'lerine erişemez.

### Test yaklaşımı

- Quiz yayınlama ve durum geçişleri için saf domain unit testleri
- Sürüm, soru ve seçenek constraint'leri için PostgreSQL integration testleri
- Authorization, draft/publish/archive ve validation için API testleri
- Yayınlanmış sürümün değişmezliğini kanıtlayan regresyon testi
- Doğru cevap alanının kullanıcı DTO'suna sızmadığını doğrulayan contract test

### Öğrenme çıktısı

Kullanıcı immutable yayın sürümünün geçmiş attempt'leri neden koruduğunu, iş
kurallarının neden entity davranışı olduğunu ve policy sürümlemenin amacını
açıklayabilir.

## Aşama 4 - Gameplay

Durum: Bekliyor

### İşler

- Kimliği doğrulanmış kullanıcı için attempt başlatma ve sahiplik
- Sunucu taraflı soru sırası
- Cevap gönderme
- Sunucu `Clock` kaynağı, UTC deadline ve süre kontrolü
- ACTIVE, COMPLETED ve EXPIRED durum makinesi
- Complete göndermeyen attempt için expire davranışı
- Aynı kullanıcının aktif/tekrar attempt politikası
- Sürümlü puanlama politikası
- Attempt tamamlama
- Idempotency ve concurrency davranışları
- Process içi `QuizAttemptCompleted` domain event tanımı

### Kabul kriterleri

- Kullanıcı başka kullanıcının attempt'ine erişemez.
- Aynı soruya ikinci cevap kalıcı olarak engellenir.
- Süresi geçen cevap sunucu saatine göre kabul edilmez.
- Tamamlanmış veya expired attempt cevap kabul etmez.
- Doğru seçenek istemciye önceden gönderilmez.
- Aynı complete isteği tek sonuç üretir.
- Attempt kullanılan quiz ve puanlama politikası sürümünü sabitler.
- Paralel istek testleri geçer.
- Domain event dış mesajın güvenilir gönderildiği anlamına gelmez.

### Test yaklaşımı

- Attempt durum makinesi, sahiplik, sabit `Clock`, süre ve skor politikası için
  deterministik saf unit testler
- Gerçek PostgreSQL ile answer/complete transaction integration testleri
- Aynı answer ve complete isteğini paralel gönderen concurrency testleri
- Unique constraint, atomik durum geçişi ve gerekiyorsa optimistic locking testi
- Idempotency-Key tekrarı ve aynı key/farklı payload çakışması için API testleri
- İstemci DTO'sunda doğru cevabın bulunmadığını doğrulayan contract test

### Öğrenme çıktısı

Kullanıcı transaction, race condition, unique constraint, idempotency,
optimistic locking ve server-authoritative time/scoring kavramlarını örnek akış
üzerinden açıklayabilir.

## Aşama 5 - PostgreSQL üzerinde XP

Durum: Bekliyor

Bu aşamada RabbitMQ kullanılmaz. Amaç önce XP iş kuralını ve veri bütünlüğünü
öğrenip kanıtlamaktır.

### İşler

- Append-only XP transaction ledger
- XP sebebi, kaynak attempt ve politika sürümü
- Quiz tamamlandığında senkron ve idempotent XP oluşturma
- XP özeti sorgusu
- Düzeltme/telafi kaydı yaklaşımı

### Kabul kriterleri

- Tamamlanmış geçerli attempt tek XP kaydı üretir.
- Aynı complete isteği veya aynı attempt ikinci XP üretmez.
- XP geçmişi geriye dönük satır değiştirmek yerine yeni işlemle düzeltilir.
- RabbitMQ kapalı/kurulu değilken bütün XP davranışı çalışır.

### Test yaklaşımı

- XP politikası ve telafi kuralları için saf domain unit testleri
- Attempt complete ve XP transaction sınırı için PostgreSQL integration testi
- XP kaynak unique constraint ve paralel complete testi
- Kullanıcı XP özeti için repository/API testleri

### Öğrenme çıktısı

Kullanıcı ledger, append-only kayıt, idempotency ve aynı veritabanındaki
transaction sınırını açıklayabilir.

## Aşama 6 - Güvenilir mesajlaşma ve RabbitMQ

Durum: Bekliyor

Bu aşama çalışan gameplay ve XP davranışını asenkronlaştırmayı öğretir.
RabbitMQ proje gereksinimi olarak bu aşamada eklenecektir. Eklenmesi XP sonucunu
değiştirmemelidir.

### İşler

- Domain event ile sürümlü integration event ayrımı
- Transactional Outbox
- RabbitMQ publisher
- XP consumer
- Inbox/deduplication
- Retry, backoff ve DLQ
- Correlation ID ve temel broker metrikleri

### Kabul kriterleri

- Broker kapalıyken tamamlanan quiz ve Outbox kaydı kaybolmaz.
- Broker geri geldiğinde bekleyen olay yayımlanır.
- Aynı event iki kez teslim edilse de tek XP işlemi oluşur.
- Listener/consumer hatası attempt sonucunu bozmaz.
- Retry ve DLQ davranışı gözlemlenebilir.

### Test yaklaşımı

- Attempt sonucu ile Outbox kaydının aynı transaction'da olduğunu doğrulayan
  PostgreSQL integration testi
- Testcontainers RabbitMQ ile publish/consume/retry/DLQ integration testleri
- Duplicate event için Inbox ve XP unique constraint testi
- Broker kesinti, uygulama yeniden başlatma ve geri dönüş senaryosu
- Senkron ve asenkron XP sonuçlarını karşılaştıran regresyon testi

### Öğrenme çıktısı

Kullanıcı domain event/integration event farkını, dual-write problemini,
at-least-once delivery'yi, Outbox/Inbox ve idempotent consumer davranışını
açıklayabilir.

## Aşama 7 - PostgreSQL üzerinde leaderboard doğruluğu

Durum: Bekliyor

Bu aşamada Redis kullanılmaz. Önce sıralamanın ürün kuralları ve doğru sonucu
kanıtlanır.

### İşler

- Global ve içerik bazlı sıralama tanımı
- En iyi/ilk/son attempt ve tekrar çözme kuralı
- Deterministik tie-break
- Dönem/sıfırlama davranışı
- İptal/hileli attempt davranışı
- Top N ve “benim sıram” PostgreSQL sorguları
- Kapasite modeli ve başlangıç performans ölçümü

### Kabul kriterleri

- Sıralama aynı veri için her zaman aynı sonucu üretir.
- Hangi attempt'in sıralamaya girdiği açıkça belgelenmiştir.
- Global ve içerik bazlı sonuçlar PostgreSQL'den okunabilir.
- Küçük örneklemde yanıltıcı yüzdelik gösterilmez.
- Örnek veri hacminde p95 ve sorgu planı kaydedilir.

### Test yaklaşımı

- Puan, tekrar, tie-break ve yüzdelik kuralları için unit/property tabanlı test
- Gerçek PostgreSQL sorgu ve index integration testleri
- Sınır değerleri, eşit skor ve diskalifiye attempt senaryoları
- Top N ve “benim sıram” için kontrollü performans testi

### Öğrenme çıktısı

Kullanıcı leaderboard iş kuralı ile veri yapısı arasındaki farkı, deterministik
sıralamayı, index ve query plan temelini açıklayabilir.

## Aşama 8 - Redis leaderboard read model

Durum: Bekliyor; öğrenme hedefi nedeniyle uygulanması zorunlu

PostgreSQL ölçümü kabul kriterini karşılasa bile Redis bu projede öğrenme hedefi
nedeniyle uygulanır. Redis kalıcı doğru kaynak olmaz; PostgreSQL sonucundan
yeniden kurulabilen leaderboard read model olarak eklenir. Önce/sonra ölçümü,
Redis'in sağladığı değeri ve eklediği tutarlılık maliyetini görünür kılar.

### İşler

- Redis sorted set read model
- Event veya kontrollü projection güncellemesi
- PostgreSQL snapshot/özet ile karşılaştırma
- Redis rebuild görevi
- Geçici kesinti ve stale-data davranışı

### Kabul kriterleri

- Redis sonucu PostgreSQL doğruluk modeliyle tutarlıdır.
- Redis silindikten sonra leaderboard yeniden kurulabilir.
- Redis kesintisi kalıcı skor/XP kaybına yol açmaz.
- Redis eklemenin ölçülen etkisi önce/sonra karşılaştırmasıyla gösterilir.

### Test yaklaşımı

- Testcontainers Redis ile sorted set integration testleri
- PostgreSQL sonucu ile Redis sonucunu karşılaştıran tutarlılık testi
- Redis silme, rebuild, duplicate event ve geçici kesinti senaryoları
- Top N ve “benim sıram” önce/sonra performans testi

### Öğrenme çıktısı

Kullanıcı cache/read model ile doğru kaynak farkını, eventual consistency'yi,
Redis sorted set'i ve rebuild stratejisini açıklayabilir.

## Aşama 9 - Operasyon ve production hazırlığı

Durum: Bekliyor

### İşler

- OpenTelemetry trace
- Prometheus metrikleri ve Grafana dashboard
- Rate limiting ve abuse kontrolleri
- Güvenlik ve dependency taraması
- k6 veya Gatling yük testi
- PostgreSQL, RabbitMQ ve Redis kesinti senaryoları
- Migration expand-contract ve kilit riskleri
- Backup/restore denemesi ve RPO/RTO hedefi
- Veri saklama/silme, KVKK ve audit retention kararları

### Kabul kriterleri

- API'den consumer'a kadar correlation/trace izlenebilir.
- p95/p99 latency, hata oranı ve kapasite varsayımı raporlanır.
- Kontrollü arıza senaryolarında kalıcı veri kaybı olmadığı gösterilir.
- Backup'tan geri yükleme denenmiştir.
- Büyük tablo migration senaryosu ve geri dönüş yöntemi belgelenmiştir.
- Kişisel veri ve audit saklama sınırları tanımlanmıştır.

### Test yaklaşımı

- k6 veya Gatling ile baseline, ramp, spike ve kısa soak profilleri
- PostgreSQL, RabbitMQ ve Redis kesintisi için kontrollü failure testleri
- Trace ID'nin HTTP, Outbox ve consumer boyunca taşındığının doğrulanması
- Rate limit, dependency/secret taraması ve temel güvenlik testleri
- Backup/restore ve örnek expand-contract migration provası

### Öğrenme çıktısı

Kullanıcı metric-log-trace, SLI/SLO, RPO/RTO, darboğaz ölçümü, güvenli migration
ve performans testinin neden yalnız istek sayısı olmadığını açıklayabilir.

## Aşama 10 - Opsiyonel genişlemeler

Çekirdek sistem ve operasyon kalitesi tamamlanmadan başlanmaz:

- Rozetler
- Arkadaş meydan okuması
- Dil öğrenme modu
- TV ve telefon QR eşleşmesi
- WebSocket canlı oturum
- AI destekli, editör onaylı soru önerisi

Bu bölüm tek bir uygulama aşaması değildir. Her madde ayrı ürün kararı, ADR,
kabul kriteri ve test planı gerektiren bağımsız bir gelecek aşamasıdır.
