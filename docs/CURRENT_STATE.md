# Güncel Proje Durumu - Kısa Referans

## 19.08.2026 Aşama 13D güvenli resmî tabii izleme bağlantısı

- Film ve dizi detayına, yalnız içerikte doğrulanmış bir bağlantı bulunduğunda görünen
  `tabii'de izle / Watch on tabii` eylemi eklendi. Bağlantı yeni sekmede açılır ve yeni
  sayfanın kaynak sekmeye erişmesini engelleyen `noopener noreferrer` kullanır.
- Editör ve yönetici, içerik detayındaki ayrı alandan bağlantıyı ekleyebilir, değiştirebilir
  veya boş değerle kaldırabilir. İşlem ayrı `PUT` endpoint'i ve
  `CONTENT_WATCH_URL_UPDATED` audit olayı üzerinden izlenir; normal kullanıcı 403 alır.
- Güven sınırı: yalnız HTTPS, tam `tabii.com`/`www.tabii.com` alan adı ve resmî
  `/detail/{id}` veya yerelleştirilmiş sayısal içerik detay yolu kabul edilir. Kullanıcı
  bilgisi, özel port, query, fragment,
  HTTP, `javascript:` ve benzer görünümlü alan adları domain kuralında reddedilir.
- V20 migration'ı nullable `watch_url` alanını ve aynı izin listesini uygulayan PostgreSQL
  `CHECK` constraint'ini ekler. Böylece uygulama katmanı atlanırsa dahi geçersiz kalıcı
  veri engellenir. İstemci de düğmeyi oluşturmadan önce URL'yi bağımsız doğrular.
- Mevcut içeriklere tahminî bağlantı yazılmadı; gerçek tabii içerik URL'si editör
  tarafından girilene kadar düğme görünmez. Sunucu URL'yi kendisi çağırmadığı için bu
  özellik yeni bir SSRF yüzeyi oluşturmaz.
- İlk yerel denemede arayüz güncelken 08:52'den beri çalışan backend eski kaldığı için
  yeni endpoint 404 `RESOURCE_NOT_FOUND` döndürdü. Backend güncel kodla yeniden
  başlatıldı ve yerel PostgreSQL şeması V19'dan V20'ye başarıyla geçirildi. Resmî Zlatan
  adresinin `/detail/588337` biçiminde dil öneki taşımadığı doğrulanınca izin listesi,
  güvenlik sınırları gevşetilmeden bu ikinci resmî yol biçimini de kabul edecek şekilde
  düzeltildi.
- İş kuralı/test eşleşmesi: domain URL normalizasyonu ve saldırı örnekleri `ContentTest`;
  migration, yayınlanan API cevabı, 403 yetki ve veritabanı constraint'i
  `ContentCatalogIntegrationTest`; yeni sekme nitelikleri ile istemci savunması
  `OfficialWatchLink.test.tsx`; yönetim endpoint sözleşmesi `content-api.test.ts` ile
  korunur. Tam Maven `verify` 135/135, odak backend testleri 21/21, frontend Vitest
  61/61 geçti; strict TypeScript ve production build başarılı oldu. Mevcut büyük
  PDF/font chunk uyarısı devam ediyor.
- Alternatif genel HTTPS URL alanı daha esnek fakat kimlik avı yüzeyini büyüttüğü için;
  uygulama içi redirect endpoint'i ise açık yönlendirme riski ve gereksiz bakım yüzeyi
  oluşturduğu için seçilmedi. Karar ADR-0035'te kayıtlıdır.
- Mutlak “hacklenemezlik” garanti edilemez. Bu dilimde URL/yeni sekme riskleri katmanlı
  olarak sınırlandı; tabii oturumu, bölgesel erişim ve dış servisin çalışabilirliği dış
  sistem sorumluluğundadır. Görsel tarayıcı incelemesi yapılmadı; proje kuralı gereği
  ayrıca açık izin gerekir.
- Sıradaki tek iş: Editörün mevcut film ve diziler için doğrulanmış gerçek tabii içerik
  bağlantılarını girmesi; sonrasında kullanıcı isterse Aşama 13B hata yerelleştirmesi.

## 19.08.2026 dil seçici görsel düzenlemesi

- Yönetim ve kullanıcı ekranları masaüstü tarayıcıda görsel olarak incelendi. Eski iki
  bayrak düğmesinin gezinme öğeleriyle yarıştığı ve yönetim başlığındaki dördüncü grid
  öğesinin hesap/çıkış alanını ikinci satıra düşürdüğü doğrulandı.
- Dil seçici, ülke bayrakları yerine dili açıkça gösteren tek yerel seçim kontrolüne
  dönüştürüldü: `TR · Türkçe` ve `EN · English`. Kontrol erişilebilir `Dil/Language`
  etiketi taşır ve mevcut `app_language:v1` tercih davranışını korur.
- Yönetim başlığında dil seçici ile hesap/çıkış aynı sağ eylem kümesine alındı. Kullanıcı
  başlığında da dil seçici hesap alanıyla; giriş ekranında hesap oluştur/giriş eylemiyle
  gruplanarak başlık hiyerarşisi sadeleştirildi.
- Kullanıcı başlığındaki `Keşfet / Quizler / Profil` sekmeleri eşit yan sütunlar
  arasında ekranın gerçek merkezine sabitlendi. Hesap işlemleri sağ tarafta kalırken
  dil seçici bu kümenin en sonuna alınarak başlığın sağ kenarına yerleştirildi.
