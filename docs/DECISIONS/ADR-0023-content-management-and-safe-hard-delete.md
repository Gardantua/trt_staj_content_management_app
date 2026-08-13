# ADR-0023: İçerik yönetimi ve güvenli kalıcı silme

## Durum

Accepted — 10.08.2026

## Bağlam

Editörün yayımlanmış dizi ve filmlerin başlık, açıklama, kapak ve alternatif
metnini düzeltebilmesi; içerik oluşturma/düzenleme işinin quiz yazarlığından ayrı
bir ekranda anlaşılır biçimde sunulması istendi. Önceki taslak-only yaklaşım bu
operasyonel ihtiyacı karşılamıyordu. Ayrıca “sil” işleminin arşivleme değil,
gerçek kalıcı silme olması istendi.

Quiz attempt'leri ve XP işlem defteri geçmiş bir oyun sonucunun denetlenebilir
kayıtlarıdır. İçerik silinirken bu kayıtları zincirleme yok etmek puan ve audit
bütünlüğünü bozar.

## Karar

- Admin içinde ayrı salt-okunur katalog tutulmaz. İçerik oluşturma, isimle arama
  ve düzenleme listesi tek **İçerik yönetimi** ekranında birleşir; `/` ve stüdyo
  logosu bu ekrana açılır. Yayınlanmış içeriklerin kullanıcı kataloğu ayrı kullanıcı
  girişinde kalır.

- Yayımlanmış içeriğin başlık, açıklama, kapak ve kapak alternatif metni yerinde
  güncellenebilir. İçerik türü değişmez; mevcut sezon/bölümler korunurken yalnız
  sona yeni kayıt eklenebilmesi ADR-0027 ile tanımlanmıştır.
- İçerik kataloğu ile quiz yazarlığı ayrı admin sekmelerinde sunulur. Quizler
  içerik detayındaki ayrı `Quizler` sekmesinden yönetilir.
- İçerik silme fiziksel satır silmedir. Hiç oynanmamış içeriğe bağlı quiz
  tanımları ve sürümleri foreign key cascade ile birlikte silinir.
- İçeriğe bağlı bir gameplay attempt'i varsa silme reddedilir ve API
  `CONTENT_DELETE_HAS_GAMEPLAY_HISTORY` koduyla `409 Conflict` döndürür.
- On sorudan az quiz için sayfadan ayrılma uyarısı bir editör deneyimi
  korumasıdır; tek başına backend yayınlama minimumu değildir.
- Yayınlanmış quiz sürümleri değişmez kalır; içerik metadata düzenlemesi bu
  sürümleme kuralını gevşetmez.

## Gerekçe

Kapak ve açıklama düzeltmesi yeni bir quiz sürümü gerektirmeyen içerik metadata
bakımıdır. Buna karşılık soru ve doğru cevapların yerinde değişmesi geçmiş
attempt'lerin anlamını değiştirir. Bu nedenle iki değişiklik farklı sınırlarda
tutulur.

Gerçek silme talebini karşılayan en güvenli sınır, henüz oyun geçmişi olmayan
içeriği fiziksel silmek; geçmişi olan içeriği ise açık bir conflict ile
korumaktır. Alternatif olarak attempt ve XP ledger'ı cascade silmek reddedildi.

## Sonuçlar

- Editör yayımdaki içeriklerin görselini ve metnini düzeltebilir.
- Katalog kartı ve detay sayfası korumalı medya endpoint'inden gerçek kapağı
  gösterir; kapak yoksa harf fallback'i kullanılır.
- Silme çoğu hazırlık içeriğinde gerçekten satırı kaldırır; oynanmış içerikte
  operatör neden silinemediğini kararlı hata koduyla görür.
- Eski attempt, skor ve XP kayıtları açıklamasız biçimde kaybolmaz.

## Yeniden değerlendirme tetikleyicileri

- Hukuki silme/anonimleştirme politikası gameplay geçmişinin de kaldırılmasını
  zorunlu kılarsa ayrı retention ve audit tasarımı gerekir.
- Mevcut sezon/bölümlerin yeniden numaralandırılması veya silinmesi istenirse
  aggregate ve quiz scope ilişkileri ayrı bir sürümleme kararıyla ele alınmalıdır.
- Quiz yayını için sunucu tarafında kesin minimum soru sayısı istenirse bu UX
  uyarısından ayrı bir domain kuralı ve migration testi eklenmelidir.
