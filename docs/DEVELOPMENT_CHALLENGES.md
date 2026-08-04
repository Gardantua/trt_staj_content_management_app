# Geliştirme Zorlukları, Beklenmedik Durumlar ve Çözüm Günlüğü

Son güncelleme: 04.08.2026

## Belgenin amacı

Bu belge, projenin yalnızca ulaştığı son durumu değil, geliştirme sırasında
karşılaşılan gerçek problemleri ve bu problemlerin nasıl çözüldüğünü kaydeder.
Final raporda “ne yaptık?” sorusunun yanında “neden böyle yaptık, ilk yaklaşım
neden yetmedi ve çözümün doğru olduğunu nasıl kanıtladık?” sorularına kaynak
olması amaçlanır.

Her kayıt aşağıdaki ayrımı korur:

- **Belirti:** Problemin dışarıdan nasıl göründüğü
- **Kök neden:** Asıl teknik veya ürün sebebi
- **Çözüm:** Uygulanan kalıcı yaklaşım
- **Kanıt:** Test, migration, ölçüm veya doğrulama sonucu
- **Rapor çıkarımı:** Bu deneyimden anlatılabilecek mühendislik dersi

Çözülmemiş konular ayrıca “Açık risk” olarak belirtilir. Bu belge yeni aşamalar
tamamlandıkça güncellenmelidir.

## Kısa kronoloji

| Aşama | Ana zorluk | Uygulanan çözüm | Durum |
|---|---|---|---|
| 0 | Sürüm, Java, Docker ve port uyumsuzlukları | Gerçek artifact sürümü, Java 21, proje portları ve wrapper | Çözüldü |
| 1 | Güvenilir kullanıcı kimliği olmadan sahiplik/yetki | Yalnız local/test geçici actor adapter'ı ve application portu | Geçici çözüm |
| 2–3 | Domain kuralı ile veritabanı bütünlüğünü birlikte korumak | Aggregate kuralları, Flyway constraint'leri ve gerçek PostgreSQL testleri | Çözüldü |
| 3 | Doğru cevap güncellenirken partial unique index sırası | Eski doğru bayraklarını merge öncesinde temizleyen adapter davranışı | Çözüldü |
| 4 | Tekrar ve paralel answer/complete istekleri | Idempotency-Key, unique constraint ve transaction | Çözüldü |
| 4.1 | Görsel soru ile erişilebilirlik arasında cevap sızması | Alt metin ve eşdeğer accessible prompt'u ayırmak | Çözüldü |
| 5 | Attempt ve XP arasında yarım sonuç/çift ödül | Append-only ledger, unique kaynak ve ilk aşamada ortak transaction | Çözüldü |
| 6 | PostgreSQL–RabbitMQ dual-write ve tekrar teslimat | Transactional Outbox, Inbox, retry ve DLQ | Çözüldü |
| 6 | Zaman hassasiyeti ve test profilinde broker health | Mikro-saniye normalizasyonu ve test health ayrımı | Çözüldü |
| 7 | “Sıralama” ürün kuralının belirsizliği | Ledger toplamı, deterministik tie-break ve ALL_TIME kapsam | Çözüldü |
| 7 | İçerik XP'sinin yanlış içeriğe bağlanabilmesi | `content_id` backfill, PostgreSQL trigger ve integration test | Çözüldü |
| 8 | Redis'in PostgreSQL sırasını korumaması ve N+1 nedeniyle yavaşlaması | Pozisyon skoru, toplu metadata okuması ve atomik generation | Çözüldü |
| 9 | HTTP ile asenkron consumer arasında trace'in kopması | Outbox'ta W3C context, RabbitMQ inject/extract ve eski satır uyumluluğu | Çözüldü |
| 9 | Yük testinin kimliksiz istekleri ve eksik p99 raporu | Local/test kimliği, açık trend istatistikleri ve ayrı load profili | Çözüldü |
| 9 | Güvenlik taramasının NVD rate limitine takılması ve gerçek açıklar | CycloneDX SBOM, OSV taraması ve güvenli patch override'ları | Çözüldü |
| 9 | Kesinti sırasında health timeout'u ve restore kanıtı ihtiyacı | Sınırlı probe, recovery veri kontrolü ve disposable restore provası | Çözüldü |

## Ayrıntılı problem kayıtları

### 1. Teknolojiyi iş davranışından önce ekleme riski

**Belirti:** RabbitMQ ve Redis proje hedeflerinde bulunuyordu; fakat quiz
tamamlama, XP ve leaderboard kuralları henüz kesin değildi. Hepsi aynı anda
eklenirse bir hatanın iş kuralından mı altyapıdan mı kaynaklandığını ayırmak
zorlaşacaktı.

**Kök neden:** Broker ve cache, iş davranışının kendisi değil; güvenilir teslimat
ve hızlı okuma gibi ayrı problemlerin araçlarıdır.

**Çözüm:** Proje aşamalara ayrıldı. Gameplay önce PostgreSQL'de, XP önce senkron
ledger olarak, leaderboard önce PostgreSQL'de doğrulandı. RabbitMQ Aşama 6'ya,
Redis Aşama 8'e bırakıldı.

**Kanıt:** Aşama 5 sonunda RabbitMQ olmadan 66 testle XP doğruluğu; Aşama 7
sonunda Redis olmadan 83 testle leaderboard doğruluğu kanıtlandı.

**Rapor çıkarımı:** Yeni teknoloji eklemek ilerleme göstergesi değildir. Önce
korunacak davranışı tanımlamak, sonra altyapının kattığı değeri ölçmek hata
ayıklamayı ve öğrenmeyi kolaylaştırır.

### 2. Spring Boot sürüm etiketinin Maven Central'da bulunmaması

**Belirti:** İlk build, `4.1.0.RELEASE` parent artifact'ini çözemedi.

**Kök neden:** Başlangıç metadata'sındaki sürüm etiketi Maven Central'daki gerçek
artifact adıyla uyuşmuyordu.

