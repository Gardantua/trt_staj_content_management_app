# ADR-0019: Kullanıcı webinde sunucu otoriteli gameplay akışı

## Durum

Accepted

## Bağlam

Kullanıcı kataloğu tek başına etkileşimli ürün akışını tamamlamıyordu. Backend,
attempt, cevap, sonuç, XP ve leaderboard sözleşmelerini zaten sunuyordu; ancak
istemcinin bu sözleşmeleri doğru cevap veya skor üretmeden kullanması gerekliydi.

## Karar

- Kullanıcı girişi keşfet, içerik detayı, quiz, sonuç, profil ve sıralama
  ekranlarıyla genişletilir.
- Quiz başlangıcında kullanıcı `STANDARD_V1` veya `EXTENDED_V1` süresini seçer;
  deadline ve puan sunucunun response'undan okunur.
- Her cevap isteği istemcinin ürettiği bir `Idempotency-Key` taşır. Ağ hatasında
  aynı question/option/key üçlüsü tekrar kullanılır.
- Doğru seçenek ve doğruluk yalnız answer response sonrasında gösterilir.
- Profil, XP özeti ile global leaderboard'u paralel okur; XP toplamının
  asenkron consumer nedeniyle kısa süreli eski kalabileceğini açıklar.
- Yerel UUID kapısı yalnız geliştirme kolaylığıdır; production login, kayıt veya
  parola sistemi olarak sunulmaz.

## Gerekçe

Mevcut backend sözleşmesinden doğrudan yararlanmak, ikinci bir skor veya süre
otoritesi üretmeden uçtan uca quiz deneyimini kanıtlar. Idempotency anahtarını
arayüzde de korumak, HTTP başarısızlığında kullanıcının güvenli tekrar deneyimi
ile kalıcı veri bütünlüğünü bağlar.

## Alternatifler

### Skoru ve doğru cevabı tarayıcıda hesaplamak

Daha hızlı görünen bir akış sunardı; ancak doğru cevabı önceden sızdırır ve
istemciyi güvenlik otoritesi yapardı. Reddedildi.

### Ağ hatasında yeni cevap isteği üretmek

Uygulaması daha kısa olurdu; fakat kullanıcı aynı cevabı güvenli biçimde tekrar
gönderemezdi. Reddedildi.

### Yerel kapıyı gerçek login gibi markalamak

Demo deneyimi daha pürüzsüz görünürdü; fakat bilinmeyen OIDC sözleşmesini
yanlış temsil ederdi. Reddedildi.

## Sonuçlar

- Kullanıcı deneyimi backend'in süre, sıra, skor ve ownership kurallarına
  bağlıdır; istemci yalnız niyet bildirir.
- Quiz bitimindeki XP, consumer çalışana kadar profil toplamından geçici olarak
  farklı olabilir; kullanıcıya bu sınır açıkça gösterilir.
- Production OIDC, token saklama, CSRF/CORS, profil verisi ve hosting ayrı
  güvenlik kararları olarak kalır.

## Yeniden değerlendirme tetikleyicileri

- Kurum kimlik sağlayıcısı ve claim sözleşmesi belirlendiğinde
- Offline veya çok cihazlı attempt devam ettirme gereksinimi oluştuğunda
- Native mobil istemci için ortak deep-link ve session sözleşmesi belirlendiğinde
