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

Durum: Tamamlandı; 37 testlik tam doğrulama başarılı

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

Durum: Tamamlandı; 47 testlik tam doğrulama başarılı

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

## Aşama 4.1 - Erişilebilir medya ve kapsayıcı gameplay sözleşmesi

Durum: Tamamlandı; 57 testlik tam doğrulama başarılı

Bu aşama görselli soru ihtiyacını, sonradan frontend'e bırakılamayacak
erişilebilirlik verilerini ve süre davranışını birlikte ele alır. Mühendislik
hedefi WCAG 2.2 AA'dır; bu hedef tek başına hukukî uygunluk veya sertifika
iddiası değildir.

### İşler

- Değişmez medya varlığı kimliği ve storage adapter portu
- İçerik için kapak görseli ve erişilebilir açıklama
- Soru için isteğe bağlı özel görsel, görsel rolü ve alternatif metin
- Görsel bilgiye dayanan soru için cevabı açığa çıkarmayan eşdeğer
  erişilebilir soru metni
- Soruya özel görsel yoksa quiz yayınında içerik kapağını quiz sürümüne
  sabitleyen fallback
- Gameplay API'sinde çözümlenmiş görsel, metin alternatifi, semantik
  doğru/yanlış durumu ve ilerleme bilgisi
- Her kullanıcının engel/sağlık verisi bildirmeden seçebildiği standart veya
  en az on kat uzun ayarlanabilir süre modu
- Gelecekte ses/video eklenirse caption, transcript ve audio-description
  referanslarını zorunlu kılacak medya sözleşmesi sınırı
- Admin publish doğrulaması ve erişilebilirlik audit kaydı

### Kabul kriterleri

- Her yayınlanmış soru, soruya özel görseli veya quiz sürümüne sabitlenmiş
  kapak fallback'ini döndürür.
- Görsele dayalı soru, cevabı sızdırmayan eşdeğer erişilebilir metin olmadan
  yayınlanamaz.
- Dekoratif ve bilgi taşıyan görsel API sözleşmesinde ayırt edilir; bilgi
  taşıyan görsel alternatif metinsiz yayınlanamaz.
- Kapak veya soru görselinin yeni sürümü eski quiz ve attempt görünümünü
  değiştirmez.
- Doğru/yanlış bilgisi yalnız renkle ifade edilmez; API semantik durum kodu
  ve doğru seçenek kimliği sağlar.
- Kullanıcı attempt başlamadan standart veya en az on kat uzun süreyi seçebilir;
  seçim attempt'te sürümlü olarak sabitlenir ve skor bonusu üretmez.
- Kullanıcının engel türü veya sağlık bilgisi saklanmaz.
- Medya URL'si istemci girdisiyle keyfî bir dış kaynağa yönlendirilemez;
  MIME türü, boyut ve bütünlük kontrolleri storage sınırında yapılır.

### Test yaklaşımı

- Medya fallback'i, değişmezlik ve publish kuralları için domain unit testleri
- Flyway ve medya/quiz referansları için PostgreSQL integration testleri
- Doğru cevabın alternatif metin veya erişilebilir prompt ile sızmadığını
  doğrulayan contract testleri
- Standart ve uzatılmış süreyi sabit `Clock` ile doğrulayan gameplay testleri
- Yetkisiz medya yazma, geçersiz MIME/boyut ve başka kaynağa ait medya
  bağlama API testleri
- Erişilebilirlik alanlarının admin ve gameplay response'larında korunduğunu
  doğrulayan sözleşme testleri

### Öğrenme çıktısı

Kullanıcı alternatif metin ile eşdeğer soru sunumunun farkını, medya
değişmezliğini, ayarlanabilir süreyi ve backend/frontend erişilebilirlik
sorumluluk sınırını açıklayabilir.

## Aşama 5 - PostgreSQL üzerinde XP

Durum: Tamamlandı; 66 testlik tam doğrulama başarılı

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

Durum: Tamamlandı. 74 testlik tam doğrulama başarıyla geçti.

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

Durum: Tamamlandı. 83 testlik tam doğrulama başarıyla geçti.

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

Durum: Tamamlandı (04.08.2026)

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

Durum: Tamamlandı (04.08.2026); 97 test ve ayrı k6/OSV doğrulamaları başarılı

OpenTelemetry HTTP trace üretimi, W3C bağlamını sürdüren Outbox/RabbitMQ akışı,
Prometheus/Grafana/Tempo profili, token-bucket rate limiter, güvenlik kapıları,
k6 profilleri, PostgreSQL/RabbitMQ/Redis kesinti kanıtları, backup/restore
provası ve production runbook'u tamamlandı. İlk retention ve RPO/RTO sınırları
ADR-0016 ile kaydedildi.

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

## Aşama 10A - Admin web temeli ve içerik yönetimi

Durum: Tamamlandı (06.08.2026); 98 backend ve 4 frontend testi başarılı

Bu aşama ayrı `admin-web` uygulamasının temelini ve yalnız içerik kataloğu
yönetimini kapsar. Production kimlik sağlayıcısı belli olmadığı için gerçek
login uygulanmaz; geçici aktör yalnız yerel geliştirme kolaylığıdır.

### İşler

- React, TypeScript ve Vite tabanlı ayrı frontend uygulaması
- EDITOR/ADMIN istemci rota koruması ve görünür yerel aktör bilgisi
- Draft dahil sayfalı admin içerik listesi
- İçerik oluşturma, detay okuma ve draft düzenleme
- Dizi sezon/bölüm ekleme, düzenleme ve silme
- Publish isteği ile backend hata kodu/trace ID sunumu
- Klavye, odak, etiket ve renk dışı hata erişilebilirliği