**Çözüm:** Parent sürümü yayımlanmış gerçek artifact olan `4.1.0` şeklinde
düzeltildi ve proje Maven Wrapper ile yeniden derlendi.

**Kanıt:** İlk smoke testlerden Aşama 7 sonundaki 83 testlik `verify` koşusuna
kadar aynı sürümle build ve çalıştırılabilir JAR üretimi başarılıdır.

**Rapor çıkarımı:** Framework sürümünü yalnız üretici metadata'sına güvenerek
değil, gerçek dependency repository ve build çıktısıyla doğrulamak gerekir.

### 3. Java, Docker ve yerel port ortamının beklenenden farklı olması

**Belirti:** Bazı terminaller Java 21 yerine Java 17 gördü veya `JAVA_HOME`
bulamadı. Host `5432` ve `8080` portları başka süreçler tarafından kullanılıyordu.
Bazı terminal oturumlarında Docker CLI PATH içinde görünmese de Docker Engine
Testcontainers tarafından erişilebiliyordu.

**Kök neden:** Ortam değişkenleri yeni terminallere farklı yansıyordu; geliştirme
makinesinde daha önce kurulmuş servisler varsayılan portları kullanıyordu.

**Çözüm:** Build doğrulamalarında Java 21 yolu açıkça kullanıldı. Proje
PostgreSQL için `5433`, uygulama için `8081`, RabbitMQ için `5673/15673` host
portlarına taşındı. Testcontainers Docker Engine'e doğrudan bağlanarak izole
container'lar kullandı.

**Kanıt:** Java 21.0.12 ile son `verify` başarılı; PostgreSQL 17.5 ve RabbitMQ
container testleri geçti.

**Rapor çıkarımı:** “Makinemde çalışıyor” yeterli değildir. Runtime sürümü,
portlar ve container bağlantısı komut çıktısıyla görünür hale getirilmelidir.

### 4. Production kimlik sağlayıcısı bilinmeden güvenli geliştirme ihtiyacı

**Belirti:** Content, gameplay ve XP kaynaklarının bir kullanıcıya ait olması
gerekiyordu; ancak kurum OIDC/JWT sözleşmesi henüz bilinmiyordu.

**Kök neden:** Kimliği ertelemek, kullanıcı ID'sini request body'den almak gibi
taklit edilebilir ve sonradan değiştirilmesi zor bir sözleşme oluşturacaktı.

**Çözüm:** `CurrentActorProvider` application portu tanımlandı. Geçici header
authentication yalnız `local` ve `test` profillerinde etkinleştirildi; varsayılan
profil bu header'larla kimlik üretmiyor. Roller `USER`, `EDITOR`, `ADMIN` olarak
ayrıldı ve hassas header değerleri loglanmadı.

**Kanıt:** Kimliksiz istek `401`, yanlış rol `403`, body ile başka kullanıcıyı
taklit etme denemesi başarısız ve varsayılan profilde test header'ı etkisizdir.

**Açık risk:** Kurum issuer, audience ve claim eşlemesi öğrenildiğinde gerçek
OIDC/JWT adapter'ı yazılmalıdır.

**Rapor çıkarımı:** Belirsiz dış entegrasyon, application katmanını belirsiz
bırakmak zorunda değildir; port sayesinde geçici adapter güvenli sınırda kalır.

### 5. Spring Boot 4 ile Jackson paket değişikliği ve artımlı build kalıntısı

**Belirti:** Eski örneklerdeki Jackson 2 paketleri mevcut dependency ağacıyla
uyuşmadı. Bir artımlı derlemede daha önceki eksik tiplerden bytecode kalıntısı
oluştu.

**Kök neden:** Spring Boot 4, Jackson 3'ün `tools.jackson` paketlerini kullanıyor.
Artımlı derleme de kaynak değişikliğinden sonra eski çıktıları tutabiliyordu.

**Çözüm:** Eski Jackson bağımlılığı eklemek yerine Boot'un yönettiği Jackson 3
API'leri kullanıldı. Şüpheli artımlı çıktı temiz build ile giderildi.

**Kanıt:** Temiz `verify` ve JSON API testleri başarılıdır.

**Rapor çıkarımı:** Eski blog/örnek kodu dependency ekleyerek zorla uyumlu hale
getirmek yerine framework'ün gerçek dependency ağacı incelenmelidir.

### 6. Domain doğrulaması ile PostgreSQL constraint'inin farklı görevleri

**Belirti:** Sezon/bölüm numarası, soru sırası, tek doğru seçenek ve aynı soruya
tek cevap gibi kurallar Java tarafında kontrol edilse bile paralel isteklerde
iki uygulama instance'ı aynı anda geçerli görebilirdi.

**Kök neden:** Domain kontrolü anlamlı hata üretir; ancak process'ler arası yarışın
son otoritesi değildir.

**Çözüm:** Kurallar aggregate davranışlarında erken kontrol edildi, ayrıca
unique/check/foreign-key constraint'leri Flyway migration'larına yazıldı. Gerçek
PostgreSQL integration ve concurrency testleri kullanıldı.

**Kanıt:** Tekrarlı sezon/bölüm/soru ve paralel aynı-answer denemelerinde
veritabanı yalnız bir geçerli satır bıraktı.

**Rapor çıkarımı:** Domain kuralı ile database constraint birbirinin alternatifi
değildir; biri açıklanabilir davranış, diğeri yarış koşullarında son savunmadır.

### 7. Doğru seçeneği değiştirirken partial unique index sıralaması

**Belirti:** Bir sorunun doğru cevabı A seçeneğinden B seçeneğine çevrilirken JPA
merge işlemi yeni doğru bayrağını, eski doğru bayrağını kapatmadan önce yazarsa
“bir soruda tek doğru seçenek” indeksi geçici olarak ihlal edilebiliyordu.

**Kök neden:** Son aggregate durumu geçerli olsa da SQL update sırası partial
unique index açısından kısa süreli iki doğru seçenek oluşturabiliyordu.

