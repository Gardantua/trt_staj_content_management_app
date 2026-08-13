# Güncel Proje Durumu

Son güncelleme: 11.08.2026

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
`QuizAttempt` aggregate'i; sahiplik, her soru için sunucu saatinden başlayan 30
saniyelik deadline, sıralı tek cevap, idempotency, timeout ve semantik doğru şık
geri bildirimiyle hazırdır. Toplam quiz süresi yoktur; erken cevap sonraki soruyu
hemen açar. Değişmez görsel kimliği, kapak fallback'i ve
eşdeğer erişilebilir soru metni sözleşmesi uygulanmıştır. `XpTransaction`
append-only ledger'ı; quiz başına ilk tamamlamada `FIRST_COMPLETION_SCORE_V2`,
kullanıcı özeti ve audit edilen yönetici düzeltmeleriyle hazırdır. Attempt ve
Outbox aynı transaction'da kesinleşir; RabbitMQ consumer'ı Inbox ile XP'yi
idempotent ve asenkron üretir. Henüz kalıcı kullanıcı tablosu veya production kimlik
sağlayıcısı yazılmadı.
Global ve içerik bazlı `ALL_TIME` leaderboard XP ledger toplamından,
deterministik tie-break ile hesaplanır; Top N yanında mevcut kullanıcının sırası
da okunabilir. Redis bu PostgreSQL sonucundan atomik nesiller halinde yeniden
kurulur; boşluk veya kesintide sorgu PostgreSQL'e düşer.

Aşama 10A ile aynı repoda ayrı React/TypeScript `admin-web` uygulaması eklendi.
EDITOR/ADMIN yerel aktörü; draft dahil sayfalı içerik listesi, içerik
oluşturma/düzenleme, sezon/bölüm yönetimi, yayınlanmış içerik değişmezliği ve
backend code/trace ID hata sunumuyla görünürdür. Production kimlik sağlayıcısı
henüz belli olmadığı için bu arayüz local-only geliştirme sınırındadır.
Mevcut akışın görsel yenilemesinde yalın, editoryal bir içerik stüdyosu dili;
açık nötr yüzeyler, mint vurgu, poster benzeri taslak kartları ve mobilde tek
sütuna inen düzen uygulandı. Bu yenileme yeni bir API veya ürün
modülü eklemez.
10C medya ve kapak bağlama dilimiyle admin, JPEG/PNG dosyasını mevcut medya
endpoint'ine multipart olarak yükleyebilir; dönen değişmez medya kimliğini
alternatif metinle taslak içeriğe kapak olarak bağlayabilir. Böylece publish
önkoşulu arayüzden tamamlanır. Dosya türü, boyutu, imzası ve çözülebilir olması
istemci tarafından değil backend tarafından otoritatif olarak doğrulanır.
10D ile yüklenmiş görsellerin yeniden kullanım altyapısı ve admin quiz yazarlığı
tamamlandı. 11.08.2026 bakımında kütüphane UI'si kaldırıldı; kapak ve soru
görselleri ilgili formdan doğrudan yüklenir. Admin içerik detayından içerik geneli, sezon veya bölüm kapsamlı
quiz taslağı; sürüm, soru, seçenek, doğru cevap ve erişilebilir görsel bilgisi
yönetebilir. Sezon/bölüm aidiyeti application doğrulaması ile Flyway V10 check
ve bileşik foreign key constraint'lerinde birlikte korunur. Public quiz özeti
kapsamı gösterir fakat doğru cevabı taşımaz.
10E ile her soru tam dört seçenekli ve otomatik normal zorluktadır. Yatay soru
geçişi, sıralı alt önizleme, seçilen soruyu güncelleme, taslak silme, yayınlama,
yayındaki sürümü görüntüleme ve yeni draft sürümü açma aynı çalışma alanında
tamamlandı. Cevap anahtarı; içerik ve quiz bilgileriyle gerçek PDF dosyası olarak
indirilir. Karar ve alternatifler ADR-0022'de kayıtlıdır.
10F ile dizi/film oluşturma ve düzenleme ayrı **İçerik yönetimi** sekmesine
taşındı. Katalog ile detay gerçek kapak görselini gösterir; tür ve yayın durumu
etiketleri düzleştirildi. Yayımlanmış içeriğin başlık, açıklama, kapak ve alternatif
metni güncellenebilir. Silme fiziksel silmedir; gameplay geçmişi bulunan içerik
attempt ve XP bütünlüğünü korumak için `409` ile reddedilir. Quiz soru formu eksik
alanlarda görünür doğrulama sunar ve on sorudan azken sayfadan ayrılmayı uyarır.
Karar ve trade-off ADR-0023'te kayıtlıdır.
10G ile admin ana menüsüne ayrı `Quizler` ve `Quiz geçmişi` sayfaları eklendi.
Editör teknik taslak/sürüm oluşturma adımlarını görmez; hazırlanmakta olan ve
kullanıma açık quizleri tek listede yönetir. Hiç yayınlanmamış quiz fiziksel
silinir. Daha önce kullanıma açılmış quiz geçmişe kaldırılır, eski sorular ve
cevap anahtarı korunur; append-only XP ledger değişmez. Kullanıcı dizi detayındaki
demo “Nerede kalmıştın?” sezon/bölüm rafı kaldırıldı. Karar ADR-0024'tedir.
10H ile draft diziler için sezon sayısı ve sezon başına bölüm sayısı aynı formda
toplanır. Backend bütün sezon/bölüm ağacını tek transaction'da oluşturur; `1. Sezon`
ve `1. Bölüm` başlıklarını otomatik verir. Uzun bölüm listeleri varsayılan kapalıdır
ve yalnız düzenleme gerektiğinde açılır. Karar ADR-0025'tedir.
Kullanıcı tarafı da aynı Vite çalışma zamanında ayrı `user.html` giriş noktası
olarak eklendi. Yalnız yayınlanmış katalog, içerik detayı, dizi bölümleri ve
quiz özeti okunur. Kullanıcı ayrıca backend'in sunucu otoriteli attempt/cevap
akışıyla quiz çözebilir; sonuç, XP özeti ve global leaderboard ekranları da
aynı dilime eklendi. Yerel UUID kapısı yalnız development kimliği üretir;
production login değildir.

Hedef klasörde bulunan uzun mimari rapor teknik referans olarak korunmaktadır.
Bu dosya günlük geliştirme bağlamına doğrudan yapıştırılmamalıdır.

## Tamamlananlar

- Aşama 10H tamamlandı: `1/29 + 2/30` sezon planı tek admin isteğiyle 59 bölümü
  oluşturur. Hatalı ikinci tanım bütün transaction'ı geri alır; otomatik bölüm
  adları mevcut tekil güncelleme akışıyla özelleştirilebilir.
- Tam doğrulama mevcut Temurin Java 21 ile `112/112` backend ve `19/19` frontend
  testiyle geçti; frontend production build ve gerçek `2 sezon / 59 bölüm`
  tarayıcı akışı başarılıdır.
- Quiz soru editöründeki iki ayrı navigasyon teke indirildi. `Soru 1`, `Soru 2`…
  sekmeleri kayıtlı form değerleriyle ilgili soruyu açar; `+` boş sıradaki soruyu
  açar ve kayıt sonrasında yeni sekme seçili kalır. Tekrarlanan alt önizleme ve
  soru sayacı kaldırıldı. Soru sıra numarası sekme sırasından otomatik üretildiği
  için formdan kaldırıldı; kaydedilmemiş formdan geçişte veri kaybı uyarısı eklendi.
