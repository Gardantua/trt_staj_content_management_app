# ADR-0018: Kullanıcı web kataloğu için ayrı giriş noktası

## Durum

Superseded by ADR-0019

## Bağlam

Admin web uygulaması editörlerin draft içeriği yönetmesi için oluşturulmuştu.
Kullanıcı, yayınlanmış içeriği keşfedebileceği bir sayfa istedi. Yönetim
arayüzünü kullanıcı görünümüyle aynı route veya rol kontrolü altında büyütmek,
iki deneyimin bilgi mimarisini ve local-only actor sınırını belirsizleştirirdi.

## Karar

- Aynı `admin-web` Vite çalışma zamanında ikinci giriş noktası olarak
  `user.html` eklenir.
- Kullanıcı sayfası yalnız `GET /api/v1/contents`,
  `GET /api/v1/contents/{contentId}` ve
  `GET /api/v1/contents/{contentId}/quizzes` yayınlanmış kaynaklarını okur.
- İlk dilim katalog, içerik detayı, dizi sezon/bölüm bilgisi ve quiz özeti
  gösterir. Attempt başlatma, cevaplama, XP ve leaderboard ekranları kapsam
  dışındadır.
- Yerel geliştirmede geçici `USER` actor header'ı kullanılır. Bu header
  production login değildir; API yetkisi backend'de kalır.
- Sayfalar, mobilde tek sütuna inen ve API kaynaklarıyla hizalı ekran
  sınırlarıyla tasarlanır; native mobil istemci bu sözleşmeleri tekrar
  kullanabilir.

## Gerekçe

İkinci HTML giriş noktası, yeni bir paket veya ikinci dependency zinciri
oluşturmadan kullanıcı ve yönetici kullanıcı deneyimini ayırır. Ayrı bir
repository ya da framework seçimi, bu küçük read-only dilim için gereksiz
dağıtım ve bakım maliyeti yaratırdı.

## Alternatifler

### Admin uygulamasına rol tabanlı tek ekran eklemek

Daha az dosya üretirdi; ancak editör işlemleri ile kullanıcı keşfini aynı
uygulama akışında karıştırırdı. Reddedildi.

### Quiz çözme akışını aynı anda eklemek

Kullanıcıya daha tamamlanmış görünen bir deneyim sunardı; fakat süre,
idempotency, sonuç geri bildirimi ve doğru cevap sızıntısı için ayrı kabul
kriterleri gerekir. Reddedildi.

## Sonuçlar

- Kullanıcı sayfası yalnız yayınlanmış verilere dayanır; taslak içerik görünmez.
- `PublishedQuizResponse` doğru cevap taşımadığı için özet görünümü cevap
  güvenliğini zayıflatmaz.
- Production OIDC, CORS/CSRF ve route hosting davranışı hâlâ ayrı bir karar
  gerektirir.

## Yeniden değerlendirme tetikleyicileri

- Quiz attempt UX'i için kullanıcı akışı açıkça onaylandığında
- Kurumun frontend hosting veya OIDC sözleşmesi belirlendiğinde
- Kullanıcı deneyiminin SSR, offline cache ya da bağımsız mobil release
  döngüsü gerektirecek ölçüde büyümesi durumunda
