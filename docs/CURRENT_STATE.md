# Güncel Proje Durumu

Son güncelleme: 30.07.2026

## Genel durum

Projenin ürün fikri, önerilen mimarisi ve aşamalı geliştirme planı hazırdır.
Aşama 0 yerel ortamda tamamlandı: minimal Spring Boot uygulaması, PostgreSQL,
Flyway, health endpoint, temel hata zarfı, Testcontainers smoke testi ve
başlangıç CI dosyası çalışır durumdadır. Henüz identity, content, quiz veya
gameplay iş davranışı yazılmadı.

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

## Henüz tamamlanmayanlar

- Java/Spring Boot seçiminin backend lead veya kurum standardıyla doğrulanması
- Git deposunun bir uzak GitHub/GitLab deposuna bağlanması
- GitHub Actions pipeline'ının gerçek hosted CI ortamındaki ilk çalışması
- Aşama 1 kimlik ve erişim davranışları

## Mevcut teknoloji temeli

- Java 21 + Spring Boot 4.1.0
- PostgreSQL 17.5
- Maven Wrapper
- Flyway
- Docker Compose
- Testcontainers

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

### Bilinçli olarak ayrıca kurulmadı

- Global Maven/Gradle: Build tool seçildikten sonra proje içindeki wrapper
  kullanılacak.
- Windows PostgreSQL: PostgreSQL, Docker Compose ile container olarak
  çalıştırılacak.
- RabbitMQ ve Redis: Çekirdek quiz aşamalarına gelmeden kurulmayacak.

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

## Öğrenme odağı

Aşama 0; Maven build lifecycle, dependency injection başlangıcı, environment
tabanlı configuration, Flyway migration, health endpoint, trace ID ve gerçek
PostgreSQL ile integration test kavramlarını gösterir. Kullanıcı Aşama 1'den
önce bu akışı çalıştırıp temel dosyaların görevini açıklayabilmelidir.

## Sıradaki tek iş

Yeni terminalde README'deki Aşama 0 komutları kullanıcı tarafından tekrar
çalıştırılmalı ve temel dosyaların görevi birlikte gözden geçirilmelidir.

Bu öğrenme kontrolü ve kullanıcı onayı olmadan Aşama 1'e geçilmemelidir.

## Yeni Codex görevi için kısa komut

```text
Repo içindeki AGENTS.md ve docs/ altındaki proje belgelerini oku.
Aşama 0 iskeletini ve testlerini kullanıcıya öğretici biçimde açıkla.
Kullanıcının README'deki komutları tekrar çalıştırmasına yardım et.
Kullanıcı açıkça onaylamadan Aşama 1'e geçme.
```

## Bilinen riskler

- Kurumun mevcut teknoloji standardı henüz bilinmiyor.
- GitHub Actions dosyası yerelde hazır olsa da uzak depoda henüz çalıştırılmadı.
- Kapsamın canlı TV, eğitim ve sosyal özelliklerle erken büyüme riski var.
- Message broker ve Redis'in çalışan çekirdek sistemden önce eklenme riski var.
- Gerçek TRT/tabii sistemlerine entegrasyon yetkisi veya sözleşmesi henüz yok.
- Authentication yöntemi ve kurum kimlik sağlayıcısı henüz belli değil.
- Attempt süre/expire, tekrar çözme, puanlama sürümü ve leaderboard tie-break
  ürün kararları ilgili aşamalarda kesinleştirilmelidir.
- Gerçek trafik hedefi bilinmediği için kapasite değerleri henüz varsayım olarak
  bile sabitlenmedi.
- Önceki durum kaydında oluşturulduğu belirtilen `docs/PROJECT_PITCH.md` ve
  `docs/java.md` dosyaları mevcut çalışma alanında bulunmuyor. Yanlış hedefe
  yönlendirmemek için README ve `bwl.md` içindeki bozuk bağlantılar kaldırıldı.
  Dosyalar ayrı bir görevde yedekten geri getirilmeli veya yeniden
  oluşturulmalıdır.