- Ürün doğrulamasında admin isteklerinin aynı uzak IP için 60 istek/dakika
  kotasını normal kullanımda doldurduğu görüldü. Varsayılan token bucket kapasitesi
  ve dakikalık dolumu 300'e çıkarıldı; koruma kapatılmadı ve environment değişkeniyle
  ortam bazında ayarlanabilir kaldı.
- Aşama 10G tamamlandı: ayrı Quizler/Quiz geçmişi sayfaları, sade düzenleme ve
  güvenli geçmişe kaldırma akışı uygulandı. Eski yayın sürümleri cevap anahtarlı
  PDF ile geçmişte görüntülenebilir.
- Hiç yayınlanmamış quiz kalıcı silinir; yayın geçmişi olan quiz için fiziksel
  silme reddedilir ve aktif sürüm geçmişe taşınır. Quiz kaldırma XP toplamını
  değiştirmez.
- Kullanıcı dizi detayındaki demo sezon/bölüm rafı ve ilgili stil kuralları
  kaldırıldı; sezon/bölüm verisi admin quiz kapsamı için backend'de korunur.
- Tam doğrulama mevcut Temurin Java 21 ile `110/110` backend testi ve `18/18`
  frontend testiyle geçti; frontend production build başarıyla üretildi.
- Aşama 10F tamamlandı: `Yeni taslak` navigasyonu yerine ayrı içerik yönetimi ve
  içerik içindeki ayrı quiz sekmeleri geldi; yayımdaki metadata/kapak düzenleme,
  gerçek kapaklı kartlar ve güvenli fiziksel silme uygulandı.
- Alternatif metin ve eşdeğer erişilebilir soru alanlarına açıklayıcı örnekler;
  soru formuna sessizce etkisiz kalmayan doğrulama eklendi. Bu aşamada eklenen on
  sorudan az taslak uyarısı Aşama 10I'de değişken soru sayısı kararıyla kaldırıldı;
  yalnız kaydedilmemiş soru değişikliği uyarılır.
- Tam backend paketi mevcut Temurin Java 21 ile 106/106 test geçti. Frontend
  16/16 testi ve TypeScript production build'i başarılıdır.
- Admin ihtiyaç haritası ve teslim sırası `docs/ADMIN_WEB_PLAN.md` içinde
  kaydedildi. 10E quiz yayın kontrolü, dört seçenek, soru sayacı ve PDF ile
  tamamlandı; operasyon araçları ayrı aday olarak kaldı.
- Her soru domain, API ve Flyway V12 deferred constraint'iyle tam dört seçenek
  taşır. Zorluk `MEDIUM` olarak sunucu tarafından atanır ve normal değer kullanıcı
  ekranında ayrıca gösterilmez.
- `QUESTION_30_SECONDS_V1`, attempt toplam süresi yerine güncel sorunun deadline'ını
  tutar. Cevap veya timeout sonrası sonraki soru için yeni 30 saniye açılır. Aynı
  anda gelen cevap/timeout pessimistic kilit ve idempotency ile tek sonuç üretir;
  timeout cevabı nullable seçenek kimliği ve sıfır puanla denetlenebilir kalır.
- Admin quiz çalışma alanına yatay soru gezgini, sıralı önizleme, yayınlama, yeni
  draft sürümü ve cevap anahtarlı PDF eklendi. `pdfmake` yalnız indirme anında
  dinamik yüklendiği için ana admin paketi yaklaşık 45 kB olarak kaldı.
- Onaylanan sınırlı TRT tabii demo kataloğu 4 dizi ve 2 film olarak resmi kaynak
  adresleri ve kapaklarıyla yerel ortama yüklendi. Manifest
  `docs/demo-data/tabii-catalog.json`, tekrar çalıştırılabilir yükleme aracı
  `ops/seed-tabii-demo.ps1` içindedir.
- Quiz kapsamı `CONTENT`, `SEASON` ve `EPISODE` olarak modellendi. Admin, içerik
  detayından quiz/sürüm/soru/şık/doğru cevap ve erişilebilir görsel alanlarını
  yönetebilir. Başka içeriğe ait sezon veya bölüm 404 ile reddedilir ve kalıcı
  yazma oluşmaz. Karar ve alternatifleri `ADR-0021` kaydeder.
- Sayfalı admin medya endpoint'i geriye uyumluluk için korunur; bağımsız görsel
  kütüphanesi 11.08.2026 bakım değişikliğinde admin arayüzünden kaldırıldı.
  Kapak ve soru görselleri ilgili formdan doğrudan yüklenir; binary içerik
  korumalı content endpoint'inden okunmaya devam eder.
- Taslak içerik detayına iki adımlı kapak akışı eklendi: JPEG/PNG dosyası
  yükleniyor, açıklayıcı alternatif metin giriliyor ve dönen medya kimliği
  kapağa bağlanıyor. Mevcut kapağın alternatif metni de aynı ekrandan
  güncellenebiliyor. `ADR-0020` bu sınırı ve alternatiflerini kaydeder.
- Yeni taslak formuna kullanıcıya görünen başlık/açıklama ile admin notu
  ayrımını açıklayan kısa rehber ve alan bazlı örnek metinler eklendi. 10D ile
  quiz yazarlığı, oluşturulan içeriğin detay ekranındaki ayrı çalışma alanına
  eklendi; içerik oluşturma formu hâlâ yalnız katalog kaydını üretir.
- 10.08.2026 tasarım iyileştirmesinde admin arayüzünden tüm gradyen ve turuncu
  vurgu kaldırıldı; açık nötr çalışma yüzeyleri ile mint ana eylem rengi
  uygulandı.
- Kullanıcı arayüzü, özgün **Hikâye İzi** anlatımıyla yenilendi. Ana sayfadaki
  numaralı yönlendirme ve boş dekoratif şekil kaldırıldı; ana, profil ve
  sıralama başlıkları ortak tipografik hiyerarşiye taşındı. Yerel platform
  görselleri üretim paketine dahil edildi; bu sunum değişikliği backend
  sözleşmesini, skor/süre otoritesini veya ürün kapsamını değiştirmez.
- `admin-web` için `npm run test` 16/16 testle geçti; `npm run build` TypeScript
  strict kontrolü ve iki giriş noktalı production paketlemesiyle başarılı oldu.

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
- Aşama 10A `admin-web` uygulaması React, TypeScript ve Vite ile ayrı frontend
  olarak kuruldu; quiz, medya yükleme, XP ve leaderboard kapsam dışında tutuldu.
- Admin içerik listesi draft kayıtları da döndüren hafif ve sayfalı
  `GET /api/v1/admin/contents` sözleşmesiyle tamamlandı; hiyerarşi detay
  endpoint'inde kaldı.
- İçerik oluşturma/düzenleme, dizi sezon/bölüm işlemleri, publish isteği,
  published değişmezliği, local rol göstergesi ve code/trace ID hata sunumu
  erişilebilir native kontrollerle uygulandı.
- Ayrı frontend, local-only kimlik ve admin liste DTO kararı ADR-0017 ile
  kaydedildi.
- Aşama 10B kullanıcı web kontrolünde belgelenmiş yerel UUID kabulü, oturumdan
  çıkış, yetkili medya istekleri, süre dolunca cevap kilidi ve ağ hatasında aynı
  idempotent cevabın güvenli yeniden denenmesi tamamlandı. İçerik detayı, quiz
  listesi yüklenemese de erişilebilir kalır.

## Henüz tamamlanmayanlar

- Java/Spring Boot seçiminin backend lead veya kurum standardıyla doğrulanması
- Kurumun production kimlik sağlayıcısının ve OIDC/JWT claim sözleşmesinin
  öğrenilmesi
- Production kimlik entegrasyonuyla doğrulanmış KVKK silme/anonimleştirme
  iş akışının ve hukuk onaylı retention sürelerinin uygulanması
