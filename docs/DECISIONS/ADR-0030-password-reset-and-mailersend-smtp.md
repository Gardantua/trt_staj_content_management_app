# ADR-0030: Tek kullanımlık şifre sıfırlama ve MailerSend SMTP

Durum: Accepted

Tarih: 12.08.2026

## Bağlam

Yerel kullanıcı hesabında şifresini unutan kişinin hesabına yeniden erişebilmesi
gerekiyordu. Uygulamanın gerçek e-posta göndermesi için kullanıcı MailerSend hesabı
sağladı. SMTP parolasını kaynak koda bağlamak, ham sıfırlama token'ını veritabanında
tutmak veya hesabın varlığını HTTP cevabında açıklamak güvenlik riski yaratırdı.

## Karar

- Uygulama MailerSend'e standart SMTP üzerinden `smtp.mailersend.net:587` ve
  STARTTLS ile bağlanır.
- MailerSend panelinde doğrulanmış domain için üretilen SMTP username ve SMTP
  password yalnız ortam değişkenlerinden okunur. Bunlar API token değildir.
- Gönderen adresi doğrulanmış domain altında seçilir ve
  `MAILERSEND_FROM_ADDRESS` ile verilir.
- Şifre sıfırlama isteği kayıtlı ve kayıtsız e-posta için aynı `202 Accepted`
  cevabını döndürür.
- Rastgele 256 bit token 30 dakika geçerlidir ve yalnız bir kez kullanılabilir.
- PostgreSQL'de ham token yerine SHA-256 özeti tutulur. Yeni istek, hesabın önceki
  kullanılmamış token'larını geçersiz kılar.
- Yeni şifre mevcut delegating password encoder/bcrypt sınırından geçirilir.
- SMTP teslim hatası token transaction'ını geri alır; endpoint yine genel `202`
  döndürür ve logda e-posta adresi ya da token bulunmaz.
- Frontend bağlantıyı `resetToken` query parametresiyle açar ve token'ı başka bir
  depoya yazmadan doğrudan reset endpoint'ine gönderir.

## Alternatifler

- MailerSend HTTP API daha gelişmiş template ve teslimat özellikleri sunar. Mevcut
  tek metin e-postası için sağlayıcıya özel bağımlılık oluşturacağı için seçilmedi.
- RabbitMQ/Outbox e-posta teslimatını retry edilebilir yapardı. Bu küçük yerel hesap
  akışında ek event, worker ve hata yönetimi oluşturacağı için ertelendi.
- Token'ı düz metin saklamak uygulamayı kısaltırdı; veritabanı okunursa aktif hesap
  ele geçirme bağlantıları açığa çıkacağı için reddedildi.

## Sonuçlar

Yerel uygulama, internet üzerinden gerçek şifre sıfırlama e-postası gönderebilir.
MailerSend SMTP bilgileri veya doğrulanmış gönderen domain yoksa gerçek teslim
yapılamaz. Production'da domain DNS kayıtları ve kurumun secret yönetimi ayrıca
tamamlanmalıdır. Merkezi session store bulunmadığından şifre yenileme, önceden açık
diğer sunucu oturumlarını otomatik kapatmaz; production kimlik aşamasında bu açıkça
ele alınmalıdır.