### Kabul kriterleri

- USER rolü hem arayüz hem backend tarafından yönetim erişiminden çıkarılır.
- Liste public DTO'yu değil, yalnız admin sayfalama sözleşmesini kullanır.
- Yayınlanmış içerik arayüzde değiştirilemez gösterilir; backend kuralı otorite
  olmaya devam eder.
- SERIES sezon/bölüm yönetebilir, FILM sezon yönetimi sunmaz.
- Backend validation/publish hataları kararlı code ve trace ID ile gösterilir.
- Yerel test header'ı production authentication olarak sunulmaz.
- Frontend test/build ve tam backend verify başarılıdır.

### Test yaklaşımı

- Rol ayrımı ve API hata ayrıştırması için frontend unit testleri
- Admin liste/pagination/yetki için gerçek PostgreSQL API integration testi
- TypeScript strict derleme ve Vite production build
- Çalışan geliştirme sunucusunda DOM, form etiketi, hata katmanı ve konsol
  kontrolü
- Bütün backend modülleri için Maven verify regresyon testi

### Öğrenme çıktısı

Kullanıcı istemci rota korumasının authorization olmadığını, backend'in neden
güvenlik otoritesi kaldığını, liste DTO'su ile aggregate detayının neden ayrı
olduğunu ve local proxy ile production CORS kararının farkını açıklayabilir.

Kalıcı karar ve alternatifler ADR-0017'de kayıtlıdır. Medya yükleme, quiz
authoring, XP ve leaderboard arayüzleri ayrı ürün dilimleri olarak kalır.

## Aşama 10B - Kullanıcı web kataloğu

Durum: Tamamlandı (08.08.2026); 6 frontend testi ve production build başarılı

Bu aşama, kullanıcıların yalnız yayınlanmış içerikleri keşfedebildiği read-only
web girişini ekler. Yönetim deneyiminden ayrı tutulur; production login
uygulamaz, ancak backend'in sunucu otoriteli gameplay akışını kullanır.

### İşler

- Aynı Vite çalışma zamanında kullanıcı için ayrı HTML giriş noktası
- Yayınlanmış içerik kataloğu ve sayfalama
- İçerik detayı, dizi sezon/bölüm görünümü ve yayınlanmış quiz özeti
- Local USER actor ile public API sözleşmesi kullanımı
- Quiz başlangıcı, standart/uzatılmış süre seçimi, cevap ve sonuç geri bildirimi
- XP özeti, profil ilerlemesi ve global leaderboard
- Mobilde tek sütuna inen katalog, detay, quiz ve bölüm düzeni
- Erişilebilir odak, anlamlı görsel alt metni ve renk dışı bilgi sunumu

### Kabul kriterleri

- Kullanıcı ekranı admin endpoint'lerini veya draft içeriklerini kullanmaz.
- İçerik ve ilgili quiz özeti yalnız public API kaynaklarından okunur.
- İstemci doğru cevabı, skoru veya deadline'ı üretmez; bunları yalnız answer ve
  attempt response'undan gösterir.
- Tekrar denenen cevap isteği aynı `Idempotency-Key` ile backend'e gider.
- Local aktör bulunmadığında production login izlenimi vermeyen açıklayıcı
  bir durum gösterilir.
- TypeScript strict derleme, production build ve frontend testleri geçer.

### Test yaklaşımı

- Public katalog isteğinin doğru yol/USER header ile, answer isteğinin ise
  caller-provided `Idempotency-Key` ile çıktığını doğrulayan unit testler
- Mevcut local rol ve API hata ayrıştırma testleri için regresyon
- TypeScript strict kontrolü ve iki giriş noktalı Vite production build

### Öğrenme çıktısı

Kullanıcı, aynı API-first backend üzerinde yönetim ve kullanıcı deneyimlerinin
neden farklı giriş noktaları olabileceğini; istemcinin yalnız yayınlanmış
kaynakları okuyarak taslak veya doğru cevap sınırını neden koruduğunu açıklar.

Kalıcı karar ve alternatifler ADR-0019'da kayıtlıdır.

## Aşama 10C - Admin medya ve kapak bağlama

Durum: Tamamlandı (10.08.2026); frontend unit testleri ve production build ile doğrulandı

Bu aşama yeni bir backend endpoint'i eklemeden, editörün mevcut medya ve içerik
sözleşmeleriyle yayın için gereken kapağı hazırlamasını sağlar.

### İşler

- JPEG/PNG görseli `multipart/form-data` ile yönetim medya endpoint'ine yükleme
- Başarılı yüklemenin medya kimliğini alternatif metinle içerik kapağına bağlama
- Mevcut kapağın alternatif metnini güncelleme ve taslak dışındaki kaydı kilitleme
- Dosya sınırlarını açıklama; son kararın backend'in imza/decode kontrolünde
  kaldığını ve hata code/trace ID'sinin korunmasını sağlama

### Kabul kriterleri

- Tarayıcı multipart sınırını bozacak elle `Content-Type` göndermeden dosyayı
  doğru endpoint'e iletir.
- EDITOR/ADMIN actor header'ı local profil için korunur; gerçek yetki backend'de
  doğrulanır.
- Kapak yalnız taslak içerikte bağlanabilir, alternatif metin boş bırakılamaz.
- Yeni kapak yüklemek yeni bir medya kimliği üretir; içerik yalnız bu kimliği
  referans alır.
- Frontend testleri ve TypeScript production build geçer.