- Admin web için production OIDC/JWT login, hosting, CORS ve CSRF sözleşmesi
- Medya yükleme/kapak bağlama, quiz authoring, XP ve leaderboard admin ekranları

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
- Admin web için React 19, TypeScript, Vite ve Vitest

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
- 05.08.2026 k6 baseline testi, `preAllocatedVUs` değeri yerel Docker zamanlama
  dalgalanmalarında iki iteration düşürmesin diye 10'dan 20'ye çıkarıldıktan
  sonra `clean -Pload-test -Dtest=Stage9K6LoadTest test` komutuyla geçti.
  600/600 istek başarılı, 0 dropped iteration, p95 `12,42 ms` ve p99
  `958,31 ms` ölçüldü; HTTP hata oranı `%0` kaldı ve k6 eşikleri sağlandı.
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
- Aşama 10A hedefli `ContentCatalogIntegrationTest` gerçek PostgreSQL 17.5 ve
  Flyway V1–V9 ile 7 testte geçti; draft admin listesi, pagination, EDITOR
  erişimi ve USER reddi doğrulandı.
- `admin-web` içinde `npm run test` 4/4 testle; local rol koruması ve backend
  code/trace ID hata ayrıştırmasını doğruladı. `npm run build` TypeScript strict
  kontrolü ve Vite production build'iyle geçti; `npm install` audit sonucu
  0 vulnerability idi.
- `admin-web` görsel yenilemesi, katalog ve içerik detay akışını yeni API
  eklemeden editoryal tasarım diline taşıdı. `docs/FRONTEND_EXPERIENCE_PLAN.md`
  tasarım kararlarını, mobil daralma davranışını ve medya yükleme/kapak bağlama
  diliminin hâlâ ayrı onay gerektirdiğini kaydeder.
- Yenileme sonrası `npm run test` 4/4 testle geçti; `npm run build` TypeScript
  strict kontrolü ve Vite production build'iyle başarıyla tamamlandı. Yerel
  Vite sunucusu HTTP 200 verdi ve doğrulama sonunda kapatıldı. Bu çalışma
  ortamında tarayıcı otomasyonu izin nedeniyle açılamadığından ekran görüntülü
  görsel inceleme yapılamadı.
- Aşama 10B kullanıcı kataloğu, aynı Vite uygulamasına ayrı `user.html` giriş
  noktası olarak eklendi. Public katalog, içerik detayı ve içerik bazlı quiz
  listesi yalnız kullanıcı API yollarını kullanır; taslak, doğru cevap ve
  puanlama sonucu istemciye eklenmedi.
- `PublicApi` unit testi yayınlanmış katalog isteğinin `/api/v1/contents`
  yoluna USER header ile gittiğini doğruladı. `npm run test` 5/5 testle,
  `npm run build` ise iki HTML giriş noktasıyla başarıyla tamamlandı.
- Kullanıcı flow'u local deneme oturumu, soru başına 30 saniyelik server deadline
  sayacı, cevap/timeout geri bildirimi, sonuç, XP profil özeti ve
  global leaderboard ile tamamlandı. Cevap yeniden denemesinde aynı
  `Idempotency-Key` korunur; istemci doğru cevabı, skoru veya süreyi üretmez.
- `PublicApi` testi answer isteğinin caller-provided idempotency anahtarını ve
  aynı soru/şık payload'ını taşıdığını doğruladı. `npm run test` 6/6 testle,
  `npm run build` iki giriş noktasıyla başarıyla tamamlandı.
- Kullanıcı web sağlamlaştırması sonrası `npm run test` 4 dosyada 9/9 testle
  geçti. Yeni testler proje dokümanındaki UUID biçimini ve korumalı medya
  isteğinde USER kimlik header'larının taşındığını kanıtladı. `npm run build`
  TypeScript strict kontrolü ve iki giriş noktalı Vite üretim derlemesiyle
  geçti. Sayfa 390×844 mobil görünümde tarayıcıyla kontrol edildi; belgelenmiş
  UUID ile yerel giriş ve `Çıkış` akışı çalıştı. Backend çalışmadığı için katalog
  verisi yerine beklenen güvenli API hata kartı görüldü.
- Çalışan Vite geliştirme sunucusu tarayıcıda incelendi: anlamlı içerik ve yeni
  draft formu render edildi, form etiketleri bulundu, Vite hata katmanı ve
  console error görülmedi. Backend kapalıyken 502 yanıtı güvenli hata kartında
  gösterildi.
- Son `.\mvnw.cmd --batch-mode verify` çalışması mevcut Temurin 21 ile 105 testle geçti: 0 failure,
  0 error, 0 skipped. Gerçek PostgreSQL 17.5, RabbitMQ ve Redis Testcontainers
  senaryoları, ArchUnit sınırları, Flyway V1–V12 ve JAR paketleme doğrulandı.
- `questionRequiresExactlyFourOptions` domain testi üç seçenekli soruyu reddeder;
  `postgresqlConstraintsRejectDuplicateOrdersAndCorrectOptions` integration testi
  deferred constraint'in eksik seçenekli transaction'ı commit etmediğini kanıtlar.
  `everyStartedQuestionGetsExactlyThirtySeconds` ve
  `elapsedQuestionIsStoredAsTimedOutAndNextQuestionGetsNewDeadline` integration
  testleri sunucu deadline'ını ve timeout ilerlemesini korur. Frontend PDF modeli
  testi içerik/quiz kimliği ile işaretli doğru cevapların belgeye girdiğini kanıtlar.
- Çalışan admin sayfasında medya kütüphanesi ile bölüm kapsamının sezon/bölüm
  seçim zinciri; kullanıcı sayfasında güvenli boş yayın durumu denetlendi.
  İki sayfanın tarayıcı konsolunda hata veya uyarı görülmedi.

## Öğrenme odağı

Aşama 10I, kayıt oluşturmakla kullanıcıya yayınlamanın aynı işlem olmadığını
gösterir. İçerik katalog yayını ile quiz sürümü yayını ayrı güvenlik sınırlarıdır;
taslak çalışma yanlışlıkla kullanıcıya açılmaz. Admin ekranı bu iki adımı görünür
kılar. PDF tarafında yetkili medya isteği görseli belge içine gömer; soruya özel
görsel yoksa yayın anındaki içerik kapağı kullanılır. Quiz soru sayısı serbesttir,
değişmeyen kurallar her sorunun dört seçenekli ve 30 saniyelik olmasıdır.

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

Aşama 10A, tarayıcıdaki rol kontrolünün yalnız kullanıcı deneyimi olduğunu;
gerçek authorization'ın backend'de kalması gerektiğini gösterir. Liste ekranı
aggregate'in sezon/bölüm ağacını her satırda taşımayan özet DTO kullanır, detay
ekranı ise tam yönetim sözleşmesini okur. Vite proxy yerel same-origin kolaylığı
sağlar; production CORS/CSRF ve OIDC kararının yerine geçmez.

Aşama 10B, kullanıcı ve yönetici deneyimlerinin aynı backend'i kullanmasına
rağmen aynı ekran akışında olmak zorunda olmadığını gösterir. Kullanıcı
sayfasının yalnız yayınlanmış kaynakları okuması draft görünürlüğü sınırını;
quiz başlangıcından sonra server-authoritative attempt sözleşmesini kullanması
ise doğru cevap, süre ve skor güvenlik sınırını korur. Idempotency anahtarı,
ağ hatasında aynı cevap niyetinin ikinci bir kalıcı sonuç üretmemesini sağlar.