**Çözüm:** Quiz persistence adapter'ı aggregate merge öncesinde mevcut doğru
bayraklarını kontrollü olarak temizledi, flush/clear sonrasında yeni aggregate
durumunu kaydetti.

**Kanıt:** Doğru cevabı A'dan B'ye değiştiren gerçek PostgreSQL testi seçenek
kimliklerini koruyarak tek doğru index davranışını doğruladı.

**Rapor çıkarımı:** ORM'nin nesne grafiği sonunda doğru görünse bile veritabanı
ara durumları ve SQL sırası constraint'lerle birlikte düşünülmelidir.

### 8. Doğru cevabı korurken kullanıcıya anlık geri bildirim verme

**Belirti:** Doğru cevap quiz başlamadan gönderilirse kolayca incelenebilir;
hiç gösterilmezse kullanıcı istediği öğrenme/geri bildirim deneyimini alamaz.

**Kök neden:** Güvenlik ile kullanıcı deneyimi aynı API anında farklı beklentiler
oluşturuyordu.

**Çözüm:** Başlangıç ve soru DTO'larında doğru cevap alanı yapısal olarak yoktur.
Cevap transaction içinde değişmez olarak kaydedildikten sonra `correct`,
`correctOptionId` ve semantik `CORRECT/INCORRECT` durumu döner; aynı response
sonraki soruyu açar.

**Kanıt:** Contract testi başlangıç response'unda doğru alanının olmadığını;
gameplay integration testi cevap sonrasında doğru şıkkın gösterildiğini
kanıtladı.

**Rapor çıkarımı:** Hassas bilginin güvenliği yalnız alanı gizlemekle değil,
bilginin hangi durum geçişinden sonra açıklanacağını tasarlamakla sağlanır.

### 9. Görsel erişilebilirliğin doğru cevabı sızdırabilmesi

**Belirti:** Dağ fotoğrafı gibi görsel tanıma sorularında ayrıntılı alt metin
doğru cevabı açığa çıkarabilir; çok genel alt metin ise görme engelli kullanıcıya
eşdeğer soru sunmaz.

**Kök neden:** Klasik “her görsele alt metin ekle” kuralı, quiz güvenliği ve
eşdeğer deneyim için tek başına yeterli değildir.

**Çözüm:** Görselin `INFORMATIVE` veya `DECORATIVE` rolü, alternatif metni ve
cevabı sızdırmayan ayrı `accessiblePrompt` alanı tanımlandı. Soruya özel görsel
yoksa yayın anındaki içerik kapağı dekoratif fallback olarak sürüme sabitlendi.

**Kanıt:** Publish ve contract testleri bilgi taşıyan görselin gerekli metinler
olmadan yayınlanamadığını ve cevap sızdırmadığını doğruladı.

**Rapor çıkarımı:** Erişilebilirlik sonradan frontend'e eklenen bir etiket değil,
domain ve yayınlama sözleşmesini etkileyen bir veri tasarımı problemidir.

### 10. Dosya uzantısı ve bildirilen MIME türüne güvenilememesi

**Belirti:** Kullanıcı `.png` adı veya `image/png` header'ı gönderse bile içerik
başka formatta veya bozuk olabilirdi.

**Kök neden:** Dosya adı ve istemci MIME bildirimi güvenilir veri değildir.

**Çözüm:** JPEG/PNG imzası, çözülebilir görsel içeriği, boyut ve piksel sınırları
storage sınırında doğrulandı. SHA-256 bütünlük bilgisi ve değişmez medya kimliği
PostgreSQL'de tutuldu; gerçek dosya port arkasındaki storage adapter'ına yazıldı.

**Kanıt:** MIME imza uyuşmazlığı, 5 MB sınırı, yetki ve okuma testleri geçti.

**Açık risk:** Dosya storage yazımı ile PostgreSQL transaction'ı atomik değildir;
metadata yazımı başarısız olduğunda sahipsiz dosya temizliği production görevi
olarak durmaktadır.

**Rapor çıkarımı:** Upload güvenliği yalnız controller validation değil, binary
içeriğin güven sınırında doğrulanmasıdır.

### 11. Idempotency ile concurrency'nin aynı problem sanılması

**Belirti:** Ağ aynı request'i yeniden gönderebilir; ayrıca farklı idempotency
key'lerine sahip iki istek aynı soruya eşzamanlı cevap vermeye çalışabilir.

**Kök neden:** Idempotency aynı komutun tekrarını çözer; farklı komutların veri
yarışını çözmez.

**Çözüm:** Answer/complete isteklerinde `Idempotency-Key`, aynı key ve payload
için aynı sonucu döndürür. Aynı key/farklı payload conflict üretir. Ayrı paralel
istekler transaction ve `(attempt_id, question_id)` unique constraint'iyle
korunur.

**Kanıt:** Aynı key tekrar testi aynı response'u; farklı payload `409`'u;
paralel answer testi veritabanında tek answer satırını kanıtladı.

**Rapor çıkarımı:** Idempotency, optimistic locking ve unique constraint farklı
hata sınıflarını çözer; tek bir “duplicate kontrolü” başlığına indirgenmemelidir.

### 12. XP toplamını doğrudan güncellemenin geçmişi kaybettirmesi

**Belirti:** Kullanıcı tablosundaki tek bir `total_xp` alanını artırmak kolaydı;
ancak aynı attempt'in iki kez ödüllendirilmesini, düzeltmenin nedenini ve audit
geçmişini açıklamak mümkün olmayacaktı.

**Kök neden:** Toplam bir sonuçtur; hangi olayların o toplamı oluşturduğunu
saklamaz.

**Çözüm:** XP append-only ledger olarak modellendi. Completion kaydı attempt'e
ve deterministik reference key'e tekil bağlandı. Düzeltme, eski satırı değiştirmek
yerine ADMIN tarafından imzalanan pozitif/negatif yeni satır oluşturdu.