- Alternatif iki metinli sekme, iki ayrı klavye odağı ve daha fazla yatay alan
  gerektirdiği için seçilmedi. Yerel `select`, klavye ve ekran okuyucu davranışını
  tarayıcıdan hazır alır; karşılığında açılan seçenek listesinin görünümü işletim
  sistemine göre küçük farklılık gösterebilir.
- İş kuralı/test eşleşmesi: tek etiketli seçim kontrolü ile iki dil seçeneğinin varlığı
  `i18n.test.ts`; dil kodu doğrulama, saklama ve belge dili mevcut i18n testleriyle
  korunur. Frontend Vitest `57/57` geçti; strict TypeScript ve production build
  başarılı oldu. Mevcut büyük PDF/font chunk uyarısı devam ediyor.
- Görsel doğrulama yerel yönetim ve kullanıcı ana sayfasında yapıldı: başlık öğeleri tek
  satırda hizalandı, dil kontrolünün odak çerçevesi görünür kaldı ve hesap işlemleriyle
  aynı kümede yer aldı.
- Bilinen konu: değişiklik henüz canlı sunucuya dağıtılmadı. Sıradaki tek iş, kullanıcı
  yeni yerleşimi onaylarsa mevcut production dağıtım akışıyla canlıya almaktır.

## 18.08.2026 Aşama 13C içerik ve quiz çevirileri

- Film/dizi, sezon/bölüm, quiz sürümü, soru, seçenek ve erişilebilirlik metinleri için
  kaynak kimliklere yabancı anahtarla bağlı İngilizce çeviri tabloları V19 migration'ıyla
  eklendi. Türkçe mevcut alanlarda ana/fallback dil olarak kaldı.
- Kullanıcı istemcisi her JSON isteğinde `Accept-Language` gönderir. Dil değişince API
  nesnesi yenilenerek katalog ve quiz verileri tekrar okunur. İngilizce çeviri varsa
  gösterilir; eksik öğe Türkçeye düşer.
- Gameplay yalnız gösterim metnini yerelleştirir. Soru/seçenek/doğru cevap kimlikleri,
  süre, skor, attempt, ilk tamamlama ve XP davranışı değişmedi.
- Admin içerik ekranına film/dizi, kapak, sezon ve bölüm için; quiz çalışma alanına
  başlık, soru, dört şık, görsel alternatif metni ve erişilebilir açıklama için
  `🇬🇧 İngilizce içerik` formları eklendi. Çeviri `PUT` işlemleri audit kaydı üretir.
- Dil seçici TR/EN yazısı yerine erişilebilir Türkiye/Birleşik Krallık bayraklarıyla
  gösterilir. Üst menüde ayrı grid alanına alındı ve quiz çözme sırasında gizlenir.
- Migration, repoda bulunan altı demo katalog özetini ve kullanıcının paylaştığı Rocky
  içerik metni/birinci soru metinlerini İngilizce geri doldurur. Çalışma alanında veya
  erişilebilir yerel veritabanında bulunmayan diğer quiz metinleri uydurulmadı; admin
  formundan girilmeleri gerekir.
- Testler: Java 21 ve yerel Testcontainers altyapısıyla tam backend paketi `133/133`;
  içerik ve quiz çeviri odaklı entegrasyon paketi `28/28`; frontend Vitest `56/56`
  geçti ve üretim derlemesi tamamlandı.
  TypeScript strict kontrolü ve production build başarılı; mevcut büyük PDF/font chunk
  uyarısı devam ediyor.
- Görsel tarayıcı incelemesi yapılmadı; proje kuralı gereği ayrıca açık izin gerekir.
- Karar ADR-0034'te kayıtlıdır. Alternatif olan her dil için ayrı quiz kopyası, geçmiş ve
  XP kimliğini böleceği için seçilmedi.
- Sıradaki tek iş: Yerel admin ekranında kalan mevcut quizlerin İngilizce alanlarını
  kaynak metinlerinden doldurmak; sonrasında istenirse Aşama 13B backend hata metinleri.

## 18.08.2026 Aşama 13A Türkçe–İngilizce arayüz yerelleştirmesi

- Kullanıcı ve yönetici web girişleri ortak, bağımlılıksız ve TypeScript anahtarlarıyla
  kontrol edilen `I18nProvider` üzerinden Türkçe (`tr`) ve İngilizce (`en`) çalışır.
- Dil seçici; kullanıcı/admin giriş ekranları ile oturum açılmış üst menülerde bulunur.
  Tercih yalnız `app_language:v1` altında saklanır; geçersiz veya okunamayan storage
  Türkçeye düşer. Dil değişiminde `html lang` ve belge başlığı da güncellenir.
- Kullanıcı keşif, quiz, cevap geri bildirimi, sonuç, profil ve leaderboard yüzeyleri;
  admin içerik, kapak, sezon/bölüm, quiz yazarlığı, quiz geçmişi ve PDF şablon metinleri
  ortak sözlüklere taşındı. Frontend'in ağ/fallback hata metinleri de seçili dili kullanır.
- Gameplay, doğru cevap, attempt, XP, leaderboard ve backend API sözleşmeleri
  değiştirilmedi. Backend hata yerelleştirmesi ile içerik/quiz veri çevirileri bu aşamaya
  dahil edilmedi.
- Karar: İki dil ve mevcut istemci ölçeğinde harici i18n paketi yerine tip kontrollü
  sözlük kullanıldı. `react-i18next` daha gelişmiş çoğul/namespace desteği sunabilirdi;
  mevcut kapsam için ek bağımlılık ve yapılandırma maliyeti nedeniyle seçilmedi.
  Karar ADR-0033'te kayıtlıdır.