Aşama 10D, bir quizin “hangi içerikle ilgili?” bilgisini serbest metin yerine
doğrulanan bir ilişki olarak modeller. Domain geçerli kimlik kombinasyonunu,
application katmanı aidiyeti, PostgreSQL ise yarış veya uygulama hatasına karşı
kalıcı bütünlüğü korur. Değişmez medya kimliği aynı dosyanın yeniden yüklenmeden
birden fazla yerde kullanılmasını sağlar; binary veriyi liste cevabına gömmemek
sayfalama ve erişim kontrolü sınırlarını temiz tutar.

Aşama 10E, tarayıcı sayacı ile güvenilir deadline'ın aynı şey olmadığını gösterir.
Tarayıcı kalan süreyi sunar; sunucu geçerliliği kendi saatiyle belirler. Pessimistic
kilit cevap-timeout yarışını, idempotency aynı komutun tekrarını, deferred
PostgreSQL constraint'i ise transaction sonunda dört seçenek bütünlüğünü korur.
Yayınlanmış quiz sürümünü yerinde değiştirmemek, geçmiş attempt'lerin hangi soru ve
cevap anahtarıyla puanlandığını değişmez tutar.

Aşama 10F, içerik metadata'sı ile quiz cevap anahtarının aynı değişmezlik sınırında
olmadığını gösterir. Kapak veya açıklama operasyonel olarak düzeltilebilir; soru
ve doğru cevap geçmiş attempt'leri etkilediği için yayımlanmış quiz sürümü değişmez
kalır. Foreign key cascade henüz oynanmamış quiz verisini gerçek silerken, RESTRICT
ilişkisi attempt ve append-only XP geçmişinin yanlışlıkla yok edilmesini önler.

Aşama 10G, ürün dilindeki “silme” ile tarihsel kanıtı fiziksel olarak yok etmenin
aynı işlem olmadığını gösterir. Hiç yayınlanmamış quiz güvenle silinir. Yayınlanmış
quiz ise public görünümden kaldırılıp geçmişe taşınır; eski attempt'in soru ve
cevap anahtarı açıklanabilir, XP ledger ise bağımsız ve append-only kalır.

Aşama 10H, transaction sınırının kullanıcı deneyimini de belirlediğini gösterir.
Frontend'in 59 ayrı istek göndermesi kısmi başarı ve rate-limit tüketimi üretirken,
aggregate'i tek komutta değiştirmek ya bütün hiyerarşiyi kaydeder ya da hiçbirini
kaydetmez. PostgreSQL constraint'leri eşzamanlı yarışları, domain kuralları ise
iş anlamını korumaya devam eder.

## Aşama 10I - Görselli PDF ve yayın görünürlüğü

- “Diriliş Ertuğrul” kaydının backend'de içerik ve 11 soruluk quiz olarak `DRAFT`
  kaldığı doğrulandı; kullanıcı sayfasında görünmemesi beklenen yayın güvenliği
  davranışıdır.
- Admin detayına iki aşamalı görünürlük rehberi eklendi. İçerik katalogda değilken
  quiz yayın düğmesi nedenini açıklayarak kapalıdır.
- PDF her soruyu ayrı quiz sayfası biçiminde, 30 saniye göstergesi, ilerleme,
  görsel, dört cevap kartı ve işaretli doğru cevapla üretir. Soru görseli yoksa
  içerik kapağı kullanılır.
- On soru kapanış şartı kaldırıldı; yalnız kaydedilmemiş soru değişikliği varsa
  sayfadan ayrılma uyarısı verilir.
- `npm test -- --run` 19/19 geçti; `npm run build` TypeScript strict ve Vite
  production paketini başarıyla üretti. Backend kodu değişmediği için Maven testi
  gereksiz yere tekrarlanmadı.
- Gerçek admin sayfasında taslak içerik, 11 soru, açıklamalı yayın adımları ve
  devre dışı quiz yayın düğmesi doğrulandı. Kullanıcı API'sinin bu taslağı
  listelemediği ayrıca doğrulandı.

## 11.08.2026 katalog asistanı çalışması

- Admin panelinde mevcut 6 içerik kontrol edildi; aynı veya çok benzer kayıt bulunmadı.
- Resmî tabii detay sayfalarında doğrulanan 5 yeni içerik DRAFT olarak oluşturuldu:
  `Keloğlan Masalları` (`be89c216-9c12-4a81-9463-684217e2d534`), `Ayşe`
  (`356261f0-9e21-4f50-b823-8eb8499873ae`), `İzler ve Çizgiler`
  (`e8988d13-ffad-4e45-959f-cae9c28c7e6d`), `Kız Kulesi Masalları`
  (`7d2ecbac-4915-4fb5-9274-388d53e48b44`) ve `Aslan Hürkuş: Kayıp Elmas`
  (`8545eecc-9e18-4618-901f-74e03a01774f`).
- Dört SERIES ve bir FILM kaydı oluşturuldu; kapak yüklenmedi, quiz/soru/XP/leaderboard
  oluşturulmadı ve hiçbir kayıt yayımlanmadı.
- Resmî kaynak URL'leri bu görev raporunda korundu. Mevcut admin içerik sözleşmesinde
  source URL alanı bulunmadığı için bağlantılar panele ayrı metadata olarak yazılamadı.
- Panel kaydetme hatası oluşmadı. İlk form etkileşiminde form kapalıydı; kayıt oluşmadan
  form açılarak devam edildi ve tekrar gönderim yapılmadı.

## 11.08.2026 katalog asistanı genişletme çalışması

- Admin panelindeki toplam kayıt sayısı 11'den 31'e çıktı. Önceki turda kapaksız kalan 5 DRAFT kayıt (`Keloğlan Masalları`, `Ayşe`, `İzler ve Çizgiler`, `Kız Kulesi Masalları`, `Aslan Hürkuş: Kayıp Elmas`) resmî tabii kapaklarıyla güncellendi; mevcut 6 kaydın doğru kapakları değiştirilmedi.
- Dört parti hâlinde 20 yeni DRAFT oluşturuldu. Gerçek kategori dağılımı 5 Dizi, 5 Film, 5 Belgesel ve 5 Çocuk olarak dengelendi. Yapısal tür dağılımı 15 SERIES ve 5 FILM oldu. Yeni SERIES kayıtlarına sezon veya bölüm eklenmedi.
- 20 yeni detay sayfası yeniden açılarak başlık, kapak, alternatif metin ve `Kataloğa ekle` bekleyen durumu kontrol edildi. Kataloğa ekleme, yayımlama, silme, quiz/soru/XP/leaderboard işlemi yapılmadı.
- Kapaklar, resmî tabii detay sayfalarında görülen tabii medya kaynaklarından indirildi; WebP kaynakları panel kurallarına uyan JPEG'e dönüştürüldü. Kullanılan görsellerin alternatif metinleri görseller incelenerek yazıldı. Tüm yeni kayıtlarda kapak bağlı durumda.
- Doğrudan resmî detay URL'si alanı admin sözleşmesinde bulunmadığı için kaynak URL'leri görev raporunda tutuldu. `Çiykinley`, arama sonucu görünmesine rağmen doğrudan tabii sayfası 404 verdiği için eklenmedi ve resmî kaynağı yetersiz aday olarak raporlandı. `Aşk Tesadüfleri Sever 2`, resmî tabii İngilizce detay sayfasındaki Türkçe Original Title bilgisiyle adlandırıldı.
- Çalıştırılan kontroller: yerel admin panelinde tarayıcı tabanlı kayıt/kapak doğrulaması; toplam 31 kayıt, 20 yeni kayıt ve 5 önceki kapak güncellemesi. Uygulama kodu değişmediği için Maven/npm testi çalıştırılmadı.

## 11.08.2026 görsel kütüphanesi bakım değişikliği

