# ADR-0013: PostgreSQL leaderboard ürün ve sıralama kuralları

## Durum

Accepted

Tekrar attempt'lerin XP uygunluğu ADR-0026 ile değiştirilmiştir. Leaderboard'un
yalnız PostgreSQL XP ledger toplamını kullanması kararı geçerlidir.

## Bağlam

Leaderboard yalnız “XP'ye göre sırala” sorgusu değildir. Tekrar çözülen quizlerin
nasıl sayılacağı, yönetici düzeltmelerinin etkisi, eşit toplam, içerik kapsamı,
dönem ve kullanıcının Top N dışında kendi yerini nasıl göreceği belirlenmeden
Redis veri yapısı seçmek yanlış davranışı hızlandırabilir.

Sistemin kalıcı oyunlaştırma doğrusu append-only `xp_transactions` ledger'ıdır.
Kalıcı kullanıcı profili ve arkadaş sistemi ise MVP'de bulunmamaktadır.

## Karar

- İlk leaderboard dönemi `ALL_TIME`'dır; sıfırlama veya sezon yoktur.
- Global sıralama kullanıcının bütün XP işlemlerinin toplamıdır.
- İçerik sıralaması yalnız ilgili `contentId` aidiyetindeki XP işlemlerinin
  toplamıdır. Yönetici düzeltmesi özgün quiz kazancının içerik aidiyetini taşır.
- Aynı quizin yalnız ilk tamamlanması XP ledger'a girer; sonraki alıştırma
  attempt'leri toplama katılmaz. “En iyi attempt” seçilmez.
- Sıra deterministik olarak şu anahtarlarla hesaplanır:
  1. toplam XP azalan,
  2. ilk XP işlem zamanı artan,
  3. kullanıcı UUID'si artan.
- Her katılımcının benzersiz pozisyonu PostgreSQL `ROW_NUMBER` ile üretilir.
- Katılımcı, sıfır XP'li completion dahil en az bir ledger kaydı bulunan
  kullanıcıdır. Yönetici düzeltmeleri nedeniyle negatif toplam mümkündür.
- API varsayılan 20, en fazla 100 Top N satırı ile kimliği doğrulanmış
  kullanıcının kendi satırını ayrıca döndürür. Kullanıcının kaydı yoksa
  `currentUser` null'dır.
- Küçük örneklemde yanıltıcı sonuç vermemek için MVP response'u yüzdelik
  hesaplamaz.
- İçerik leaderboard'u yalnız yayınlanmış içerik için okunur.
- Kalıcı profil bulunmadığından satırlar kullanıcı UUID'sini taşır; görünen ad,
  avatar ve arkadaş sıralaması bu aşamanın kapsamında değildir.
- `xp_transactions.content_id` migration sırasında kaynak attempt ve özgün
  adjustment ilişkisinden geri doldurulur. PostgreSQL trigger'ı yeni completion
  ve adjustment satırlarının kullanıcı/içerik aidiyetini kaynaklarıyla
  karşılaştırır.
- PostgreSQL sorgusu ledger'ı kullanıcı bazında toplar, window function ile
  sıralar ve tek sorguda Top N ile mevcut kullanıcıyı seçer. Global ve içerik
  sorguları için kapsayıcı indeksler eklenir.

## Değerlendirilen alternatifler

### Yalnız en iyi attempt'i saymak

Tekrar denemeyi sınırlı etkilerdi; ancak mevcut ürün doğrusu toplam XP ledger'ı
ve audit edilen düzeltmelerdir. Ledger toplamını leaderboard'dan ayıracağı için
reddedildi.

### Eşit toplamda aynı rank'i vermek

`DENSE_RANK` rekabet eşitliğini daha görünür kılardı; fakat Top N sınırı ve Redis
sorted-set karşılaştırması için benzersiz, tekrar üretilebilir pozisyon gerekir.
Toplam eşitliği korunurken ilk katılım zamanı ve UUID tie-break olarak seçildi.

### Haftalık/aylık dönem eklemek

Daha yüksek tekrar kullanım sağlayabilir; fakat timezone, dönem kapanışı,
geçmiş dönem ve adjustment politikaları henüz ürün kararı değildir. İlk sürüm
`ALL_TIME` ile sınırlandı.

### Leaderboard'u gameplay skorundan hesaplamak

Admin düzeltmelerini ve gelecekteki XP politika değişikliklerini dışarıda
bırakacağı için reddedildi. PostgreSQL XP ledger kalıcı doğru kaynak kaldı.

## Sonuçlar

- `GET /api/v1/leaderboards/global` ve
  `GET /api/v1/leaderboards/contents/{contentId}` global/içerik sonuçlarını
  sunar.
- Bir kullanıcının birden fazla completion kazancı ve adjustment'ları sırasını
  doğrudan etkiler.
- Aynı veri her okumada aynı pozisyonları üretir.
- 2.000 kullanıcılık yerel örneklemde içerik Top N sorgusu yaklaşık 5,45 ms p95;
  `EXPLAIN ANALYZE` yürütmesi yaklaşık 1,67 ms ölçüldü ve içerik leaderboard
  indeksini kullandı. Bu kapasite garantisi değil, Aşama 8 öncesi başlangıç
  karşılaştırmasıdır.
- Redis Aşama 8'de bu PostgreSQL sonucundan yeniden oluşturulabilir read model
  olacak; ürün kuralını yeniden tanımlamayacaktır.

## Yeniden değerlendirme tetikleyicileri

- Ürün sezonluk, haftalık veya aylık leaderboard isterse
- Hileli/iptal attempt ve XP geri alma operasyonu tanımlanırsa
- Profil/görünen ad veya arkadaş kapsamı MVP'ye alınırsa
- Tekrar çözmenin yalnız en iyi sonucu sayması kararlaştırılırsa
- Gerçek veri hacmi sorgu planını veya gecikme hedefini kabul edilemez hale
  getirirse
