# ADR-0017: Admin Web Uygulaması ve Yerel Kimlik Sınırı

## Durum

Accepted

## Bağlam

Çekirdek backend Aşama 0–9 ile tamamlandıktan sonra editörlerin içerik, sezon ve
bölüm use case'lerini doğrudan HTTP istemcisi kullanmadan yönetebilmesi istendi.
Repoda frontend çalışma zamanı bulunmuyordu. Kurumun production OIDC/JWT
sağlayıcısı, rol claim sözleşmesi ve frontend dağıtım topolojisi ise hâlâ belli
değildir.

İlk arayüz dilimine quiz, medya yükleme, XP ve leaderboard operasyonlarını aynı
anda eklemek hem roadmap'in tek-aşama kuralını bozar hem de doğru cevap ve
yetkilendirme sözleşmelerini gereksiz yere genişletirdi.

## Karar

- Yönetim arayüzü aynı repoda, backend'den ayrı `admin-web` uygulaması olarak
  React, TypeScript ve Vite ile geliştirilecektir.
- Aşama 10A yalnız içerik listesi, draft oluşturma/düzenleme, sezon/bölüm
  yönetimi ve publish isteğinin backend hata davranışını kapsayacaktır.
- Quiz authoring, medya yükleme, XP düzeltmesi ve leaderboard operasyonları bu
  aşamanın dışında kalacaktır.
- Admin listeleme için `GET /api/v1/admin/contents?page=&size=` tam aggregate
  yerine hafif, sayfalı yönetim özetleri döndürecektir. Detay ve hiyerarşi
  mevcut `GET /api/v1/admin/contents/{contentId}` yolundan okunacaktır.
- Vite geliştirme proxy'si yerel backend'e aynı-origin görünümü sağlayacaktır.
  Production CORS, CSRF ve dağıtım kararı gerçek topoloji belli olmadan
  sabitlenmeyecektir.
- `VITE_LOCAL_ACTOR_*` değerleri yalnız yerel geliştirme kolaylığıdır. Tarayıcı
  rol kontrolü güvenlik sınırı değildir; gerçek authorization Spring Security
  ve backend rollerinde kalır. Varsayılan backend profilinde test header'ları
  kimlik üretmez.
- API hata zarfındaki kararlı `code` ve `traceId` kullanıcıya güvenli biçimde
  gösterilecektir.
- Arayüz klavye kullanımı, görünür odak, etiketli native kontroller, renk dışı
  hata durumu ve canlı hata bildirimiyle WCAG 2.2 AA hedefine hazırlanacaktır.

## Gerekçe

- Ayrı uygulama, frontend build zincirini backend domain ve Maven yaşam
  döngüsünden ayırırken tek repo içinde sözleşme değişikliklerini birlikte
  incelemeyi sağlar.
- React ve TypeScript form durumunu ve API sözleşmesini açık tiplerle yönetir;
  Vite bu küçük istemci için düşük yapılandırma maliyeti sunar.
- Hafif liste DTO'su sezon/bölüm koleksiyonlarını her satır için yüklemeyerek
  gereksiz sorgu ve payload büyümesini önler.
- Yerel kimliği production login gibi göstermemek, bilinmeyen issuer/claim
  sözleşmesini erkenden sabitlemez.
- Küçük dikey dilim, backend'in değişmez yayın ve rol kurallarını arayüzde
  görünür kılarken sonraki modülleri ayrı kabul kriterlerine bırakır.

## Alternatifler

- Spring MVC/Thymeleaf tek build ve aynı-origin dağıtımını kolaylaştırırdı;
  ancak frontend yaşam döngüsünü backend'e bağlar ve ayrı istemci sözleşmesini
  öğrenme hedefini azaltırdı.
- Next.js hazır routing ve server rendering sunardı; mevcut local-only yönetim
  istemcisinde SSR veya ayrı Node sunucusu gereksinimi olmadığı için ek çalışma
  zamanı ve dağıtım kararı doğururdu.
- Bütün admin modüllerini tek aşamada geliştirmek daha erken geniş ekran kapsamı
  sağlardı; fakat test, güvenlik ve erişilebilirlik risklerini tek doğrulanabilir
  dilimin dışına taşırdı.
- Public içerik listesini kullanmak yeni endpoint gerektirmezdi; draft içerikleri
  gizlediği ve admin sözleşmesini yanlış temsil ettiği için reddedildi.

## Sonuçlar

- Frontend bağımlılıkları `admin-web/package-lock.json` ile sabitlenir ve Maven
  doğrulamasından ayrı `npm run test` ile `npm run build` kapılarına sahiptir.
- Yerel geliştirmede backend `local` profiliyle, frontend ise Vite proxy ile
  çalıştırılır.
- Yeni draft'ı başarıyla yayımlamak için gereken medya yükleme ve kapak bağlama
  arayüzü ayrı bir sonraki ürün dilimidir; mevcut publish butonu backend'in
  publish önkoşulu hatalarını code ve trace ID ile gösterir.
- Production authentication ve hosting topolojisi gelmeden bu arayüz production
  hazır kabul edilmez.

## Yeniden değerlendirme tetikleyicileri

- Kurumun OIDC/OAuth2 sağlayıcısı, issuer/audience ve rol claim sözleşmesi
  belirlendiğinde
- Frontend ile backend farklı origin veya ayrı deployment olarak yayınlandığında
- Aynı draft üzerinde eşzamanlı çok editör ihtiyacı ortaya çıktığında
- Admin ekranlarının SSR, büyük ölçekli routing veya ortak tasarım sistemi
  gerektirecek ölçüde büyümesi durumunda
