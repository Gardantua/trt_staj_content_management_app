# ADR-0027: Yayımlanmış dizi hiyerarşisine yalnız sona ekleme

## Durum

Accepted — 11.08.2026

## Bağlam

Devam eden dizilere yayından sonra yeni sezon ve bölüm eklenmesi gerekir. Ancak
mevcut sezon veya bölüm kimliğinin silinmesi ya da yeniden anlamlandırılması,
`SEASON` ve `EPISODE` kapsamlı quizlerin geçmişini bozabilir.

## Karar

- Yayımlanmış `SERIES` içeriğine yalnız mevcut en büyük numaradan sonra sezon veya
  bölüm eklenebilir.
- Mevcut sezon/bölüm güncelleme ve silme işlemleri yasak kalır.
- Yeni kayıtlar mevcut `Content` aggregate transaction'ı ve audit akışını kullanır.
- Mevcut PostgreSQL foreign key ve unique constraint'leri yeterlidir; migration
  eklenmez.

## Alternatif ve sonuç

Tüm hiyerarşiyi düzenlenebilir yapmak daha esnek olurdu; fakat geçmiş quiz
kapsamlarını korumak için içerik sürümleme gerektirirdi. Sona ekleme, devam eden
dizi ihtiyacını mevcut kimlikleri değiştirmeden karşılar.
