# Güncel Proje Durumu

Son güncelleme: 04.08.2026

## Genel durum

Projenin Aşama 0 proje temeli, Aşama 1 kimlik/erişim sınırı, Aşama 2 içerik
kataloğu, Aşama 3 quiz authoring/sürümleme, Aşama 4 gameplay, Aşama 4.1
erişilebilir medya, Aşama 5 PostgreSQL XP ledger ve Aşama 6 güvenilir
mesajlaşma, Aşama 7 PostgreSQL leaderboard, Aşama 8 Redis leaderboard read
model ve Aşama 9 production hazırlığı tamamlandı. OpenTelemetry W3C trace
zinciri, Prometheus/Grafana/Tempo, rate limiting, güvenlik/yük/kesinti testleri,
backup/restore provası ve operasyon politikaları hazırdır. `Content`
aggregate'ine ek olarak `Quiz` aggregate'i; draft/published/archived sürüm
yaşam döngüsü, soru/seçenek yönetimi, doğru cevap güvenliği, `STANDARD_V1`
politika kimliği, Flyway V3 constraint'leri, EDITOR/ADMIN yönetim API'leri,
güvenli kullanıcı DTO'su, kalıcı admin audit ve ArchUnit sınırlarıyla hazırdır.
`QuizAttempt` aggregate'i; sahiplik, standart/uzatılmış sunucu deadline'ı, sıralı
tek cevap, idempotency, semantik doğru şık geri bildirimi ve `STANDARD_V1`
sunucu puanlamasıyla hazırdır. Değişmez görsel kimliği, kapak fallback'i ve
eşdeğer erişilebilir soru metni sözleşmesi uygulanmıştır. `XpTransaction`
append-only ledger'ı; skorla birebir `SCORE_MATCH_V1`, attempt başına tek ödül,
kullanıcı özeti ve audit edilen yönetici düzeltmeleriyle hazırdır. Attempt ve
Outbox aynı transaction'da kesinleşir; RabbitMQ consumer'ı Inbox ile XP'yi
idempotent ve asenkron üretir. Henüz kalıcı kullanıcı tablosu veya production kimlik
sağlayıcısı yazılmadı.
Global ve içerik bazlı `ALL_TIME` leaderboard XP ledger toplamından,
deterministik tie-break ile hesaplanır; Top N yanında mevcut kullanıcının sırası
da okunabilir. Redis bu PostgreSQL sonucundan atomik nesiller halinde yeniden
kurulur; boşluk veya kesintide sorgu PostgreSQL'e düşer.

Hedef klasörde bulunan uzun mimari rapor teknik referans olarak korunmaktadır.
Bu dosya günlük geliştirme bağlamına doğrudan yapıştırılmamalıdır.

## Tamamlananlar

- Ürün fikri değerlendirildi.
- İlk kullanım senaryosu fan/bölüm quizi olarak seçildi.
- Proje, "İçerik Etkileşim Backend Platformu" olarak konumlandırıldı.
- MVP dışındaki özellikler ayrıldı.
- Modüler monolith başlangıç yaklaşımı seçildi.
- PostgreSQL, RabbitMQ ve Redis'in görevleri ayrıştırıldı.
- Başlangıç dokümanları oluşturuldu.
- Uzun DOCX mimari raporu ile kısa çalışma belgelerinin kararları karşılaştırıldı.
- MVP akışı `yayınla → başlat → cevapla → tamamla → XP ver → sırala` olarak
  sadeleştirildi.
- Her geliştirme aşamasına açık test yaklaşımı ve öğrenme çıktısı eklendi.
- Kullanıcıya sistemi öğretme biçimi `README.md` ve `AGENTS.md` içinde çalışma
  kuralına dönüştürüldü.
- Bilgisayardaki geliştirme araçları envanteri çıkarıldı.
- Git 2.51.0 ve VS Code'un kurulu olduğu doğrulandı.
- Eclipse Temurin JDK 21.0.11 LTS kullanıcıya özel olarak kuruldu.
- JDK ZIP paketinin SHA-256 değeri Adoptium'un yayımladığı checksum ile
  eşleştirildi.
- Kullanıcı seviyesinde `JAVA_HOME` ve `PATH` Java 21 için ayarlandı.
- Docker Desktop Windows x64 kurulum dosyası resmi Docker adresinden indirildi
  ve Docker Inc. dijital imzası doğrulandı.
- Yarım ve imzasız kalan Docker indirme dosyası silindi.
- WSL 2 kullanıcı tarafından etkinleştirildi; WSL 2.7.11 ve varsayılan sürüm 2
  olarak doğrulandı.
- Backend lead'e sunulabilecek sade proje anlatımı `docs/PROJECT_PITCH.md`
  dosyasına kaydedildi.
- Projede kullanılan ve projeden bağımsız backend teknolojilerini açıklayan,
  karşılaştıran ve uygulamalı sorular içeren
  `docs/bwl.md` çalışma defteri oluşturuldu.
- `docs/bwl.md` içindeki programlama dilleri bölümü; çalışma zamanı, tip ve nesne
  modeli, hata yönetimi, bellek yönetimi ve concurrency yaklaşımlarıyla
  derinleştirildi.
- Java'yı temel dilden backend düşüncesine kadar sistemli biçimde öğrenmek için
  OOP, encapsulation, inheritance/composition, collections, generics, JVM
  belleği, concurrency ve test konularını içeren `docs/java.md` çalışma kitabı
  oluşturuldu.
- Akademik proje ister belgesi düzeninde; kapsam, kullanıcı/yönetici akışları,
  modüler mimari, veri modeli, teknoloji gerekçeleri, bütün geliştirme
  aşamalarının kabul kriterleri ve test yaklaşımlarını içeren
  `docs/PROJECT_SPECIFICATION.md` hazırlandı.
- Aynı içerik, örnek Yazılım Laboratuvarı proje belgesinin sade ve numaralı
  teslim biçimine uygun olarak
  `output/pdf/TRT_Icerik_Etkilesim_Backend_Proje_Dokumani.pdf` dosyasına
  dönüştürüldü.
- Harici mimari eleştiri; modül iletişimi, concurrency, transaction, rich domain
  model, Flyway ve test stratejisi açısından mevcut planla karşılaştırıldı.
- Kimlik ve erişim temeli content/gameplay çalışmalarından önceye alındı.
- XP iş kuralı RabbitMQ'dan ayrıldı: önce PostgreSQL ledger, sonra ayrı
  mesajlaşma aşaması olarak planlandı.
- Leaderboard iş kuralları Redis'ten ayrıldı: önce PostgreSQL doğruluk ve
  performans ölçümü, sonra ihtiyaç varsa Redis read model olarak planlandı.
- Domain event, integration event ve Spring process-içi event davranışlarının
  farkı mimari belgeye eklendi.
- Attempt için sunucu `Clock` kaynağı, UTC deadline, expire, sahiplik ve politika
  sürümleme karar noktaları yol haritasına eklendi.
- ArchUnit'in boş iskelet yerine gerçek modül sınırları oluştuğunda küçük kural
  setiyle kullanılması planlandı.
- Büyük tablo migration yaklaşımı, kapasite modeli, backup/restore, KVKK ve veri
  saklama konuları ilgili ileri aşamalara eklendi.
- İş davranışını altyapıdan önce kanıtlama kararı
  `ADR-0002-davranis-once-altyapi-sonra.md` olarak kaydedildi.
