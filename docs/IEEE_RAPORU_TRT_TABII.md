# TRT tabii İçerikleri İçin Sunucu Otoriter, Modüler Monolit ve Olay Güdümlü Etkileşim Backend Platformu

**Server-Authoritative, Modular Monolith and Event-Driven Engagement Backend Platform for TRT tabii Content**

**Yazar Bilgileri:**  
Yunus Emre Arı  
Bilgisayar Mühendisliği Bölümü, Kocaeli Üniversitesi, Kocaeli, Türkiye  
yunuseemreari@gmail.com  

---

## Özetçe (Abstract in Turkish)
Bu çalışma, TRT tabii dijital yayın platformundaki dizi, film ve program içeriklerinin pasif izleme deneyiminden çıkarılarak quiz ve oyunlaştırma yoluyla etkileşimli hale getirilmesini sağlayan, kurumsal ölçeklenebilirlikte ve API-first bir backend platformunun mimari tasarımını, gerçeklenmesini ve canlı dağıtımını sunmaktadır. Geleneksel istemci odaklı quiz sistemlerindeki doğru cevap sızıntıları, süre manipülasyonları, ağ kesintilerinde çift işlem (duplicate request) riskleri ve yüksek trafikte sıralama (leaderboard) sorgularının getirdiği darboğazlar; sunucu otoriter (server-authoritative) oyun motoru, transactional outbox/inbox desenli asenkron mesajlaşma ve generation tabanlı Redis önbellek modeliyle çözülmüştür. Çekirdek sistem, dağıtık transaction karmaşasından kaçınmak amacıyla Clean Architecture prensipleriyle modüler monolit olarak tasarlanmış; okuma-yoğun sıralama yükü ise bağımsız ölçeklenebilen bir Spring Boot mikroservisine ayrıştırılmıştır. Geliştirilen sistem Oracle Cloud Infrastructure üzerinde Caddy ters vekiliyle 8 izole Docker konteyneri halinde canlıya alınmış; 130 backend, 47 frontend ve gerçek Testcontainers senaryolarıyla doğrulanmıştır.

**Anahtar Kelimeler —** TRT tabii, oyunlaştırma, modüler monolit, sunucu otoriter, transactional outbox, idempotency, Spring Boot, RabbitMQ, Redis, PostgreSQL.

---

## Abstract
This paper presents the architectural design, implementation, and live deployment of an enterprise-grade, API-first backend platform that transforms the passive viewing experience of TRT tabii video streaming content into an interactive engagement through quizzes and gamification. Traditional client-centric quiz applications suffer from answer leakage, client-side timer manipulation, race conditions causing duplicate XP rewards, and severe database bottlenecks during real-time leaderboard aggregations. These challenges are resolved by introducing a server-authoritative gameplay engine, asynchronous event-driven messaging powered by the Transactional Outbox/Inbox pattern, and a generation-based Redis caching model. The core system is structured as a Modular Monolith adhering to Clean Architecture principles to eliminate distributed transaction overheads, while the read-heavy leaderboard projection is decoupled into an independently scalable Spring Boot microservice. The resulting platform has been deployed to Oracle Cloud Infrastructure utilizing an 8-container topology behind a Caddy reverse proxy and verified with 130 backend, 47 frontend, and containerized integration test suites.

**Keywords —** TRT tabii, gamification, modular monolith, server-authoritative, transactional outbox, idempotency, Spring Boot, RabbitMQ, Redis, PostgreSQL.

---

# I. GİRİŞ (Introduction)

Dijital yayın platformlarının yaygınlaşmasıyla birlikte kullanıcıların video içeriklerini tüketim biçimi büyük ölçüde tek yönlü ve pasif bir izleme deneyimine dönüşmüştür. TRT tabii ekosisteminde yer alan zengin dizi, film, belgesel ve çocuk programı katalogları; kullanıcı bağlılığını, içerik hatırlanırlığını ve topluluk etkileşimini artıracak modern bir oyunlaştırma (gamification) katmanına ihtiyaç duymaktadır. 

Bu çalışmanın temel amacı; TRT tabii izleyicilerine bölüm ve içerik bazlı yarışma deneyimi sunan, güvenilir, ölçeklenebilir ve kötüye kullanıma karşı korumalı (cheat-resistant) bir backend altyapısı geliştirmektir. Projede quiz ve puanlama mekanizmasının seçilme nedeni; izleyicinin izlediği yapımla doğrudan zihinsel bağ kurmasını sağlamak, içerik tüketimini pekiştirmek ve adil bir rekabet ortamı oluşturmaktır.

Sistemin hedef kullanıcı kitlesi; TRT tabii izleyicileri, içerik ve soru editörleri, operasyon/yönetim ekipleri ve gelecekte sisteme entegre edilecek web, mobil ve Smart TV istemcileridir. Çalışmanın kapsamı, tek ve dikey bir MVP (Minimum Viable Product) akışı üzerine kurgulanmıştır: **İçeriği yayınla → Quiz başlat → Cevapla → Tamamla → XP ver → Sırala**.

Projenin literatüre ve sektörel uygulamalara sunduğu temel teknik katkılar şunlardır:
1. **Sunucu Otoriter Oyun Motoru:** Süre, aktif soru durumu, doğru cevap gizliliği ve puanlama yetkisinin tamamen sunucu saatine ve iş kurallarına bağlanması.
2. **Değişmez (Immutable) Quiz Sürümleme:** Yayınlanan bir quiz sürümünün geçmiş kullanıcı denemelerinin veri bütünlüğünü bozmaması için dondurulması ve düzenlemelerin yeni sürümlerle yapılması.
3. **Tekrarlı İşlemlere Dayanıklı (Idempotent) XP Üretimi:** Ağ kesintilerinde veya mükerrer tamamlama isteklerinde çift XP kazancını veritabanı kısıtlamaları ve append-only defterle engelleyen kurgu.
4. **Transactional Outbox/Inbox ile Güvenilir Mesajlaşma:** Dual-write problemini ortadan kaldıran, PostgreSQL ve RabbitMQ entegrasyonu.
5. **PostgreSQL Kaynaklı ve Redis ile Hızlandırılan Leaderboard:** PostgreSQL'in tek kalıcı doğru kaynak (Single Source of Truth) olduğu, Redis'in ise kaybedilebilir ve yeniden üretilebilir bir okuma modeli (Read Model) olarak konumlandırıldığı sıralama altyapısı.
6. **Modüler Monolitten Kontrollü Servis Ayrıştırması:** Gereksiz dağıtık karmaşadan kaçınarak çekirdeği modüler monolit tutan, yalnızca okuma-yoğun Leaderboard bileşenini mikroservise çıkaran dengeli mimari.
7. **Erişilebilir Medya ve Süre Sözleşmesi:** WCAG 2.2 AA standartları doğrultusunda cevabı sızdırmayan alternatif metinler ve sağlık verisi toplamadan süre uyarlaması sağlayan API tasarımı.

