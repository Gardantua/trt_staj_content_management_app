# Yönetim Arayüzü Deneyim Planı

Bu belge, mevcut Aşama 10A içerik yönetimi akışının görsel yenileme planıdır.
Yeni backend endpoint'i, yeni kullanıcı rolü veya yeni ürün modülü tanımlamaz.

## Tasarım yönü

- Tabii'nin koyu, sinematik ve editoryal tonundan ilham alınır; logo, ekran
  yerleşimi veya marka varlıkları kopyalanmaz.
- Ana palet koyu mürekkep, sıcak kâğıt yüzeyi ve ölçülü kiremit vurgudan oluşur.
  Bu, kritik yayın eylemini belirginleştirir; hata ve başarı durumları kendi
  anlamlı renkleriyle ayrı kalır.
- Taslak içeriğe, gerçek kapak gelene kadar tipografik bir poster alanı verilir.
  Böylece boş/yer tutucu görsel kullanan yapay bir galeri hissi oluşmaz.
- Serif başlıklar editoryal ritmi, sans-serif arayüz metinleri günlük çalışma
  hızını destekler. Ekran metni kısa, eylem metni doğrudandır.

## Bilgi mimarisi

## Uygulanan görsel revizyon (10.08.2026)

- Admin çalışma alanı artık gradyensiz açık nötr yüzey, koyu metin ve mint ana
  eylem rengini kullanır. Turuncu/kiremit vurgu kaldırılmıştır.
- Taslak posterleri gradyen yerine düz, türü ayırt eden renk bloklarıyla
  gösterilir. Durum bilgisi rozet ve metinle de taşınır.
- Bu yalnız sunum katmanı değişikliğidir; endpoint, rol, publish kuralı veya
  backend sözleşmesi eklemez/değiştirmez.

1. **Katalog:** İçeriğin türü, yayın durumu, özeti ve tek bir açma eylemi.
2. **Taslak oluşturma:** Önce zorunlu katalog bilgisi; ayrıntılar sonraki adımda.
3. **İçerik detayı:** Durum, yayın eylemi, temel bilgi ve dizi ise sezon/bölüm
   editörü. Yayındaki içerik değiştirilemez olarak görünür.

Bu üç ekran, mobil uygulamanın da aynı ekran sınırlarını kullanabilmesi için
API kaynaklarıyla bire bir hizalı tutulur. Görsel bileşenler veri taşımadığı
için daha sonra React Native, Flutter veya native istemcide tekrar üretilebilir.

## Mobil ilkeler

- Masaüstünde katalog satırı poster, bilgi ve eylemden oluşur; 760 px altında
  eylem kendi satırına iner, 430 px altında tam genişlikte olur.
- Gezinme ve sayfalama dokunmaya uygun en az 44 px hedef boyutunu korur.
- Formlar tek sütuna iner; uzun metinler ve aktör kimliği taşma yerine kırılır.
- Renk tek bilgi taşıyıcısı değildir: taslak/yayın durumu metin ve rozetle de
  belirtilir. Klavye odağı görünür kalır.

## Sonraki onay gerektiren iş

Yol haritasındaki sonraki bağımsız dilim medya yükleme ve içerik kapağı
bağlamadır. Bu yenileme o dilimi başlatmaz; poster alanı gerçek kapak API'si
geldiğinde yalnızca sunum katmanında değiştirilecektir.
