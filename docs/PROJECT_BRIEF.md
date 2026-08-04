# Proje Özeti

## Problem

TRT ve tabii içerikleri çoğunlukla izleme deneyimi olarak sunulur. Bu proje,
içerikleri quiz ve oyunlaştırma yoluyla etkileşimli hale getirecek tekrar
kullanılabilir bir backend altyapısı oluşturmayı hedefler.

## Çözüm

Kullanıcı bir dizi veya bölüm için quiz başlatır, soruları cevaplar ve sunucu
tarafından puanlanır. Tamamlanan quiz sonucunda XP ve sıralama bilgisi oluşur.
Editörler içerik, quiz ve soruları yönetebilir.

MVP tek bir dikey akışa indirgenmiştir: **yayınla → başlat → cevapla → tamamla
→ XP ver → sırala**. Bir özellik bu akış için gerekli değilse ilk sürümden
çıkarılır veya sonraki aşamaya ertelenir.

Quiz, platformun ilk kullanım senaryosudur. İleride aynı altyapı:

- Dil öğrenme
- Eğitim içeriği
- Arkadaş meydan okumaları
- TV ve telefon eşleşmesi
- Canlı yarışmalar

için genişletilebilir. Bu özellikler MVP kapsamında değildir.

## Hedef kullanıcılar

- tabii izleyicileri
- İçerik ve quiz editörleri
- Yönetici ve operasyon ekipleri
- Gelecekte tabii web, mobil ve Smart TV istemcileri

## MVP kullanıcı akışı

1. Kullanıcı giriş yapar.
2. İçerik ve bölüm seçer.
3. Yayındaki quiz sürümünü başlatır.
4. Soruları süre içinde cevaplar.
5. Sunucu cevapları doğrular ve puanlar.
6. Kullanıcı sonucu ve kazandığı XP'yi görür.
7. Kullanıcı global veya içerik bazlı sıralamasını görür.

## MVP yönetici akışı

1. Yönetici içerik, sezon ve bölüm oluşturur.
2. Quiz ve draft sürümü oluşturur.
3. Soru, seçenek, doğru cevap ve zorluk tanımlar.
4. Quiz sürümünü yayınlar.
5. Yayındaki sürüm değiştirilmez; değişiklik yeni sürüm oluşturur.

## Fonksiyonel gereksinimler

### Çekirdek

- İçerik, sezon ve bölüm hiyerarşisi
- Quiz, soru, seçenek ve yayınlanan quiz sürümü
- Attempt yaşam döngüsü
- Süre kontrollü cevap kabulü ve sunucu taraflı puanlama
- Kullanıcı ve yönetici yetkilendirmesi
- Her soruda sürüme sabitlenmiş kapak veya soruya özel görsel
- Görsele dayalı sorular için cevabı sızdırmayan eşdeğer erişilebilir sunum
- Kullanıcının engel/sağlık verisini toplamadan ayarlanabilir quiz süresi
- Tekrarlı işlem üretmeyen XP kaydı
- Global ve içerik bazlı leaderboard

### Destekleyici

- Admin audit kayıtları
- OpenAPI sözleşmesi
- Kararlı hata kodu ve trace ID
- API'den asenkron consumer'a W3C trace continuation
- Abuse/rate limit kontrolü ve dependency/secret güvenlik kapıları
- Ölçülebilir p95/p99, hata oranı ve tekrarlanabilir yük profilleri
- Migration ve otomatik testler
- Kimlik ve kaynak sahipliği kontrollerinin attempt akışından önce kurulması

## Fonksiyonel olmayan gereksinimler

- API-first tasarım
- Veri bütünlüğü ve idempotency
- Güvenli sunucu taraflı puanlama
- Test edilebilir modüler mimari
- İzlenebilir hata ve işlem kayıtları
- Yatay ölçeklenmeye uygun stateless uygulama
- Yeni quiz türlerine genişleyebilir domain modeli
- WCAG 2.2 AA'yı hedefleyen API ve medya sözleşmeleri

## MVP dışında

- Tam sosyal ağ
- Mesajlaşma ve yorum sistemi
- Canlı TV entegrasyonu
- İngilizce öğrenme/eğitim modu
- Rozet sistemi
- AI tarafından otomatik yayınlanan sorular
- Çok bölgeli production dağıtımı
- İlk günden mikroservis mimarisi
- Kubernetes zorunluluğu
- Gelişmiş medya işleme, canlı caption üretimi ve gerçek TRT/tabii
  entegrasyonu

## Başarı ölçütleri

- Aynı soruya ikinci cevap veri katmanında engellenir.
- Aynı complete isteği ikinci kez XP üretmez.
- Doğru cevap quiz başlamadan istemciye sızmaz.
- Görsel soru, alternatif metin veya erişilebilir soru sunumu yoluyla doğru
  cevabı istemeden açığa çıkarmaz.
- Yayınlanan her soru, sürüme sabitlenmiş bir görsel ve uygun metin alternatifi
  ile okunabilir.
- Quiz süresi kullanıcı tarafından engel/sağlık verisi vermeden erişilebilir
  aralıkta ayarlanabilir.
- Yayındaki quiz sürümü geçmiş attempt'leri değiştirmez.
- Kullanıcı yalnız kendi attempt ve sonuçlarına erişir.
- Süre, skor ve attempt kapanma davranışları sunucu saatine göre deterministiktir.
- XP, RabbitMQ olmadan da PostgreSQL üzerinde güvenilir biçimde üretilebilir.
- RabbitMQ aşamasında quiz tamamlama olayı Outbox üzerinden güvenilir biçimde
  yayımlanır ve tekrar teslimat ikinci XP işlemi üretmez.
- Leaderboard sonucu Redis olmadan önce PostgreSQL üzerinde doğru ve
  deterministik olarak hesaplanabilir.
- PostgreSQL, RabbitMQ ve Redis kesintileri kalıcı veri kaybı üretmez.
- PostgreSQL backup'ı ayrı bir veritabanına geri yüklenebilir.
- Dependency envanteri transitive SBOM olarak üretilebilir ve bilinen açıklara
  karşı CI'da taranabilir.
- Redis leaderboard PostgreSQL sonucuyla aynı sırayı verir; silindiğinde yeniden
  kurulur ve kesintisinde kalıcı XP kaybı yaşanmaz.
- Kritik domain ve entegrasyon testleri otomatik çalışır.
- API OpenAPI üzerinden anlaşılır biçimde belgelenir.

## Öğrenme başarısı

Proje, yalnız teknik kabul kriterleri sağlandığında değil, kullanıcı aşağıdaki
soruları kendi cümleleriyle açıklayabildiğinde amacına ulaşmış sayılır:

- Bir HTTP isteği controller'dan veritabanına hangi sınırlardan geçer?
- Domain kuralı ile veritabanı constraint'i neden birlikte gerekir?
- Transaction nerede başlar, hangi veriler birlikte kesinleşir?
- Idempotency ile optimistic locking/unique constraint hangi farklı sorunları
  çözer?
- PostgreSQL neden doğru kaynak, Redis neden yeniden üretilebilir katmandır?
- RabbitMQ neden çekirdek akıştan sonra eklenir ve Outbox hangi hatayı önler?
- Domain event ile dış sistemlere taşınan integration event arasındaki fark nedir?
- Bir test hangi riski kanıtlar; unit ve integration test neden farklıdır?
