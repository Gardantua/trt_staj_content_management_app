# ADR-0001: Modüler Monolith ile Başlama

## Durum

Accepted

## Bağlam

Proje başlangıçta tek geliştirici veya küçük ekip tarafından, staj süresi
içinde geliştirilecektir. Domain sınırları bulunmasına rağmen bağımsız
deployment, ayrı ekip sahipliği veya farklı ölçek ihtiyacı henüz
kanıtlanmamıştır.

İlk günden mikroservis kullanmak; servisler arası iletişim, dağıtık tracing,
deployment, sözleşme versiyonlama ve hata yönetimi maliyetini artıracaktır.

## Karar

Sistem tek deploy edilebilir uygulama olan modüler monolith olarak
başlatılacaktır.

Identity, content, quiz, gameplay, gamification, leaderboard ve admin
modüllerinin kod ve veri sahipliği sınırları korunacaktır.

## Gerekçe

- MVP geliştirme hızını artırır.
- PostgreSQL transaction'larından yararlanmayı kolaylaştırır.
- Yerel geliştirme ve test ortamını sade tutar.
- Domain sınırları korunursa ileride servis ayrıştırmasına engel olmaz.

## Sonuçlar

- Modüller birbirlerinin tablolarına doğrudan erişmemelidir.
- İş yaptıran modüller arası çağrılar yayınlanmış application API/port
  üzerinden; gerçekleşmiş iş gerçeklerinin bildirimi olaylar üzerinden
  yapılmalıdır.
- Process içi domain event, güvenilir ve dışarıya taşınan integration event ile
  aynı kabul edilmemelidir.
- Aynı deployment içindeki modül sınırları kod incelemesi ve testlerle
  korunmalıdır.
- Mikroservis operasyon altyapısı MVP kapsamında kurulmayacaktır.

## Yeniden değerlendirme tetikleyicileri

- Bir modülün bağımsız ve sürekli ölçeklenmesi gerekirse
- Farklı ekip sahipliği oluşursa
- Farklı SLA veya hata izolasyonu gerekirse
- Bağımsız deployment gerçek teslim hızını artıracaksa
- Modüler monolith içindeki bağımlılıklar ölçülebilir sorun oluşturursa