### Öğrenme çıktısı

Kullanıcı, multipart isteğinin JSON isteğinden farklı olarak neden tarayıcının
üreteceği bir sınır bilgisine ihtiyaç duyduğunu; medya yükleme ile içerik kapağı
bağlamanın neden iki ayrı, doğrulanabilir iş adımı olduğunu açıklayabilir.

Kalıcı karar ve alternatifler ADR-0020'de kayıtlıdır. Bu aşamadan sonra seçilen
quiz yazarlığı 10D'de tamamlanmıştır.

## Aşama 10D - Admin quiz yazarlığı ve medya yeniden kullanımı

Durum: Tamamlandı (10.08.2026); güncel tam pakette 105 backend testi, 16 frontend testi, production
build ve çalışan sayfa denetimiyle doğrulandı

Bakım notu (11.08.2026): Sayfalı medya endpoint'i ve değişmez medya modeli
korunurken gömülü görsel kütüphanesi admin arayüzünden kaldırıldı. Kapak ve soru
görselleri ilgili formdan doğrudan yüklenir.

Bu aşama editörün içerik detayından quiz taslağını uçtan uca yazabilmesini ve
daha önce yüklenmiş görselleri kapak veya soru için yeniden kullanabilmesini
sağlar. Yayınlama/arşivleme kontrolü sonraki bağımsız dilim olarak kalır.

### İşler

- Quiz için `CONTENT`, `SEASON` ve `EPISODE` kapsam modeli
- Sezon/bölüm aidiyetinin application doğrulaması ve PostgreSQL constraint'leri
- İçeriğe ait quizlerin hafif admin özeti
- Taslak sürüm, soru, seçenek, doğru cevap ve erişilebilir görsel yönetimi
- Sayfalı medya kütüphanesi ve korumalı görsel önizlemesi
- Public quiz özetinde doğru cevap sızdırmadan kapsam etiketi

### Kabul kriterleri

- Film quizi içerik geneli; dizi quizi içerik, sezon veya bölüm kapsamında
  oluşturulabilir.
- Başka içeriğe ait sezon/bölüm quiz kapsamı yapılamaz; uygulama ve veri tabanı
  aynı bütünlük kuralını korur.
- Bir medya kimliği yeni dosya yüklenmeden birden fazla kapak/soruya bağlanabilir.
- Kullanıcı DTO'su doğru cevap alanı içermez; doğru cevap yalnız admin yazarlık
  sözleşmesinde görünür.
- Yayındaki sürüm yerinde değişmez; yeni düzenleme yeni draft sürümü açar.
- Backend verify, frontend test/build ve çalışan admin/user sayfaları geçer.

### Test yaklaşımı

- Quiz scope invariant'ları için domain unit testleri
- Kapsam oluşturma/listeleme, çapraz içerik reddi ve medya listesi için gerçek
  PostgreSQL integration testleri
- Structured scope payload'ı, doğru cevap admin sözleşmesi ve medya sayfalaması
  için frontend API unit testleri
- TypeScript strict production build ve çalışan tarayıcıda form/konsol kontrolü

### Öğrenme çıktısı

Kullanıcı, quiz kapsamının neden serbest metin değil doğrulanan bir ilişki
olduğunu; application doğrulaması ile foreign key/check constraint'inin farklı
hata katmanlarını nasıl koruduğunu ve değişmez medya kimliğinin dosyayı
kopyalamadan yeniden kullanımı nasıl sağladığını açıklayabilir.

Kalıcı karar ve alternatifler ADR-0021'de kayıtlıdır. Bu temel 10E'de yayın
kontrolü ve kesin ürün kurallarıyla tamamlanmıştır.

## Aşama 10E - Quiz yayın kontrolü, dört seçenek, soru sayacı ve PDF

Durum: Tamamlandı (10.08.2026); backend verify, frontend test/build ve yerel katalog
akışıyla doğrulandı

Bu aşama quiz yazarlığını yayınlanabilir bir operasyon akışına dönüştürür. Editör
aynı ekranda sorular arasında yatay geçer, aşağıdaki sıralı önizlemeden istediği
soruyu düzeltir, taslağı silebilir veya yayınlar. Yayındaki sürüm yerinde
değiştirilmez; düzenleme yeni draft sürümü açar.

### İşler

- Her soruda tam dört seçenek ve otomatik normal zorluk
- Toplam süre olmadan, sunucu otoriteli soru başına 30 saniyelik deadline
- Erken cevapta anında sonraki soruya geçiş ve süre dolunca idempotent timeout
- Yatay soru gezgini, sıralı alt önizleme ve seçilen soruyu yerinde güncelleme
- Taslak silme; yayınlanmış sürümü görüntüleme ve yeni draft sürümüyle düzenleme
- İçerik/quiz bilgileri ve işaretli cevap anahtarıyla gerçek PDF indirme
- Onaylı TRT tabii örnek kataloğunu kapaklarıyla yerel ortama yükleyen idempotent script

### Kabul kriterleri

- Dört dışında seçenek sayısı API ve domain'de reddedilir; PostgreSQL transaction
  sonunda aynı bütünlüğü korur.
- Her yeni soruda 30 saniye başlar; kullanıcı daha erken cevap verirse beklemez.
- Timeout ile cevap yarışı tek kalıcı sonuç üretir ve doğru cevap soru açılmadan
  istemciye sızmaz.
- PDF `.pdf` dosyası olarak iner; içerik adı/türü/açıklaması, quiz numarası/sürümü,
  kapsamı ve işaretli doğru cevapları içerir.
