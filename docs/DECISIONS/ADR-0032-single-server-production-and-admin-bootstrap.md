# ADR-0032: Tek sunucu production dağıtımı ve ilk yönetici hesabı

## Durum

Accepted — 12.08.2026

## Bağlam

İlk canlı demo için en düşük maliyetli hedef Oracle Cloud Always Free A1 sanal
makinesidir. Uygulama; monolith backend, leaderboard servisi, iki PostgreSQL,
RabbitMQ, iki Redis ve web katmanından oluşur. Kullanıcı hesabı ve sunucu oturumu
hazırdır; fakat admin web yerel geliştirme header'larına bağlıydı, CSRF koruması
kapalıydı ve production çalıştırma paketi yoktu.

## Karar

- Bileşenler ilk aşamada tek Oracle sunucusunda Docker Compose ile çalışır.
- Yalnız Caddy'nin `80/443` portları internete açılır. Backend, veritabanları,
  RabbitMQ, Redis ve leaderboard servisi yalnız Compose ağı içinde kalır.
- Caddy HTTPS sertifikasını yönetir; `/` kullanıcı uygulamasını, `/admin` yönetim
  uygulamasını, `/api` ise backend'i aynı origin altında sunar.
- Production profili güvenli oturum cookie'si ve SPA uyumlu CSRF cookie/header
  doğrulaması kullanır. Tarayıcıdaki rol kontrolü yalnız görünüm içindir; gerçek
  `ADMIN/EDITOR` yetkisi backend'de uygulanır.
- Herkese açık kayıt yalnız `USER` üretir. İlk `ADMIN`, yalnız açıkça etkinleştirilen
  başlangıç ayarıyla ve sistemde hiç admin yokken oluşturulur. İlk başarılı açılıştan
  sonra ayar kapatılır ve başlangıç parolası ortam dosyasından silinir.
- PostgreSQL, RabbitMQ ve yüklenen medya kalıcı volume kullanır. Redis verisi
  yeniden üretilebilir olduğu için kalıcı volume zorunlu değildir.

## Gerekçe ve alternatifler

Tek sunucu, ücretsiz başlangıç ve sade operasyon sağlar. Bedeli tek hata noktası ve
sınırlı kaynak paylaşımıdır. Yönetici rolünü doğrudan SQL ile vermek daha kısa olurdu;
uygulama iş kuralını atladığı ve yanlış hesabı yükseltme riski taşıdığı için seçilmedi.
Keycloak/OIDC merkezi kimlik ve daha güçlü oturum yönetimi sağlayabilirdi; ilk demo
için ek servis ve bakım maliyeti doğurduğu, kurum kimlik sözleşmesi de henüz belli
olmadığı için ertelendi. Yönetilen veritabanı daha dayanıklıdır fakat ücretsiz tek
sunucu hedefini bozar.

## Sonuçlar

- Sunucu veya disk kaybı bütün sistemi etkileyebilir; volume backup değildir.
- Backend yeniden başladığında bellek içi oturumlar kaybolur ve kullanıcı tekrar
  giriş yapar; kalıcı session store ancak gerçek ihtiyaç oluşursa eklenir.
- Gerçek domain, MailerSend doğrulanmış gönderici domaini, güçlü secret'lar ve ayrı
  konumda düzenli PostgreSQL/medya yedeği canlı açılış önkoşuludur.
- Oracle hesabı, VM, DNS, firewall, image build ve restore provası bu repository
  değişikliğinin dışında, hedef sunucuda ayrıca doğrulanacaktır.

## Test eşleştirmesi

- `InitialAdminBootstrapIntegrationTest`: ilk adminin bir kez oluşturulduğunu,
  rolünü ve parolanın yalnız bcrypt özetiyle saklandığını kanıtlar.
- `CsrfProtectionIntegrationTest`: token olmadan yazma isteğinin reddedildiğini,
  CSRF cookie/header çiftiyle kabul edildiğini kanıtlar.
- `ProductionComposeConfigurationTest`: production Compose içindeki servisleri ve
  yalnız web servisinin host portu yayımladığını korur.
- Frontend CSRF testleri, yazma isteklerinde cookie token'ının doğru header ile
  gönderildiğini korur.

## Yeniden değerlendirme tetikleyicileri

- İkinci uygulama instance'ı veya kesintisiz dağıtım ihtiyacı
- Kurumsal OIDC/SSO sözleşmesinin kesinleşmesi
- Tek sunucu kapasitesinin veya kabul edilen RPO/RTO'nun yetersiz kalması
- Ücretli yönetilen veritabanı/object storage bütçesinin açılması