- Kapak ve quiz soru formlarına gömülü sayfalı **Görsel kütüphanesi** kaldırıldı.
  Editör yeni görseli ilgili formdan yükleyip doğrudan kapağa veya soruya bağlar.
- Backend medya listeleme endpoint'i, mevcut medya kayıtları ve bağlı medya
  kimlikleri silinmedi; değişiklik yalnız admin web yüzeyini sadeleştirir.
- Kullanılmayan `MediaLibrary` bileşeni, listeleme istemci metodu ve ilgili stil/test
  kalıntıları kaldırıldı. Kapak yükleme, alternatif metin, korumalı görsel okuma ve
  PDF görsel okuma akışları korundu.
- `npm run test` 8 dosyada 18/18 testle geçti. `npm run build`, TypeScript strict
  kontrolünü ve iki giriş noktalı Vite production paketini başarıyla tamamladı.
  Büyük ve yalnız gerektiğinde yüklenen PDF parçaları için mevcut Vite boyut uyarısı
  devam ediyor; bu bakım değişikliğinin ürettiği yeni bir hata değildir.
- Katalog kartları yaklaşık 3:4, içerik detayı ise 2:3 çerçeve kullandığı için
  kapakların 3:4 tuval ve merkezde 2:3 güvenli alanla hazırlanması gerekir;
  katalog asistanı için anlamlı içeriği kırpmak yerine güvenli alan/kenar dolgusu
  kullanan görsel düzenleme talimatı hazırlandı.

## 11.08.2026 ilk tamamlama XP politikası

- Aynı `(userId, quizId)` için yalnız ilk tamamlanan attempt XP kazandırır. Sonraki
  çözümler engellenmez; skor hesaplanır fakat `earnedXp = 0` olan alıştırma olarak
  tamamlanır.
- `gameplay_attempts.earned_xp` API ile Outbox'ın aynı ödül kararını taşımasını,
  `gameplay_quiz_reward_claims` primary key'i ise iki farklı attempt'in aynı ödül
  hakkını almasını veritabanında engeller.
- Yeni completion olayları ve XP ledger satırları `FIRST_COMPLETION_SCORE_V2`
  kullanır. Eski `SCORE_MATCH_V1` olayları okunmaya devam eder. Migration geçmiş
  XP'yi geri almaz; geçmiş ilk tamamlamayı yalnız gelecekteki ödül hakkı sahibi
  olarak işaretler.
- Temiz Java 21 ana/test derlemesi geçti. İlgili domain, olay, gameplay ve
  messaging grubu 26/26; PostgreSQL, Redis, RabbitMQ ve mimari sınır testlerini
  içeren tam backend paketi 114/114 geçti.
- Kalıcı ürün kararı, alternatifler ve test eşleştirmesi ADR-0026'da kayıtlıdır.

### Birlikte bildirilen sorunların teşhisi ve sonraki dilimler

- Quiz ekranı: 5.8rem'e ulaşan soru başlığı, geniş dikey padding, tek sütun şıklar
  ve koyu kart yüzeyi taşma/okunabilirlik sorununun doğrudan nedenidir. Sonraki tek
  dilimde daha açık yüzey, kart arka planının kaldırılması, kontrollü başlık ölçüsü,
  masaüstünde 2x2 şık ve küçük ekran fallback'i uygulanmalıdır.
- İçerik arama: admin API yalnız sayfalı liste sunar. İsim araması istemcide mevcut
  sayfayı filtrelemek yerine backend'de normalize edilmiş, sayfalı başlık sorgusu
  olmalıdır.
- Sezon/bölüm: editör yalnız `SERIES + DRAFT` için gösterilir. Yayındaki dizinin
  hiyerarşisini immutable tutan karar nedeniyle bazı dizilerde alan görünmez.
  Ayrı bakım diliminde yayındaki diziye yalnız ekleme ile güncelleme/silmenin riskleri
  ayrıştırılmalıdır.
- Görünmeyen 25 kayıt bölüm değil, 25 yeni DRAFT içeriktir. Public katalog yalnız
  PUBLISHED kayıtları sunduğu için mevcut 6 yayın görünür. Toplu yayınlama yapılmadan
  önce SERIES kayıtlarının sezon/bölüm bütünlüğü editörce tamamlanmalıdır.
- Bağımsız quiz keşfi ürün açısından gereklidir. Public tarafta tek sayfalı quiz
  listeleme endpoint'i ve Quizler görünümü tasarlanmalıdır. İçerikleri tek tek
  dolaşan N+1 istemci yaklaşımı kullanılmamalı; bu aynı zamanda admin Quizler
  ekranındaki 429/rate-limit sorununu kalıcı olarak çözmelidir.

## 11.08.2026 quiz çözme yüzeyi iyileştirmesi

- Quiz oynama alanı koyu sayfadan ayrılan açık, nötr ve tek parça çalışma yüzeyine
  dönüştürüldü. Soru ve görselin arkasındaki ayrı renkli kart/çerçeve kaldırıldı.
- Görselli sorular geniş ekranda görsel–metin yan yana, dar ekranda alt alta
  yerleşir. Soru başlığı önceki 5.8rem üst sınırından yaklaşık 38px masaüstü ve
  25px mobil ölçüsüne indirildi.
- Dört şık masaüstünde 2×2, 590px altındaki küçük ekranlarda tek sütundur. A–D
  işaretleri ve açık cevap yüzeyleri seçenekleri daha kolay taranır hale getirir.
- `earnedXp = 0` sonuç metni artık “+0 XP kazandın” demez; çözümün alıştırma
  olduğunu ve ek XP vermediğini açıklar.
- Cevap sonrası sonuç yüzeylerinin kontrastı düzeltildi. Doğru cevap açık yeşil
  (`#dff3e7`) zemin ve koyu yeşil (`#164c39`) metin; yanlış cevap veya süre aşımı
  açık gül (`#f7e2e6`) zemin ve koyu bordo (`#652b38`) metin kullanır. `100 puan`
  ve `0 puan` başlıkları da yüzeyin okunabilir metin rengini devralır.
- `npm run test` 9 dosyada 20/20 geçti. `npm run build` TypeScript strict ve iki
  giriş noktalı Vite production paketini başarıyla üretti. Mevcut, yalnız PDF
  parçalarını ilgilendiren büyük chunk uyarısı devam ediyor.
- Gerçek kullanıcı akışında 1440×900 ve 390×844 viewport'lar doğrulandı. Her iki
  görünümde de içerik viewport yüksekliğine sığdı; masaüstünde 2×2, mobilde tek
  sütun seçenek düzeni ve temiz tarayıcı konsolu gözlendi.
- Gerçek quiz akışında hem doğru (`100 puan`) hem yanlış (`0 puan`) sonuç ekranı
  doğrulandı; zemin, metin ve büyük puan başlığı için yeni kontrast renklerinin
  tarayıcıda uygulandığı görüldü.

## 11.08.2026 admin içerik isminde arama

- `GET /api/v1/admin/contents` isteğe bağlı `query` parametresi alır. Application
  servisi metnin baş/son boşluklarını temizler; persistence adapter'ı başlığın
  herhangi bir bölümünde büyük/küçük harf duyarsız eşleşmeyi PostgreSQL üzerinde
  çalıştırır. Boş sorgu mevcut bütün-katalog davranışını korur.
- Filtre veritabanında sayfalamadan önce uygulanır. Böylece `totalItems`,
  `totalPages` ve sayfa öğeleri yalnız eşleşen kümeyi temsil eder; tarayıcı yalnız
  açık 20 kaydı süzmez. Sıralama `updatedAt DESC, id DESC` olarak korunur.
