# ADR-0005: Aşama 1 Geçici Kimlik Yaklaşımı

## Durum

Accepted

## Bağlam

Content ve gameplay kaynakları yazılmadan önce güvenilir kullanıcı kimliği, rol
kontrolü ve kaynak sahipliği sınırı gereklidir. Kurumun production kimlik
sağlayıcısı ve OIDC/JWT sözleşmesi henüz bilinmemektedir.

İstemciden request body içinde alınan `userId` güvenilir kimlik sayılamaz.
Diğer yandan sağlayıcı bilinmeden Keycloak veya başka bir OIDC ürününe bağlanmak
yanlış production sözleşmesini erkenden sabitleyebilir.

## Karar

- Spring Security istek ve metot seviyesinde authorization sağlayacaktır.
- `USER`, `EDITOR` ve `ADMIN` rolleri identity domain dilinde modellenecektir.
- Application use case'leri aktörü `CurrentActorProvider` portundan alacaktır;
  request body aktör kaynağı olmayacaktır.
- Geçici `X-Test-Actor-Id` ve `X-Test-Actor-Roles` header adapter'ı yalnız
  `local` ve `test` profillerinde etkin olacaktır.
- Varsayılan profilde test header'ları kabul edilmeyecek ve korunan API'ler
  production sağlayıcısı bağlanana kadar güvenli biçimde kapalı kalacaktır.
- Yerel parola veritabanı oluşturulmayacak, parola veya token loglanmayacaktır.
- Gerçek sağlayıcı belirlendiğinde geçici adapter OIDC/JWT adapter'ıyla
  değiştirilecek; application portu korunacaktır.

## Gerekçe

- Sonraki modüller authentication ürününden bağımsız, güvenilir aktör
  sözleşmesine dayanır.
- Sahte kimlik mekanizması production'da yanlışlıkla etkin olmaz.
- Authentication ile authorization ayrı sorumluluklar olarak öğrenilir.
- Parola saklama, resetleme ve credential güvenliği gibi henüz gerekmeyen riskler
  projeye eklenmez.

## Alternatifler

- Şimdiden Keycloak/OIDC kurmak production'a daha yakın olurdu; ancak kurum
  sağlayıcısı bilinmediği için yanlış issuer, claim ve rol eşleme sözleşmesini
  sabitleme riski taşır.
- HTTP Basic daha küçük bir kurulumdur; fakat parola saklama ve doğrulama
  sorumluluğu doğurur ve hedeflenen dış kimlik sağlayıcısı yönüyle uyumsuzdur.
- Controller'ın header'ı doğrudan okuması daha az sınıf gerektirir; ancak
  framework ve kimlik ayrıntısını use case'lere sızdırır.

## Sonuçlar

- Yerel identity denemeleri için `local` profili açıkça etkinleştirilmelidir.
- Test header'ına sahip olmak production kimliği anlamına gelmez.
- Kurum sağlayıcısı belli olduğunda yeni bir ADR ve gerçek authentication
  integration testleri gerekir.
- Kaynak sahipliği kontrolleri Aşama 2 ve gameplay use case'lerinde
  `CurrentActor` üzerinden kurulabilir.

## Yeniden değerlendirme tetikleyicileri

- Kurumun OIDC/OAuth2 sağlayıcısı ve claim sözleşmesi öğrenildiğinde
- Gerçek kullanıcı onboarding veya parola yönetimi proje kapsamına alındığında
- Service-to-service kimlik veya farklı actor türü gerektiğinde