- Yayınlanmış quiz yerinde değiştirilmez; düzenleme yeni taslak sürümünde yapılır.

### Test yaklaşımı

- Dört seçenek, deadline ilerlemesi ve timeout için domain unit testleri
- Deferred constraint, API doğrulaması, answer-timeout yarışı ve nullable timeout
  cevabı için gerçek PostgreSQL integration testleri
- Admin API, PDF belge modeli, kullanıcı timer/timeout istemcisi için frontend testleri
- TypeScript strict production build ve Maven `verify`

### Öğrenme çıktısı

Kullanıcı, ekrandaki sayacın neden tek başına güvenlik sınırı olmadığını; sunucu
deadline'ı, pessimistic lock, idempotency ve veritabanı constraint'inin aynı akışın
farklı risklerini nasıl koruduğunu açıklayabilir. Ayrıca yayınlanmış sürümü yerinde
değiştirmek yerine yeni draft açmanın geçmiş attempt'lerin anlamını nasıl koruduğunu
takip edebilir.

Kalıcı karar ve alternatifler ADR-0022'de kayıtlıdır.

## Aşama 10F - İçerik yönetimi ve admin yazarlık kullanılabilirliği

Durum: Tamamlandı (10.08.2026); 106 backend testi, 16 frontend testi, production
build ve çalışan admin sayfası denetimiyle doğrulandı

Bu aşama dizi/film bakımını quiz yazarlığından ayırır ve yayımdaki içeriklerin
kapak ile metadata düzeltmelerini güvenli hale getirir.

### İşler

- Ayrı içerik yönetimi sekmesinde dizi/film ekleme, düzenleme ve kalıcı silme
- Katalog kartı ile detay sayfasında gerçek kapak; kapak yoksa harf fallback'i
- Yayımlanmış içerikte başlık, açıklama, kapak ve alternatif metin güncelleme
- Düz tür/yayın durumu etiketleri ve sade içerik/quiz sekmeleri
- Açıklayıcı erişilebilir metin placeholder'ları ve görünür soru formu doğrulaması
- On sorudan az quizde sayfa kapatma/ayrılma uyarısı (Aşama 10I'de değişken
  soru sayısı kararıyla kaldırıldı)

### Kabul kriterleri

- Dizi ve film yönetimi ayrı admin sekmesinden yapılır; belirsiz `Yeni taslak`
  navigasyonu kaldırılır.
- Kart ve detay görünümü, yetkili medya isteğiyle kapak görselini gösterir.
- Silme fiziksel silmedir; gameplay geçmişi varsa attempt ve XP kaydı korunarak
  kararlı `409` conflict üretilir.
- Eksik soru alanlarında buton sessizce etkisiz kalmaz; tarayıcı veya form açık
  doğrulama mesajı gösterir.
- Bu aşamada on sorunun altındaki quizden ayrılma uyarısı uygulanmıştı; Aşama
  10I'de yalnız kaydedilmemiş değişiklik uyarısına dönüştürüldü.

### Test yaklaşımı

- Yayımdaki metadata/kapak değişikliği için domain unit testi
- Yayımdaki içeriği güncelleme ve fiziksel silme için PostgreSQL integration testi
- Boş admin quiz listesinin `200 []` dönmesi için API integration testi
- Frontend API/PDF testleri, TypeScript production build ve çalışan tarayıcı
  DOM/konsol kontrolü

Kalıcı karar ve silme trade-off'u ADR-0023'te kayıtlıdır.

## Aşama 10G - Quizler, quiz geçmişi ve sade kaldırma akışı

Durum: Tamamlandı (10.08.2026); 110/110 backend testi, 18/18 frontend testi,
frontend production build ve çalışan admin/kullanıcı sayfalarıyla doğrulandı

### İşler

- Admin ana menüsünde ayrı `Quizler` ve `Quiz geçmişi` sayfaları
- Hazırlanan ve aktif quizleri tek listede görme, ilgili içerik editörüne geçiş
- Kaldırılan quizler ve eski yayın sürümleri için cevap anahtarlı geçmiş listesi
- Hiç yayınlanmamış quizde kalıcı silme; yayınlanmış quizde geçmişe kaldırma
- Quiz kaldırıldığında veya sorular değiştiğinde kazanılmış XP'yi koruma
- Kullanıcı dizi detayından demo sezon/bölüm rafını kaldırma

### Kabul kriterleri

- Editör arayüzünde `Taslak` veya teknik `yeni sürüm oluştur` dili görünmez.
- Aktif quiz geçmişe kaldırılınca public quiz listesinden çıkar, admin geçmişinde
  eski sorularıyla görünür ve cevap anahtarı indirilebilir.
- Yayın geçmişi olan quiz için fiziksel silme reddedilir; hiç yayınlanmamış quiz
  fiziksel olarak silinir.
- Quiz işlemleri append-only XP ledger toplamını değiştirmez.
- Kullanıcı dizi detayında yalnız ilgili quizleri görür; “Nerede kalmıştın?” demo
  bölümü görünmez.

Kalıcı kararlar ADR-0024'te kayıtlıdır.

## Aşama 10H - Toplu sezon ve bölüm planlama

Durum: Tamamlandı (10.08.2026); 112/112 backend testi, 19/19 frontend testi,
frontend production build ve çalışan admin formuyla doğrulandı

### İşler

- Sezon sayısı ile her sezonun bölüm sayısını aynı admin formunda toplama
- Bütün hiyerarşiyi tek backend transaction'ında oluşturma
- Sezon ve bölüm numarası ile varsayılan başlıkları otomatik üretme
- Uzun bölüm listelerini yalnız düzenleme istendiğinde açma
- Tekil sezon/bölüm güncelleme ve silme yeteneğini bakım amacıyla koruma