---

# II. SİSTEM GEREKSİNİMLERİ VE TASARIM HEDEFLERİ

## A. Fonksiyonel Gereksinimler
- **İçerik Kataloğu Yönetimi:** İçerik, sezon ve bölüm hiyerarşisinin `DRAFT`, `PUBLISHED` ve `ARCHIVED` yaşam döngüsüyle yönetilmesi.
- **Quiz Yazarlık ve Sürümleme:** Editörlerin soru metni, 4 seçenek, erişilebilirlik açıklaması, kapak/soru görseli ve süre tanımlayabilmesi; yayınlanan sürümün dondurulması.
- **Gameplay (Oyun Akışı):** Kullanıcının yayınlanmış sürüm üzerinden attempt başlatması, soruları süre kısıtında cevaplaması, doğru şıkkın ancak cevaplandıktan sonra açıklanması ve oturumun tamamlanması/terk edilmesi.
- **XP ve Ödül Sistemi:** Kullanıcının ilk tamamlamada skoruna göre XP kazanması; tekrar çözümlerde 0 ek XP kuralının işletilmesi.
- **Liderlik Tablosu (Leaderboard):** Global ve içerik bazlı anlık sıralamaların hesaplanması ve listelenmesi.
- **Yönetici ve Kullanıcı Arayüzleri:** Kullanıcılar için yarışma ve profil, yöneticiler için içerik/quiz yayınlama ve medya kütüphanesi arayüzlerinin sunulması.

## B. Fonksiyonel Olmayan Gereksinimler
- **Veri Bütünlüğü (Data Integrity):** Veritabanı seviyesinde tekillik (`UNIQUE`), yabancı anahtar (`FOREIGN KEY`) ve kontrol (`CHECK`) kısıtlamalarıyla korunan veri tutarlılığı.
- **Güvenlik (Security):** `HttpOnly`, `Secure`, `SameSite=Strict` oturum çerezleri, SPA CSRF double-submit token doğrulaması, Bcrypt parola özetleri ve SHA-256 şifre sıfırlama token'ları.
- **Idempotency:** Ağ tekrarında aynı cevabın veya tamamlamanın ikinci kez yan etki üretmemesi.
- **Erişilebilirlik (Accessibility):** WCAG 2.2 AA seviyesine uyumlu API ve medya modelleri.
- **Gözlemlenebilirlik (Observability):** W3C Trace Context, OpenTelemetry, Prometheus ve Spring Boot Actuator ile dağıtık izleme.
- **Hata Toleransı (Resilience):** Broker veya önbellek kesintilerinde ana quiz ve puanlama akışının kesintisiz sürmesi.
- **Test Edilebilirlik:** Mock bağımlılıkları yerine gerçek PostgreSQL, RabbitMQ ve Redis kapsayıcıları ile uçtan uca doğrulanabilirlik.

## C. Temel Tasarım İlkeleri ve Kavramsal Çerçeve
Mimari kararların zeminini oluşturan beş temel kavram aşağıda tanımlanmıştır:

1. **PostgreSQL Neden Kalıcı Doğru Kaynaktır?**  
   Kullanıcı hesapları, tamamlanan denemeler, cevaplar ve kazanılan puanlar ilişkisel bütünlük, ACID güvencesi ve güçlü tutarlılık (strong consistency) gerektirir. PostgreSQL sistemin tek ve nihai gerçeklik kaynağıdır (Single Source of Truth).
2. **Redis Neden Kaybedilebilir Bir Read Model'dir?**  
   Redis, milyonlarca kullanıcı arasındaki sıralama hesaplarını O(log(N)) hızında sunan bir önbellek katmanıdır. Redis verisi her an silinebilir veya çökebilir; sistem PostgreSQL'deki ham işlem kayıtlarından (`xp_transactions`) tüm Redis verisini saniyeler içinde sıfırdan yeniden üretebilecek şekilde tasarlanmıştır.
3. **Domain Kuralı ve Veritabanı Constraint'i Neden Birlikte Kullanılır?**  
   Uygulama kodundaki domain kuralları kullanıcıya anlamlı hata mesajları dönmek için gereklidir; ancak eşzamanlı (concurrency) yarış durumlarında yalnız uygulama mantığı veri bozulmasını engelleyemez. Veritabanı constraint'leri (`UNIQUE`, `CHECK`) sistemin son savunma hattıdır.
4. **İstemci Neden Skor ve Doğru Cevap Otoritesi Değildir?**  
   Tarayıcı veya mobil istemciler kullanıcı kontrolündedir ve manipüle edilebilir. İstemci yalnızca bir gösterim aracıdır; doğru cevap soru çözülene kadar istemciye gönderilmez, süre ve skor sunucu saatine göre sunucuda belirlenir.
5. **At-Least-Once Mesaj Teslimatında Idempotency Neden Zorunludur?**  
   RabbitMQ gibi mesaj kuyrukları ağ kesintilerinde aynı mesajı birden fazla kez iletebilir (at-least-once). Tüketici servislerin mükerrer mesaj aldığında fazladan XP yazmaması için idempotency anahtarları ve Inbox tabloları zorunludur.

---

# III. SİSTEM MİMARİSİ