- İş kuralı/test eşleşmesi: sözlük anahtar eşitliği ve parametreler `i18n.test.ts`;
  storage fallback/saklama ve belge dili aynı test dosyası; İngilizce scope ve kullanıcı
  adı fallback'leri `UserApp.test.ts` ile korunur.
- `npm test`: 12 dosyada 56/56 test geçti. `npm run build`: strict TypeScript kontrolü
  ve Vite production derlemesi geçti. Mevcut büyük `pdfmake`/font chunk uyarısı sürüyor.
- React kalite kontrolünde Context değeri memoize edildi, storage erişimi `try/catch`
  ile korundu ve yalnız sürümlü küçük dil kodu saklandı.
- Oracle Cloud Free Tier canlı ortamı (`https://hikayeizi.duckdns.org`): Güncellenen frontend i18n kaynakları, görseller ve rapor dokümanları sunucuya aktarıldı, `compose.production.yaml` ile `web` container'ı sıfırdan derlenip canlıya alındı ve HTTPS yanıtı (HTTP/2 200) doğrulandı.
- GitHub repository (`codex/stage-10a-admin-web`): Tüm değişiklikler, rapor belgeleri, diyagramlar ve görseller commit edilerek pushlandı.
- Sıradaki tek iş: Kullanıcının canlı ortamda Türkçe/İngilizce dil geçişini ve yeni arayüzü denemesi.

## 17.08.2026 soru görseli alternatif metni ile cevap geri bildirimi ve Oracle güncellemesi

- Soru çözüldüğünde (`AnswerReveal`), eğer soru görselinde **alternatif metin** (`alternativeText`) tanımlıysa:
  - Hem doğru hem de yanlış/timeout sonuçlarında, doğru cevap kutucuğu formatında ve başlıksız (`Doğru Cevap:` ibaresi olmadan) doğrudan alternatif metin gösterilir.
  - Alternatif metin bulunduğu durumda doğru cevap metni (`correctOptionText`) gizlenir.
- Eğer soru görselinde alternatif metin yoksa:
  - Yanlış ve süre dolumu durumlarında `Doğru Cevap:` başlığıyla doğru cevap seçeneği gösterilmeye devam eder; doğru cevaplandığında ekstra kutucuk açılmaz.
- `resolveAnswerRevealDetails` saf fonksiyonu eklendi; `UserApp.test.ts` içine 3 yeni birim test eklenerek tüm durumlar (alt metin var/doğru, alt metin var/yanlış, alt metin yok/yanlış, alt metin yok/doğru) doğrulandı.
- Frontend testleri: `11` dosyada `50/50` Vitest testi başarıyla geçti; `tsc --noEmit` ve `npm run build` production derlemesi doğrulandı.
- Oracle Cloud Free Tier canlı ortamı (`https://hikayeizi.duckdns.org`): Güncellenen kaynak dosyalar aktarıldı, `compose.production.yaml` ile `web` container'ı sıfırdan derlenip canlıya alındı ve HTTPS yanıtı (HTTP/2 200) doğrulandı.
- Sıradaki tek iş: Kullanıcının yeni görsel alternatif metinlerini ve quiz akışını canlı ortamda denemesi.

## 17.08.2026 ana README çalıştırma rehberi

- Ana `README.md`; Java, Docker ve Node.js gereksinimleriyle birlikte altyapı,
  backend ve web uygulamasını ayrı adımlarda başlatacak şekilde güncellendi.
- Kullanıcı, yönetim, health ve RabbitMQ adresleri ile yerel portlar açıkça yazıldı.
- İlk admin bootstrap ayarları tek kullanımlık ve şifre sıfırlama SMTP ayarları
  isteğe bağlı olarak belirtildi; secret değerlerin repoya yazılmaması korundu.
- Test ve `docker compose down` komutları eklendi. Uygulama kodu değişmediği için
  yeni test çalıştırılmadı; komutlar mevcut yapılandırma dosyalarıyla doğrulandı.
- Sıradaki tek iş: Yeni geliştiricinin README adımlarını temiz bir ortamda uygulayıp
  eksik bir önkoşul olup olmadığını doğrulaması.

## 14.08.2026 Oracle Cloud Free Tier canlıya alma ve production dağıtımı