### Kabul kriterleri

- `1. sezon / 29 bölüm` ve `2. sezon / 30 bölüm` tek istekle oluşturulur.
- İkinci sezon geçersizse ilk sezon da kalıcılaşmaz.
- Film içeriğinde sezon planı gösterilmez; yayınlanmış dizinin hiyerarşisi değişmez.
- Otomatik oluşan bölüm adı ve açıklaması sonradan düzenlenebilir.
- Eski tekil endpoint'ler API geriye uyumluluğu için korunur; ana admin akışında
  tek tek ekleme formu gösterilmez.

Kalıcı karar ve transaction trade-off'u ADR-0025'te kayıtlıdır.

## Aşama 10I - Görselli quiz PDF'i ve açık yayın görünürlüğü

Durum: Tamamlandı (10.08.2026); 19/19 frontend testi, production build, çalışan
admin API verisi ve gerçek admin sayfasıyla doğrulandı

### İşler

- PDF'de soruya özel görseli, yoksa içerik kapağını kullanma
- Her soruyu 30 saniye, ilerleme bilgisi, soru ve dört seçenekle quiz ekranına
  yakın ayrı bir PDF sayfasında sunma
- Doğru cevabı yeşil cevap kartı ve işaretle gösterme
- İçerik ile quizin ayrı yayın adımlarını admin ekranında görünür kılma
- İçerik yayında değilken quiz yayın düğmesini açıklamalı biçimde kapatma
- Sabit on soru beklentisini kaldırıp yalnız kaydedilmemiş değişiklikte çıkış uyarma

### Kabul kriterleri

- Soruya özel görsel PDF'e girer; yoksa kapak aynı medya kimliği üzerinden tekrar
  kullanılır.
- Bir görsel okunamazsa metin ve cevap anahtarıyla PDF üretimi devam eder.
- Taslak içerik ve taslak quiz kullanıcı kataloğunda görünmez; admin iki yayın
  adımını ve mevcut durumu açıkça gösterir.
- 11 veya daha fazla sorulu quiz oluşturulabilir; her soru dört seçenekli ve 30
  saniyelik kalır.

Kalıcı ürün ve PDF kararları ADR-0022'de güncellenmiştir.

## Aşama 10J - Quiz başına ilk tamamlama XP'si

Durum: Tamamlandı (11.08.2026); temiz Java 21 derlemesi, 26/26 hedefli test ve
114/114 tam backend test paketiyle doğrulandı

### İşler

- Aynı kullanıcı ve quiz için veritabanında tek ilk-tamamlama ödül hakkı
- İlk sonuçta skor kadar XP, sonraki alıştırma sonuçlarında `earnedXp = 0`
- API, Outbox olayı ve asenkron XP consumer'ında aynı ödül kararının korunması
- Geçmiş XP'yi silmeden eski tamamlamaların migration sırasında tanınması

### Kabul kriterleri

- Aynı quiz iki farklı attempt ile tamamlansa bile yalnız bir XP transaction'ı oluşur.
- İkinci attempt skoru korur ancak sonuç cevabı `earnedXp: 0` döndürür.
- `(user_id, quiz_id)` tekilliği eşzamanlı iki ödülü veritabanında engeller.
- Eski `SCORE_MATCH_V1` olayları okunabilir; yeni olay ve ledger kaydı
  `FIRST_COMPLETION_SCORE_V2` kullanır.

Kalıcı karar ve alternatifler ADR-0026'da kayıtlıdır.

## Aşama 10K - Okunabilir ve ekrana sığan quiz çözme yüzeyi

Durum: Tamamlandı (11.08.2026); 20/20 frontend testi, TypeScript/Vite production
build ve 1440×900 ile 390×844 gerçek tarayıcı doğrulamasıyla tamamlandı

### İşler

- Koyu katalog kimliği içinde açık ve nötr bir quiz çalışma yüzeyi
- Görsel/soru arkasındaki ayrı renkli kart ve çerçevenin kaldırılması
- Soru başlığının kontrollü, okunabilir ölçüye indirilmesi
- Masaüstünde 2×2 cevap seçenekleri; küçük ekranda tek sütun fallback
- Görsel bulunan soruda masaüstü yan yana, mobilde alt alta yerleşim
- Cevap seçeneklerinde A–D işaretleri ve daha belirgin hover/focus yüzeyi
- Doğru cevapta açık yeşil/koyu yeşil, yanlış veya süre aşımında açık gül/koyu
  bordo sonuç yüzeyi
- Tekrar çözüm sonucunda `+0 XP kazandın` yerine alıştırma açıklaması

### Kabul kriterleri

- 1440×900 görünümde soru ve dört cevap seçeneği sayfa kaydırmadan görünür.
- 390×844 görünümde yatay taşma olmaz; dört seçenek okunabilir tek sütuna iner.
- Soru metni 5.8rem'e kadar büyümez; geniş ekranda yaklaşık 38px ile sınırlanır.
- Soru/görsel için ayrı yeşilimsi kart görünmez; tüm oyun alanı tek nötr yüzeydir.
- `100 puan` ve `0 puan` sonuçları açık zemin üzerinde yüksek kontrastlı koyu
  metinle okunur.
- `earnedXp = 0`, kullanıcıya yeni XP kazanımı gibi sunulmaz.
- Tarayıcı konsolunda hata/uyarı bulunmaz.

## Aşama 10L - Admin içerik isminde sunucu taraflı arama