- İçerik yönetimine etiketli `İsme göre içerik ara` alanı, 300 ms debounce,
  temizleme düğmesi, eşleşme sayısı ve aramaya özel boş sonuç metni eklendi.
  Sorgu değiştiğinde liste ilk sayfaya döner. Public katalog değişmedi.
- PostgreSQL kullanan `ContentCatalogIntegrationTest` aramanın iki sayfaya yayılan
  sonuçlarını, büyük/küçük harf duyarsızlığını ve boşluk temizlemeyi kanıtlar.
  Hedefli katalog paketi 11/11; PostgreSQL, RabbitMQ, Redis ve mimari testleri
  içeren tam backend paketi 115/115 geçti.
- Frontend API testi sorgunun temizlenip URL-safe kodlandığını kanıtlar. `npm run
  test` 9 dosyada 21/21 geçti; `npm run build` TypeScript strict ve Vite production
  paketini üretti. Yalnız mevcut PDF chunk boyutu uyarısı devam ediyor.
- Gerçek admin ekranında arama alanı, klavye odağı, debounce sonrası istek,
  eşleşme sayısı görünümü ve temizleme düğmesi kontrol edildi. Açık yerel backend
  önceki süreç olduğu için yeni filtre davranışının veri doğrulaması izole
  PostgreSQL integration testinde yapıldı; uygulama yeniden başlatılınca yeni API
  kodu yerel ekrana da yüklenecektir.

## 11.08.2026 on puanlık skor ve doğru cevap özeti

- Geliştirme aşamasına uygun basit çözüm seçildi: mevcut `STANDARD_V1` doğru cevap
  başına doğrudan 10 puan verir. Yeni politika sürümü, enum veya migration eklenmedi.
  Yanlış cevap ve zaman aşımı sıfır puan kalır; puanın tek otoritesi `QuizAttempt`
  domain modelidir.
- İlk tamamlamada XP kesin skora eşittir; sonraki alıştırma çözümü skoru koruyup
  `earnedXp = 0` üretir.
- Quiz sonuç ekranı puanın altında kalıcı cevap kayıtlarındaki `correct` değerlerini
  sayarak `8 / 10 doğru` biçiminde doğru/toplam soru özetini gösterir. Hesap gerçek
  `totalQuestionCount` değerini kullanır; sabit on soru varsaymaz.
- `QuizAttemptTest`, doğru cevabın 10 puan olduğunu; `GameplayIntegrationTest`,
  `STANDARD_V1` snapshot'ını, iki doğru=20 puanı ve 20 XP'yi gerçek PostgreSQL ile
  kanıtladı. İlgili backend paketi 27/27 geçti.
- `UserApp.test.ts` değişken toplamda doğru sayısı metnini kanıtladı. Frontend
  paketi 9 dosyada 22/22 geçti; TypeScript strict ve Vite production build başarılı.
- Gerçek iki soruluk quiz akışında sonuç ekranının `1 / 2 doğru` gösterdiği,
  720px viewport yüksekliğine kaydırma olmadan sığdığı ve tarayıcı konsolunun temiz
  kaldığı doğrulandı. Backend yeniden başlatıldığında soru başına 10 puanlık güncel
  domain kodu yüklenecektir; veritabanı migration'ı gerekmez.

## 11.08.2026 yayımlanmış diziye sona ekleme

- Yayımlanmış `SERIES` içeriğinde yeni sezon ve bölüm ekleme açıldı. Yeni numara
  mevcut en büyük numaradan büyük olmalıdır; mevcut kayıtların güncellenmesi ve
  silinmesi yasak kalır.
- Bu sınır eski sezon/bölüm UUID'lerini koruduğu için `SEASON` ve `EPISODE`
  kapsamlı quizlerin anlamı değişmez. Mevcut aggregate transaction'ı, audit ve
  PostgreSQL unique/foreign key constraint'leri kullanılır; migration eklenmedi.
- Admin ekranı yayımlanmış dizilerde sezon planını ve her sezon için **Yeni bölüm
  ekle** formunu gösterir. Mevcut kayıtlar salt okunurdur.
- `ContentTest` domain ekleme-only kuralını; `ContentCatalogIntegrationTest` gerçek
  PostgreSQL üzerinde ekleme, kimlik koruma, public görünürlük ve düzenleme reddini
  kanıtladı. İlgili backend paketi 18/18 geçti.
- Frontend paketi 10 dosyada 24/24 geçti; TypeScript strict ve Vite production
  build başarılıdır.
- Kalıcı karar ve tüm hiyerarşiyi düzenlenebilir yapma alternatifinin riski
  ADR-0027'de kayıtlıdır.

## 11.08.2026 kullanıcı quiz keşif sayfası

- Kullanıcı menüsüne ayrı **Quizler** sekmesi eklendi. Kullanıma açık quizler ad,
  içerik/sezon/bölüm kapsamı ve soru sayısıyla tek listede gösterilir; karttan
  doğrudan quiz başlangıç ekranına geçilir.
- `GET /api/v1/quizzes` bütün yayımlanmış quizlerin yalnız güvenli özetini döndürür.
  Soru, seçenek ve doğru cevap alanları liste response'una eklenmedi.
- İçerik başına ayrı quiz isteği atmak rate-limit tüketimini artıracağı için tek
  özet isteği seçildi. Ayrıntı mevcut `GET /api/v1/quizzes/{quizId}` ile yalnız
  kullanıcı quizi açtığında yüklenir.
- `QuizAuthoringIntegrationTest` yayımlanmış quiz görünürlüğünü ve güvenli response'u
  gerçek PostgreSQL ile kanıtladı; ilgili backend paketi 14/14 geçti.
- Frontend paketi 10 dosyada 26/26 geçti; TypeScript strict ve Vite production
  build başarılıdır.

## 11.08.2026 kullanıcı dizi detayında sezon ve bölümler

- Kullanıcı dizi detayına quizlerden bağımsız **Sezonlar ve Bölümler** alanı
  eklendi. İlk sezon açık, diğer sezonlar kapalı başlar; böylece uzun bölüm
  listeleri sayfayı gereksiz uzatmaz.
- Sezon sayısı, toplam bölüm sayısı ve her sezonun bölüm sayısı gösterilir. Bölüm
  başlığı ile varsa açıklaması kullanıcı tarafından okunabilir.
- Sezon/bölüm verisi bulunmayan eski diziler hata üretmez; açıklayıcı boş durum
  gösterir. Filmlerde bu alan render edilmez.
- İçerik detayındaki sezon ve bölüm kapsamlı quizler, kimlik yerine gerçek
  `2. Sezon · 5. Bölüm` biçiminde etiketlenir; eşleşmeyen eski veri generic etikete
  güvenli biçimde geri döner.
- Frontend paketi 10 dosyada 28/28 geçti; TypeScript strict ve Vite production
  build başarılıdır. Backend sözleşmesi değişmedi.

## 11.08.2026 quiz kartında içerik adı

- Kullanıcı **Quizler** sayfasındaki her kart artık bağlı dizi veya film adını
  quiz adının üzerinde gösterir.
- Quiz özeti ile public içerik kataloğu `contentId` üzerinden istemcide eşleştirilir.
  Katalog 100 kayıtlık sayfalarla tamamen okunur; içerik başına ayrı istek atılmaz.
- Eşleşmeyen eski veya tutarsız kayıtta UUID yerine güvenli `İçerik` etiketi
  gösterilir. Backend ve veritabanı değişmedi.
- Frontend paketi 10 dosyada 30/30 geçti; TypeScript strict ve Vite production
  build başarılıdır.

## 11.08.2026 quiz keşif araması ve kapsam filtresi

