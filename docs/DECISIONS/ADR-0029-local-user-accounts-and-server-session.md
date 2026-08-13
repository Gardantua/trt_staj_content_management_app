# ADR-0029: Yerel kullanıcı hesapları ve sunucu oturumu

Durum: Accepted

Tarih: 11.08.2026

## Bağlam

Kullanıcı webi geçici UUID girişiyle çalışıyordu. Bu, API geliştirmesini
kolaylaştırsa da gerçek bir kullanıcının kendi quiz geçmişi, XP'si ve sıralamasıyla
tekrar giriş yapmasını sağlamıyordu. Kurumun production kimlik sağlayıcısı ve OIDC
sözleşmesi hâlâ bilinmiyor.

## Karar

- Yerel geliştirme için e-posta, görünen ad ve şifreyle hesap oluşturulur.
- E-posta küçük harfe dönüştürülerek benzersiz saklanır; benzersizlik PostgreSQL
  constraint'iyle de korunur.
- Şifre düz metin saklanmaz; Spring Security delegating password encoder ve bcrypt
  kullanılır.
- Kayıt olan hesap yalnız `USER` rolü alır. `EDITOR` veya `ADMIN` rolü kullanıcı
  isteğiyle atanamaz.
- Başarılı kayıt/girişten sonra kimlik, HttpOnly ve SameSite=Strict çerezli sunucu
  oturumunda tutulur. Girişte session kimliği değiştirilir.
- Mevcut `CurrentActorProvider` sınırı korunur; gameplay, XP ve leaderboard
  modülleri hesabın nasıl doğrulandığını bilmez.
- Geçici header adapter'ı admin geliştirmesi ve otomatik testler için yalnız
  `local`/`test` profillerinde kalır.

## Alternatifler

- JWT access/refresh token: Ayrı mobil istemci veya servisler arası token ihtiyacı
  olmadığı için seçilmedi. Token yenileme, iptal ve güvenli tarayıcı saklama yükü
  getirirdi.
- Kurumsal OIDC: Production için daha doğru adaydır; issuer, client ve rol claim
  sözleşmesi bilinmediğinden bu aşamada uygulanmadı.

## Sonuçlar

Kullanıcı artık hatırlaması zor UUID yerine hesabıyla giriş yapar ve tüm oyun
verisi kalıcı hesap UUID'sine bağlanır. Bu karar production kimliği sayılmaz.
Mevcut CSRF kapalı yapı yalnız yerel geliştirme sınırındadır; dış erişim kararı
alınırsa OIDC veya CSRF token'lı güvenli oturum ayrı aşamada tamamlanmalıdır.