Durum: Tamamlandı (11.08.2026); 115/115 backend testi, 21/21 frontend testi,
production build ve gerçek admin arama alanı doğrulamasıyla tamamlandı

### İşler

- Admin içerik listeleme API'sinde isteğe bağlı, en fazla 200 karakterlik `query`
- Başlığın herhangi bir bölümünde büyük/küçük harf duyarsız PostgreSQL araması
- Arama metninin application katmanında baş/son boşluklardan temizlenmesi
- Filtrelenmiş sonuçlarda mevcut `page`/`size` ve kararlı sıralamanın korunması
- İçerik yönetiminde etiketli arama alanı, 300 ms debounce ve temizleme düğmesi
- Eşleşme sayısı ile aramaya özel boş durum açıklaması

### Kabul kriterleri

- Arama yalnız ekrandaki 20 kaydı değil, bütün admin kataloğunu sorgular.
- Arama sonucu `totalItems` ve `totalPages` değerleri filtrelenmiş kümeyi anlatır.
- Büyük/küçük harf farkı sonucu değiştirmez; baş/son boşluklar yok sayılır.
- Boş arama önceki sayfalı admin listeleme davranışını korur.
- Kullanıcı yazarken her tuş için istek atılmaz; sorgu değişince ilk sayfaya dönülür.
- Arama yalnız admin içerik yönetiminde görünür; public katalog sözleşmesi değişmez.

## Aşama 10M - On puanlık skor ve doğru cevap özeti

Durum: Tamamlandı (11.08.2026); 27/27 hedefli backend ve 22/22 frontend testi,
production build ve gerçek sonuç ekranıyla tamamlandı

### İşler

- Mevcut `STANDARD_V1` ile her doğru cevaba 10 puan
- Sonuç ekranında puanın yanında doğru cevap sayısı / gerçek toplam soru sayısı
- İlk tamamlama XP ve tekrar çözüm alıştırma davranışının yeni skorla korunması

### Kabul kriterleri

- Sekiz doğru içeren on soruluk tamamlanmış attempt `80 puan` ve `8 / 10 doğru`
  gösterir.
- Yanlış ve zaman aşımı sıfır puandır; istemci skor hesaplamaz.
- Yeni attempt veritabanında `STANDARD_V1` snapshot'ı taşır.
- İlk tamamlamada XP kesin skora eşittir; tekrar çözüm ek XP üretmez.
- Soru sayısı sabit 10 varsayılmaz.

## Aşama 10N - Yayımlanmış diziye sona ekleme

Durum: Tamamlandı (11.08.2026); 18/18 hedefli backend ve 24/24 frontend testi ile
production build başarılı

### İşler

- Yayımlanmış dizinin sonuna yeni sezon planı ekleme
- Mevcut sezonun sonuna yeni bölüm ekleme
- Mevcut sezon/bölüm güncelleme ve silme yasağını koruma
- Admin ekranında yayımlanmış hiyerarşiyi salt okunur gösterip ekleme araçlarını açma

### Kabul kriterleri

- Yeni sezon ve bölüm numarası mevcut en büyük numaradan büyük olmalıdır.
- Eski sezon/bölüm kimlikleri değişmez; kapsamlı quiz ilişkileri korunur.
- Güncelleme ve silme girişimleri `PUBLISHED_CONTENT_IMMUTABLE` ile reddedilir.
- Yeni kayıtlar public katalogda görünür ve mevcut transaction/audit akışını kullanır.
- Yeni migration veya ek teknoloji gerekmez.

Kalıcı karar ve alternatif ADR-0027'de kayıtlıdır.

## Aşama 10O - Kullanıcı quiz keşif sayfası

Durum: Tamamlandı (11.08.2026); 14/14 hedefli backend ve 26/26 frontend testi ile
production build başarılı

### İşler

- Kullanıcı ana menüsüne ayrı `Quizler` sekmesi
- Quiz adı, kapsamı ve soru sayısını gösteren keşif kartları
- Karttan doğrudan quiz başlangıç ekranına geçiş
- Bütün yayımlanmış quizleri tek istekte döndüren güvenli özet API

### Kabul kriterleri

- Kullanıcı içerik detaylarını tek tek açmadan bütün aktif quiz adlarını görebilir.
- Liste yalnız yayımlanmış quizleri döndürür.
- Özet response soru, seçenek ve doğru cevap bilgisi taşımaz.
- Sayfa bir içerik başına ayrı istek üretmez.
- Quizden çıkıldığında kullanıcı quiz keşif sayfasına dönebilir.

Keşif akışı ve N+1 istek alternatifi ADR-0019'da kayıtlıdır.

## Aşama 10P - Kullanıcı dizi detayında sezon ve bölümler

Durum: Tamamlandı (11.08.2026); 28/28 frontend testi ve production build başarılı

### İşler

- Dizi detayında quizlerden ayrı `Sezonlar ve Bölümler` alanı
- Çok bölümlü diziler için açılır sezon kartları
- Sezon/bölüm verisi olmayan eski diziler için güvenli boş durum
- İçerik detayındaki quizlerde gerçek sezon ve bölüm numarası etiketi

### Kabul kriterleri

- Film detayında sezon alanı gösterilmez.
- Dizi sezonu yoksa ekran hata vermez ve açıklayıcı mesaj gösterir.
- Sezon başlığı bölüm sayısını, sayfa başlığı toplam sezon/bölüm sayısını gösterir.
- Quizler sezon listesinin içine karışmaz; ayrı bölümde kalır.
- Bölüm kapsamlı quiz mevcut hiyerarşide `2. Sezon · 5. Bölüm` gibi gösterilir.