- Açıklayıcı değişken, metot ve sınıf adları kullanılması; yeni aşamaya
  geçmeden önce yazılan kodun çalışma biçiminin ve testlerinin kullanıcıya
  anlatılması kalıcı çalışma kuralı hâline getirildi.
- Eksik veya taşınmış dosyaların izinsiz yeniden oluşturulmaması ve PDF
  sayfalarını görsele çevirme, OCR ya da görsel inceleme gibi işlemlerden önce
  kullanıcıdan açık izin alınması çalışma kurallarına eklendi.
- Git deposu `main` başlangıç dalıyla oluşturuldu.
- Java 21, Spring Boot 4.1.0, Maven Wrapper ve PostgreSQL 17 teknoloji temeli
  seçildi; gerekçeleri `ADR-0003-asama-0-teknoloji-temeli.md` içine kaydedildi.
- Minimal Spring Boot 4.1.0 uygulama iskeleti ve Maven bağımlılık yönetimi
  oluşturuldu.
- Yalnız PostgreSQL içeren `compose.yaml` eklendi; RabbitMQ ve Redis eklenmedi.
- Flyway ve iş tablosu oluşturmayan `V1__baseline.sql` migration'ı eklendi.
- Actuator health endpoint, kararlı API hata zarfı ve `X-Trace-Id` filtresi
  eklendi.
- Testcontainers ile gerçek PostgreSQL kullanan application, Flyway, health ve
  hata zarfı integration testleri yazıldı.
- GitHub Actions için Java 21 üzerinde `bash ./mvnw --batch-mode verify` çalıştıran
  başlangıç CI pipeline'ı eklendi.
- Windows'ta çalışan mevcut PostgreSQL `5432` portunu kullandığı için Docker
  PostgreSQL host portu `5433` olarak seçildi.
- Mevcut başka bir uygulama `8080` portunu kullandığı için backend varsayılan
  portu `8081` olarak seçildi.
- Kullanıcı düzeyindeki `JAVA_HOME` Java 21'e ayarlandı; Java 21 ve Docker CLI
  yolları kullanıcı `PATH` değerinin başına eklendi.
- README'ye doğrulanmış PostgreSQL başlatma, test, uygulama çalıştırma ve health
  kontrol komutları eklendi.
- VS Code için PlantUML eklentisi kuruldu.
- Çalışma zamanı bileşenlerini, backend modül/katmanlarını ve planlanan
  PostgreSQL veri modelini ayrı ve okunabilir sayfalarda gösteren
  `docs/diagrams/system-architecture.puml` oluşturuldu.
- PlantUML kaynağından üç adet ölçeklenebilir SVG diyagram
  `docs/diagrams/rendered/` altında üretildi. RabbitMQ ve Redis mevcut bileşen
  gibi değil, gelecek aşama olarak işaretlendi.
- Üç diyagram da geniş yatay akış yerine yukarıdan aşağı okunacak biçimde
  düzenlendi; backend diyagramındaki çapraz modül okları, bilgi kaybetmeden dikey
  modül kataloğu ve iletişim örnekleri olarak sadeleştirildi.
- Stajdan sorumlu mühendis RabbitMQ kullanımını proje gereksinimi olarak
  bildirdi. RabbitMQ'nun Aşama 6'da uygulanacağı ve çekirdek iş davranışından
  önce eklenmeyeceği `ADR-0004-rabbitmq-kurum-gereksinimi.md` ile kaydedildi.
- `docs/README.md`, temel Markdown belgeleri ile PlantUML/SVG diyagramlarının
  GitHub üzerindeki giriş dizini olarak oluşturuldu.
- GitHub CLI 2.94.0 kullanıcı hesabına kuruldu ve `Gardantua` hesabıyla giriş
  doğrulandı.
- Yerel Git deposu
  `https://github.com/Gardantua/trt_staj_content_management_app` adresine
  `origin` olarak bağlandı.
- Kaynak kodu, proje belgeleri ve `docs/diagrams/rendered/` altındaki üç SVG
  diyagram ilk commit ile public GitHub reposunun `main` dalına gönderildi.
- Spring Security bağımlılığı ve stateless API güvenlik zinciri eklendi.
- Health ve info endpoint'leri açık bırakılırken `/api/**` yolları kimlik
  doğrulaması gerektirecek biçimde kapatıldı.
- Identity domain dilinde `USER`, `EDITOR` ve `ADMIN` rolleri modellendi.
- `CurrentActor`, `CurrentActorProvider` ve `GetCurrentActorUseCase` ile
  application katmanının HTTP header'larından ve Spring Security ayrıntısından
  bağımsız aktör sözleşmesi oluşturuldu.
- Yalnız `local` ve `test` profillerinde çalışan geçici test-header
  authentication adapter'ı eklendi; varsayılan profil fail-closed kaldı.
- Kimlik ve yetki hataları `AUTHENTICATION_REQUIRED`,
  `AUTHENTICATION_INVALID` ve `ACCESS_DENIED` kodlarıyla mevcut trace ID'li hata
  zarfına bağlandı.
- `GET /api/v1/identity/me` endpoint'i doğrulanmış aktör kimliğini ve rollerini
  göstermek için eklendi.
- Yerel parola deposu oluşturulmadı ve Spring'in rastgele geliştirme parolasını
  loglaması engellendi.
- Windows Maven Wrapper'ın normal `.m2` klasöründe null junction hedefi nedeniyle
  çökmesine yol açan kontrol düzeltildi.
- Aşama 1'in geçici kimlik sınırı `ADR-0005-asama-1-gecici-kimlik.md` ile
  kaydedildi.
- Redis'in Aşama 8'de zorunlu öğrenme bileşeni olması
  `ADR-0006-redis-ogrenme-gereksinimi.md` ile kaydedildi; RabbitMQ'nun Aşama
  6'daki zorunluluğu ADR-0004 ile korunuyor.
- Aşama 2 `Content`, `Season` ve `Episode` saf domain modeli ile tamamlandı.
- İçerik hiyerarşisi `Content` aggregate root'u üzerinden değiştiriliyor.
- Flyway `V2__content_catalog.sql`; içerik, sezon, bölüm ve admin audit tabloları
  ile unique, foreign key, check constraint ve listeleme index'lerini ekledi.
- EDITOR/ADMIN için içerik, sezon ve bölüm CRUD API'leri ile publish use case'i
  eklendi; normal USER yönetim yollarında `403 ACCESS_DENIED` alır.
- Normal kullanıcı API'si yalnız `PUBLISHED` içeriği, `page`/`size` pagination ve
  en fazla 100 kayıt sınırıyla döndürür.
- İçerik değişikliği ve admin audit kaydı aynı transaction'da yazılır; audit
  aktörü request body'den değil doğrulanmış `CurrentActorProvider` bağlamından
  alınır.
- Domain'in Spring/JPA'dan ve content modülünün identity infrastructure'dan
  bağımsızlığı iki ArchUnit testiyle korunur.
- Aggregate, yayınlama, audit ve pagination kararları
  `ADR-0007-asama-2-content-aggregate.md` ile kaydedildi.
- Aşama 3 `Quiz`, `QuizVersion`, `Question` ve `AnswerOption` saf domain modeli
  ile tamamlandı; `Quiz` aggregate root olarak sürüm ağacını korur.
- İlk quiz sürümü draft oluşturulur; yayın için en az bir soru, her soruda en az
  iki seçenek ve tam bir doğru seçenek gerekir.