**Kanıt:** Tekrar ve paralel complete tek XP üretti; düzeltme özgün kazancı
değiştirmedi ve audit kaydı oluşturdu.

**Rapor çıkarımı:** İşlem defteri, hem idempotency kaynağı hem de denetlenebilir
geçmiş sağlar; toplam gerektiğinde kayıtlardan üretilir.

### 13. PostgreSQL ve RabbitMQ arasında dual-write boşluğu

**Belirti:** Attempt'i veritabanına kaydettikten sonra RabbitMQ publish başarısız
olursa XP olayı kaybolabilirdi. Önce publish edilirse de henüz commit olmamış bir
sonuç dışarı çıkabilirdi.

**Kök neden:** PostgreSQL commit'i ile broker publish'i tek atomik transaction
değildir.

**Çözüm:** Attempt sonucu ve Outbox satırı aynı PostgreSQL transaction'ında
yazıldı. Publisher bekleyen satırları `FOR UPDATE SKIP LOCKED` ile alıp kalıcı
mesaj gönderdi ve yalnız publisher confirm sonrasında yayımlandı işaretledi.

**Kanıt:** Broker kapalıyken attempt/Outbox kaybolmadı; broker açılınca olay
yayımlandı. Outbox conflict'i completion'ı geri alırken consumer hatası önceden
kesinleşmiş attempt'i bozmadı.

**Rapor çıkarımı:** Outbox broker'ı transaction'a katmaz; gönderilecek olayı
kalıcı doğru kaynakla aynı transaction'da saklayarak teslimat boşluğunu kapatır.

### 14. At-least-once teslimatta aynı olayın tekrar gelebilmesi

**Belirti:** RabbitMQ mesajı consumer işlemesine rağmen ack kaybolursa yeniden
teslim edebilir ve ikinci XP oluşturabilirdi.

**Kök neden:** At-least-once teslimat “en az bir”, “tam bir” teslimat değildir.

**Çözüm:** Consumer, `(consumer_name, event_id)` tekilliğine sahip Inbox claim'i
ile XP ledger yazımını aynı PostgreSQL transaction'ında gerçekleştirdi. XP
kaynak unique constraint'i ikinci savunma oldu. Üç deneme sonrası kalıcı hata
DLQ'ya taşındı.

**Kanıt:** Aynı event iki kez işlendiğinde tek Inbox ve tek XP kaydı kaldı;
geçersiz JSON üç denemeden sonra dead-letter queue'ya gitti.

**Rapor çıkarımı:** Exactly-once iddiası yerine at-least-once teslimat ve
idempotent consumer çoğu iş sistemi için daha gerçekçi ve test edilebilir bir
modeldir.

### 15. RabbitMQ entegrasyonunda framework API ve dependency sürprizleri

**Belirti:** İlk uygulamada ayrıca eklenen retry dependency'sinin sürümü Spring
Boot tarafından yönetilmedi. Publisher confirm API'si beklenen `Duration`
yerine milisaniye `long` parametresi istiyordu.

**Kök neden:** Kullanılan Spring AMQP 4 sürümünün dependency ve metot sözleşmesi,
eski örneklerden farklıydı.

**Çözüm:** Gereksiz explicit retry dependency'si kaldırıldı ve Boot/Spring
AMQP'nin mevcut retry altyapısı kullanıldı. Confirm bekleme çağrısı gerçek API
sözleşmesine uygun milisaniye değeriyle düzeltildi.

**Kanıt:** Derleme, publish confirm ve gerçek RabbitMQ integration testleri
başarılı oldu.

**Rapor çıkarımı:** Framework örnekleri sürüme duyarlıdır; çözüm rastgele yeni
dependency eklemek değil, kullanılan sürümün gerçek API/dependency yönetimini
incelemektir.

### 16. Test profilinde RabbitMQ health nedeniyle ilgisiz `503`

**Belirti:** RabbitMQ kullanmayan API integration testlerinde uygulama health
endpoint'i broker bağlantısı olmadığı için `DOWN/503` döndü.

**Kök neden:** Test profilinde publisher/listener kapalı olsa bile otomatik
RabbitMQ health indicator hâlâ broker'ı zorunlu bağımlılık sayıyordu.

**Çözüm:** Yalnız test profilinde RabbitMQ health indicator kapatıldı. Gerçek
RabbitMQ davranışı ayrı Testcontainers integration testlerinde tutuldu; normal
profil broker health bilgisini göstermeye devam etti.

**Kanıt:** Temel health testi tekrar `UP`; gerçek RabbitMQ publish/consume testleri
ayrıca başarılıdır.

**Rapor çıkarımı:** Bir test profilini kolaylaştırmak production gözlemlenebilirliğini
kapatmamalıdır; bağımlılık testi doğru test sınırına taşınmalıdır.

### 17. Deterministik Outbox olayında zaman hassasiyeti çakışması

**Belirti:** Tekrarlanan completion aynı deterministik event ID'yi üretmesine
rağmen Java `Instant` ile PostgreSQL'in sakladığı zaman hassasiyeti farklı
temsil edildiğinde payload eşitliği bazen conflict üretti.

**Kök neden:** PostgreSQL zaman değeri mikro-saniye hassasiyetindeyken JVM'deki
`Instant` daha hassas olabilir. Aynı iş olayı farklı serialization temsilleri
üretebiliyordu.

**Çözüm:** Integration event zamanı PostgreSQL mikro-saniye hassasiyetine
normalize edildi. Aynı `quiz.completed` olayı karşılaştırılırken event/attempt/
kullanıcı/quiz/skor/politika gibi sabit iş alanları esas alındı ve ilk saklanan
olay zamanı korundu.

**Kanıt:** Tekrarlanan ve paralel complete testleri tek Outbox olayıyla kararlı
biçimde geçti; 74 ve 83 testlik tam koşular başarılı oldu.