```
+-------------------------------------------------------------------------------------------------------+
| Şekil 1. Sistem mimarisi ve modül sınırları (Yatay Genel Bakış)                                      |
|                                                                                                       |
|  [ Kullanıcı / Admin Web ] ---> [ Caddy Reverse Proxy (HTTPS/Let's Encrypt) ]                         |
|                                         |                                                             |
|          +------------------------------+------------------------------+                              |
|          | (Same-Origin /api)                                          |                              |
|          v                                                             v                              |
|  +--------------------------------------------+            +---------------------------------------+  |
|  |  ÇEKİRDEK MODÜLER MONOLİT (Port 8081)      |            |  LEADERBOARD MİKROSERVİSİ (Port 8082) |  |
|  |  • API: Controllers & DTOs                 |            |  • RabbitMQ Consumer (Inbox)          |  |
|  |  • Application: Use Cases & Transactions   |            |  • Projection Service (Deterministik) |  |
|  |  • Domain: Pure Entities & State Machines  |            |  • Internal REST Endpoint             |  |
|  |  • Ports & Infrastructure: JDBC, AMQP, SMTP|            +---------------------------------------+  |
|  +--------------------------------------------+                     |                     |           |
|          |                      |                                   v                     v           |
|          v                      v                          [ Leaderboard DB ]      [ Redis ZSET ]     |
|   [ Monolith DB ]      [ RabbitMQ Broker ]                 (PostgreSQL 17)           (Read Model)     |
|   (PostgreSQL 17)        (quiz.exchange)                                                              |
+-------------------------------------------------------------------------------------------------------+
```
*(Detaylı şema için bkz. [01_SISTEM_MIMARISI_VE_MODUL_SINIRLARI.md](file:///c:/Users/yunus/Desktop/staj/content_angagement_app/docs/DIAGRAMS/01_SISTEM_MIMARISI_VE_MODUL_SINIRLARI.md))*

## A. Genel Mimari
Sistem üç ana düzlemden oluşur:
1. **Kenar ve İstemci Katmanı:** React 19 ve TypeScript ile geliştirilen SPA arayüzleri, Caddy ters vekili arkasında tek bir origin (`https://hikayeizi.duckdns.org`) altında toplanmıştır.
2. **Çekirdek Modüler Monolit (Spring Boot 4.1):** Kimlik, içerik, quiz yazarlık, oyunlaştırma ve attempt yönetimini sağlayan ana backend uygulamasıdır.
3. **Bağımsız Leaderboard Mikroservisi:** Monolitten bağımsız çalışan, RabbitMQ üzerinden XP olaylarını tüketen ve Redis Sorted Set (ZSET) ile liderlik tablosu üreten servistir.

## B. Modüler Monolit Yapısı ve İzolasyon Kuralları
Çekirdek uygulama 10 mantıksal modüle ayrılmıştır:
- `identity`: Kullanıcı kaydı, parola özetleme (Bcrypt), oturum ve şifre sıfırlama token yönetimi (`identity_user_accounts`, `password_reset_tokens`).
- `content`: Dizi, film, sezon ve bölüm kataloğu (`content_catalog_items`, `content_seasons`, `content_episodes`).
- `media`: Görsel dosyaların fiziksel depolanması, SHA-256 checksum doğrulaması ve metadata yönetimi (`media_assets`).
- `quiz`: Soru, 4 seçenek, süre ve değişmez sürüm yaşam döngüsü (`quiz_definitions`, `quiz_versions`, `quiz_questions`, `question_options`).
- `gameplay`: Attempt başlatma, sunucu deadline üretimi, cevap doğrulama, `AWAITING_NEXT_QUESTION` durumu ve skorlama (`gameplay_attempts`, `attempt_answers`, `gameplay_quiz_reward_claims`).
- `gamification`: Append-only XP işlem defteri ve bakiye hesaplaması (`xp_transactions`).
- `messaging`: Transactional Outbox ve Inbox altyapısı (`outbox_events`, `inbox_messages`).
- `leaderboard`: Monolit içi geçiş adaptörü ve dış servis entegrasyonu.
- `admin`: Yetkili yönetim arayüzü ve operasyonel denetim koordinasyonu.
- `shared`: Ortak hata kodları, istisnalar ve W3C trace yardımcıları.

**Modül İzolasyon Kuralı:** Modüller birbirlerinin veritabanı tablolarına doğrudan SQL sorgusu atamaz veya repository sınıflarını enjekte edemez. İletişim yalnızca Java port arayüzleri, servis sözleşmeleri veya asenkron olaylar üzerinden yürütülür.

## C. Katmanlar ve Bağımlılık Yönü (Clean Architecture)
```text
api (Controllers / DTOs)
  ↓
application (Use Cases & @Transactional Sınırları)
  ↓
domain (Saf İş Kuralları, Varlıklar, Durum Makineleri)
  ↖
ports ← infrastructure (Spring Data JDBC/JPA, AMQP, SMTP Adapters)
```
- **Domain:** Sıfır framework bağımlılığı taşır; yalnızca saf Java sınıflarından oluşur.
- **Application:** Use case'leri yürütür ve veritabanı transaction sınırlarını (`@Transactional`) çizer.
- **API:** HTTP protokol detayları, DTO dönüşümleri ve CSRF doğrulamasıyla ilgilenir; iş kuralı içermez.
- **Infrastructure:** Veritabanı, mesaj kuyruğu ve dış e-posta servislerine ait adaptörleri barındırır.

## D. Leaderboard Servisinin Ayrıştırılması ve Tasarım Dengesi (ADR-0031)
1. **Neden İlk Günden Tam Mikroservis Değil?**  
   Tüm sistemi mikroservislere bölmek; dağıtık transaction (2PC/Saga), ağ gecikmesi ve operasyonel karmaşıklık getirir. Bu nedenle çekirdek domain monolit içinde tutulmuştur.
2. **Neden Yalnızca Leaderboard Ayrıştırıldı?**  
   Sıralama sorguları yoğun aggregation gerektirir ve write-heavy gameplay akışından farklı olarak read-heavy karaktere sahiptir. Sıralama yükünün ana veritabanını kilitlemesini önlemek için ideal bir mikroservis adayıdır.
3. **XP'nin Sahibi Neden Monolit Kaldı?**  
   XP kazanımı bir attempt tamamlama sonucudur. Veri bütünlüğünü sağlamak adına XP defteri monolit PostgreSQL'de tutulur; Leaderboard servisi ise bu verinin salt okunur bir türevidir (projection).
4. **Özellik Bayrağı (Feature Flag) ve Rollback Güvencesi:**  
   Monolit API'si dış istemciler için sabit kalmıştır. `LEADERBOARD_REMOTE_ENABLED=true` bayrağı ile trafik mikroservise yönlendirilir; olası bir servis arızasında bayrak kapatılarak monolit içi eski yerel sorgu bir rollback yolu olarak kullanılır.

---

# IV. DOMAİN VE VERİTABANI TASARIMI

## A. İçerik ve Quiz Sürümleme Modeli
İçerik kataloğu `content_catalog_items` → `content_seasons` → `content_episodes` hiyerarşisinde yapılandırılmıştır. Bir quiz, bir içeriğe veya doğrudan bir bölüme bağlanabilir (`quiz_definitions`).

Quiz yapısında **Definition** (Tanım) ile **Version** (Sürüm) kavramları birbirinden ayrılmıştır:
- `quiz_versions` tablosundaki bir sürüm `PUBLISHED` durumuna geçtiğinde **tamamen değişmez (immutable)** kabul edilir.
- Editör soruları değiştirmek istediğinde yayındaki sürüm güncellenmez; otomatik olarak yeni bir `version_number` ile `DRAFT` sürüm oluşturulur. Böylece geçmişte o quizi çözen kullanıcıların attempt kayıtları ve başarı istatistikleri asla bozulmaz.

## B. Gameplay ve Attempt Yaşam Döngüsü
Kullanıcı denemeleri katı bir durum makinesi (state machine) ile yönetilir:

```text
[Başlat] ──> IN_PROGRESS (30 sn Sunucu Deadline)
                  │
                  ├── Cevap Verildi / Timeout
                  v
             AWAITING_NEXT_QUESTION (Sayaç Durduruldu / Doğru Şık Gösterildi)
                  │
                  ├── "Sonraki Soruya Geç" Butonu (Sıfırdan Temiz 30 sn Deadline)
                  v
             IN_PROGRESS
                  │
                  ├── Son Soru Cevaplandı / "Quizden Çık" (Abandon)
                  v
             COMPLETED / ABANDONED (XP Hesabı & Outbox Kaydı)
```

**`AWAITING_NEXT_QUESTION` Durumunun Amacı:**  
Kullanıcı bir soruyu cevapladığında doğru şıkkı ve kazandığı puanı incelemesi için sonuç kartı açılır. Bu esnada aktif soru deadline'ı durdurulur. Böylece kullanıcının sonuç ekranında geçirdiği süre bir sonraki sorunun süresinden düşmez; "Sonraki Soru" tıklandığında sunucu saati baz alınarak yeni soruya ait 30 saniyelik temiz bir deadline üretilir.

## C. Append-Only XP İşlem Defteri ve Ödül Kurgusu
- `xp_transactions` tablosu append-only (yalnızca ekleme yapılabilir) bir muhasebe defteri mantığıyla çalışır; mevcut satırlar güncellenmez veya silinmez.
- **İlk Tamamlama Ödülü:** Bir kullanıcı bir quizi ilk kez tamamladığında skoruna karşılık gelen XP miktarı deftere işlenir ve `gameplay_quiz_reward_claims` tablosuna tekil kayıt atılır (ADR-0026).
- **Tekrar Çözüm Kuralı:** Kullanıcı aynı quizi tekrar çözdüğünde `earned_xp = 0` olarak kaydedilir; önceki kazanılmış kalıcı XP korunur.
- **Sıfır Puan Alan Kullanıcılar:** İlk tamamlamasında 0 puan alan kullanıcı için de `amount = 0` değerinde bir defter kaydı tutularak kullanıcının o quizdeki ödül hakkını tükettiği açıkça belgelenir (Flyway `V14`).
- **Yönetici Düzeltmeleri:** Puan iadesi veya düzeltmeler, eski satır değiştirilmeden `ADMIN_ADJUSTMENT` gerekçesiyle yeni bir negatif/pozitif satır eklenerek gerçekleştirilir.

## D. Veri Bütünlüğü Kısıtlamaları (Constraints)
Sistemdeki kritik iş kuralları veritabanı seviyesinde şu kısıtlamalarla garanti altına alınmıştır:
- **Tek Cevap:** `attempt_answers` tablosunda `UNIQUE(attempt_id, question_id)` kısıtı ile aynı soruya çift cevap engellenir.
- **Tek İlk Ödül:** `gameplay_quiz_reward_claims` tablosunda `PRIMARY KEY(user_id, quiz_id)` kısıtı ile mükerrer XP ödülü önlenir.
- **Tek Açık Attempt:** `gameplay_attempts` üzerinde kullanıcının aynı anda tek bir aktif denemeye sahip olabilmesi kuralı.
- **Tekil Sürüm Numarası:** `quiz_versions` üzerinde `UNIQUE(quiz_id, version_number)`.
- **Inbox Tekilliği:** `inbox_messages` üzerinde `PRIMARY KEY(event_id)` ile mükerrer mesaj tüketim engeli.

## E. Veritabanı Varlık-İlişki Modelleri (ERD)
IEEE formatına uygun olarak veritabanı modeli iki mantıksal şemaya bölünmüştür:
- **Şekil 2-a: Monolit Çekirdek Veritabanı Şeması:** Kimlik, içerik kataloğu, quiz sürümleri, gameplay attempt'leri, XP defteri ve Outbox/Inbox tablolarını içerir (Bkz. [02_VERITABANI_SEMA_ERD.md](file:///c:/Users/yunus/Desktop/staj/content_angagement_app/docs/DIAGRAMS/02_VERITABANI_SEMA_ERD.md)).
- **Şekil 2-b: Leaderboard Servisi Projeksiyon Şeması:** Yalnızca tekil `transaction_id` kısıtlamasına ve zaman damgası indekslerine sahip `leaderboard_xp_entries` tablosunu içerir.

---

# V. KRİTİK SİSTEM AKIŞLARI

```
+-------------------------------------------------------------------------------------------------------+
| Şekil 3. Sunucu Otoriteli Quiz Çözme ve Cevaplama Akışı                                              |
|                                                                                                       |
|  Kullanıcı              React SPA              Caddy Proxy           Spring Boot Backend    PostgreSQL|
|     |                       |                       |                         |                 |     |
|     |---(Quiz'e Başla)----->|---POST /start-attempt>|--->(Cookie & CSRF)----->|---(Deadline Üret)->[INSERT] |
|     |                       |<--Question 1 DTO (Doğru Şık Gizli, deadline)---|                 |     |
|     |                       | [30sn Sayaç Başlar]   |                         |                 |     |
|     |---(B Şıkkını Seçer)-->| [Sayacı Dondurur]     |                         |                 |     |
|     |                       |---POST /answers------>|------------------------>|---(Süre Kontrol)      |
|     |                       |                       |                         |---(Puan Hesapla)      |
|     |                       |<--AnswerFeedback (isCorrect, earnedScore, correctOptionText)-----[UPDATE] |
|     |                       | [Sonuç Kartı Açılır & Soru 2 Görseli Preload Edilir]                    |
|     |---(Sonraki Soru)----->|---POST /next-question>|------------------------>|---(Yeni Deadline)----->[UPDATE]|
|     |                       |<--Question 2 DTO (Temiz 30sn Deadline ile)------|                 |     |
+-------------------------------------------------------------------------------------------------------+
```
*(Detaylı sequence şeması için bkz. [03_QUIZ_VE_GAMEPLAY_AKISI.md](file:///c:/Users/yunus/Desktop/staj/content_angagement_app/docs/DIAGRAMS/03_QUIZ_VE_GAMEPLAY_AKISI.md))*

## A. Quiz Başlatma ve Cevaplama Akışı
1. **Başlatma:** Kullanıcı oturumu `identity_user_accounts` üzerinden doğrulanır. Aktif `PUBLISHED` sürümün ilk sorusu çekilir. Sunucu saatiyle `deadline = now() + 30sn` hesaplanır ve `gameplay_attempts` tablosuna `IN_PROGRESS` olarak yazılır. İstemciye dönen DTO'da doğru şık bilgisi **yer almaz**.
2. **Cevaplama ve Süre Doğrulama:** Kullanıcı şıkkı seçtiğinde arayüz sayacı durdurur ve isteği iletir. Backend `server_now <= question_deadline` kontrolünü yapar. Zamanında gelmişse şık doğrulanır; doğruysa 10 puan eklenir, süre aşılmışsa `TIMEOUT` (0 puan) işlenir. Attempt `AWAITING_NEXT_QUESTION` durumuna geçirilir.
3. **Doğru Şık Açıklaması ve Görsel Önyükleme:** Doğru şıkkın metni (`correctOptionText`), sunucu puanı kaydettikten sonra dönen `AnswerFeedback` DTO'su ile iletilir. Kullanıcı sonuç kartını okurken frontend bir sonraki sorunun görselini sessizce önbelleğe indirir (`preloading`).
4. **Sonraki Soru:** Kullanıcı butona bastığında `POST /next-question` çağrısı yapılır; sunucu yeni soru için temiz 30 saniyelik deadline üretir ve sayaç yeniden başlar.

```
+-------------------------------------------------------------------------------------------------------+
| Şekil 4. Transactional Outbox/Inbox ve Asenkron Olay Akışı                                            |
|                                                                                                       |
|  [ Gameplay Complete ]                                                                                |
|          |                                                                                            |
|          v (Atomic SQL Transaction)                                                                   |
|  +-------------------------------------------------------------+                                      |
|  | Monolith DB: UPDATE gameplay_attempts + INSERT outbox_events|                                      |
|  +-------------------------------------------------------------+                                      |
|          |                                                                                            |
|          v (Scheduled Poller / FOR UPDATE SKIP LOCKED)                                                |
|  [ OutboxPublisherService ] ---> RabbitMQ Exchange (quiz.completed.v1)                                |
|                                           |                                                           |
|          +--------------------------------+--------------------------------+                          |
|          |                                                                 |                          |
|          v (Idempotent Inbox)                                              v                          |
|  [ Gamification Consumer ]                                         [ Leaderboard Consumer ]           |
|          |                                                                 |                          |
|          v                                                                 v                          |
|  [ Monolith DB: INSERT xp_transactions ]                           [ Leaderboard DB: Projection ]     |
|  [ Monolith DB: INSERT outbox (xp.changed.v1) ]                            |                          |
|                                                                            v                          |
|                                                                    [ Redis Generation ZSET ]          |
+-------------------------------------------------------------------------------------------------------+
```
*(Detaylı sequence şeması için bkz. [04_OLAY_GUDUMLU_MESAJLASMA_VE_OUTBOX.md](file:///c:/Users/yunus/Desktop/staj/content_angagement_app/docs/DIAGRAMS/04_OLAY_GUDUMLU_MESAJLASMA_VE_OUTBOX.md))*

## B. Transactional Outbox ve Asenkron XP Akışı
- **Dual-Write Koruması:** Attempt'in `COMPLETED` yapılması ile `quiz.completed.v1` olayının oluşturulması aynı PostgreSQL transaction'ında commit edilir. Broker kapalı olsa dahi olay kaybolmaz.
- **Outbox Worker:** Arka plan servisi `PENDING` durumundaki olayları toplu çeker (`FOR UPDATE SKIP LOCKED`), RabbitMQ'ya basar ve publisher confirm (ACK) aldıktan sonra durumu `PUBLISHED` yapar.
- **Inbox ve Tekil İşleme:** `GamificationConsumer` mesajı aldığında `inbox_messages` tablosuna yazar. İlk kez geliyorsa `xp_transactions` tablosuna ilk tamamlama XP'sini kaydeder ve yeni bir `xp.changed.v1` olayını Outbox'a bırakır. Tekrar iletilen mesajlar Inbox kalkanı sayesinde yok sayılır.

## C. Leaderboard Akışı ve Generation Tabanlı Redis Modeli
- Leaderboard servisi `xp.changed.v1` olayını tüketir ve `leaderboard_xp_entries` tablosuna yazar (`UNIQUE(transaction_id)`).
- Servis, PostgreSQL projeksiyonu üzerinden toplam XP, ilk kazanım zaman damgası (`occurred_at`) ve UUID tie-break kurallarıyla deterministik sıralamayı hesaplar.
- Sıralama Redis üzerinde doğrudan ezilmez; `leaderboard:global:{generation}` şeklinde yeni bir generation anahtarı üretilerek atomik olarak devreye alınır.
- Redis'in çökmesi durumunda `LeaderboardClient` otomatik olarak PostgreSQL projeksiyon sorgusuna geri düşer (fallback).

---

# VI. GÜVENLİK, ERİŞİLEBİLİRLİK VE DAYANIKLILIK

## A. Kimlik Doğrulama ve Oturum Güvenliği
- **Yerel Hesaplar ve Parola Güvenliği:** Kullanıcı parolaları tek yönlü `Bcrypt` algoritması ile tuzlanarak (salted) özetlenir (ADR-0029).
- **Çerez Tabanlı Oturum:** JWT'lerin LocalStorage'da tutulmasıyla doğan XSS risklerini önlemek amacıyla `HttpOnly`, `Secure` ve `SameSite=Strict` niteliklerine sahip sunucu oturum çerezleri kullanılmıştır.
- **SPA CSRF Koruması:** Durum değiştiren (`POST`, `PUT`, `DELETE`) tüm isteklere karşı double-submit CSRF cookie/header doğrulaması zorunlu kılınmıştır (ADR-0032).
- **Yetkilendirme Sınırları:** Sistemde `USER`, `EDITOR` ve `ADMIN` rolleri tanımlıdır. Tarayıcı tarafındaki rol bilgisi yalnızca UI görünümünü şekillendirir; tüm yetki kontrolleri sunucu API katmanında icra edilir.
- **Güvenli Şifre Sıfırlama:** 256-bit rastgele üretilen sıfırlama token'ının veritabanında yalnızca **SHA-256 özeti** tutulur. Token 30 dakika geçerlidir ve tek kullanımlıktır; e-posta iletimi MailerSend SMTP relay üzerinden yapılır (ADR-0030).

## B. Quiz Güvenliği ve Hile Engelleme
- Doğru cevap ID'si ve metni soru çözülmeden önce istemciye gönderilmez.
- Süre ve puan sunucu saatine göre işletilir; istemci tarafında yerel saat manipülasyonu puan kazandırmaz.
- Bir kullanıcı yalnızca kendi oturumuna ait attemptId üzerinden işlem yapabilir; başkasının denemesine cevap gönderilemez.
- Parola, ham token, doğru cevap ve kişisel veriler log kayıtlarına sızdırılmaz.

## C. Erişilebilirlik (Accessibility - WCAG 2.2 AA)
- **Görsel ve Metin Ayrımı:** Her soru görseli için bilgilendirici veya dekoratif ayrımı yapılmıştır.
- **Cevabı Sızdırmayan Alternatif Metin:** Görsel tabanlı sorularda görme engelli kullanıcılar için sunulan `accessible_prompt`, doğru cevabı ifşa etmeden görseldeki bağlamı eşdeğer biçimde aktarır.
- **Renk Bağımsız Geri Bildirim:** Doğru/yanlış sonuçlarında yalnız yeşil/kırmızı renkler değil; metin etiketleri ve ikonlar birlikte kullanılır.
- **Sağlık Verisi Toplamadan Süre Uyarlaması:** Engelli kullanıcılara ek süre tanınırken kullanıcıdan sağlık/rapor verisi talep edilmez; süre seçimi genel bir erişilebilirlik tercihi olarak sunulur.

## D. Kesinti Senaryoları ve Hata Toleransı
- **RabbitMQ Çökerse:** Monolit attempt tamamlama ve XP hesaplama işlemlerini PostgreSQL üzerinde kesintisiz sürdürür; olaylar `outbox_events` tablosunda birikir ve broker açıldığında otomatik iletilir.
- **Redis Çökerse:** Leaderboard servisi ve monolit liderlik tablosu isteklerini doğrudan PostgreSQL projeksiyon sorgularına yönlendirir; veri kaybı yaşanmaz.
- **Leaderboard Servisi Çökerse:** Çekirdek quiz çözme, içerik izleme ve XP kazanım akışları etkilenmez; monolit eski yerel sıralama sorgusuna geçici fallback yapar.

---

# VII. UYGULAMA VE CANLI DAĞITIM

## A. Kullanılan Teknolojiler ve Rolleri
- **Java 21 & Spring Boot 4.1:** Çekirdek iş mantığı, REST API ve sanal thread destekli modern çalışma zamanı.
- **PostgreSQL 17 & Flyway:** İlişkisel veri modeli, ACID transaction'lar ve 18 aşamalı sürüm kontrollü veritabanı migration yönetimi.
- **RabbitMQ 4.1 & Spring AMQP:** Güvenilir, at-least-once garantili asenkron olay dağıtımı.
- **Redis 8.2 & Spring Data Redis:** O(log(N)) liderlik tablosu okuma modeli ve generation önbelleği.
- **React 19, TypeScript & Vite:** Tip güvenli, performanslı ve responsive SPA kullanıcı/admin arayüzleri.
- **Caddy 2:** Otomatik Let's Encrypt SSL/TLS sertifikası, statik SPA dağıtımı ve Same-Origin API ters vekili.
- **Testcontainers 1.20:** Testlerde mock yerine gerçek PostgreSQL, RabbitMQ ve Redis kapsayıcıları ile uçtan uca doğrulama.
- **OpenTelemetry & Actuator:** Dağıtık izleme ve sistem sağlık kontrolleri.

```
+-------------------------------------------------------------------------------------------------------+
| Şekil 5. Oracle Cloud Canlı Dağıtım ve Ağ Topolojisi                                                  |
|                                                                                                       |
|  Genel İnternet (HTTPS: 443 / HTTP: 80)                                                              |
|        |                                                                                              |
|        v                                                                                              |
|  [ Oracle Cloud Free Tier VM (AMD EPYC, 12 GB RAM, Ubuntu Linux) ]                                    |
|  +-------------------------------------------------------------------------------------------------+  |
|  | [ Caddy Container ] (https://hikayeizi.duckdns.org)                                             |  |
|  |   • Port 80/443 Açık (Tüm diğer portlar DIŞA KAPALIDIR)                                         |  |
|  |   • / ve /admin -> web SPA                                                                      |  |
|  |   • /api/* -> backend:8081                                                                      |  |
|  |                                                                                                 |  |
|  |   İzole Dahili Ağ (app-network):                                                                |  |
|  |   +------------------------------------------------------------------------------------------+  |  |
|  |   | [web] (React)        | [backend] (Spring Boot)   | [leaderboard-service] (Spring Boot)     |  |  |
|  |   | [postgres] (Port5432)| [leaderboard-postgres]    | [rabbitmq] (Port 5672)                  |  |  |
|  |   | [redis] (Monolith)   | [leaderboard-redis]       |                                         |  |  |
|  |   +------------------------------------------------------------------------------------------+  |  |
|  |                                                                                                 |  |
|  |   Kalıcı Disk Birimleri (Named Volumes):                                                        |  |
|  |   postgres_data | leaderboard_postgres_data | media_data | rabbitmq_data | caddy_data           |  |
|  +-------------------------------------------------------------------------------------------------+  |
+-------------------------------------------------------------------------------------------------------+
```
*(Detaylı topoloji şeması için bkz. [05_CANLI_DAGITIM_VE_AG_TOPOLOJISI.md](file:///c:/Users/yunus/Desktop/staj/content_angagement_app/docs/DIAGRAMS/05_CANLI_DAGITIM_VE_AG_TOPOLOJISI.md))*

## B. Production Topolojisi (Oracle Cloud Free Tier)
Sistem, Oracle Cloud her zaman ücretsiz (Always Free) VM üzerinde `compose.production.yaml` yapılandırmasıyla 8 izole Docker konteyneri halinde canlıya alınmıştır:
1. `caddy` (Uç yönlendirme ve SSL/TLS sonlandırma)
2. `web` (React SPA statik dosyaları)
3. `backend` (Çekirdek Spring Boot modüler monolit)
4. `leaderboard-service` (Sıralama Spring Boot mikroservisi)
5. `postgres` (Monolit veritabanı)
6. `leaderboard-postgres` (Sıralama veritabanı)
7. `rabbitmq` (Mesajlaşma brokerı)
8. `leaderboard-redis` (ve `redis` monolit önbelleği)

**Sıfır Güven Port İzolasyonu:** İnternete **yalnızca Port 80 ve Port 443** açılmıştır. Veritabanları ve RabbitMQ dış dünyaya tamamen kapalı olup yalnızca `app-network` köprü ağı içinde iletişim kurar.

## C. Operasyonel Özellikler
- **Let's Encrypt Otomasyonu:** Caddy, `hikayeizi.duckdns.org` alan adı için SSL sertifikalarını otomatik üretir ve yeniler.
- **Sağlık Kontrolleri (Health Checks):** `/actuator/health` uç noktası veritabanı, Redis ve disk durumunu izler; posta kontrolü Actuator'dan ayrılarak bağımsızlaştırılmıştır.
- **Kalıcı Diskler (Volumes):** Veritabanı tabloları, yüklenen görseller (`media_data`) ve kuyruk verileri konteyner yaşam döngüsünden bağımsız Named Volume'larda tutulur.

---

# VIII. TEST VE DOĞRULAMA

## A. Test Stratejisi
Projede test piramidi prensiplerine sadık kalınmış; birim testlerle saf iş kuralları, entegrasyon testleriyle veritabanı ve kuyruk sözleşmeleri, mimari testlerle (ArchUnit) modül sınırları doğrulanmıştır.

```text
       /  API & E2E Testleri  \         (47 Frontend Vitest & Build Doğrulaması)
      /------------------------\
     / Concurrency & Broker Test \      (Testcontainers: PostgreSQL, RabbitMQ, Redis)
    /------------------------------\
   /    Domain & State Unit Test    \   (130 Monolith + 4 Leaderboard Backend Testi)
  /----------------------------------\
```

## B. İş Kuralı ve Test Kanıtı Eşleştirmesi

### TABLO I. İŞ KURALI VE TEST KANITI EŞLEŞTİRMESİ
| Korunan Risk / İş Kuralı | Uygulanan Mekanizma | Doğrulayan Test Kanıtı |
| :--- | :--- | :--- |
| **Aynı soruya ikinci cevap** | `UNIQUE(attempt_id, question_id)` + Domain kontrolü | `GameplayIntegrationTest.shouldRejectDuplicateAnswer` |
| **İkinci tamamlama ile mükerrer XP** | `gameplay_quiz_reward_claims` + `xp_transactions` | `GamificationIdempotencyTest.shouldAwardXpOnlyOnce` |
| **Doğru cevabın soru öncesi sızması** | Soru DTO'sundan `correctOption` çıkarılması | `QuizAuthoringIntegrationTest.shouldHideCorrectOption` |
| **Sonuç kartında süre kaybı** | `AWAITING_NEXT_QUESTION` + Yeni deadline | `GameplayIntegrationTest.shouldPauseTimerUntilNextQuestion` |
| **RabbitMQ tekrar mesajı ile çift XP** | `inbox_messages` tekil `event_id` kalkanı | `MessagingIntegrationTest.shouldIgnoreDuplicateOutboxEvents` |
| **Leaderboard duplicate XP olayı** | `leaderboard_xp_entries.transaction_id` UK | `LeaderboardServiceIntegrationTest.shouldConsumeIdempotently` |
| **CSRF saldırısı ve sahte istek** | Cookie + Double submit header kontrolü | `CsrfProtectionIntegrationTest.shouldRejectMissingCsrfHeader` |
| **İlk admin hesabının tekliği** | Bootstrap tekil kontrolü + Bcrypt özeti | `InitialAdminBootstrapIntegrationTest.shouldCreateOnlyOneAdmin` |
| **Şifre sıfırlama token güvenliği** | SHA-256 özeti, 30 dk süre ve tek kullanım | `AccountAuthenticationIntegrationTest.shouldResetPasswordWithToken` |

## C. Doğrulama Sonuçları
- **Backend Testleri:** Java 21 çalışma zamanında `./mvnw.cmd verify` ile monolit paketinde **130/130**, leaderboard servisinde **4/4** test başarıyla tamamlanmıştır.
- **Frontend Testleri:** 11 test dosyasında **47/47** Vitest testi geçmiş; `tsc --noEmit` strict tip kontrolü ve Vite production build doğrulanmıştır.
- **Canlı Sistem Doğrulaması:** Oracle Cloud VM üzerinde HTTPS arayüzü (`/`), yönetim paneli (`/admin`) ve API (`/api/v1/...`) erişimleri test edilmiştir.

## D. Sınırlar ve Kapasite Notu
Mevcut yerel k6 yük testleri ve tek sunuculu Compose dağıtımı mimari tasarımın doğruluğunu ve fonksiyonel yetkinliğini kanıtlar; ancak bu durum çok bölgeli (multi-region) kurumsal production SLA/SLO kapasite garantisi yerine geçmez.

---

# IX. KARŞILAŞILAN SORUNLAR VE MÜHENDİSLİK ÇÖZÜMLERİ

Bu bölüm, geliştirme sürecinde karşılaşılan somut mühendislik problemlerini, kök neden analizlerini ve uygulanan çözümleri belgeler:

### 1. PostgreSQL – RabbitMQ Dual-Write Problemi
- **Gözlenen Problem:** Attempt tamamlandığında veritabanı güncellenip RabbitMQ'ya mesaj gönderilirken ağ koparsa veritabanında tamamlanmış görünen quiz için XP olayı kuyruğa iletilemiyordu.
- **Kök Neden:** İlişkisel veritabanı ile mesaj brokerı arasında dağıtık iki fazlı commit (2PC) olmaması (Dual-Write problemi).
- **Uygulanan Çözüm:** Transactional Outbox deseni uygulandı. Attempt durumu ile `outbox_events` kaydı aynı yerel SQL transaction'ında atomik commit edildi. Ayrı bir worker servisi olayları kuyruğa güvenle taşıdı.
- **Değerlendirilen Alternatif:** Dağıtık XA transaction'ları değerlendirildi ancak yüksek gecikme ve broker bağımlılığı nedeniyle elendi.
- **Test Kanıtı:** `TransactionalOutboxIntegrationTest.shouldPersistEventAtomicallyWithBusinessData`.

### 2. `AWAITING_NEXT_QUESTION` Durumu ve `VARCHAR(20)` Sınırı
- **Gözlenen Problem:** Sonuç ekranında süreyi korumak için yeni eklenen `AWAITING_NEXT_QUESTION` durumu kaydedilirken `DATA_INTEGRITY_CONFLICT` hatası alındı.
- **Kök Neden:** Veritabanındaki `gameplay_attempts.status` sütununun eski migration'da `VARCHAR(20)` olarak tanımlanması (22 karakterlik yeni durum sütuna sığmadı).
- **Uygulanan Çözüm:** Yeni bir `V18__increase_attempt_status_length.sql` migration dosyası oluşturularak sütun uzunluğu `VARCHAR(30)` değerine yükseltildi.
- **Değerlendirilen Alternatif:** Durum adını kısaltmak değerlendirildi; ancak domain dilinin (Ubiquitous Language) netliğini bozmamak için şema genişletildi.
- **Test Kanıtı:** `GameplayIntegrationTest.shouldPersistAwaitingNextQuestionState`.

### 3. Sonuç Ekranında Sonraki Soru Süresinin Erimesi
- **Gözlenen Problem:** Kullanıcı bir soruya cevap verdikten sonra sonuç kartındaki doğru şıkkı incelerken geçen süre, sonraki sorunun 30 saniyelik hakkından eksiliyordu.
- **Kök Neden:** Soru deadline'ının attempt başlatıldığında tüm quiz için tekil veya ardışık otomatik işletilmesi.
- **Uygulanan Çözüm:** Cevap geldiği anda aktif deadline silinip attempt `AWAITING_NEXT_QUESTION` durumuna geçirildi. Kullanıcı "Sonraki Soru" butonuna bastığında çağrılan `POST /next-question` endpoint'i ile sunucu saatinden sıfırdan 30 saniyelik yeni bir deadline üretildi.
- **Test Kanıtı:** `GameplayIntegrationTest.shouldGenerateFreshDeadlineOnNextQuestion`.

### 4. Tekrar Çözümde Quiz Kartında Kazanılan XP'nin Sıfır Görünmesi
- **Gözlenen Problem:** Bir quizi ilk çözüşünde 40 XP kazanan kullanıcı, quizi pekiştirmek için ikinci kez çözdüğünde (ikinci çözüşte kural gereği 0 XP kazanıldığı için) ana sayfadaki quiz kartında "Kazanılan XP: 0" görünüyordu.
- **Kök Neden:** Frontend'in son denemenin (`latest attempt`) anlık kazanımını okuması.
- **Uygulanan Çözüm:** `/api/v1/me/quiz-results` sorgusu kullanıcının o quizdeki tüm tamamlanmış denemeleri arasından kalıcı en yüksek XP değerini (`MAX(earned_xp)`) döndürecek şekilde güncellendi. ADR-0026 kuralı korunurken karttaki görsel tutarsızlık giderildi.
- **Test Kanıtı:** `GameplayIntegrationTest.shouldReturnMaxEarnedXpForQuizCard`.

### 5. Canlı Ağ Ortamında Soru Görselinin Gecikmeli Yüklenmesi
- **Gözlenen Problem:** Canlı yayında sonraki soruya geçildiğinde soru metni anında gelirken görselin 200-400 ms sonra açılması kullanıcı deneyimini bozuyordu.
- **Kök Neden:** Görsel isteğinin ancak soru metni render edildikten sonra tarayıcı tarafından tetiklenmesi.
- **Uygulanan Çözüm:** `MediaController` endpoint'ine `Cache-Control: public, max-age=2592000, immutable` eklendi; frontend tarafında ise kullanıcı sonuç kartını okurken sıradaki sorunun görselini arka planda sessizce indiren `Image Preloading` mekanizması uygulandı.
- **Test Kanıtı:** `MediaControllerIntegrationTest.shouldReturnImmutableCacheHeaders`.

---

# X. TARTIŞMA VE TRADE-OFF DEĞERLENDİRMESİ

Mühendislik kararları mutlak doğrular değil, belirli kısıtlar altında yapılan bilinçli ödünleşimlerdir (trade-offs):

1. **Modüler Monolit vs Dağıtık Mikroservisler:**  
   Modüler monolit seçimi geliştirme hızını artırmış, modüller arası transaction yönetimini kolaylaştırmış ve tek veritabanı ile güçlü tutarlılık sağlamıştır. Karşılığında bağımsız dağıtım (deployment) esnekliği sınırlandırılmıştır.
2. **Eventual Consistency vs Strong Consistency:**  
   Leaderboard servisinin RabbitMQ ile ayrılması monolit üzerindeki okuma yükünü sıfırlamıştır; ancak XP kazanımı ile liderlik tablosuna yansıması arasında milisaniyelik bir gecikme (eventual consistency) kabul edilmiştir.
3. **Tek Sunucu (Single Host) vs Yüksek Erişilebilirlik (HA):**  
   Oracle Cloud Always Free VM üzerinde tek Compose kümesi kurulum ve işletme maliyetini sıfıra indirmiştir. Ancak sunucu arızasında sistemin tamamen durması (Single Point of Failure - SPOF) bilinçli bir MVP ödünleşimidir.
4. **Redis Read Model vs İlişkisel Veritabanı:**  
   Redis kullanımı sıralama sorgularını O(log(N)) seviyesine indirmiştir; ancak Redis'in kalıcı kaynak olmaması nedeniyle generation tabanlı senkronizasyon ve fallback mantığı geliştirme maliyeti getirmiştir.
5. **Sunucu Oturumu (Stateful Session) vs Durumsuz JWT (Stateless JWT):**  
   `HttpOnly` çerez oturumu XSS saldırılarına karşı üstün güvenlik sağlamış ve anında oturum iptaline izin vermiştir; ancak çoklu backend instance'larına geçildiğinde merkezi bir Redis Session Store ihtiyacı doğuracaktır.

---

# XI. SONUÇ VE GELECEK ÇALIŞMALAR

## A. Elde Edilen Sonuçlar
TRT tabii içerikleri için tasarlanan etkileşim ve oyunlaştırma platformu; uçtan uca **yayınla → başlat → cevapla → tamamla → XP ver → sırala** dikey akışını başarıyla tamamlamıştır. Sistem; sunucu otoriter oyun motoru, transactional mesajlaşma, değişmez sürümleme ve canlı Oracle Cloud ortamındaki kararlı çalışmasıyla kurumsal backend hedeflerine ulaşmıştır.

## B. Bilinen Sınırlamalar
- Kurumsal merkezi OIDC/Single Sign-On (SSO) sözleşmesi henüz entegre edilmemiştir (yerel hesaplar devrededir).
- Tek sunucu dağıtımı yüksek erişilebilirlik (High Availability) kümesine sahip değildir.
- Çok editörlü eşzamanlı quiz taslağı düzenlemeleri için optimistic locking henüz eklenmemiştir.
- KVKK veri silme/anonimleştirme ve yasal saklama süreleri operasyonu tamamlanmamıştır.

## C. Gelecek Çalışmalar
- **Canlı TV Senkronizasyonu:** TRT tabii canlı yayın akışıyla zaman uyumlu anlık soru ve yarışma modülünün eklenmesi.
- **WebSocket Tabanlı Çoklu Oyuncu (Multiplayer):** Kullanıcıların birbirleriyle eşzamanlı düello yapabileceği anlık soket altyapısının kurulması.
- **Merkezi Kimlik Entegrasyonu:** Keycloak / TRT OIDC altyapısı ile kurumsal kullanıcı oturumlarının birleştirilmesi.
- **Kubernetes (K8s) Dağıtımı:** Artan trafik için yatay pod ölçekleme (HPA) ve çok düğümlü yüksek erişilebilirlik altyapısına geçiş.

---

# BİLGİLENDİRME (Acknowledgment)
Bu çalışma, TRT bünyesinde gerçekleştirilen staj programı kapsamında, tabii platformunun etkileşimli geleceğine yönelik kurumsal backend mimarilerini ve modern yazılım mühendisliği pratiklerini araştırmak ve uygulamak amacıyla geliştirilmiştir. Süreç boyunca teknik rehberlik ve destek sağlayan TRT mühendislik ekiplerine teşekkür ederiz.

---

# KAYNAKLAR (References)

- **[1]** Spring Boot Documentation Team, "Spring Boot Reference Documentation (v4.1.0)," VMware Tanzu, 2026.
- **[2]** PostgreSQL Global Development Group, "PostgreSQL 17.5 Documentation: Concurrency Control and Transaction Isolation," 2025.
- **[3]** C. Richardson, *Microservices Patterns: With examples in Java*, Manning Publications, 2018 (Transactional Outbox and Inbox Patterns, pp. 87-112).
- **[4]** RabbitMQ Core Team, "Reliable Delivery and Publisher Confirms in RabbitMQ," Broadcom, 2025.
- **[5]** Redis Documentation, "Redis Sorted Sets and Rank Aggregation Mechanics," Redis Ltd., 2025.
- **[6]** R. C. Martin, *Clean Architecture: A Craftsman's Guide to Software Structure and Design*, Prentice Hall, 2017.
- **[7]** World Wide Web Consortium (W3C), "Web Content Accessibility Guidelines (WCAG) 2.2," W3C Recommendation, 2023.
- **[8]** OpenTelemetry Authors, "W3C Trace Context Specification and Distributed Tracing," Cloud Native Computing Foundation, 2024.
- **[9]** E. Evans, *Domain-Driven Design: Tackling Complexity in the Heart of Software*, Addison-Wesley, 2003.
- **[10]** Internet Engineering Task Force (IETF), "HTTP State Management Mechanism (Cookies) and SameSite Attribute," RFC 6265bis, 2024.