- Oracle Cloud Free Tier VM (AMD EPYC, 12 GB RAM, 4 GB Swap) üzerinde `compose.production.yaml` ile tüm mimari başarıyla canlıya alındı.
- Canlı alan adı: `https://hikayeizi.duckdns.org` (Caddy Let's Encrypt otomatik SSL/TLS sertifikası ile).
- Servis topolojisi: 8 izole container (`postgres`, `leaderboard-postgres`, `rabbitmq`, `redis`, `leaderboard-redis`, `leaderboard-service`, `backend`, `web`) birbirine bağlı iç Docker ağında `healthy` durumunda çalışmaktadır. Yalnızca Port 80 ve 443 internete açıktır.
- `RemoteLeaderboardClient` doğrudan `RestClient.builder()` kullanacak şekilde sadeleştirildi; `application.yml` içinde `management.health.mail.enabled: false` yapılarak SMTP bağlantı kontrolü Actuator healthcheck'inden ayrıştırıldı ve Redis timeout değerleri optimize edildi.
- Testler ve doğrulama: Yerel Maven suite'inde `130/130` backend testi geçti; Oracle sunucusunda HTTPS kullanıcı arayüzü (`/`), yönetim paneli (`/admin`) ve API (`/api/v1/...`) erişimleri doğrulandı.
- İlk yönetici hesabı (`admin@hikayeizi.duckdns.org`) ve CSRF/HttpOnly oturum güvenliği devrede.
- Quiz kartlarında gösterilen `Kazanılan XP` sorgusu (`/api/v1/me/quiz-results`), kullanıcının o quizden kazandığı kalıcı XP'yi (`MAX(earned_xp)`) gösterecek şekilde güncellendi. Böylece ADR-0026 kuralı (2. çözüşte 0 ek XP kazanılması ve toplam XP'nin korunması) tam olarak muhafaza edilirken, tekrar çözülen quizlerin kartlarında kazanılmış önceki XP değerinin 0'a düşmesi engellendi.
- Soru görselleri için `MediaController` endpoint'ine `CacheControl.maxAge(30 days).cachePublic().immutable()` eklendi; frontend'de ise kullanıcı cevap verdikten sonra sonuç kartını incelerken bir sonraki sorunun görselini arka planda sessizce önden indiren (preloading) mekanizma uygulandı. Böylece canlı ağ ortamında soru metni ile görselin eşzamanlı ve sıfır gecikmeyle açılması sağlandı.
- Quiz esnasında `Quizden çık` butonuna basıldığında aktif oturumu güvenle tamamlayan `POST /api/v1/attempts/{attemptId}/abandon` uç noktası eklendi. Kullanıcı ilk kez çözüyorsa o ana kadar bildiği sorulardan kazandığı XP hesabına kalıcı olarak işlenir, oturum kapatılır ve kullanıcı quize bir sonraki girişinde yarım kalmış eski soruda takılmak yerine doğrudan 1. sorudan tertemiz başlar.
- Sıradaki tek iş: İlk yönetici girişi sonrasında `.env.production` içindeki `INITIAL_ADMIN_ENABLED=false` yapılarak başlangıç parolasının kaldırılması ve canlı sistem üzerinde smoke kontrollerinin tamamlanması.

## 13.08.2026 cevaptan sonra doğru cevap metni gösterimi

- Kullanıcı soruyu yanlış cevapladığında veya süre dolduğunda çıkan `AnswerReveal` (puan/sonuç) kartına **doğru şıkkın metni** (`correctOptionText`) eklendi.
- Kullanıcı isteği üzerine `Açıklama:` bölümü kaldırıldı; sonuç ekranında yalnızca kazanılan puan ve yanlış/timeout durumlarında doğru cevabın metni gösterilir.
- Güvenlik kuralı korundu: Doğru cevap bilgisi soru çözülmeden önce istemciye **gönderilmez**; yalnızca server-authoritative olarak cevap/timeout kaydedildikten sonra `AnswerFeedback` DTO'su ile dönülür.
- Backend testleri: `GameplayIntegrationTest` dahil olmak üzere `130/130` backend testi geçti.
- Frontend testleri: `11` test dosyasında `47/47` test geçti; `tsc --noEmit` strict kontrolü ve Vite production build'i başarıyla tamamlandı.
- Bilinen konu: Tarayıcıda görsel inceleme yapılmadı; bu proje kuralı gereği ayrı açık izin ister.
- Sıradaki tek iş: Kullanıcının yeni soruları ve quizleri deneyimlemesi.

## 13.08.2026 quiz cevap sonrası sayaç durdurma

- Kullanıcı bir şıkkı seçtiği anda, cevap isteği işlenirken ve cevap sonucu gösterilirken görünür geri sayım durur.
- Kullanıcı sonraki soruya geçtiğinde, sunucunun döndürdüğü yeni soru deadline'ı ile sayaç yeniden başlar.
- Sunucu deadline'ı, timeout ve puanlama kuralları değişmedi; bu yalnızca istemci ekranının doğru quiz durumunu yansıtmasıdır.
- `npm run test`: 11 dosya, 46 test başarılı. `questionTimerDeadline` testi cevap gönderimi/sonucu boyunca sayacın durduğunu, aktif yeni soruda yeniden başladığını korur.
- `npm run build`: TypeScript strict kontrolü ve Vite production build başarılı. Mevcut büyük PDF bundle uyarısı sürüyor.
- Bilinen konu: Tarayıcıda görsel inceleme yapılmadı; bu proje kuralı gereği ayrı açık izin ister.
- Sıradaki tek iş: Kullanıcının seçeceği sonraki ürün veya production hazırlığı işi.

## 13.08.2026 sonuç ekranı sırasında soru süresini koruma ve DATA_INTEGRITY_CONFLICT düzeltmesi

- Cevap ya da timeout sonrasında attempt `AWAITING_NEXT_QUESTION` durumuna geçer; bu sırada sonraki soru için deadline yoktur.
- Kullanıcı `Sonraki soruya geç` düğmesine bastığında yeni endpoint sunucu saatinden yeni 30 saniyelik deadline üretir. Böylece sonuç kartında geçirilen süre sonraki sorudan düşmez.
- `AWAITING_NEXT_QUESTION` durumunun 22 karakter olması ancak veritabanı `status` sütununun `VARCHAR(20)` ile sınırlı olması nedeniyle ortaya çıkan `DATA_INTEGRITY_CONFLICT` hatası, yeni bir `V18__increase_attempt_status_length.sql` migration dosyası oluşturularak düzeltildi.
- Sütun uzunluğu 30 karaktere çıkarıldı ve ilgili ORM varlığı (`JpaQuizAttemptEntity`) bu uzunluğu kullanacak şekilde güncellendi.
- Backend testleri `GameplayIntegrationTest` dahil olmak üzere tamamlandı ve `AWAITING_NEXT_QUESTION` durumu sorunsuzca kaydedildi. `V17__pause_timer_until_next_question.sql` içindeki constraint kontrolleri korunmaktadır.
- Frontend `npm run test`: 11 dosya, 47 test başarılı. `npm run build`: başarılı; mevcut büyük PDF bundle uyarısı sürüyor.