- Yayınlanan ve arşivlenen sürümler yerinde değiştirilemez; yeni düzenleme aktif
  yayın sürümünü yeni kimliklerle kopyalayan bir sonraki draft sürümünü üretir.
- Yeni draft yayımlandığında önceki aktif sürüm arşivlenir; açık archive use
  case'i aktif yayını kullanıcı sorgusundan kaldırır.
- Puanlama davranışı Aşama 4'e bırakıldı; korunacak politika sınırı
  `STANDARD_V1` olarak quiz sürümünde sabitlendi.
- Flyway `V3__quiz_authoring.sql`; quiz, sürüm, soru ve seçenek tablolarını;
  foreign key, check, sıra unique, tek draft ve tek doğru seçenek index'lerini
  ekledi.
- EDITOR/ADMIN quiz/sürüm/soru yönetim ve publish/archive API'leri eklendi;
  normal USER yönetim yollarında `403 ACCESS_DENIED` alır.
- Kullanıcı API'si yalnız aktif yayın sürümünü döndürür; kullanıcı DTO'sunda
  doğru cevabı gösterebilecek `correct` alanı bulunmaz.
- Quiz application servisi içerik varlığını content modülünün
  `ContentReferenceVerifier` portundan doğrular; content infrastructure'a
  doğrudan bağımlı değildir.
- Admin audit portu ve JPA adapter'ı content paketinden gerçek sahibi olan
  `admin` modülüne taşındı; content ve quiz değişiklikleri audit ile aynı
  transaction'da kesinleşir.
- Doğru cevap değişikliğinde partial unique index korunurken Hibernate yazma
  sırası çakışmasını önlemek için eski doğru bayrakları aynı transaction'da
  temizlenir; aynı sıra numaralı seçenek kimliği korunur.
- Quiz sürümleme kararları
  `ADR-0008-asama-3-quiz-surumleme.md` ile kaydedildi.
- Aşama 4 `QuizAttempt`, `SubmittedAnswer`, ACTIVE/COMPLETED/EXPIRED durum
  makinesi ve `QuizAttemptCompleted` domain event'i ile tamamlandı.
- Attempt yayınlanmış quiz/politika sürümünü sabitler; `STANDARD_V1` beş dakika
  ve doğru başına 100 puan uygular.
- Cevap kalıcılaştırıldıktan sonra doğru seçenek aynı response'ta açıklanır ve
  sıradaki soru açılır; son cevap attempt'i otomatik tamamlar.
- Aynı kullanıcı/quiz için tek aktif attempt vardır; completed/expired attempt
  sonrasında tekrar çözmeye izin verilir.
- Flyway `V4__gameplay_attempts.sql`, attempt/answer tablolarını ve aynı soru,
  idempotency ve tek aktif attempt constraint'lerini ekledi.
- Start, get, answer ve complete API'leri eklendi; aktör request body yerine
  `CurrentActorProvider` bağlamından alınır.
- Gameplay kararları `ADR-0009-asama-4-gameplay.md` ile kaydedildi.
- Kullanıcı her soruda soruya özel görsel veya içerik kapağı görmek; görme,
  işitme, motor ve bilişsel farklılıklar için erişilebilirlik temelini backend
  sözleşmesine almak istedi.
- Aşama 4 ile XP arasına `Aşama 4.1 - Erişilebilir medya ve kapsayıcı
  gameplay sözleşmesi` ayrı ek seviye olarak eklendi.
- WCAG 2.2 AA mühendislik hedefi; değişmez medya, kapak fallback'i, cevabı
  sızdırmayan eşdeğer soru sunumu, semantik feedback ve herkese açık en az
  on kat uzun süre modu kararları
  `ADR-0010-erisilebilir-medya-ve-gameplay.md` ile kaydedildi.
- Aşama 4.1 `media` modülü, yerel dosya storage adapter'ı ve Flyway
  `V5__accessible_media_and_timing.sql` migration'ıyla tamamlandı.
- JPEG/PNG yüklemede 5 MB, 4096×4096, dosya imzası, çözülebilir içerik ve
  SHA-256 bütünlük kontrolleri eklendi; yalnız EDITOR/ADMIN yazabilir.
- Yayınlanan içerikte kapak ve alternatif metin zorunlu oldu. Soruya özel
  `INFORMATIVE`/`DECORATIVE` görsel sözleşmesi ve cevabı sızdırmayan eşdeğer
  `accessiblePrompt` publish kuralı eklendi.
- Soruya özel görsel yoksa içerik kapağı quiz sürümüne sabitlenir; gameplay
  response'u çözümlenmiş görseli ve `CORRECT`/`INCORRECT` durumunu döndürür.
- Herkese açık `STANDARD_V1` (5 dakika) ve `EXTENDED_V1` (50 dakika) süre
  politikaları attempt'e sabitlenir; sağlık/engel bilgisi saklanmaz ve skor
  hesabı değişmez.
- Aşama 5 `gamification` modülü, `XpTransaction` domain modeli ve Flyway
  `V6__xp_ledger.sql` migration'ıyla tamamlandı.
- `SCORE_MATCH_V1`, tamamlanan attempt'in kesin skorunu XP'ye birebir çevirir;
  sıfır skorlu tamamlanma da işlenen kaynağı gösteren tek ledger kaydı bırakır.
- Aşama 5'te attempt'in tamamlanması ile XP ekleme aynı PostgreSQL transaction'ında
  çalışarak iş kuralının doğruluğunu kanıtladı; Aşama 6 bu senkron sınırı Outbox
  ve idempotent consumer ile değiştirdi.
- Kullanıcı XP özeti API'si ile yalnız ADMIN'e açık, özgün kazancı değiştirmeden
  imzalı yeni kayıt ve audit üreten düzeltme API'si eklendi.
- XP ledger ve transaction sınırı kararları
  `ADR-0011-postgresql-xp-ledger.md` ile kaydedildi.
- Aşama 6'da Flyway `V7__transactional_outbox_and_inbox.sql`, sürümlü
  `quiz.completed` integration event'i, Transactional Outbox publisher'ı ve
  Inbox korumalı XP consumer'ı eklendi.
- RabbitMQ direct exchange, dayanıklı XP queue'su, üç denemeli artan gecikme,
  dead-letter queue, publisher confirm, trace header'ları ve temel Micrometer
  sayaçlarıyla yapılandırıldı.
- Broker kesintisi attempt'i bozmaz; bekleyen Outbox olayı broker geri geldiğinde
  yayımlanır. Duplicate teslimat Inbox ve XP ledger tekillikleriyle korunur.
- Mesajlaşma ve transaction kararları
  `ADR-0012-transactional-outbox-rabbitmq.md` ile kaydedildi.
- Aşama 7 `leaderboard` modülü, global/içerik API'leri ve Flyway
  `V8__postgresql_leaderboard.sql` migration'ıyla tamamlandı.
- XP satırları içerik aidiyeti taşır. Migration eski kayıtları kaynak attempt ve
  özgün adjustment ilişkisinden geri doldurur; PostgreSQL trigger'ı yeni
  satırların kullanıcı/içerik kaynağını doğrular.
- Sıra toplam XP azalan, ilk XP zamanı artan, kullanıcı UUID'si artan olarak
  `ROW_NUMBER` ile hesaplanır. Top N dışında kalan current user ayrıca döner;
  yüzdelik üretilmez.
