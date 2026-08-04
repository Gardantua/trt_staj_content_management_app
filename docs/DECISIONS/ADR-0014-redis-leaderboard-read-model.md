# ADR-0014: Redis leaderboard read model ve PostgreSQL fallback

## Durum

Accepted

## Bağlam

ADR-0013 sıralamanın doğru kaynağını ve deterministik düzenini PostgreSQL
üzerinde tanımladı. Redis sorted set farklı skorlarda sayısal, eşit skorlarda
ise üye değerine göre sözlüksel sıralama yapar. Yalnız toplam XP'yi skor olarak
saklamak, “ilk XP zamanı, sonra UUID” tie-break sözleşmesini korumaz.

Redis ayrıca PostgreSQL transaction'ına katılan kalıcı bir veri deposu değildir.
XP yazımı ile Redis artırımını birlikte yapmak dual-write, duplicate teslimat
ve kısmi hata riski oluşturur.

## Karar

- PostgreSQL `xp_transactions` kalıcı doğru kaynak kalır.
- Redis, PostgreSQL'den bütünüyle yeniden oluşturulabilen read modeldir.
- Sorted set skoru toplam XP değil, PostgreSQL'in hesapladığı benzersiz
  `ROW_NUMBER` pozisyonudur. Üye kullanıcı UUID'sidir; toplam XP ve ilk XP
  zamanı ayrı hash metadata'sında tutulur.
- Global ve içerik sıralamaları aynı PostgreSQL read-only transaction'ında
  snapshot olarak okunur. Transaction kapandıktan sonra Redis yazılır.
- Her rebuild yeni bir generation altında hazırlanır. Bütün scope'lar hazır
  olduktan sonra `leaderboard:v1:active` işaretçisi tek işlemle değiştirilir;
  eski generation best-effort silinir.
- Varsayılan kontrollü yenileme aralığı beş saniyedir. `ADMIN`,
  `POST /api/v1/admin/leaderboards/rebuild` ile elle rebuild yapabilir.
- Redis anahtarı yoksa, nesil eksikse, kayıt bozuksa veya bağlantı kesilirse
  okuma PostgreSQL'e düşer. Redis hatası XP yazımını geri almaz.
- API `dataSource` ve `projectionGeneratedAt` alanlarıyla eventual consistency'yi
  görünür kılar. Micrometer hit/fallback sayaçları operasyonel ölçüm sağlar.

## Değerlendirilen alternatifler

### XP'yi `ZINCRBY` ile olay bazlı artırmak

Daha kısa stale pencere sağlardı. Ancak duplicate olay, adjustment, rebuild ve
eşitlikte çok sayıda pozisyonun değişmesi ek idempotency/tutarlılık protokolü
gerektirir. İlk sürümde snapshot rebuild daha kolay doğrulandığı için seçildi.

### Sorted set skorunda toplam XP saklamak

Redis'in doğal kullanımına daha yakındır; fakat eşit XP'deki ilk katılım zamanı
tie-break'ini tek skorla kayıpsız ifade etmez. Ürünün kesin sırası performans
kolaylığı için değiştirilmedi.

### Redis yokken hata döndürmek

Okuma yükünü PostgreSQL'den tamamen ayırırdı; fakat geçici cache kesintisini
kullanıcı kesintisine çevirirdi. PostgreSQL zaten doğru ve ölçülmüş sorguyu
sağladığından kontrollü fallback seçildi.

## Sonuçlar

- Cache silme, tekrar rebuild ve Redis kesintisi XP kaybı üretmez.
- Varsayılan beş saniyeye kadar stale sonuç görülebilir; üretim zamanı response'ta
  açıktır.
- 2.000 kullanıcılık yerel örnekte ilk N+1 hash okuması Redis p95'ini yaklaşık
  16,8 ms yaptı. Toplu hash okuması bunu yaklaşık 7,1 ms'ye indirdi; aynı koşuda
  PostgreSQL yaklaşık 4,0 ms ölçüldü. Redis küçük yerel örnekte daha hızlı
  değildir; sağladığı değer PostgreSQL okuma yükünü ayırmak ve bağımsız yeniden
  üretilebilir projeksiyon sınırı oluşturmaktır.
- Testcontainers testleri PostgreSQL/Redis sonuç eşitliğini, Top N dışındaki
  current user'ı, stale/rebuild davranışını, generation temizliğini, yetkiyi ve
  geçici Redis kesintisindeki fallback'i doğrular.

## Yeniden değerlendirme tetikleyicileri

- Stale pencere ürün deneyimi için kabul edilemez hale gelirse
- Rebuild süresi/veri hacmi PostgreSQL veya Redis üzerinde aşırı yük oluşturursa
- Gerçek trafik altında Redis sorgusu gecikme/SLO hedefini karşılamazsa
- Dönemsel veya arkadaş kapsamlı leaderboard eklenirse
- Event tabanlı incremental projection için replay ve idempotency altyapısı
  kurulursa

## Kaynaklar

- [Redis sorted set belgeleri](https://redis.io/docs/latest/develop/data-types/sorted-sets/)
- [Spring Data Redis RedisTemplate](https://docs.spring.io/spring-data/redis/reference/redis/template.html)
- [Spring Data Redis transaction davranışı](https://docs.spring.io/spring-data/redis/reference/redis/transactions.html)