Son kısa durum güncellemesi: 13.08.2026
Son ayrıntılı geçmiş kaydı: 11.08.2026

## Genel durum

Çekirdek backend roadmap'i Aşama 0-9 ve web/ürün dilimleri Aşama 10A-10AA
tamamlanmıştır. Sistem; içerik ve quiz yayınlama, server-authoritative gameplay,
XP, RabbitMQ mesajlaşması, PostgreSQL/Redis leaderboard, admin web, kullanıcı webi
ve yerel hesap/oturum akışlarını içerir.

## 12.08.2026 leaderboard kullanıcı adı sunumu

- Leaderboard dış API satırlarına nullable `displayName` alanı eklendi. Sıralama ve XP
  verisi yine leaderboard servisinden gelir; adlar monolith'in sahip olduğu yerel hesap
  kayıtlarından, cevap hazırlanırken tek toplu sorguyla eklenir.
- `xp.changed.v1` olayına kullanıcı adı eklenmedi. Böylece kişisel sunum verisi RabbitMQ
  ve leaderboard servisinde kopyalanmaz; ileride ad değişikliği olursa güncel hesap adı
  doğrudan gösterilebilir.
- Frontend UUID kısaltması yerine `displayName` gösterir. Yerel hesabı bulunmayan eski
  demo/test kimlikleri için UUID sızdırmadan kişinin kendi satırında `Sen`, diğer
  satırlarda `Kullanıcı` yedeği kullanılır.
- PostgreSQL kullanan `LeaderboardIntegrationTest` içindeki `7/7` test geçti; test dış
  API'nin lider ve mevcut kullanıcı için doğru hesap adını verdiğini kanıtlar.
- Frontend `10/10` test dosyasında `41/41` geçti; strict TypeScript production build'i
  tamamlandı. Yeni frontend testi hesap adı ve güvenli yedek davranışını korur.
- Tam backend `./mvnw.cmd verify` doğrulamasında `125/125` test geçti; mimari sınır
  testleri ve çalıştırılabilir JAR üretimi de başarılı oldu.
- Alternatif olarak kullanıcı adını XP olayına eklemek daha bağımsız bir okuma cevabı
  sağlardı; ancak adın iki sistemde tutulması ve eski kalması nedeniyle seçilmedi.
- Bilinen konu: Local hesap kaydı olmayan tarihî actor kimliklerinde gerçek ad bilinemez;
  bu satırlar güvenli genel etiketle görünür.
- Sıradaki tek iş: Çalışan backend/frontend süreçlerini yeniden başlatıp profil
  leaderboard'unda UUID yerine kayıt sırasında girilen kullanıcı adını manuel görmek.

## 12.08.2026 Aşama 11 leaderboard mikroservisi

- Çekirdek uygulama modüler monolith kalırken leaderboard ayrı
  `leaderboard-service` Spring Boot uygulamasına çıkarıldı.
- Monolith XP ledger'ın kalıcı sahibidir. Her yeni XP transaction'ı ile sürümlü
  `xp.changed.v1` olayı aynı PostgreSQL transaction'ında Outbox'a yazılır.
- Servis olayı kendi RabbitMQ kuyruğundan tüketir; `event_id` primary key ve
  `transaction_id` unique constraint ile duplicate teslimat ikinci XP etkisi
  üretmez.
- Servis monolith tablolarını okumaz. Kendi PostgreSQL projeksiyonundan toplam XP,
  ilk XP zamanı ve UUID tie-break'iyle global/içerik sıralaması üretir.
- Redis ayrı servisin yeniden oluşturulabilir read model'idir; Redis kesintisinde
  servis kendi PostgreSQL'ine düşer.
- Dış leaderboard API'si değişmedi. `LEADERBOARD_REMOTE_ENABLED=true` ile monolith
  internal servise yönlenir; eski yerel modül kontrollü rollback için geçici olarak
  korunur.
- Mevcut XP geçmişi ADMIN yetkili `replay-xp-events` endpoint'iyle idempotent olarak
  Outbox'a alınabilir. Bu, RabbitMQ'nun geçmiş event log'u olmaması nedeniyle gereken
  bootstrap yoludur.
- Compose'a ayrı leaderboard PostgreSQL (`5434`), Redis (`6381`) ve servis (`8082`)
  eklendi. Kalıcı karar ADR-0031'de kayıtlıdır.
- Leaderboard servisinde PostgreSQL, gerçek RabbitMQ ve gerçek Redis kullanan `4/4`
  test geçti. Monolith hedefli event/messaging testlerinde `5/5`, düzeltilen gameplay
  Outbox sözleşmesi testinde `1/1` test geçti.
- Tam monolith `./mvnw verify` doğrulaması `125/125` geçti ve çalıştırılabilir JAR
  üretildi. İlk koşudaki eski Outbox toplam-satır beklentisi olay türlerine bağlanarak
  düzeltildi; quiz ve XP olaylarının ayrı ayrı tek kayıt olduğu kanıtlandı.
- Bilinen konu: Production internal API ağ politikası/servis kimliği ve büyük veri
  için batch/cursor replay bu yerel öğrenme aşamasının dışındadır.
- Sıradaki tek iş: XP replay → remote bayrağı → eski/yeni sonuç eşitliği manuel
  kontrolü. Eşitlik görülmeden eski monolith leaderboard kodu silinmez.

## 12.08.2026 şifre sıfırlama ve MailerSend