- Leaderboard kuralları ve alternatifleri
  `ADR-0013-postgresql-leaderboard-kurallari.md` ile kaydedildi.
- Aşama 8'de Redis 8.2 sorted set read model, beş saniyelik kontrollü refresh,
  ADMIN rebuild, atomik generation işaretçisi ve PostgreSQL fallback eklendi.
- Redis skoru toplam XP yerine PostgreSQL'in benzersiz pozisyonudur; XP ve ilk
  işlem zamanı hash metadata'sında tutulur. Böylece eşitlik sırası değişmez.
- API okuma kaynağını `dataSource`, projeksiyon zamanını
  `projectionGeneratedAt` ile görünür kılar; hit/fallback Micrometer sayaçları
  eklendi.
- Redis kararları ve alternatifleri
  `ADR-0014-redis-leaderboard-read-model.md` ile kaydedildi.
- Aşama 9'un ilk artımında Spring Boot OpenTelemetry HTTP trace'leri,
  Prometheus registry/scrape endpoint'i, HTTP p95/p99 histogramları ve
  Outbox/consumer Observation'ları eklendi.
- İstemci `X-Trace-Id` korelasyon değeri `requestTraceId` alanına ayrıldı;
  gerçek OpenTelemetry `traceId/spanId` değerlerinin üzerine yazılması önlendi.
- Compose `observability` profiline OpenTelemetry Collector, Tempo,
  Prometheus ve provision edilmiş operasyon dashboard'uyla Grafana eklendi.
- Gözlemlenebilirlik kararı ve alternatifleri
  `ADR-0015-opentelemetry-prometheus-observability.md` ile kaydedildi.
- W3C `traceparent`/`tracestate` Outbox V9 migration'ında nullable saklanır;
  publisher ve RabbitMQ consumer aynı dağıtık trace'i sürdürür. Eski context'siz
  Outbox satırının yayımlanabilirliği rolling-deploy testiyle korunur.
- `/api/**` için IP anahtarlı, bounded bellek kullanan token bucket eklendi;
  `429`, kararlı hata kodu, `Retry-After` ve RateLimit header'ları test edildi.
- CI'a transitive CycloneDX SBOM + commit SHA'sına sabitlenmiş OSV-Scanner,
  Gitleaks ve haftalık Dependabot kontrolleri eklendi.
- k6 baseline/ramp/spike/soak profilleri eklendi. Gerçek PostgreSQL ve uygulama
  üzerinde bir dakikalık 10 istek/s baseline 601 istekte %0 hata ve 0 dropped
  iteration ile geçti; p95 `15,75 ms`, p99 `332,82 ms` ölçüldü.
- PostgreSQL pause/recovery testi commit edilmiş veriyi korudu; `pg_dump` çıktısı
  ayrı disposable veritabanına restore edilip Flyway geçmişi ve marker okundu.
  Mevcut RabbitMQ ve Redis testleri broker recovery ile PostgreSQL fallback'i
  yeniden doğrular.
- Expand-contract, migration kilit/rollback yaklaşımı, DLQ replay, ilk
  `RPO ≤ 15 dk` / `RTO ≤ 60 dk`, retention/KVKK/audit sınırları
  `docs/OPERATIONS.md` ve ADR-0016'da kaydedildi.
- Aşama 0–9 boyunca karşılaşılan ortam, framework, veri bütünlüğü,
  concurrency, erişilebilirlik, Outbox/RabbitMQ, leaderboard, trace, yük,
  güvenlik taraması ve disaster-recovery problemleri;
  kök neden, çözüm, test kanıtı ve rapor çıkarımıyla
  `docs/DEVELOPMENT_CHALLENGES.md` günlüğünde toplandı.

## Henüz tamamlanmayanlar

- Java/Spring Boot seçiminin backend lead veya kurum standardıyla doğrulanması
- Kurumun production kimlik sağlayıcısının ve OIDC/JWT claim sözleşmesinin
  öğrenilmesi
- Production kimlik entegrasyonuyla doğrulanmış KVKK silme/anonimleştirme
  iş akışının ve hukuk onaylı retention sürelerinin uygulanması

## Mevcut teknoloji temeli

- Java 21 + Spring Boot 4.1.0
- PostgreSQL 17.5
- Maven Wrapper
- Flyway
- Docker Compose
- Testcontainers
- Spring Security
- Spring AMQP ve RabbitMQ 4.1
- Spring Data Redis ve Redis 8.2
- OpenTelemetry/OTLP, Prometheus, Tempo ve Grafana

Bu seçimler `ADR-0003` ile gerekçelendirilmiştir; kurum standardı farklıysa
yeniden değerlendirilir.

## Yerel geliştirme ortamı

### Hazır

- Eclipse Temurin JDK 21.0.11 LTS
- Git 2.51.0
- VS Code
- WSL 2.7.11
- Docker Desktop 4.84.0
- Docker Engine 29.6.2
- Docker Compose 5.3.1
- Maven Wrapper
- `5433` host portunda sağlıklı PostgreSQL 17.5 geliştirme container'ı
- `5673` AMQP ve `15673` yönetim portlarında RabbitMQ geliştirme container'ı
- `6380` host portunda Redis 8.2 geliştirme container'ı

### Bilinçli olarak ayrıca kurulmadı

- Global Maven/Gradle: Build tool seçildikten sonra proje içindeki wrapper
  kullanılacak.
- Windows PostgreSQL: PostgreSQL, Docker Compose ile container olarak
  çalıştırılacak.

### Terminal notu

Kullanıcı düzeyindeki `JAVA_HOME` ve `PATH` güncellendi. Değişikliğin normal
terminale yansıması için yeni terminal açılmalı; `java -version`,
`docker version` ve `docker compose version` yeniden çalıştırılmalıdır.

## Bu görevde yapılan doğrulamalar

- Markdown belgeleri ve ADR içerikleri UTF-8 olarak okundu.
- DOCX raporundaki 231 paragraf ve 34 tablo yapısal olarak incelendi.
- Belgeler arasında MVP sınırı, modüler monolith, PostgreSQL doğru kaynak,
  ertelenmiş RabbitMQ/Redis ve test stratejisi tutarlılığı kontrol edildi.
- Backend çalışma defterindeki 24 bölüm, 112 kontrol/alıştırma maddesi, README
  bağlantısı ve proje kapsam kuralları doğrulandı.
- Genişletilen dil karşılaştırmalarının ve 21 ana bölüm ile 129
  kontrol/alıştırma maddesi içeren Java çalışma kitabının başlık yapısı, iç
  bağlantıları ve kontrol soruları doküman düzeyinde doğrulandı.
- Proje ister PDF'sinin bütün sayfaları görsel olarak render edildi; Türkçe
  karakterler, sayfa numaraları, başlık geçişleri, madde hizaları ve metin
  taşmaları kontrol edildi.
- Güncellenen resmî proje tanımı yeniden PDF'e dönüştürüldü. 11 sayfanın tamamı
  yeniden render edilip başlıklar, etiketler, Türkçe karakterler, kenar
  taşmaları ve sayfa numaraları görsel olarak kontrol edildi.
- PDF metin kontrolünde yeni kimlik, PostgreSQL XP, RabbitMQ, PostgreSQL
  leaderboard ve Redis read model aşamalarının bulunduğu; bozuk karakter
  oluşmadığı doğrulandı.
