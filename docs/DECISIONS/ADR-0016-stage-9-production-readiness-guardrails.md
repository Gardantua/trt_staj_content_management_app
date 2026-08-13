# ADR-0016: Aşama 9 production hazırlığı koruma sınırları

## Durum

Accepted

## Bağlam

Çekirdek quiz akışı PostgreSQL, RabbitMQ ve yeniden üretilebilir Redis read model
üzerinde çalışıyor. Production'a yaklaşmak için yalnız dashboard eklemek yeterli
değildir; iz devamlılığı, abuse kontrolü, güvenlik taraması, kapasite ölçümü,
kesinti davranışı, restore provası, güvenli migration ve veri yaşam döngüsü aynı
operasyon sözleşmesinde tanımlanmalıdır.

## Karar

- HTTP → Outbox → RabbitMQ zinciri W3C `traceparent`/`tracestate` ile devam eder;
  kullanıcıdan gelen `X-Trace-Id` yalnız destek korelasyon kimliğidir.
- Prometheus düşük cardinality metriklerin doğru kaynağı, Tempo dağıtık trace
  deposu, yapılandırılmış log ise olay ayrıntısının kaynağıdır.
- Tek-instance MVP abuse kontrolü uzak IP anahtarlı bellek içi token bucket'tır.
- Token bucket normal ve production profillerinde varsayılan olarak açıktır; yalnız
  yerel geliştirme profili, yoğun admin/görsel çalışmasının 429 üretmemesi için
  filtreyi kapatır.
- Güvenlik taraması normal testlerden ayrı CI kapılarıyla yapılır.
- Kapasite baseline/ramp/spike/soak k6 profilleriyle ve p95/p99/hata oranıyla
  raporlanır.
- PostgreSQL doğru kaynak olmaya devam eder; RabbitMQ kesintisinde Outbox bekler,
  Redis kesintisinde PostgreSQL fallback çalışır.
- Şema değişiklikleri expand-contract yaklaşımını izler. Geri dönüş öncelikle
  eski uygulama sürümüne dönmektir; veri kaybettiren ters migration değildir.
- İlk RPO/RTO hedefleri 15 dakika ve 60 dakikadır; restore provası düzenli
  çalıştırılmadan bu hedefler sağlanmış sayılmaz.
- Retention süreleri `docs/OPERATIONS.md` içindeki başlangıç politikasıdır ve
  production öncesi hukuk/veri sorumlusu onayı gerektirir.

## Gerekçe

Dağıtık Redis rate limiter tüm instance'larda kesin kota sağlayabilirdi; ancak
tek-instance MVP'de her isteğe ağ bağımlılığı ve yeni failure mode ekler. Bellek
içi limiter daha basit ve Redis kesintisinden bağımsızdır. Gerçek yatay ölçek ve
global kota gereksinimi oluşursa karar yeniden açılır.

Veritabanı rollback migration'ı otomatik çözüm seçilmedi; kolon silme veya ters
veri dönüşümü restore edilemeyen kayıp üretebilir. Uyumluluk penceresi bırakan
expand-contract, uygulama rollback'ini güvenli kılar.

## Sonuçlar

- Outbox satırında nullable W3C context alanları vardır; eski satırlar geçerlidir.
- Rate limit instance bazlıdır ve process restart token durumunu sıfırlar.
- `local` profil production abuse korumasını temsil etmez; production'da limiter'ı
  kapatmak ayrı ve açık bir environment kararı gerektirir.
- Security scan dış CVE veri kaynağına bağlı olduğu için ayrı CI işi zaman zaman
  altyapı kaynaklı başarısız olabilir; ürün test sonucunu değiştirmez.
- Retention uygulayan otomatik purge/anonymization job henüz yoktur. Production
  kimlik ve hukuk sözleşmesi tamamlanınca ayrı, audit edilen işler gerekir.
- İlk performans sınırları gerçek trafik ve veri dağılımıyla yeniden ölçülmelidir.

## Yeniden değerlendirme tetikleyicileri

- Birden çok uygulama instance'ı veya kullanıcı/API-key bazlı ortak kota ihtiyacı
- Kurum SLO, alarm, SIEM, trace retention veya secret yönetimi standardı
- Gerçek trafik modelinin p95/p99 ya da kapasite varsayımlarını geçersiz kılması
- Hukuk/veri sorumlusunun farklı retention veya silme yükümlülüğü belirlemesi
- Büyük tablo migration'larında ölçülen kilit süresinin bakım penceresini aşması