- Kullanıcı **Quizler** sayfasında quiz başlığı, açıklaması veya bağlı dizi/film
  adına göre arama yapabilir.
- Liste içerik geneli, sezon ve bölüm kapsamına göre daraltılabilir; ekranda eşleşen
  quiz sayısı ve sonuç bulunamadığında açıklayıcı boş durum gösterilir.
- Arama Türkçe büyük/küçük harf dönüşümünü kullanır. Filtreleme zaten yüklenmiş
  güvenli quiz özetlerinde istemcide gerçekleşir; yeni API isteği, backend veya
  veritabanı değişikliği yoktur.
- `filterQuizDiscoveries` testleri içerik adıyla Türkçe aramayı, quiz metni aramasını,
  kapsam filtresini ve sonuç bulunamamasını korur.
- Frontend paketi 10 dosyada 32/32 geçti; TypeScript strict ve Vite production
  build başarılıdır.

## 11.08.2026 quiz keşif sıralaması

- Kullanıcı **Quizler** sayfasını bağlı içerik adına, quiz adına veya soru sayısına
  göre sıralayabilir. Varsayılan içerik sıralaması aynı dizi/filmin quizlerini bir
  arada tutar.
- Türkçe alfabetik karşılaştırma `Intl.Collator("tr-TR")` ile yapılır. Soru sayısı
  eşitse quiz adı kararlı ikincil sıralamadır.
- Public quiz özetinde yayın tarihi bulunmadığı için gerçeği yansıtmayan `En yeni`
  seçeneği eklenmedi. Yeni backend veya veritabanı değişikliği yapılmadı.
- `sortQuizDiscoveries` testleri üç sıralama seçeneğini ve kaynak listenin yerinde
  değiştirilmediğini korur.
- Frontend paketi 10 dosyada 34/34 geçti; TypeScript strict ve Vite production
  build başarılıdır. Mevcut PDF bağımlılıklarının chunk boyutu uyarısı sürer.

## 11.08.2026 kullanıcı içerik detayını quiz odaklı sadeleştirme

- Kullanıcı dizi detayındaki bağımsız **Sezonlar ve Bölümler** rehberi kaldırıldı.
  İçerik kapağı, açıklaması ve ilgili quizler doğrudan gösterilmeye devam eder.
- Sezon ve bölüm kayıtları silinmedi; backend, public response ve admin yönetimi
  değişmedi. Bu veriler sezon/bölüm kapsamlı quiz kartlarında `2. Sezon · 5. Bölüm`
  gibi anlaşılır kapsam etiketi üretmek için kullanılmaya devam eder.
- Kullanılmayan `ContentHierarchy`, `totalEpisodeCount`, ilgili test ve CSS kuralları
  kaldırıldı. Alternatif olarak rehberi kapalı bir açılır alanda tutmak mümkündü;
  quiz odaklı ürün amacı nedeniyle tamamen gizlemek seçildi.
- Kapsam etiketi testleri korunarak frontend paketi 10 dosyada 33/33 geçti;
  TypeScript strict ve Vite production build başarılıdır. Backend testi gerekmedi.

## 11.08.2026 yerel kota ve admin görsel istek verimliliği

- Rate-limit filtresinin bütün `/api/` isteklerini aynı IP kovasından düşürdüğü ve
  her korumalı kapak indirmesinin de token tükettiği doğrulandı.
- `local` Spring profilinde `app.rate-limit.enabled=false` yapıldı. Böylece yerel
  admin geliştirmesinde kota yoktur; varsayılan/production profillerindeki 300/dakika
  token bucket koruması değişmeden ve environment ile ayarlanabilir durumda kaldı.
- `MediaApi`, görsel blob isteklerini `contentUrl` ile oturum boyunca paylaşır. Aynı
  kapak liste ve detay arasında yeniden indirilmez; eşzamanlı render'lar tek HTTP
  isteğini bekler. Object URL yaşam döngüsü bileşende kaldığı için ekran kapanınca
  URL kaldırılırken paylaşılan binary blob bozulmaz.
- Başarısız istek cache'den silinir; ağ veya geçici 429 sonrasında yenileme gerçek
  bir tekrar denemesi yapar. Yeni kütüphane veya backend endpoint'i eklenmedi.
- `MediaApi` testleri tek indirmeyi ve hata sonrası tekrar denemeyi kanıtladı;
  frontend 10 dosyada 35/35 ve production build başarılıdır.
- `RateLimitingIntegrationTest` normal korumada 429 zarfını/başlıklarını,
  `TokenBucketRateLimiterTest` kota kurallarını korudu; hedefli backend toplamı 5/5
  geçti. Test için indirilen geçici Java 21 çalışma zamanı görev sonunda silindi.

## 11.08.2026 kullanıcı keşif sadeleştirmesi

- **Keşfet** sayfasına yalnız bir metin araması eklendi. Arama bütün yayınlanmış
  içeriğe istemcide uygulanır; tür veya durum filtresi yoktur.
- İçerik açıldığında kimliği aynı tarayıcıda küçük bir listede tutulur. Katalog son
  tıklanandan eskiye sıralanır; hiç açılmayan içeriklerin mevcut sırası korunur.
  Bu kayıt cihazlar arası profil geçmişi değildir.
- **Quizler** kapsam filtresi kaldırıldı. Kartlar bağlı içeriğin ana görselini
  gösterir; kart üstündeki renkli çizgi kaldırıldı. Metin araması ve üç sıralama
  seçeneği korunur.
- Public görseller aynı URL için tek blob isteğini paylaşır; liste ve quiz kartında
  tekrar kullanılan kapak gereksiz ek istek üretmez. Başarısız istek yeniden
  denenebilir. Backend, veritabanı ve API sözleşmesi değişmedi.
- Arama, son tıklama sırası, sayfalama ve medya istek paylaşımı testlerle korundu;
  frontend 10 dosyada 38/38 geçti ve production build başarılıdır. Vite'ın mevcut
  büyük PDF chunk uyarısı sürer.

## 11.08.2026 yerel demo kullanıcı ve gameplay verisi

- Sekiz sabit demo UUID ve hedef doğruluk oranı
  `docs/demo-data/demo-users.json` içinde tanımlandı.
- `ops/seed-demo-gameplay.ps1`, EDITOR sözleşmesinden yayın cevap anahtarını
  okuyup USER attempt/answer API'lerini kullanır. Böylece skor, ilk-tamamlama,
  Outbox, RabbitMQ ve XP akışı gerçek uygulama kurallarından geçer; SQL ile
  doğrudan veri yazılmaz.
- Script daha önce XP üretilmiş kullanıcıları atlar, XP consumer'ını en fazla on
  saniye kontrollü bekler ve Redis leaderboard projeksiyonunu yeniler.
- Çalışan local backend üzerinde sekiz demo kullanıcının verisi üretildi. Mevcut
  bir eski kullanıcıyla birlikte global leaderboard `9` katılımcı gösterdi;
  tekrar çalıştırmada sekiz demo kullanıcı da atlandı ve sayı değişmedi.
- PowerShell parser doğrulaması ve gerçek API akışı başarılıdır. Bu aşama
  production kullanıcı profili veya yüzdelik API alanı eklemez.

## 11.08.2026 sıralamayı profil içinde birleştirme

- Kullanıcı menüsündeki bağımsız **Sıralama** sekmesi kaldırıldı. Profil artık
  toplam XP, işlem sayısı, kullanıcının global sırası ve genel Top 10 listesini
  tek ekranda gösterir.
- Profil mevcut XP ve global leaderboard isteklerini paralel kullanmaya devam
  eder; backend, API veya veritabanı değişmedi. Eski `view=leaderboard` adresleri
  profile yönlenerek geriye uyumlu kaldı.