- Uygulama kodu bulunmadığı için build veya otomatik test çalıştırılmadı.
- Güncellenen roadmap aşamalarının her birinde kabul kriteri, test yaklaşımı ve
  öğrenme çıktısı bulunduğu doküman düzeyinde kontrol edildi.
- README, proje özeti, mimari, yol haritası, resmî proje tanımı ve ADR'ler
  arasında kimlik sırası ile PostgreSQL/RabbitMQ/Redis sınırı karşılaştırıldı.
- `README.md` ile `AGENTS.md` içindeki öğretici çalışma, isimlendirme, aşama
  geçişi ve görsel işleme kurallarının birbiriyle uyumlu olduğu kontrol edildi.
- DOCX görsel render denemesi, ortamda LibreOffice bulunmadığı için
  tamamlanamadı; DOCX üzerinde değişiklik yapılmadı.
- Docker Desktop 4.84.0, Engine 29.6.2 ve Compose 5.3.1 çalışır durumda
  doğrulandı.
- Java 21.0.11 doğrudan kurulum yolundan doğrulandı.
- `.\mvnw.cmd --batch-mode verify` son çalıştırmada başarıyla tamamlandı:
  3 test çalıştı, 0 failure, 0 error, 0 skipped.
- Testcontainers 2.0.5 gerçek PostgreSQL 17.5 container'ı başlattı.
- Flyway, boş veritabanına `V1 - baseline` migration'ını başarıyla uyguladı.
- Health testi `UP`, hata zarfı testi `RESOURCE_NOT_FOUND` ve aynı
  `X-Trace-Id` değerini doğruladı.
- `docker compose up --detach --wait` sonucunda geliştirme PostgreSQL'i
  `5433 -> 5432` eşlemesiyle healthy durumuna geldi.
- Paketlenen uygulama yerel PostgreSQL'e bağlanarak `8081` portunda açıldı;
  `http://127.0.0.1:8081/actuator/health` yanıtı `UP` olarak doğrulandı ve
  uygulama doğrulama sonrasında kapatıldı.
- İlk build'de Spring Initializr metadata'sının ürettiği
  `4.1.0.RELEASE` etiketi Maven Central'da bulunamadı; gerçek artifact sürümü
  `4.1.0` olarak düzeltilip yeniden test edildi.
- Host `5432` portunun mevcut Windows PostgreSQL süreci, `8080` portunun da
  başka bir uygulama tarafından kullanıldığı saptandı; mevcut süreçlere
  dokunulmadan proje portları ayrıldı.
- Hosted CI çalıştırılmadı; henüz uzak Git deposu bulunmuyor.
- PlantUML kaynağı, kurulan eklentinin kendi `plantuml.jar` motoruyla
  `-checkonly` kullanılarak doğrulandı; sözdizimi hatası bulunmadı.
- Aynı doğrulanmış kaynaktan `runtime_architecture.svg`,
  `backend_code_structure.svg` ve `planned_database_model.svg` başarıyla
  üretildi. Kullanıcı isteği diyagrama dönüştürmeyi kapsadığı için render
  gerçekleştirildi; ayrıca bir görsel kalite incelemesi yapılmadı.
- Son yerleşim ölçümleri sırasıyla `854x1036`, `831x1034` ve `1407x2078`
  pikseldir; üç SVG'nin de yüksekliği genişliğinden fazladır.
- GitHub'a gönderim öncesi `.\mvnw.cmd --batch-mode verify` Java 21 ve Docker
  Desktop üzerinde yeniden çalıştırıldı: 3 test geçti, 0 failure, 0 error ve
  0 skipped.
- `docs/README.md` içindeki yerel doküman ve diyagram bağlantılarının hedefleri
  kontrol edildi; çalışma alanında bulunmayan `docs/bwl.md` yeniden
  oluşturulmadı ve bozuk bağlantı dizine eklenmedi.
- PDF ve DOCX dosyaları mevcut halleriyle GitHub'a gönderildi; bu görevde
  yeniden üretilmedi, görsele dönüştürülmedi ve görsel kalite incelemesi
  yapılmadı.
- GitHub Actions `Backend CI` çalışması `30543010143`, `main` dalındaki
  `ef3ae6f` commit'i için başarıyla tamamlandı. Ubuntu ortamında repository
  checkout, Java 21 kurulumu ve `mvnw verify` adımlarının tamamı geçti.
- Aşama 1 için `.\mvnw.cmd --batch-mode clean test` Java 21 ve çalışan Docker
  Engine ile başarıyla tamamlandı: 8 test geçti, 0 failure, 0 error, 0 skipped.
- Son teslim doğrulamasında `.\mvnw.cmd --batch-mode verify` başarıyla
  tamamlandı; aynı 8 test yeniden geçti ve çalıştırılabilir Spring Boot JAR'ı
  üretildi.
- Testcontainers gerçek PostgreSQL 17.5 container'ını başlattı ve Flyway
  baseline migration'ı temiz veritabanına uygulandı.
- Aşama 0 health, migration ve trace ID/hata zarfı testlerinin güvenlik
  değişikliğinden sonra da geçtiği doğrulandı.
- Kimliksiz korunan isteğin `401 AUTHENTICATION_REQUIRED` döndürdüğü test edildi.
- `USER`, `EDITOR` ve `ADMIN` test kimliklerinin farklı aktör ve rol olarak
  çözüldüğü test edildi.
- Geçersiz test kimliğinin `401 AUTHENTICATION_INVALID` döndürdüğü; hassas header
  değerinin response veya güvenlik loguna taşınmadığı doğrulandı.
- `USER` aktörünün editor korumalı use case'e erişiminin
  `403 ACCESS_DENIED` ile reddedildiği doğrulandı.
- Request body içinde gönderilen farklı aktör kimliğinin yok sayıldığı ve
  application use case'inin yalnız doğrulanmış security actor'ünü kullandığı
  doğrulandı.
- Spring Boot 4.1'in Jackson 3 `tools.jackson` paketini kullandığı bağımlılık
  ağacıyla doğrulandı; eski Jackson 2 bağımlılığı eklenmedi.
- İlk artımlı derleme çıktısındaki eksik-tip bytecode kalıntısı `clean` build ile
  giderildi; temiz build başarıyla sonuçlandı.
- Docker Desktop 4.84.0 ve Engine 29.6.2 yeniden başlatıldı; Compose PostgreSQL
  `5433 -> 5432` eşlemesiyle healthy duruma geldi.
- Aşama 1 teslim noktası `clean verify` ile yeniden doğrulandı: 8 test geçti,
  0 failure, 0 error, 0 skipped ve çalıştırılabilir JAR üretildi.
- Aşama 2 test çalışmasında toplam 21 test geçti: 8 temel/identity integration,
  6 content PostgreSQL/API integration, 5 saf domain unit ve 2 ArchUnit testi.
- Flyway V1 ve V2 migration'ları iki ayrı geçici PostgreSQL 17.5 container'ında
  temiz şemaya uygulandı; Hibernate `ddl-auto=validate` ile şemayı doğruladı.
- PostgreSQL unique constraint testi aynı içerikte sezon ve aynı sezonda bölüm
  numarası tekrarını veri katmanında reddetti.
- Uçtan uca API testi create → season → episode → publish → USER read akışını ve
  aynı transaction sınırındaki editör audit kayıtlarını doğruladı.
- Draft yayın filtresi, USER yönetim yasağı, validation/pagination sınırları ve
  update/delete davranışları API testleriyle doğrulandı.