**Rapor çıkarımı:** Dağıtık sözleşmelerde zaman hassasiyeti de veri sözleşmesinin
bir parçasıdır; yalnız görünen timestamp değerine güvenilmemelidir.

### 18. Broker kesinti testlerinde korkutucu ama beklenen loglar

**Belirti:** RabbitMQ container bilinçli durdurulduğunda connection exception ve
listener restart logları görüldü; ilk bakışta test hatası gibi görünüyordu.

**Kök neden:** Testin amacı gerçekten bağlantıyı kesmek, retry ve recovery
davranışını gözlemlemekti. Beklenen altyapı hatası log üretir.

**Çözüm:** Test sonucu log seviyesinden değil, kalıcı durum ve assertion'lardan
değerlendirildi: Outbox pending kaldı, attempt tamamlandı, broker geri geldiğinde
publish ve XP gerçekleşti.

**Kanıt:** Kesinti/recovery testi ve tam `verify` sıfır hatayla geçti.

**Rapor çıkarımı:** Failure testlerinde exception logu başarısızlık anlamına
gelmez; kabul kriteri sistemin arıza sonrasındaki kalıcı durumudur.

### 19. Leaderboard'un teknik sorgudan önce ürün kuralı gerektirmesi

**Belirti:** “XP'ye göre sırala” ifadesi; en iyi attempt mi, bütün XP mi, eşit
puan ne olacak, adjustment nasıl sayılacak ve dönem ne olacak sorularını
cevaplamıyordu.

**Kök neden:** Veri yapısı seçimi ürün semantiğini kendiliğinden belirlemez.

**Çözüm:** `ALL_TIME`, bütün ledger toplamı, global/içerik kapsamı ve toplam XP
azalan → ilk XP zamanı artan → UUID artan deterministik sıra seçildi. Top N
yanında current user döndürüldü; küçük örneklem yüzdeliği ve arkadaş sıralaması
kapsam dışı bırakıldı.

**Kanıt:** Eşit XP, tekrar, adjustment, farklı içerik, current user ve boş sonuç
integration testleri geçti.

**Rapor çıkarımı:** Redis sorted set gibi bir araç seçmeden önce “doğru sıra”nın
PostgreSQL üzerinde tanımlanması yanlış davranışı hızlandırmayı önler.

### 20. İçerik leaderboard'unda XP aidiyetinin sessizce bozulma riski

**Belirti:** Global XP doğru olsa bile bir XP satırı yanlış `contentId` taşırsa
içerik leaderboard'u yanlış sonuç verebilirdi.

**Kök neden:** İçerik aidiyeti leaderboard performansı için XP satırına
denormalize edildi; denormalize veri kaynak attempt ile tutarlı tutulmalıydı.

**Çözüm:** Flyway V8 eski completion satırlarını attempt → quiz → content
ilişkisinden, adjustment satırlarını özgün transaction'dan geri doldurdu.
PostgreSQL trigger'ı yeni completion ve adjustment satırlarının kullanıcı/içerik
aidiyetini kaynak kayıtlarıyla doğruladı.

**Kanıt:** Yanlış içeriğe bağlanan XP insert'i PostgreSQL tarafından reddedildi;
global ve içerik sorguları 83 testlik tam doğrulamada geçti.

**Rapor çıkarımı:** Denormalizasyon okuma hızını artırabilir ama yeni bir
tutarlılık yükümlülüğü doğurur; kritik aidiyet yalnız application koduna
bırakılmamalıdır.

### 21. Leaderboard performansını Redis olmadan ölçme gereği

**Belirti:** Redis'in ne kadar değer katacağını söylemek için PostgreSQL
başlangıç ölçümü yoktu.

**Kök neden:** “Redis daha hızlıdır” ifadesi, veri hacmi ve sorgu planı olmadan
ölçülebilir bir proje sonucu değildir.

**Çözüm:** 2.000 kullanıcılık örneklem oluşturuldu. İçerik Top N sorgusu 25 kez
çalıştırıldı, p95 kaydedildi ve `EXPLAIN ANALYZE, BUFFERS` planı incelendi.
İçerik ve global sorgular için kapsayıcı indeksler eklendi.

**Kanıt:** Yerel örneklem p95 yaklaşık 5,45 ms; query plan execution yaklaşık
1,67 ms ve içerik leaderboard index'i kullanıldı.

**Açık risk:** Bu değer production kapasite garantisi değildir. Aşama 8'de aynı
veri ve sorguyla PostgreSQL/Redis önce-sonra karşılaştırması yapılmalıdır.

**Rapor çıkarımı:** Performans teknolojisi seçimi benchmark, veri dağılımı ve
query plan ile gerekçelendirilmelidir.

### 22. Dokümantasyon kaynakları ile türetilmiş dosyaların ayrışması

**Belirti:** Markdown belgeleri güncellendiğinde mevcut PDF/DOCX otomatik olarak
güncellenmiyor. Önceki kayıtlarda adı geçen bazı Markdown dosyaları çalışma
alanında bulunmadı.

**Kök neden:** Kaynak belge ile türetilmiş çıktı arasında otomatik pipeline yok;
eksik dosyaların gerçek içeriği de bilinmiyor.

**Çözüm:** Eksik dosyalar izin olmadan yeniden oluşturulmadı. Her aşamada
Markdown kaynaklar güncellendi; PDF/DOCX yeniden üretilmediyse bu açıkça
belirtildi. Görsel render/OCR işlemleri kullanıcı iznine bağlı tutuldu.

**Kanıt:** README ve doküman dizinindeki mevcut bağlantılar kontrol edildi;
eksik belgeler bilinen risk olarak kaydedildi.

**Rapor çıkarımı:** Dokümantasyon da sürümlü bir artifact zinciridir. Kaynak,
türetilmiş çıktı ve görsel kalite kontrolünün hangisinin güncel olduğu açıkça
belirtilmelidir.

### 23. Redis sorted set'in eşit XP sırasını kendiliğinden korumaması

