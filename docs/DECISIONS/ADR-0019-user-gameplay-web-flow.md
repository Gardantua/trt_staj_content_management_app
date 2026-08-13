# ADR-0019: Kullanıcı webinde sunucu otoriteli gameplay akışı

## Durum

Accepted

Zamanlama seçimi kararı ADR-0022 ile geçersiz kılındı; diğer istemci güvenliği ve
idempotency kararları geçerlidir.

## Bağlam

Kullanıcı kataloğu tek başına etkileşimli ürün akışını tamamlamıyordu. Backend,
attempt, cevap, sonuç, XP ve leaderboard sözleşmelerini zaten sunuyordu; ancak
istemcinin bu sözleşmeleri doğru cevap veya skor üretmeden kullanması gerekliydi.

## Karar

- Kullanıcı girişi keşfet, içerik detayı, quiz, sonuç, profil ve sıralama
  ekranlarıyla genişletilir.
- Kullanıcı menüsündeki ayrı `Quizler` ekranı bütün yayımlanmış quizleri ad, kapsam,
  soru sayısı ve bağlı içeriğin ana görseliyle listeler. Kart bağlı içerik adını ve
  görselini public katalogdan çözümler; keşif listesi soru veya seçenekleri taşımaz.
  Quiz/içerik adı araması ile içerik/quiz/soru sayısı sıralaması, yeni ağ isteği
  üretmeden yüklenmiş güvenli özetlere uygulanır. Ayrı kapsam filtresi gösterilmez.
  Yayın tarihi özette bulunmadığı sürece `En yeni` sıralaması sunulmaz.
- `Keşfet` kataloğu yüklenmiş bütün yayınlarda tek metin araması yapar. Aynı
  tarayıcıda açılan içerik kimlikleri `localStorage` içinde son tıklanandan eskiye
  tutulur; bu kayıt sunucu profili veya cihazlar arası geçmiş olarak yorumlanmaz.
- Dizi detayında bağımsız sezon/bölüm rehberi gösterilmez; ekran içerik bilgisi ve
  quizlere odaklanır. Hiyerarşi verisi silinmez ve admin yönetiminde kalır; bölüm
  kapsamlı quizler mevcut veri varsa gerçek sezon ve bölüm numarasıyla etiketlenir.
- Quiz başlangıcında kullanıcı süre seçmez; ADR-0022 uyarınca her soru için 30
  saniyelik deadline ve puan sunucunun response'undan okunur.
- Her cevap isteği istemcinin ürettiği bir `Idempotency-Key` taşır. Ağ hatasında
  aynı question/option/key üçlüsü tekrar kullanılır.
- Doğru seçenek ve doğruluk yalnız answer response sonrasında gösterilir.
- Profil, XP özeti ile global leaderboard'u paralel okur; XP toplamının
  asenkron consumer nedeniyle kısa süreli eski kalabileceğini açıklar. Ayrı
  sıralama menüsü kullanılmaz; global sıra ve Top 10 profil içinde gösterilir.
  Eski sıralama route'u profile yönlenir.
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

### Her içerik için ayrı quiz isteği atmak

Yeni backend endpoint'i gerektirmezdi; fakat içerik sayısı kadar istek üretip
rate-limit tüketirdi. Tek güvenli özet endpoint'i seçildi.

### Her arama ve filtre değişikliğini backend'e göndermek

Çok büyük kataloglarda daha iyi ölçeklenirdi; mevcut veri hacminde her tuşta ek ağ
isteği ve yeni API sözleşmesi üretirdi. Yüklenmiş özetlerde istemci filtresi seçildi;
quiz hacmi tarayıcı performansını etkilerse yeniden değerlendirilecektir.

### Son tıklama geçmişini backend'de saklamak

Cihazlar arası ortak bir sıra sağlardı; fakat kalıcı kullanıcı profili, yeni yazma
endpoint'i ve veri saklama politikası gerektirirdi. MVP'de yalnız aynı tarayıcıdaki
küçük kimlik listesi seçildi.

### Sıralamayı bağımsız bir kullanıcı sayfasında tutmak

Daha geniş bir leaderboard alanı sağlardı; fakat mevcut küçük kullanıcı akışında
profilde zaten global sıra gösterilirken ikinci bir menü ve sayfa tekrarına yol
açıyordu. Top 10 listesi profile taşındı.

### Sezon ve bölüm rehberini kullanıcı detayında tutmak

İçerik kataloğunu daha kapsamlı gösterirdi; ancak quiz bulunmayan uzun bölüm listeleri
ana etkileşimi geri plana atıyordu. Rehber kullanıcı ekranından kaldırıldı; veri ve
admin yönetimi korundu.

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