- Paketlenen JAR Compose PostgreSQL'e bağlanarak `8081` portunda başlatıldı;
  health `UP`, yerel Flyway V2 kaydı `2:true` ve varsayılan profilde test
  header'larıyla content erişimi `401` olarak doğrulandı. Uygulama kontrolden
  sonra kapatıldı; PostgreSQL container'ı healthy durumda bırakıldı.
- GitHub Actions `Backend CI` çalışması `30705476787`, pull request dalındaki
  Aşama 1 ve Aşama 2 commit'leri için başarıyla tamamlandı. Ubuntu ortamında
  checkout, Java 21 kurulumu ve build/integration test adımlarının tamamı geçti.
- Aşama 3 geliştirmesi öncesi mevcut teslim noktası yeniden doğrulandı: 21 test
  geçti, 0 failure, 0 error ve 0 skipped.
- Quiz saf domain adımı 6 unit testle doğrulandı: yayın bütünlüğü, doğru cevap,
  değişmezlik, yeni draft kopyası, eski sürümün arşivlenmesi ve sıra tekilliği.
- Quiz PostgreSQL/API integration testleri 8 senaryoda geçti: author/publish,
  audit, draft filtresi, yetki, yeni sürüm, doğru cevap sızmaması, soru
  update/delete, açık archive ve gerçek constraint davranışları.
- Quiz domain'inin Spring/JPA'dan, quiz modülünün diğer modüllerin
  infrastructure paketlerinden bağımsızlığı 2 ArchUnit testiyle doğrulandı.
- Flyway V1, V2 ve V3 migration'ları üç ayrı geçici PostgreSQL 17.5
  container'ında temiz şemaya başarıyla uygulandı; Hibernate şemayı doğruladı.
- Doğru seçeneği A'dan B'ye değiştiren gerçek PostgreSQL testi, seçenek kimliği
  koruma ve tek-doğru partial unique index davranışını birlikte kanıtladı.
- Kullanıcı quiz contract testi response içinde `correct` ve `isCorrect`
  alanlarının bulunmadığını doğruladı.
- Son `.\mvnw.cmd --batch-mode verify` çalışması başarıyla tamamlandı: toplam
  37 test geçti, 0 failure, 0 error, 0 skipped ve çalıştırılabilir JAR üretildi.
- Gameplay saf domain kuralları 5 unit testle; durum makinesi, deadline,
  sunucu skoru, cevap değişmezliği, idempotency ve tamamlanma olayıyla doğrulandı.
- Gameplay API/PostgreSQL akışı 3 integration testte; doğru cevap sızmaması,
  cevap sonrası açıklama, sıradaki soru, sahiplik ve idempotency ile doğrulandı.
- Paralel aynı-soru testinde PostgreSQL unique constraint yalnız bir answer
  satırını kabul etti ve diğer istek `409` aldı.
- Gameplay modül sınırı 2 ArchUnit testiyle doğrulandı.
- Son `.\mvnw.cmd --batch-mode verify` çalışması toplam 47 testle başarılı oldu:
  0 failure, 0 error, 0 skipped; Flyway V1–V4 ve JAR paketleme geçti.
- Aşama 4.1 medya API testleri gerçek PostgreSQL ile görsel yükleme/okuma,
  yetki, MIME imzası uyuşmazlığı ve 5 MB sınırını doğruladı.
- Quiz domain ve contract testleri bilgilendirici görsel metinlerinin zorunlu
  olduğunu, doğru cevabı sızdıramadığını ve admin/gameplay alanlarının
  kalıcılaştırılıp güvenli kullanıcı sözleşmesine taşındığını doğruladı.
- Gameplay integration testi `EXTENDED_V1` seçiminin 50 dakikalık deadline
  ürettiğini ve attempt satırında sürümlü olarak saklandığını doğruladı.
- Media domain'inin framework'ten ve media modülünün identity infrastructure'dan
  bağımsızlığı iki ArchUnit testiyle korundu.
- Son `.\mvnw.cmd --batch-mode verify` çalışması toplam 57 testle başarılı oldu:
  0 failure, 0 error, 0 skipped; Flyway V1–V5 ve çalıştırılabilir JAR paketleme geçti.
- Aşama 5 saf domain testleri skorun XP'ye birebir çevrilmesini, sıfır skorlu
  ledger kaydını ve geçmişi değiştirmeyen pozitif/negatif düzeltmeleri doğruladı.
- Gameplay/PostgreSQL integration testleri son cevap ile XP'nin aynı transaction
  içinde kesinleştiğini, tekrar ve paralel complete isteklerinin tek kayıt
  ürettiğini ve XP hatasında attempt/cevap değişikliklerinin geri alındığını
  doğruladı.
- XP API testleri kullanıcı özetini, yalnız ADMIN'in düzeltme yapabildiğini,
  aynı düzeltme referansının idempotent olduğunu ve audit kaydını doğruladı.
- Gamification domain'inin framework'ten, modülün diğer modüllerin infrastructure
  katmanlarından bağımsızlığı ArchUnit testleriyle korundu.
- Son `.\mvnw.cmd --batch-mode verify` çalışması toplam 66 testle başarılı oldu:
  0 failure, 0 error, 0 skipped; Flyway V1–V6 temiz PostgreSQL şemasına uygulandı
  ve çalıştırılabilir JAR paketlendi.
- Aşama 6 PostgreSQL integration testleri attempt ile Outbox olayının aynı
  transaction'da kesinleştiğini; Outbox çakışmasının son cevap ve completion'ı
  geri aldığını; consumer hatasının ise tamamlanmış attempt'i bozmadığını
  doğruladı.
- Duplicate ve paralel complete senaryoları tek Outbox olayı üretti. Aynı event
  iki kez işlendiğinde Inbox ve XP kaynak unique constraint'leri tek XP ledger
  kaydı bıraktı.
- Gerçek RabbitMQ Testcontainers testleri publish/consume akışını, broker
  kesintisinde Outbox retry'sını ve broker geri geldiğinde teslimatı doğruladı.
  Geçersiz mesaj üç denemeden sonra dead-letter queue'ya taşındı.
- Messaging application katmanının RabbitMQ/JDBC ayrıntılarından ve gameplay'in
  messaging infrastructure paketinden bağımsızlığı ArchUnit testleriyle korundu.
- Son `.\mvnw.cmd --batch-mode verify` çalışması toplam 74 testle başarılı oldu:
  0 failure, 0 error, 0 skipped; Flyway V1–V7 temiz PostgreSQL şemasına
  uygulandı, gerçek RabbitMQ testleri geçti ve çalıştırılabilir JAR paketlendi.
- Aşama 7 API/PostgreSQL testleri global ve içerik toplamını, adjustment
  etkisini, tekrar çözme toplamını, deterministik tie-break'i, current user'ın
  Top N dışındaki sırasını, yayın/yetki/limit sınırlarını ve boş sonucu doğruladı.
- Veri bütünlüğü testi yanlış içerik veya kullanıcıya bağlanan XP satırını
  PostgreSQL trigger'ının reddettiğini kanıtladı.
- 2.000 kullanıcılık örneklemde içerik Top N sorgusu yaklaşık 5,45 ms p95,
  `EXPLAIN ANALYZE` yürütmesi 1,67 ms ölçüldü; içerik leaderboard indeksi
  kullanıldı.
- Leaderboard domain'inin framework'ten ve modülün diğer modüllerin
  infrastructure paketlerinden bağımsızlığı iki ArchUnit testiyle korundu.