Yerleşim kararı ADR-0019'da kayıtlıdır.

## Aşama 10Q - Quiz kartında içerik adı

Durum: Tamamlandı (11.08.2026); 30/30 frontend testi ve production build başarılı

### İşler ve kabul kriterleri

- Genel Quizler sayfasındaki her kart bağlı dizi veya film adını gösterir.
- Quiz ile içerik `contentId` üzerinden eşleştirilir; teknik kimlik kullanıcıya yazılmaz.
- Eşleşmeyen eski kayıtta güvenli `İçerik` etiketi kullanılır.
- Public katalog 100 kayıtlık sayfalarla tamamen okunur; içerik başına istek atılmaz.
- Backend ve veritabanı sözleşmesi değişmez.

İstemci taraflı eşleştirme kararı ADR-0019'da kayıtlıdır.

## Aşama 10R - Quiz keşif araması ve kapsam filtresi

Durum: Tamamlandı (11.08.2026); 32/32 frontend testi ve production build başarılı

### İşler ve kabul kriterleri

- Kullanıcı quiz veya bağlı dizi/film adına göre mevcut quiz listesini arayabilir.
- Arama quiz açıklamasını da kapsar ve Türkçe büyük/küçük harf dönüşümünü kullanır.
- Kullanıcı listeyi içerik geneli, sezon veya bölüm kapsamına göre daraltabilir.
- Eşleşme sayısı ve sonuç bulunamayan aramalar için anlaşılır boş durum gösterilir.
- Filtreleme yüklenmiş güvenli özetler üzerinde istemcide yapılır; yeni backend isteği,
  endpoint veya veritabanı değişikliği oluşturmaz.

İstemci taraflı filtreleme kararı ADR-0019'da kayıtlıdır.

## Aşama 10S - Quiz keşif sıralaması

Durum: Tamamlandı (11.08.2026); 34/34 frontend testi ve production build başarılı

### İşler ve kabul kriterleri

- Kullanıcı quizleri bağlı içerik adına veya quiz adına göre Türkçe alfabetik
  sıralayabilir.
- Soru sayısı sıralaması çoktan aza çalışır; eşitlikte quiz adı belirleyicidir.
- Varsayılan sıralama aynı içeriğe bağlı quizleri bir arada tutar.
- Sıralama yüklenmiş listenin kopyasında yapılır; API'den gelen dizi değiştirilmez.
- Tarih alanı bulunmadığı için yanıltıcı bir `En yeni` seçeneği gösterilmez.
- Kontroller tablet görünümünde iki, mobil görünümde tek sütuna düşer.

İstemci taraflı sıralama kararı ADR-0019'da kayıtlıdır.

## Aşama 10T - Kullanıcı içerik detayını quiz odaklı sadeleştirme

Durum: Tamamlandı (11.08.2026); 33/33 frontend testi ve production build başarılı

### İşler ve kabul kriterleri

- Kullanıcı dizi detayındaki bağımsız **Sezonlar ve Bölümler** rehberi kaldırılır.
- İçerik detayında kapak, açıklama ve ilgili quizler korunur.
- Sezon/bölüm verisi backend'de ve admin yönetiminde değişmeden kalır.
- Sezon ve bölüm kapsamlı quiz kartları gerçek kapsam etiketini göstermeye devam eder.
- Artık render edilmeyen bileşen, yardımcı metot, test ve CSS kuralları temizlenir.
- Backend, API ve veritabanı değişmez.

Quiz odaklı kullanıcı detayı kararı ADR-0019'da kayıtlıdır; Aşama 10P'deki bağımsız
hiyerarşi görünümü bu aşamayla geri alınmıştır.

## Aşama 10U - Yerel kota ve admin görsel istek verimliliği

Durum: Tamamlandı (11.08.2026); 35/35 frontend ve 5/5 hedefli backend testi ile
production build başarılı

### İşler ve kabul kriterleri

- `local` Spring profili HTTP rate limit filtresini kapatır.
- Normal ve production profillerinde rate limit varsayılan olarak açık kalır.
- Aynı korumalı görsel, tek admin oturumu içinde yalnız bir kez indirilir.
- Aynı görsele eşzamanlı istekler ortak devam eden blob isteğini kullanır.
- Bileşenler kendi object URL'lerini üretip kaldırabilir; paylaşılan blob geçerli kalır.
- Başarısız görsel isteği önbellekten çıkarılır ve sonraki açılışta yeniden denenir.

Profil ayrımı ADR-0016, medya önbelleği ADR-0020'de kayıtlıdır.

## Aşama 10V - Kullanıcı keşif sadeleştirmesi

Durum: Tamamlandı (11.08.2026); 38/38 frontend testi ve production build başarılı

### İşler ve kabul kriterleri

- **Keşfet** sayfası dizi, film ve belgesellerde tek metin alanıyla arama yapar;
  tür veya durum filtresi göstermez.
- Açılan içerikler aynı tarayıcıda son tıklanandan eskiye sıralanır; hiç açılmayan
  içeriklerin katalog sırası korunur.
- **Quizler** sayfasındaki kapsam filtresi kaldırılır; metin araması ve sıralama
  seçenekleri korunur.
- Quiz kartı bağlı içeriğin ana görselini gösterir ve kartın üst vurgu çizgisini
  kullanmaz.
- Katalog ve quiz görsellerinde aynı medya URL'si oturum içinde tek binary istek
  paylaşır; backend, veritabanı veya yeni endpoint eklenmez.