**Belirti:** Toplam XP sorted set skoru yapılırsa eşit puanlı kullanıcıları
Redis üye değerine göre sıralıyor; PostgreSQL'in “ilk XP zamanı, sonra UUID”
kuralı kayboluyordu.

**Kök neden:** Veri yapısının doğal sıralaması ile ürünün üç alanlı sıralama
sözleşmesi aynı değildi.

**Denenen fakat yeterli olmayan yaklaşım:** XP'yi doğrudan skor yapmak kolay ve
alışılmıştı; fakat ilk katılım zamanını tek sayısal skora kayıpsız eklemek kırılgan
bir bileşik skor üretirdi.

**Çözüm:** PostgreSQL doğru sıralamayı `ROW_NUMBER` ile hesapladı. Redis sorted
set skoru bu benzersiz pozisyon, üye kullanıcı UUID'si oldu; XP ve zaman ayrı
hash metadata'sına taşındı.

**Kanıt/test:** Gerçek PostgreSQL/Redis tutarlılık testi aynı XP ve aynı zamanda
UUID tie-break sırasını ve Top N dışındaki current user pozisyonunu eşit buldu.

**Trade-off veya kalan risk:** Bir XP değişikliği çok sayıda pozisyonu
değiştirebildiği için incremental `ZINCRBY` yerine snapshot rebuild gerekir.

**Rapor çıkarımı:** Veri yapısı ürün kuralını tanımlamamalı; önceden tanımlanmış
doğru davranışı temsil etmelidir.

### 24. Redis'in ilk ölçümde PostgreSQL'den yavaş çıkması

**Belirti:** 2.000 kullanıcı örneğinde ilk Redis Top N p95 yaklaşık 16,8 ms,
PostgreSQL ise yaklaşık 4,0 ms ölçüldü.

**Kök neden:** Top 20 metadata'sı 20 ayrı hash isteğiyle okunuyordu. Hızlı veri
deposu kullanılmış olsa da N+1 ağ turu toplam gecikmeyi büyüttü.

**Denenen fakat yeterli olmayan yaklaşım:** Her sorted set üyesi için ayrı
`HGET`, kodu basit tuttu ancak performans hedefini karşılamadı.

**Çözüm:** Metadata `multiGet` ile tek toplu hash isteğinde alındı. Redis p95
yaklaşık 7,1 ms'ye indi.

**Kanıt/test:** Aynı Testcontainers ortamında aynı 2.000 kullanıcı ve 25 okuma
ile önce/sonra ölçümü çalıştırıldı; tam doğrulamadaki performans testi geçti.

**Trade-off veya kalan risk:** Redis yerel küçük örnekte hâlâ PostgreSQL'den
hızlı değildir. Değeri bu ölçekte hız değil, PostgreSQL okuma yükünü ayıran
yeniden üretilebilir projection sınırıdır. Aşama 9'da uygulama HTTP kapasitesi
ayrıca k6 ile ölçüldü; Redis karşılaştırması gerçek production trafiği ve veri
dağılımıyla yeniden kalibre edilmelidir.

**Rapor çıkarımı:** “Redis hızlıdır” teknoloji seçimi için kanıt değildir;
komut sayısı, ağ turu ve gerçek veri dağılımı ölçülmelidir.

### 25. Yarım rebuild ve uzun transaction riski

**Belirti:** Redis anahtarları yerinde silinip yeniden yazılırsa okuyan istek
yarım liste görebilirdi. PostgreSQL snapshot transaction'ı Redis yazımı boyunca
açık tutulursa veritabanı bağlantısı gereksiz süre işgal edilirdi.

**Kök neden:** PostgreSQL snapshot bütünlüğü, Redis yayınlama atomikliği ve iki
sistemin transaction sınırı farklı problemlerdir.

**Çözüm:** Global/içerik snapshot kısa read-only transaction'da belleğe alındı
ve transaction kapandı. Yeni Redis generation tamamen hazırlandıktan sonra tek
`active` işaretçisi değiştirildi; eski generation best-effort temizlendi. Eksik
veya erişilemeyen generation PostgreSQL fallback üretir.

**Kanıt/test:** Tekrarlı rebuild eski generation'ları biriktirmedi; Redis
container'ı duraklatıldığında API PostgreSQL sonucunu döndürdü ve XP aynı kaldı.
Tam doğrulama 89/89 testle geçti.

**Trade-off veya kalan risk:** İşaretçi değişiminden hemen önce okuma yapan bir
istek, eski generation temizliğiyle yarışırsa bir defalık fallback görebilir;
yanlış sonuç veya veri kaybı görmez. Beş saniyelik yenileme stale pencere yaratır.

**Rapor çıkarımı:** Dağıtık atomiklik her sistemi tek transaction'a sokmak
değildir; immutable generation, atomik pointer ve güvenli fallback ile yarım
durum gözlenmesi engellenebilir.

### 26. Korelasyon kimliği ile dağıtık trace kimliğinin karıştırılması

**Belirti:** HTTP isteği bir trace üretiyor, fakat Outbox publisher ve XP
consumer yeni ve birbirinden bağımsız trace'ler başlatıyordu. Outbox'ta yalnız
32 karakterlik trace ID tutmak parent-child ilişkisini kurmaya yetmiyordu.

**Kök neden:** Kullanıcının gönderdiği `X-Trace-Id` destek amaçlı korelasyon
değeridir. OpenTelemetry'nin örnekleme bayrağı, parent span kimliği ve vendor
durumunu taşıyan W3C `traceparent`/`tracestate` sözleşmesinin yerine geçmez.

**Denenen fakat yeterli olmayan yaklaşım:** Aynı trace ID'yi log ve event
alanına kopyalamak log aramasını kolaylaştırdı, fakat gözlem sisteminde span
ağacı oluşturmadı.