- Son `.\mvnw.cmd --batch-mode verify` çalışması toplam 83 testle başarılı oldu:
  0 failure, 0 error, 0 skipped; Flyway V1–V8, gerçek PostgreSQL/RabbitMQ
  senaryoları ve çalıştırılabilir JAR paketleme geçti.
- Aşama 8 gerçek Redis/PostgreSQL testleri iki kaynağın sıralama eşitliğini,
  Top N dışındaki current user'ı, cache silme ve tekrarlı rebuild'i, stale veri
  penceresini, ADMIN rebuild yetkisini ve Redis container kesintisindeki
  PostgreSQL fallback'i doğruladı.
- 2.000 kullanıcı ve 25 okuma örneğinde PostgreSQL global Top N p95 yaklaşık
  4,0 ms, toplu hash okuması sonrası Redis p95 yaklaşık 7,1 ms ölçüldü. İlk
  Redis N+1 uygulaması yaklaşık 16,8 ms idi; optimizasyon etkisi ayrıca kaydedildi.
- Son `.\mvnw.cmd verify` çalışması toplam 89 testle başarılı oldu: 0 failure,
  0 error, 0 skipped. Gerçek PostgreSQL 17.5, RabbitMQ 4.1 ve Redis 8.2
  Testcontainers senaryoları, ArchUnit sınırları ve JAR paketleme geçti.
- Geliştirme zorlukları günlüğü görevi yalnız Markdown belge ve bağlantı
  değişikliği içerdiği için uygulama testleri yeniden çalıştırılmadı; belge
  biçimi ve yerel bağlantılar ayrıca kontrol edildi.
- Aşama 9 gözlemlenebilirlik artımında ana kod ve 20 test kaynağı Java 21 ile
  başarıyla derlendi.
- Prometheus integration testi gerçek HTTP isteğinden sonra
  `http_server_requests_seconds_count` ve application label'ını scrape
  endpoint'inde doğruladı. Spring Boot test profilinin exporter'ları varsayılan
  kapatması testte açık yapılandırmayla görünür hale getirildi.
- RabbitMQ integration testi Outbox publish ve XP consumer işlemlerinin
  `messaging.outbox.publish` ile `messaging.quiz.completed.consume` timer'larını
  ürettiğini doğruladı.
- Son `.\mvnw.cmd --batch-mode verify` çalışması toplam 90 testle başarılı oldu:
  0 failure, 0 error, 0 skipped. Gerçek PostgreSQL 17.5, RabbitMQ 4.1 ve Redis
  8.2 Testcontainers senaryoları geçti ve çalıştırılabilir JAR paketlendi.
- Kullanılmayan OTLP metric exporter'ı kapatıldıktan sonra dokuz foundation
  integration testi yeniden geçti; Prometheus scrape ve uygulama context'i
  son yapılandırmayla doğrulandı.
- Compose, Collector, Tempo, Prometheus ve Grafana provisioning YAML dosyaları
  SnakeYAML ile; Grafana dashboard JSON dosyası JSON parser ile sözdizimsel
  olarak doğrulandı. Observability container profili bu görevde uçtan uca
  başlatılmadı.
- Aşama 9 final `clean verify` çalışması 97 testle geçti: 0 failure, 0 error,
  0 skipped. Gerçek PostgreSQL 17.5, RabbitMQ 4.1 ve Redis 8.2 kesinti/geri
  dönüş senaryoları, Flyway V1–V9 ve çalıştırılabilir JAR paketleme doğrulandı.
- Ayrı k6 baseline testi 601/601 başarılı istek, 0 dropped iteration,
  p95 `15,75 ms` ve p99 `332,82 ms` ile bütün eşikleri geçti.
- CycloneDX SBOM + OSV ilk taramada 164 bileşende 4 düzeltilebilir bulgu yakaladı.
  Netty `4.2.16.Final`, PostgreSQL JDBC `42.7.12` ve Jackson `3.1.5` patch
  sürümlerine yükseltildikten sonra tekrar tarama `No issues found` sonucu verdi.
- Aşama 9 sonrası dokümantasyon tutarlılığı düzeltmesinde
  `DEVELOPMENT_CHALLENGES.md` günlüğüne trace, k6, SBOM/OSV, PostgreSQL
  outage/restore ve rate limiter kayıtları eklendi; `PROJECT_BRIEF.md`,
  `ARCHITECTURE.md` ve `PROJECT_SPECIFICATION.md` tamamlanan Aşama 9, 97 test ve
  sırada zorunlu aşama kalmadığı bilgisiyle eşitlendi. Uygulama kodu değişmediği
  için testler yeniden çalıştırılmadı; stale durum ifadeleri, Markdown bağlantıları
  ve diff biçimi kontrol edildi.

## Öğrenme odağı

Aşama 4, sunucu otoriteli sistemde istemcinin yalnız niyet bildirdiğini
gösterir: kullanıcı option ID gönderir; deadline, sıra, doğruluk ve puan sunucu
tarafından belirlenir. Cevap transaction içinde değişmezleşmeden doğru seçenek
açıklanmaz. Idempotency aynı isteğin tekrarını, unique constraint ise farklı
paralel isteklerin veri yarışını çözer; bunlar aynı problem değildir.

Aşama 4.1, klasik alt metnin görsel tanıma sorularında tek başına
yeterli olmadığını gösterir: fazla açıklama cevabı sızdırır, az açıklama ise
eşdeğer deneyim sağlamaz. Bu nedenle medya alternatifi ile cevabı sızdırmayan
eşdeğer erişilebilir soru metni ayrı kavramlardır. Storage portu dosyanın nereye
yazıldığıyla iş kurallarını ayırır. Erişilebilir süre tercihi hassas engel/sağlık
verisi toplamadan herkese açık uygulanır.

Aşama 5, toplam XP alanını doğrudan güncellemek ile işlem defteri tutmak
arasındaki farkı gösterir. Ledger her kazanç ve düzeltmeyi yeni bir olgu olarak
saklar; toplam sonradan bu imzalı kayıtların toplamından hesaplanır. Idempotency
aynı iş kaynağını yeniden oynatmayı güvenli kılar, veritabanı unique constraint'i
paralel yazma yarışını durdurur ve ortak PostgreSQL transaction'ı attempt ile XP
arasında yarım sonuç kalmasını engeller.

Aşama 6, bir veritabanı yazımı ile broker publish işlemini doğrudan peş peşe
yapmanın dual-write riski oluşturduğunu gösterir. Outbox iş sonucu ile olay
kaydını tek transaction'da kesinleştirir; publisher olayı sonradan taşır.
RabbitMQ at-least-once teslim ettiği için consumer Inbox ve ledger tekilliğiyle
idempotent olmalıdır. XP özeti artık eventual consistency gösterir: sonuçtaki
beklenen XP hemen bilinir, ledger toplamı consumer çalışınca güncellenir.

Aşama 7, leaderboard iş kuralı ile hızlandırıcı veri yapısının ayrı problemler
olduğunu gösterir. Önce PostgreSQL ledger toplamı, kapsam ve tie-break kesinleşir;
sonra Redis aynı sonucu yeniden üretir. `ROW_NUMBER` benzersiz pozisyon verir,
indeks aday satırları bulmayı hızlandırır, `EXPLAIN ANALYZE` ise veritabanının
gerçekte seçtiği planı ve ölçülen süreyi gösterir.