- Giriş ekranına `Şifremi unuttum` akışı, e-posta isteme görünümü ve bağlantıdan
  açılan yeni şifre görünümü eklendi.
- Backend genel `202` cevaplı istek endpoint'i ve tek kullanımlık reset endpoint'i
  sunar; kayıtlı hesap olup olmadığı istemciye açıklanmaz.
- 256 bit rastgele token'ın yalnız SHA-256 özeti PostgreSQL'de tutulur. Token 30
  dakika geçerlidir, ikinci kullanım engellenir ve yeni istek eski aktif token'ı
  geçersiz kılar.
- Yeni şifre bcrypt sınırından kaydedilir. Ham token, API/Secret Key, şifre veya
  e-posta adresi loglanmaz.
- E-posta MailerSend'in standart SMTP relay'i üzerinden gönderilir. SMTP username,
  SMTP password ve doğrulanmış domain altındaki gönderen adres yalnız ortam
  değişkenlerinden okunur.
- Alternatif MailerSend HTTP API'si sağlayıcıya özel bağımlılık; RabbitMQ/Outbox ise bu
  tek akış için ek event ve worker maliyeti doğuracağı için seçilmedi. Karar
  ADR-0030'da kayıtlıdır.
- Frontend `40/40` testi ve strict TypeScript production build geçti.
- PostgreSQL Testcontainers kullanan `AccountAuthenticationIntegrationTest` içindeki
  `4/4` test geçti. Şifre sıfırlama testi genel cevabı, token özetini, tek kullanımı,
  eski şifrenin reddini ve yeni şifrenin kabulünü kanıtlar.
- `./mvnw verify` sonucu backend paketinin tamamında `124/124` test geçti; çalıştırılabilir
  JAR üretildi. Bu sonuç yeni mail bağımlılığı ve migration'ın mevcut PostgreSQL,
  RabbitMQ, Redis, güvenlik ve modül sınırı testlerini bozmadığını kanıtlar.
- SMTP sağlayıcısı Mailjet'ten MailerSend'e çevrildikten sonra backend derlemesi
  yeniden geçti ve çalışma alanında Mailjet yapılandırma referansı kalmadığı doğrulandı.
- Bilinen sorun: Merkezi session store olmadığı için önceden açık diğer oturumlar
  şifre değişiminde otomatik kapanmaz. Gerçek MailerSend teslim testi, kullanıcı
  SMTP bilgilerini terminal ortamına ekledikten sonra yapılmalıdır.
- Sıradaki tek iş: Doğrulanmış MailerSend domaini ve terminal ortam değişkenleriyle
  tek bir gerçek alıcıya uçtan uca sıfırlama e-postası göndermek.

## 12.08.2026 Tabii esintili giriş ekranı

- Kullanıcı giriş/kayıt ekranı; Tabii logolu üst alan, koyu medya zemini, ortalanmış
  yarı saydam hesap kartı ve Tabii yeşili ana aksiyonla sade biçimde yenilendi.
- Mevcut e-posta/şifre, hesap oluşturma ve sunucu oturumu davranışı değiştirilmedi;
  backend'de karşılığı olmayan şifre sıfırlama veya sosyal giriş eklenmedi.
- Yeni frontend paketi ya da tasarım sistemi eklenmedi. Mevcut React bileşeni,
  mevcut medya dosyaları ve CSS kullanıldı.
- `npm run test` sonucu 10 test dosyasında `39/39` test geçti. Bu testler mevcut
  kullanıcı akışındaki API ve iş kuralı yardımcılarının bozulmadığını gösterir.
- `npm run build` strict TypeScript kontrolü ve Vite production build ile geçti.
- Karar: Bu değişiklik yalnız sunum katmanıdır; kimlik doğrulama sınırı ADR-0029'daki
  yerel hesap ve HttpOnly sunucu oturumu olarak kalır. Hazır UI kütüphanesi daha hızlı
  ortak bileşen sağlayabilirdi, fakat tek ekran için bağımlılık ve soyutlama maliyeti
  doğuracağı için seçilmedi.
- Öğrenilen kavram: Görsel yenileme ile kimlik doğrulama davranışı ayrı tutulduğunda
  UI değişirken güvenlik ve oturum sözleşmesi sabit kalabilir. Kullanıcıyla mobil ve
  masaüstü görünümün görsel olarak doğrulanması gerekir.
- Bilinen sorun: Proje kuralı gereği ayrıca açık izin alınmadan sonuç ekranının
  tarayıcı tabanlı görsel kalite incelemesi yapılmadı.
- Sıradaki tek iş: Kullanıcı izin verirse giriş ve kayıt modlarını masaüstü/mobil
  genişliklerde görsel olarak kontrol edip yalnız gözlenen yerleşim kusurlarını düzeltmek.

## 12.08.2026 dokümantasyon arşivleme

- Uzun `README.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md` ve
  `docs/CURRENT_STATE.md` dosyaları değiştirilmeden
  `docs/archive/2026-08-11/` altında korundu.
- Günlük AI görevlerinde okunacak kısa sürümler aynı yollarla yeniden oluşturuldu.
- Eski ayrıntılar kaybolmadı; gerektiğinde arşiv bağlantılarından açılabilir.

## Tamamlanan başlıca davranışlar

