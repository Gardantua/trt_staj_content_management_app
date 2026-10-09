# ADR-0033: Frontend arayüz yerelleştirmesi

## Durum

Accepted

## Bağlam

Kullanıcı ve yönetici web uygulamalarındaki sabit sunum metinleri doğrudan React
bileşenlerinde Türkçe yazılmıştı. Aynı Vite uygulamasındaki iki giriş noktasının
Türkçe ve İngilizce çalışması, eksik çeviri anahtarlarının derlemede yakalanması ve
dil seçiminin kişisel veri toplamadan aynı tarayıcıda korunması istendi.

Bu aşama yalnız arayüz metinlerini kapsar. Backend hata mesajlarının
`Accept-Language` ile yerelleştirilmesi ve içerik/quiz metinlerinin kalıcı çok dilli
veri modeli ayrı aşamalardır.

## Karar

- Desteklenen arayüz dilleri `tr` ve `en`, varsayılan ve fallback dil Türkçe olacaktır.
- `admin-web/src/i18n` altında ortak React Context, tip kontrollü sözlükler ve
  erişilebilir bir dil seçici kullanılacaktır.
- Türkçe sözlüğün anahtar birleşimi derleme sözleşmesi olacak; İngilizce sözlük aynı
  anahtarların tamamını sağlamak zorunda olacaktır.
- Parametreli metinler adlandırılmış `{parameter}` yer tutucularıyla biçimlendirilecektir.
- Dil tercihi yalnız `app_language:v1` anahtarında `tr` veya `en` olarak tutulacak;
  storage okunamazsa ya da değer geçersizse Türkçeye düşülecektir.
- Dil değiştiğinde React Context tüketicileri yeniden render edilecek, `html lang`
  ve belge başlığı güncellenecektir.
- Kullanıcı ve admin girişleri, ana ekranları, formları, hata yüzeyleri ve admin quiz
  PDF şablon metinleri aynı sözlüğü kullanacaktır.
- İçerik, quiz, soru ve seçeneklerin backend'den gelen domain metinleri bu aşamada
  çevrilmeyecektir.

## Gerekçe

İki dil ve mevcut küçük istemci yüzeyi için bağımlılıksız çözüm yeni çalışma zamanı
paketi ve yapılandırma maliyeti getirmez. TypeScript anahtar sözleşmesi, bir dilde
bulunup diğerinde unutulan metni production yerine derleme sırasında yakalar.
Ortak provider iki HTML giriş noktasının aynı dil tercihini kullanmasını sağlar.

## Alternatifler

- `react-i18next` çoğul kuralları, namespace, lazy sözlük yükleme ve daha geniş
  ekosistem sunardı. Mevcut iki dil ve sınırlı metin hacmi için ek bağımlılık ve
  yapılandırma maliyeti nedeniyle seçilmedi. Dil veya çeviri ekibi büyürse yeniden
  değerlendirilebilir.
- Dili yalnız bileşen state'inde tutmak daha az kod üretirdi; yenilemede tercihi
  kaybeder ve kullanıcı/admin giriş noktaları arasında ortak davranış sağlamazdı.
- Tarayıcı dilini otomatik varsaymak İngilizce tarayıcılarda daha hızlı uyum
  sağlayabilirdi; ürünün mevcut Türkçe varsayılanını sessizce değiştireceği için
  seçilmedi.

## Sonuçlar

- Yeni arayüz metni ekleyen geliştirici iki sözlüğü de güncellemelidir.
- Sözlükler başlangıç paketine eklenir; ölçülmüş bundle sorunu oluşursa namespace ve
  dinamik yükleme değerlendirilebilir.
- Dil tercihi hesaplar veya cihazlar arasında senkronize edilmez.
- Backend mesajları ve domain içeriği çevrilene kadar İngilizce arayüzde bu kaynaklardan
  gelen Türkçe metinler görülebilir.
- Görsel yerleşim kontrolü proje kuralı gereği ayrıca kullanıcı izni ister.

## Yeniden değerlendirme tetikleyicileri

- Üçüncü bir dil veya çeviri yönetim sistemi gereksinimi
- ICU çoğul kuralları ya da karmaşık tarih/sayı biçimlendirme ihtiyacı
- Sözlüklerin ölçülmüş başlangıç bundle veya bakım sorunu oluşturması
- Kullanıcı hesabında cihazlar arası dil tercihi gereksinimi