Aşama 8, cache/read model ile doğru kaynağın aynı şey olmadığını gösterir.
Redis silinebilir ve yeniden üretilebilir; PostgreSQL ledger kaybedilemez.
Eventual consistency, projeksiyon yenilenene kadar sonucun kısa süreli eski
olabilmesidir. Atomik generation işaretçisi yarım rebuild'in okunmasını önler;
fallback ise Redis kesintisini veri kaybına veya API kesintisine dönüştürmez.
Sorted set kullanmak tek başına performans garantisi değildir: ayrı metadata
okumaları N+1 ağ turu oluşturdu, toplu hash okuması gecikmeyi belirgin düşürdü.

Aşama 9 metric ile trace'in farklı soruları cevapladığını gösterir. Metric düşük
cardinality ile eğilim ve alarm üretir; trace tek bir isteğin span akışını
açıklar. Kullanıcıdan gelen korelasyon kimliği gerçek trace kimliği değildir.
W3C context parent-child ilişkisini asenkron sınırda korur. Token bucket burst
ile sürdürülebilir hızı ayırır. SLI ölçüm, SLO hedef; RPO kabul edilen veri kaybı
penceresi, RTO geri dönüş hedefidir. Yük testinde istek sayısı tek başına yeterli
değildir: p95/p99, hata ve dropped iteration birlikte değerlendirilir.

## Sıradaki tek iş

Aşama 0–9 çekirdek backend roadmap'i tamamlandı. Sırada zorunlu aşama yoktur.
Yeni çalışma ancak Aşama 10'daki opsiyonel ürünlerden biri açıkça seçilirse veya
kurum kimlik/hukuk/altyapı sözleşmeleri gelirse başlamalıdır.

## Yeni Codex görevi için kısa komut

```text
Repo içindeki AGENTS.md ve docs/ altındaki proje belgelerini oku. Aşama 0–9'un
tamamlandığını CURRENT_STATE, ROADMAP, OPERATIONS ve ADR-0015/0016 üzerinden
doğrula. Kullanıcıdan Aşama 10'daki hangi bağımsız ürün genişlemesinin istendiği
gelmeden yeni teknoloji veya özellik ekleme.
```

## Bilinen riskler

- RabbitMQ ve Redis kullanımı zorunlu; dil, framework ve diğer kurum teknoloji
  standartları henüz bütünüyle bilinmiyor.
- GitHub Actions başarılı çalışıyor; kullanılan `actions/checkout@v4` ve
  `actions/setup-java@v4` sürümleri Node.js 20 deprecation uyarısı veriyor ve
  ayrı bir bakım görevinde güncel major sürümleri değerlendirilmeli.
- Kapsamın canlı TV, eğitim ve sosyal özelliklerle erken büyüme riski var.
- Redis read modelinin PostgreSQL doğru kaynak gibi kullanılma riski rebuild,
  sonuç eşitliği ve kesinti/fallback testleriyle korunur; üretimde stale pencere
  ve fallback oranı için alarm eşikleri henüz belirlenmedi.
- Gerçek TRT/tabii sistemlerine entegrasyon yetkisi veya sözleşmesi henüz yok.
- Production authentication yöntemi, kurum kimlik sağlayıcısı, issuer/audience
  ve rol claim eşlemesi henüz belli değil. Geçici header adapter'ı production'da
  etkin değildir.
- Mockito/Byte Buddy, Java 21 test koşusunda gelecekte varsayılan olarak
  engellenecek dinamik agent yükleme uyarısı veriyor; testler bugün geçiyor,
  ayrı bir test-tooling bakım görevinde explicit agent yapılandırması
  değerlendirilmeli.
- Attempt otomatik expire ve puanlama sürümü ürün kararları ilgili aşamalarda
  kesinleştirilmelidir.
- `SCORE_MATCH_V1` bugün XP'yi sunucu skoruna eşitler. Ürün ileride taban XP,
  bonus veya çarpan isterse yeni politika sürümü gerekir.
- ADMIN düzeltmesi toplam XP'yi sıfırın altına indirebilir; bu yetkili ve audit
  edilen davranış gerçek operasyon politikası belirlenirken yeniden
  değerlendirilmelidir.
- XP özeti RabbitMQ consumer'ı çalışana kadar kısa süreli eski değer dönebilir;
  frontend sonuç yanıtındaki beklenen XP ile ledger toplamını farklı anlamlarda
  sunmalıdır.
- DLQ replay ve retention politikası belgelendi; otomatik purge/anonimleştirme
  işleri production kimlik ve hukuk sözleşmesi gelene kadar uygulanmadı.
- Production RabbitMQ bağlantısında kurum secret yönetimi, TLS, kullanıcı
  yetkileri ve queue alarm eşikleri henüz belirlenmedi.
- Publisher confirm beklerken Outbox satır kilidi tutulur; gerçek trafik hedefi
  ortaya çıktığında Outbox birikimi ve publish gecikmesi ölçülmelidir.
- Kurumun production media storage/CDN, görsel kullanım hakkı ve zorunlu
  erişilebilirlik standardı henüz bilinmiyor. Mevcut yerel dosya adapter'ı
  geliştirme içindir; production kararı geldiğinde port arkasında değiştirilmelidir.
- Dosya storage yazımı ile PostgreSQL transaction'ı atomik değildir; metadata
  veya audit yazımı sonradan başarısız olursa sahipsiz dosya temizliği ayrı bir
  production dayanıklılık görevi olarak ele alınmalıdır.
- Dönemsel leaderboard, hile/diskalifiye, görünen ad/avatar ve arkadaş kapsamı
  henüz yoktur; bunlar ayrı ürün ve veri sözleşmesi gerektirir.
- 2.000 kullanıcı ölçümü yalnız yerel başlangıç karşılaştırmasıdır; gerçek veri
  dağılımı ve trafik hedefiyle production kapasite garantisi sayılmaz.
- `docs/PROJECT_SPECIFICATION.md` Aşama 7 ile güncellendi; Aşama 8 kaynak
  Markdown belgeleri güncellendi. Türetilmiş PDF ve
  uzun DOCX bu görevde yeniden üretilmedi veya görsel olarak incelenmedi.
- Aynı quiz draft'ını eşzamanlı düzenleyen birden fazla editör için optimistic
  locking henüz yoktur; gerçek çok-editör ihtiyacı oluşursa Aşama 3 aggregate
  yazma yarışları ayrı concurrency testleriyle ele alınmalıdır.
- Aşama 9'un 10 istek/s baseline sınırı geçti; gerçek trafik hedefi bilinmediği
  için ilk SLO değerleri production kapasite garantisi değildir.
- Yerel dashboard'daki 100 ms–2 s bucket'ları ölçüm başlangıcıdır; ürün SLO'su
  veya kapasite garantisi değildir.
- Outbox W3C context'i taşır; nullable kolonlar eski satır uyumluluğunu korur.
- `/actuator/prometheus` yalnız `observability` profilinde açılır; production
  scraper ağı, kimliği ve retention/maliyet politikası henüz belirlenmedi.
- Önceki durum kaydında oluşturulduğu belirtilen `docs/PROJECT_PITCH.md` ve
  `docs/java.md` dosyaları mevcut çalışma alanında bulunmuyor. Yanlış hedefe
  yönlendirmemek için README ve `bwl.md` içindeki bozuk bağlantılar kaldırıldı.
  Dosyalar ayrı bir görevde yedekten geri getirilmeli veya yeniden
  oluşturulmalıdır.