- İçerik ve quiz sürümleri yayınlanmadan kullanıcıya görünmez.
- Yayınlanmış quiz sürümü değişmez; düzenleme yeni draft sürümü üretir.
- Kullanıcı attempt'i, cevabı ve timeout'u sunucu saatiyle doğrulanır.
- Aynı soru ikinci kez cevaplanamaz; tekrar komutlar idempotent işlenir.
- Skor her doğru cevap için 10'dur; yanlış ve timeout 0 puandır.
- Quiz başına yalnız ilk tamamlamada XP ödülü vardır.
- İlk tamamlaması 0 puan olan kullanıcı için de tek `amount = 0` ledger kaydı tutulur.
- Attempt ve Outbox aynı PostgreSQL transaction'ında kesinleşir.
- RabbitMQ duplicate teslimatları Inbox ve unique kurallarıyla ikinci XP üretmez.
- Redis leaderboard PostgreSQL'den yeniden kurulabilir ve kesintide fallback vardır.
- Kullanıcı hesabı bcrypt parola özeti ve HttpOnly, SameSite=Strict sunucu oturumu kullanır.
- Admin web gerçek sunucu oturumunu kullanır; local/test actor yalnız API test ve
  yerel geliştirme adaptörü olarak production dışında kalır.

## Teknoloji temeli

- Java 21, Spring Boot 4.1.0, Maven Wrapper
- PostgreSQL 17.5 ve Flyway
- RabbitMQ 4.1 ve Spring AMQP
- Redis 8.2 ve Spring Data Redis
- Testcontainers, OpenTelemetry, Prometheus, Grafana, Tempo
- React 19, TypeScript, Vite ve Vitest

## Bilinen açık konular

- Kurumun production OIDC/JWT, issuer/audience ve rol claim sözleşmesi.
- Hedef Oracle dağıtımı, domain/DNS, sunucu secret erişimi, otomatik dış backup ve
  ileride gerekirse object storage/CDN.
- KVKK silme/anonimleştirme ve hukuk onaylı retention süreleri.
- Gerçek trafik hedefleri, production SLO ve Redis fallback alarm eşikleri.
- Çok editörlü quiz draft düzenlemesi için optimistic locking.
- Dönemsel leaderboard, hile/diskalifiye, profil adı/avatar ve sosyal özellikler.

## Test ve doğrulama özeti

- Java 21 ile tam backend paketi `129/129`, leaderboard servisi `4/4` geçti.
- Frontend testleri `44/44`, TypeScript strict kontrolü ve production build geçti.
- PostgreSQL, RabbitMQ ve Redis Testcontainers senaryoları kullanıldı.
- PDF dosya üretimi doğrulandı; PDF sayfalarının görsel kalite incelemesi kullanıcı izni olmadığı için yapılmadı.
- Yerel k6 ve operasyon doğrulamaları başlangıç ölçümüdür; production kapasite garantisi değildir.

## Sıradaki tek iş

Yeni bir roadmap dilimi seçilene kadar yeni özellik eklenmeyecek. Sıradaki operasyon
adımı Oracle VM'de production Compose smoke testi ve ilk dış backup/restore provasıdır.

## Yeni görev için kısa bağlam

```text
AGENTS.md ile kısa README, PROJECT_BRIEF, ARCHITECTURE, ROADMAP ve CURRENT_STATE
dosyalarını oku. Aşama 0-12 tamamlandı. Kullanıcı yeni bir roadmap dilimi seçmeden
XP, leaderboard, production kimliği veya sosyal özellik ekleme.
```

## Arşiv

[Ayrıntılı current state geçmişi](archive/2026-08-11/CURRENT_STATE.md)

## 12.08.2026 kod öğrenme haritası - başlangıç ve gamification dilimi

- `docs/LEARNING_MAP` altında gerçek kaynak yollarını aynalayan açıklama alanı
  oluşturuldu. Açıklama dosyaları derlemeye girmez ve çalışan kodun yerine geçmez.
- İlk dilim, Spring Boot başlangıç sınıfı ile gamification modülünün API, application,
  domain, port, PostgreSQL adapter ve test kanıtlarını kapsar.
- `XpController` ve `XpService` metotları gerçek dosyadaki sırayla; amaç, girdi,
  çıktı, bağımlılık, hata davranışı, transaction ve sistem akışındaki yer bilgileriyle
  açıklandı.
- Karar: Yaklaşık 250 Java ve TypeScript dosyasının tamamını tek seferde ikinci kez yazmak yerine,
  gerçek koddan kopma riskini azaltmak için use-case bazlı ve küçük öğrenme dilimleri
  kullanılacak. Her açıklama gerçek kaynak yolunu gösterecek.
- Alternatif birebir yalancı kod kopyası daha tanıdık bir görünüm sağlayabilirdi;
  ancak davranış değiştiğinde sessizce eski kalma ve gerçek kodla iki ayrı doğru kaynak
  oluşturma riski nedeniyle seçilmedi.
- Test çalıştırılmadı; üretim kodu, yapılandırma ve test kodu değişmedi. Yeni Markdown
  yollarının ve başlıklarının varlığı dosya kontrolüyle doğrulanacaktır.
- Öğrenilen kavram: Controller HTTP sınırıdır, service kullanım senaryosu ve transaction
  sınırıdır, domain geçerli XP işlemini tanımlar, repository portu JDBC ayrıntısını
  application katmanından ayırır.
- Bilinen konu: Harita henüz bütün modülleri kapsamaz. Açıklama ile kod çelişirse çalışan
  kod ve testler doğru kaynaktır.
- Sıradaki tek iş: Kullanıcı bir sonraki öğrenme dilimini seçerse ana kullanıcı akışını
  takip eden `quiz keşfi -> attempt başlat -> cevapla -> tamamla` zincirini aynı biçimde
  açıklamak.

## 12.08.2026 quiz erişilebilirlik metni görünürlüğü