**Çözüm:** Aktif context, attempt ve Outbox'ın ortak transaction'ında nullable
W3C alanlarına yazıldı. Publisher bu parent'ı çıkarıp PRODUCER span başlattı;
RabbitMQ header'ına enjekte edilen context consumer tarafından çıkarılıp
CONSUMER span'a bağlandı. V9 alanları nullable bırakılarak eski Outbox satırları
rolling deployment sırasında geçerli tutuldu.

**Kanıt/test:** Gameplay integration testi trace ID ile `traceparent` eşliğini,
messaging testi gerçek RabbitMQ publish/consume akışını ve W3C alanı olmayan eski
satırın hâlâ yayımlanabildiğini doğruladı.

**Trade-off veya kalan risk:** Context propagation iz sürekliliğini sağlar;
Tempo retention'ı, sampling ve production exporter ağı kurum altyapısıyla
kesinleştirilmelidir.

**Rapor çıkarımı:** Correlation ID bir arama anahtarıdır; dağıtık trace context'i
ise nedensel parent-child ilişkisini taşıyan protokol sözleşmesidir.

### 27. İlk k6 ölçümünün hızlı ama tamamen yanlış olması

**Belirti:** İlk bir dakikalık baseline'da p95 `8,57 ms` görünmesine rağmen 600
isteğin tamamı başarısızdı. Ayrıca k6 eşik olarak p99'u kontrol ediyor fakat
varsayılan JSON summary sayısal p99 değerini yazmıyordu.

**Kök neden:** Korunan content API'sine local/test actor header'ları
gönderilmediği için ölçülen şey veritabanı okuması değil hızlı `401` yanıtıydı.
Yalnız latency'ye bakmak hatalı koşuyu başarılı gibi gösterebilirdi.

**Denenen fakat yeterli olmayan yaklaşım:** Yalnız `http_req_duration` eşikleri
tanımlamak performansı ölçtü, fakat iş yolunun başarıyla tamamlandığını kanıtlamadı.

**Çözüm:** Senaryo geçici USER kimliğini yalnız local/test akışında gönderdi;
HTTP status, response trace header'ı, hata oranı, dropped iteration ve
`summaryTrendStats` içindeki p95/p99 birlikte kontrol edildi. Baseline, ramp,
spike ve kısa soak aynı endpoint yolunu paylaşacak biçimde tanımlandı.

**Kanıt/test:** Düzeltilmiş baseline gerçek PostgreSQL ve tek uygulama örneğinde
601/601 başarılı istek, `%0` hata, `0` dropped iteration, p95 `15,75 ms` ve p99
`332,82 ms` üretti.

**Trade-off veya kalan risk:** Bu tek makine ve boş/küçük veri baseline'ıdır;
150 istek/s spike veya production kapasite garantisi değildir. Ramp, spike ve
soak hedef ortamda release öncesi tekrar çalıştırılmalıdır.

**Rapor çıkarımı:** Performans testi yalnız “kaç istek attık?” değildir; doğru
iş yolunu çalıştırdığı ve yanıtların başarılı olduğu kanıtlanmadan latency
sayısının anlamı yoktur.

### 28. Dependency taramasının dış veri kaynağına bağımlılığı

**Belirti:** OWASP Dependency-Check ilk NVD indirmesinde uzun süre çalıştı ve
NVD `429` rate limit'i nedeniyle sonuç üretmeden durdu. OSV'nin doğrudan
`pom.xml` çözümü de Maven Central rate limit'i ve yönetilen boş sürümler nedeniyle
transitive ağacı güvenilir çıkaramadı.

**Kök neden:** Tarayıcı hem dependency resolution hem vulnerability verisini
aynı anda dış servislerden kurmaya çalışıyordu. Böylece güvenlik kapısının sonucu
ürün bağımlılıklarından çok üçüncü taraf rate limit'ine bağlı hale geldi.

**Denenen fakat yeterli olmayan yaklaşım:** NVD API anahtarı olmadan uzun retry
beklemek ve OSV'ye yalnız kaynak `pom.xml` vermek deterministik değildi.

**Çözüm:** Maven'in yerel ve kesin dependency graph'ından 164 bileşenli
CycloneDX SBOM üretildi; OSV yalnız bu sabit envanteri taradı. İlk taramada
Netty, PostgreSQL JDBC ve Jackson için dört düzeltilebilir bulgu çıktı. Spring
Boot BOM'u korunarak güvenli patch sürümleri override edildi. CI'daki OSV ve
Gitleaks referansları tam commit SHA'sına sabitlendi.

**Kanıt/test:** Patch sonrasında aynı 164 bileşenli SBOM taraması `No issues
found` verdi; ardından 97 testlik `clean verify` bütünüyle geçti.

**Trade-off veya kalan risk:** Açık veritabanı zamanla değişir; temiz sonuç
yalnız tarama anını ifade eder. Dependabot ve her CI çalışmasındaki yeniden
tarama bu yüzden gereklidir.

**Rapor çıkarımı:** Güvenlik taramasının kendisi de tekrarlanabilir bir veri
akışıdır; önce dependency envanterini deterministik üretmek, sonra açığı eşlemek
failure mode'ları ayırır.

### 29. PostgreSQL hard pause sırasında health kontrolünün de askıda kalması

**Belirti:** PostgreSQL container'ı duraklatıldığında health endpoint'i her
zaman hızlı `503` üretmedi; havuzdaki mevcut JDBC bağlantısı yüzünden istemci
beş saniyelik timeout'a ulaştı.

**Kök neden:** Yeni bağlantı alma timeout'u ile var olan TCP bağlantısında
başlatılmış sorgunun socket/query timeout'u aynı sınır değildir.

**Çözüm:** Failure testi `DOWN/503` veya sınırlandırılmış probe timeout'unu
erişilemezlik olarak kabul etti, fakat container geri geldiğinde health'in
yeniden `200` olmasını ve önceden commit edilen marker'ın korunmasını ayrıca
zorunlu tuttu. İkinci prova `pg_dump` backup'ını ayrı disposable veritabanına
restore edip Flyway geçmişi ve marker verisini okudu. İlk RPO/RTO hedefleri ve
restore sırası operasyon rehberine yazıldı.