- Gelecekteki “bu diziyi diğerlerinden daha iyi biliyorsun” ölçümü için gereken
  kaynak veri zaten saklanıyor: attempt kullanıcı/quiz/sürüm/skoru, answer kaydı
  doğruluk bilgisini; quiz kaydı içerik aidiyetini taşır. XP bu ölçümün kaynağı
  olmayacaktır çünkü yalnız ilk tamamlamayı ve yönetici düzeltmelerini içerir.
- Kalıcı kolon/migration bugün gerekmez. Gelecekte ayrı bir performance sorgusu/API
  gerekir; önce `en iyi`, `ilk`, `son` veya `ortalama` attempt kuralı ve farklı
  soru sayılarının nasıl ağırlıklandırılacağı ürün kararı olarak seçilmelidir.
- Eski route yönlendirme testiyle frontend 10 dosyada 39/39 geçti; TypeScript
  strict ve production build başarılıdır.

## 11.08.2026 admin katalog tekrarını kaldırma

- Admin menüsündeki bağımsız **Katalog** kaldırıldı. Stüdyo logosu ve `/` adresi
  artık doğrudan **İçerik yönetimi** ekranını açar.
- Yalnız kapak listeleyen salt-okunur admin varyantı temizlendi. Yeni içerik
  oluşturma, isimle arama ve mevcut içeriği açma tek ekranda toplandı.
- Kullanıcı tarafındaki yayın kataloğu etkilenmedi. Backend, API ve veritabanı
  değişmedi.
- Frontend 10 dosyada 39/39 geçti; TypeScript strict ve production build başarılıdır.

## 11.08.2026 sıfır puanlı ilk tamamlamayı sıralamaya katma

- İlk tamamlamasında 0 puan alan kullanıcı artık `amount = 0` olan tek
  `QUIZ_COMPLETED` ledger kaydı üretir ve leaderboard katılımcısı sayılır.
- Sonraki alıştırmalar da `earnedXp = 0` döndürür; ancak yeni v2 integration event
  ilk-tamamlama hakkını ayrı taşıdığı için ikinci ledger kaydı üretmez.
- `quiz.completed` v2 deterministik yeni event kimliği ve
  `firstCompletionReward` alanı kullanır. Consumer eski v1 payload'larını okumaya
  devam eder; Outbox/Inbox idempotency ve rollback davranışı korunur.
- Flyway V14, geçmiş reward claim'lerinde bulunan eksik sıfır-puan ledger
  kayıtlarını deterministik UUID ve kaynak attempt referansıyla tamamlar.
- Java 21 ile önce 15/15 hedefli test, ardından PostgreSQL/RabbitMQ/Redis içeren
  120/120 tam `verify` paketi geçti. V14 eklendikten sonra migration, gameplay ve
  v2 sözleşmesini kapsayan son hedefli paket 22/22 geçti.

## 11.08.2026 yerel kullanıcı hesabı ve oturum

- Kullanıcı webindeki UUID kapısı kaldırıldı; e-posta/şifreyle giriş ve görünen
  adla hesap oluşturma eklendi. Kayıt olan hesap yalnız `USER` rolü alır.
- Şifre Spring Security delegating encoder üzerinden bcrypt özetiyle saklanır.
  Normalize e-posta PostgreSQL unique constraint'iyle korunur; aynı e-posta için
  `ACCOUNT_EMAIL_ALREADY_USED` kodlu `409` döner.
- Kayıt ve giriş, kimliği açıkça security context repository'ye kaydeden HttpOnly,
  SameSite=Strict sunucu oturumu başlatır ve session fixation'a karşı oturum
  kimliğini değiştirir. Çıkış oturumu geçersiz kılar.
- `CurrentActorProvider` sınırı değişmedi. Bu nedenle quiz attempt, cevap, XP ve
  leaderboard mevcut iş kurallarıyla hesap UUID'sine bağlanır.
- Alternatif JWT access/refresh yapısı, tek tarayıcı istemcisinde yenileme/iptal
  karmaşıklığı getireceği için eklenmedi. Kurumsal OIDC sözleşmesi bilinmediğinden
  production kimliği ayrıca ertelendi.
- `AccountAuthenticationIntegrationTest`; şifre hash'ini, oturum devamlılığını,
  genel hatalı-giriş yanıtını, logout'u, duplicate e-postayı ve zayıf şifreyi
  korur. Java 21 ile 25 sınıfta 123/123 tam backend testi geçti. Frontend 10
  dosyada 39/39 test ve production build başarılıdır.

## Sıradaki tek iş

Aşama 10AA tamamlandı. Kullanıcı yeni bir sorun veya roadmap dilimi belirtmeden
Aşama 11, production OIDC ya da sosyal özelliklere geçilmeyecektir. İlk manuel
kontrol, yerel kullanıcı sayfasından hesap açıp bir quiz çözerek profil XP'sini
aynı hesapla yeniden giriş sonrasında görmektir.

## Yeni Codex görevi için kısa komut

```text
Repo içindeki AGENTS.md ve docs/ altındaki proje belgelerini oku. Aşama 0–9 ile
Aşama 10A–10AA'nın tamamlandığını CURRENT_STATE, ROADMAP ve ADR-0019–ADR-0029
üzerinden doğrula. Kullanıcı yeni roadmap dilimini açıkça seçmeden XP,
leaderboard, production kimliği veya sosyal özellik ekleme.
```

## Bilinen riskler


- PDF gerçek dosya indirme kodu, belge modeli testi ve production build ile
  doğrulandı. Kullanıcı PDF sayfalarını resme dönüştürüp görsel inceleme izni
  vermediği için render tabanlı sayfa kalite kontrolü bu görevde yapılmadı.
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
  ve rol claim eşlemesi henüz belli değil. Yerel hesap/session ve geçici header
  adapter'ı production kimliği değildir; dış erişimden önce OIDC veya CSRF
  token'lı güvenli oturum, secure cookie ve CORS sözleşmesi gerekir.
- `admin-web` local actor header'ları production login değildir. Gerçek hosting
  topolojisi belli olunca OIDC, CORS, CSRF ve secret/config dağıtımı ayrıca
  tasarlanıp test edilmelidir.
- Admin kapak akışı yerel backend'in medya endpoint'ini kullanır; production
  object storage/CDN, telif/kullanım hakkı ve doğrudan görsel önizleme kararı
  henüz verilmedi. Dosya storage yazımı ile PostgreSQL metadata kaydı atomik
  değildir; sahipsiz dosya temizliği ayrı production dayanıklılık işidir.
- Mockito/Byte Buddy, Java 21 test koşusunda gelecekte varsayılan olarak
  engellenecek dinamik agent yükleme uyarısı veriyor; testler bugün geçiyor,
  ayrı bir test-tooling bakım görevinde explicit agent yapılandırması
  değerlendirilmeli.
- Attempt otomatik expire ve puanlama sürümü ürün kararları ilgili aşamalarda
  kesinleştirilmelidir.
- `FIRST_COMPLETION_SCORE_V2` ilk tamamlamada XP'yi sunucu skoruna eşitler, sonraki
  çözümlerde sıfır XP verir. Ürün ileride en-iyi-skor farkı, taban XP, bonus veya
  çarpan isterse yeni politika sürümü gerekir.
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
- Windows güç tasarrufu ve yerel Docker/WSL zamanlaması k6 p99 ve
  `dropped_iterations` değerlerini etkileyebilir. Performans karşılaştırması,
  güç modu `En iyi performans` ayarında ve ağır arka plan işleri kapalıyken
  tekrarlanmalıdır; bu durum uygulama davranışından ayrı test ortamı riskidir.
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