Keşif davranışı ADR-0019, medya isteği paylaşımı ADR-0020'de kayıtlıdır.

## Aşama 10W - Yerel demo kullanıcı ve gameplay verisi

Durum: Tamamlandı (11.08.2026); script sözdizimi, gerçek API akışı ve tekrar
çalıştırma doğrulandı

### İşler ve kabul kriterleri

- Sekiz sabit yerel UUID ve farklı hedef doğrulukları okunabilir bir manifestte
  tutulur; bunlar production profili değildir.
- Script yayımlanmış quizleri gerçek attempt/answer API'leri üzerinden çözer;
  gameplay, ilk-tamamlama, Outbox ve XP kurallarını atlamaz.
- En fazla ilk üç quiz kullanılarak veri miktarı sınırlı tutulur.
- Daha önce XP üretilmiş demo kullanıcı varsayılan çalıştırmada atlanır; tekrar
  çalışma yeni leaderboard kazancı üretmez.
- XP consumer'ı kısa süre kontrollü beklenir ve Redis leaderboard projeksiyonu
  veri üretimi sonunda yenilenir.

API üzerinden test verisi kararı ADR-0028'de kayıtlıdır.

## Aşama 10X - Sıralamayı profil içinde birleştirme

Durum: Tamamlandı (11.08.2026); 39/39 frontend testi ve production build başarılı

### İşler ve kabul kriterleri

- Kullanıcı menüsündeki ayrı **Sıralama** sekmesi kaldırılır.
- Profil; XP özeti, kullanıcının global sırası ve genel Top 10 listesini aynı
  ekranda gösterir.
- XP ve leaderboard mevcut iki istekte paralel okunur; yeni endpoint veya
  veritabanı değişikliği eklenmez.
- Eski `?view=leaderboard` bağlantıları hata vermek yerine profile yönlenir.

Profil ve sıralama sunumu ADR-0019'da kayıtlıdır.

## Aşama 10Y - Admin katalog tekrarını kaldırma

Durum: Tamamlandı (11.08.2026); 39/39 frontend testi ve production build başarılı

### İşler ve kabul kriterleri

- Admin menüsündeki ayrı **Katalog** sekmesi kaldırılır.
- `/` adresi ve stüdyo logosu **İçerik yönetimi** ekranını açar.
- Salt-okunur admin katalog varyantı temizlenir; oluşturma, arama ve düzenleme tek
  içerik yönetimi ekranında kalır.
- Kullanıcıların yayın kataloğu ayrı `user.html` girişinde değişmeden korunur.
- Backend, API ve veritabanı değişmez.

Admin içerik yönetimi kararı ADR-0023'te kayıtlıdır.

## Aşama 10Z - Sıfır puanlı ilk tamamlamayı sıralamaya katma

Durum: Tamamlandı (11.08.2026); 120/120 tam backend paketi, V14 migration ve
22/22 son hedefli test başarılı

### İşler ve kabul kriterleri

- İlk tamamlamasında 0 puan alan kullanıcı için tek, sıfır tutarlı XP ledger kaydı
  oluşur ve kullanıcı leaderboard katılımcısı sayılır.
- Aynı quizin sonraki alıştırma tamamlamaları `earnedXp = 0` döndürmeye devam eder
  fakat ikinci ledger kaydı oluşturmaz.
- `quiz.completed` v2 olayı ilk-tamamlama ödül hakkını skordan ayrı ve açık bir
  boolean olarak taşır; v1 olayları geriye uyumlu okunur.
- V14 migration daha önce ilk tamamlamasında 0 puan almış ve ledger kaydı oluşmamış
  kullanıcıları idempotent biçimde tamamlar.
- Attempt, reward claim, Outbox ve XP aynı transaction/idempotency sınırlarını korur.

Karar ADR-0012, ADR-0013 ve ADR-0026'da kayıtlıdır.

## Aşama 10AA - Yerel kullanıcı hesabı ve oturum

Durum: Tamamlandı (11.08.2026); 123/123 backend ve 39/39 frontend testi ile
production frontend build başarılı

### İşler ve kabul kriterleri

- Kullanıcı e-posta, görünen ad ve en az sekiz karakterli şifreyle hesap açar;
  kendi kendine yalnız `USER` rolü alabilir.
- Şifre tek yönlü bcrypt özeti olarak saklanır; normalize e-posta veritabanında
  benzersizdir.
- Kayıt ve giriş, session fixation korumalı HttpOnly sunucu oturumu başlatır;
  çıkış oturumu geçersiz kılar.
- Kullanıcı webi UUID istemez; mevcut oturumu kontrol eder ve giriş/hesap oluşturma
  ekranını sade biçimde sunar.
- Mevcut gameplay, XP ve leaderboard akışları değişmeden oturumdaki hesap UUID'sini
  kullanır.
- Geçici header kimliği local/test admin ve otomasyon akışlarında korunur;
  production OIDC/CSRF bu aşamaya dahil değildir.

Karar ADR-0029'da kayıtlıdır.

## Aşama 11 - Opsiyonel genişlemeler

Çekirdek sistem ve operasyon kalitesi tamamlanmadan başlanmaz:

- Rozetler
- Arkadaş meydan okuması
- Dil öğrenme modu
- TV ve telefon QR eşleşmesi
- WebSocket canlı oturum
- AI destekli, editör onaylı soru önerisi

Bu bölüm tek bir uygulama aşaması değildir. Her madde ayrı ürün kararı, ADR,
kabul kriteri ve test planı gerektiren bağımsız bir gelecek aşamasıdır.