**Kanıt/test:** İki operasyon integration testi gerçek PostgreSQL 17.5
container'ında geçti; final `clean verify` 97/97 başarılı oldu.

**Trade-off veya kalan risk:** Production readiness/liveness timeout'ları
deployment platformunda uygulama ve altyapı ağ davranışına göre ayarlanmalıdır.
Yerel restore süresi, production `RTO ≤ 60 dakika` hedefinin sağlandığını tek
başına kanıtlamaz.

**Rapor çıkarımı:** “Dependency kapalı” testi yalnız hata koduna bakmamalı;
tespit süresi, recovery ve commit edilmiş verinin bütünlüğünü birlikte ölçmelidir.

### 30. Rate limiter'ın kendisinin abuse yüzeyi oluşturması

**Belirti:** IP başına ayrı bucket tutmak basit görünse de sınırsız farklı IP
anahtarı belleği büyütebilirdi. Ayrıca doğrudan `X-Forwarded-For` kullanmak,
güvenilir proxy sınırı yokken saldırganın her istekte yeni kimlik üretmesini
sağlardı.

**Kök neden:** Abuse kontrolü yalnız istek sayısını değil, anahtarın kim
tarafından doğrulandığını ve limiter state'inin ne kadar büyüyebileceğini de
tanımlamalıdır.

**Çözüm:** İlk MVP limiter'ı `request.getRemoteAddr()` kullanır, proxy header'ına
güvenmez, en fazla 10.000 ayrı istemci bucket'ı izler ve sınırdan sonraki
istemcileri ortak overflow bucket'ında toplar. Idle bucket'lar periyodik
temizlenir; bucket seçimi eşzamanlı eklemelerde sınırın aşılmaması için atomik
kritik bölümle korunur.

**Kanıt/test:** Unit test burst/refill, istemci izolasyonu ve overflow kotasını;
API testi üçüncü isteğin `429`, `RATE_LIMIT_EXCEEDED`, `Retry-After`, RateLimit
header'ları ve trace ID ürettiğini doğruladı.

**Trade-off veya kalan risk:** Kota instance bazlıdır ve restart ile sıfırlanır.
Güvenilir load balancer gerçek istemci adresini normalize etmeli; yatay ölçekte
global kota gerekirse gateway veya dağıtık limiter seçilmelidir.

**Rapor çıkarımı:** Rate limiter güvenlik kontrolüdür ama sınırsız state veya
spoof edilebilir kimlik kullanırsa yeni bir kaynak tüketimi açığına dönüşebilir.

## Açık teknik uyarılar ve gelecek rapor konuları

- Mockito/Byte Buddy Java agent'i bugün testleri geçiriyor; gelecekte JDK'nın
  dinamik agent yükleme davranışı değişmeden önce explicit test agent ayarı
  değerlendirilmelidir.
- Production OIDC/JWT claim sözleşmesi henüz bilinmiyor.
- Production media storage/CDN, sahipsiz dosya temizliği ve kullanım hakları
  belirlenmedi.
- RabbitMQ TLS, secret yönetimi ve kurum queue alarm kanalı belirlenmedi. DLQ
  replay ile Outbox/Inbox başlangıç retention politikası `OPERATIONS.md` içinde
  tanımlandı; otomatik purge işi henüz uygulanmadı.
- Redis stale penceresi, rebuild süresi ve fallback oranı için izlenecek SLI'lar
  tanımlandı; kesin production alarm eşikleri gerçek trafikle kalibre edilmelidir.
- Dönemsel leaderboard, hile/diskalifiye, kullanıcı görünen adı/avatarı ve
  arkadaş kapsamı ayrı ürün kararlarıdır.
- 10 istek/s baseline ve ilk SLO koruma sınırları ölçüldü; gerçek trafik hedefi
  bilinmediği için production kapasite garantisi değildir.
- KVKK retention süreleri başlangıç mühendislik politikasıdır; production
  öncesinde hukuk/veri sorumlusu onayı ve kimlik sağlayıcısıyla doğrulanmış
  silme/anonimleştirme iş akışı gerekir.
- Observability Compose dosyaları doğrulandı; production scraper/exporter ağı,
  secret yönetimi ve kurum alarm kanalı henüz belirlenmedi.

## Yeni problem ekleme şablonu

Yeni bir beklenmedik durum yaşandığında aşağıdaki yapı kopyalanmalıdır:

```text
### Başlık

Durum: Çözüldü / Geçici çözüm / Açık risk
Aşama ve tarih:

Belirti:
Kök neden:
Denenen fakat yeterli olmayan yaklaşım:
Çözüm:
Kanıt/test:
Trade-off veya kalan risk:
Rapor çıkarımı:
İlgili dosya/ADR:
```

## Final raporda önerilen kullanım

Bu günlük rapora bütünüyle kopyalanmamalıdır. Final raporda aşağıdaki anlatı
seçilebilir:

1. Ortam ve framework uyumsuzluklarından iki somut örnek
2. Domain kuralı + database constraint birlikteliğinden bir örnek
3. Idempotency/concurrency ayrımından bir örnek
4. Outbox/Inbox ile çözülen dağıtık sistem problemi
5. Erişilebilirlik ile cevap güvenliği arasındaki tasarım problemi
6. PostgreSQL leaderboard doğruluğu ve Redis öncesi/sonrası ölçüm
7. W3C trace continuation, hatalı ilk k6 koşusu ve p95/p99 doğrulaması
8. SBOM taramasının bulduğu açıklar ile dependency patch süreci
9. Kesinti, backup/restore, RPO/RTO ve çözülmemiş production riskleri

Her örnekte problem, yanlış/eksik ilk varsayım, uygulanan çözüm ve testi birlikte
vermek raporu yalnız özellik listesi olmaktan çıkarıp mühendislik süreci haline
getirecektir.