- Kullanıcı quiz ekranında `accessiblePrompt` artık sorunun altında görünür paragraf olarak gösterilmiyor.
- Backend/API alanı, admin soru formu ve cevap anahtarlı PDF akışı korunuyor; yalnızca normal quiz görünümü sadeleştirildi.
- `npm run test`: 10 dosya, 41 test başarılı.
- `npm run build`: TypeScript strict kontrolü ve Vite production build başarılı.
- Görsel tarayıcı incelemesi yapılmadı; proje kuralı gereği bunun için ayrıca açık izin gerekir.

## 12.08.2026 quiz soru puntosu

- Kullanıcı quiz ekranındaki soru başlığı küçültüldü; masaüstünde yaklaşık `1.8rem`, küçük ekranlarda yaklaşık `1.2rem` sınırında çalışır.
- Değişiklik yalnızca `admin-web/src/user/user-styles.css` içindeki sunum stilini etkiler; soru verisi, gameplay ve erişilebilirlik sözleşmeleri korunur.
- `npm run test`: 10 dosya, 41 test başarılı.
- `npm run build`: TypeScript strict kontrolü ve Vite production build başarılı; mevcut büyük PDF bundle uyarısı devam ediyor.

## 12.08.2026 quiz kartlarında kazanılan XP

- Kullanıcının tamamladığı quizler için son tamamlanmış denemenin `earnedXp` değeri backend'den okunuyor.
- Quiz kartlarında yalnızca `Kazanılan XP: …` gösteriliyor; skor bilgisi karta veya XP özet endpoint'ine eklenmiyor.
- Hiç çözülmemiş quizlerde XP satırı görünmüyor; tekrar çözümden gelen `0 XP` değeri görünür kalıyor.
- Yeni `GET /api/v1/me/quiz-results` endpoint'i yalnızca oturum sahibinin quiz sonuç özetlerini döndürüyor.
- Backend gameplay entegrasyon testleri `12/12`, frontend testleri `43/43` geçti; production build başarılı.
- Build sırasında mevcut büyük PDF bundle uyarısı devam ediyor.

## 12.08.2026 eski backend ile quiz keşfi uyumluluğu

- Yeni XP özet endpoint'i henüz yeniden başlatılmamış eski backend'de bulunamadığında kullanıcı quiz sayfası artık tamamen hata ekranına düşmüyor.
- XP bilgisi endpoint mevcutsa gösteriliyor; eski backend ile yalnızca XP satırı geçici olarak boş kalıyor.
- Çalışan `8081` backend'i Java 21 ile yeniden başlatıldı ve `GET /api/v1/me/quiz-results` isteği `200 []` döndürdü.

## 12.08.2026 tek sunucu production hazırlığı

- Oracle Always Free A1 hedefi için backend, frontend ve leaderboard servisi ayrı
  container'lara; PostgreSQL, RabbitMQ, Redis ve medya kalıcı/geçici volume sınırlarına
  yerleştirilen `compose.production.yaml` hazırlandı. İnternete yalnız Caddy `80/443`
  portlarını yayımlar.
- Caddy kullanıcı uygulamasını `/`, yönetim uygulamasını `/admin`, backend API'sini
  aynı origin altında `/api` yolunda sunar ve HTTPS sertifikasını yönetir.
- Production profili güvenli HttpOnly oturum cookie'si ve SPA CSRF cookie/header
  doğrulaması kullanır. Token olmadan yazma isteği reddedilir. Local/test geçici actor
  kolaylığı production'da bulunmaz.
- Admin web artık gerçek backend oturumunu ve backend'den gelen `ADMIN/EDITOR`
  rollerini kullanır. Herkese açık kayıt yalnız `USER` üretmeye devam eder.
- İlk `ADMIN`, yalnız sistemde hiç admin yokken ve başlangıç bayrağı açıkken bir kez
  oluşturulur. Parola bcrypt özetiyle saklanır; başarılı ilk girişten sonra bayrak
  kapatılıp başlangıç parolası ortam dosyasından silinmelidir.
- Tek sunucu seçimi ücretsiz ve sade başlangıç sağlar; karşılığı tek hata noktasıdır.
  Keycloak/OIDC daha güçlü merkezi kimlik sunar ama ilk demo için ek kaynak ve bakım
  getirir. Karar ADR-0032'de kayıtlıdır.
- `./mvnw.cmd verify`: monolith `129/129` test geçti ve çalıştırılabilir JAR üretildi.
- `./mvnw.cmd -f leaderboard-service/pom.xml test`: servis `4/4` test geçti.
- Frontend Vitest `44/44` geçti; strict TypeScript ve Vite production build başarılı.
  Mevcut büyük PDF bundle uyarısı devam ediyor.
- İş kuralı/test eşleşmesi: ilk admin tekliği ve parola özeti
  `InitialAdminBootstrapIntegrationTest`; CSRF reddi/kabulü
  `CsrfProtectionIntegrationTest`; dış port sınırı
  `ProductionComposeConfigurationTest`; istemci token header'ı frontend CSRF testiyle
  korunur.
- Bilinen konular: Yerel makinede Docker CLI PATH'te olmadığı için production
  image'ları ve tam Compose kümesi burada ayağa kaldırılmadı. Oracle VM/DNS/firewall,
  MailerSend doğrulanmış domain, gerçek HTTPS smoke testi ve ayrı konuma otomatik
  backup/restore henüz yapılmadı. Backend restartında bellek içi oturumlar kaybolur.
- Sıradaki tek iş: Oracle VM'yi hazırlayıp production Compose'u hedefte çalıştırmak;
  kullanıcı/admin/şifre sıfırlama/leaderboard smoke testleri ile ilk şifreli dış
  yedek ve restore provasını tamamlamak.
