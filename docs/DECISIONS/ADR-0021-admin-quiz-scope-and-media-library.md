# ADR-0021: Admin quiz kapsamı ve yeniden kullanılabilir medya kütüphanesi

## Durum

Accepted; medya kütüphanesi UI kararı 11.08.2026'da değiştirildi

## Bağlam

Admin web içerik ve kapak yönetebiliyor, fakat quiz taslağı ile soru/şık
yazarlığını tamamlayamıyordu. Ayrıca yüklenen görseller yalnız kimlikleri
biliniyorsa yeniden bağlanabiliyordu. Dizi quizlerinin içerik geneli, belirli
sezon veya belirli bölüm için hazırlanması gerekirken bu yerleşim ilişkisi
kalıcı modelde açık değildi.

## Karar

- Her quiz tam olarak bir `CONTENT`, `SEASON` veya `EPISODE` kapsamına sahiptir.
  Sezon kapsamı `seasonId`, bölüm kapsamı ise hem `seasonId` hem `episodeId`
  taşır. Film quizleri içerik geneli kalır.
- Application katmanı seçilen sezon ve bölümün quizin içeriğine ait olduğunu
  content modülünün yayımlanmış doğrulama portu üzerinden denetler.
- PostgreSQL check ve bileşik foreign key constraint'leri aynı ilişkiyi veri
  tabanında da korur. Böylece uygulama hatası veya yarış koşulu yetim/çapraz
  içerik kapsamı üretemez.
- Admin içerik detayı, o içeriğe ait quiz özetlerini listeler; taslak sürüm,
  soru, doğru cevap ve erişilebilir görsel bilgisi aynı çalışma
  alanından yönetilir. Yayınlama ve arşivleme 10E'nin ayrı kontrol dilimidir.
- Yüklenmiş görseller sayfalı bir admin endpoint'iyle listelenir. Kapak ve soru
  editörleri aynı değişmez medya kimliğini yeniden bağlayabilir; dosya yerinde
  değiştirilmez.
- Kullanıcı quiz DTO'su yalnız kapsam bilgisini gösterir. Doğru cevap bilgisi
  admin sözleşmesinden dışarı çıkmaz.

## Gerekçe

Kapsamı başlık metnine veya etikete gömmek sorgulanabilir ve doğrulanabilir bir
iş kuralı üretmez. Açık enum ile ilişkisel kimlikler hem admin formunu hem public
sunumu kararlı hale getirir. İlişkiyi yalnız uygulama kodunda doğrulamak yerine
veri tabanı constraint'iyle tekrarlamak kalıcı veri bütünlüğünü korur.

Medya için dosya kopyalamak veya yeniden yüklemek yerine değişmez kimliği tekrar
kullanmak depolama tekrarını önler. Sayfalı özet endpoint'i de binary içeriği
liste cevabına gömmez; önizleme gerektiğinde mevcut korumalı content endpoint'i
ayrıca çağrılır.

## Alternatifler

- Quiz kapsamını serbest etiket veya yalnız `episodeId` ile tutmak daha az alan
  gerektirirdi; ancak içerik/sezon aidiyetini ve sezon bazlı sorguyu belirsiz
  bırakırdı.
- Sezon ve bölüm için ayrı quiz tabloları kullanılabilirdi; üç benzer yaşam
  döngüsü ve API üretip modüler monolith içinde gereksiz çoğaltma yaratırdı.
- Medya dosyasını her kullanımda yeniden yüklemek liste endpoint'ini gereksiz
  kılardı; fakat aynı görsel için farklı kimlikler, depolama tekrarı ve içerik
  takibi zorluğu oluştururdu.

## Sonuçlar

- Eski quiz kayıtları migration varsayılanıyla `CONTENT` kapsamındadır.
- İçerik, sezon ve bölüm aidiyeti hem domain/application hem PostgreSQL
  seviyesinde korunur.
- Admin artık yazarlığı tamamlar; yayın kararı ve uygunluk özeti bilinçli olarak
  ayrı 10E aşamasında kalır.
- Medya listesi büyüdükçe sayfalama korunur; istemci kapalı soru editörlerini
  gecikmeli açarak gereksiz önizleme isteklerini üretmez.

## Yeniden değerlendirme tetikleyicileri

- Bir quizin birden fazla bölüm veya sezona bağlanması ürün gereksinimi olursa
- Medya arama, etiket, kullanım hakkı veya silme/retention politikası netleşirse
- Çok editörlü eşzamanlı quiz düzenleme ihtiyacı optimistic locking gerektirirse

## 11.08.2026 değişikliği

Kullanıcı geri bildirimiyle kapak ve soru editörlerine gömülü sayfalı görsel
kütüphanesi kaldırıldı. Editör her kullanımda ilgili formdan yeni dosya yükler.
Backend'in sayfalı medya endpoint'i, değişmez medya kimlikleri ve mevcut bağlı
görseller geriye uyumluluk ve veri bütünlüğü için korunur. Bu değişiklik quiz
kapsamı, medya doğrulaması veya erişilebilirlik kurallarını değiştirmez.

Alternatif olarak kütüphaneyi gizlenebilir bir panel hâline getirmek yeniden
kullanımı korurdu; ancak kullanıcı editör akışında bu yüzeyi istemediği için daha
sade doğrudan yükleme seçildi. Bedeli, aynı dosyanın farklı kullanımlarda yeniden
yüklenebilmesi ve yeni medya kimlikleri üretebilmesidir.
