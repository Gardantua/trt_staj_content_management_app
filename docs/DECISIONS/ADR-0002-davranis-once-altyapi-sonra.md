# ADR-0002: İş Davranışını Altyapıdan Önce Kanıtlama

## Durum

Accepted

Redis'in opsiyonel olup olmadığına ilişkin bölüm
`ADR-0006-redis-ogrenme-gereksinimi.md` ile değiştirilmiştir. Davranışı önce
PostgreSQL üzerinde kanıtlama ve Redis'i daha sonra ekleme sırası geçerlidir.

## Bağlam

MVP; attempt tamamlama, XP üretme ve leaderboard gösterme davranışlarını
içerir. RabbitMQ ve Redis bu davranışların kendisi değil, belirli güvenilir
mesajlaşma ve okuma performansı problemleri için altyapı seçenekleridir.

XP ile RabbitMQ veya leaderboard ile Redis aynı anda geliştirilirse, bir hata
oluştuğunda iş kuralı ile altyapı davranışını ayırmak zorlaşır. Ayrıca gerçek
ihtiyaç ölçülmeden altyapı eklemek öğrenme ve operasyon maliyetini artırır.

Gameplay ve yönetim kaynaklarının güvenli sahiplik/yetki kontrolü için kullanıcı
kimliği de bu kaynaklardan önce bulunmalıdır.

## Karar

- Kimlik ve erişim temeli content yönetimi ve gameplay'den önce kurulacaktır.
- Gameplay PostgreSQL üzerinde güvenilir tamamlanmadan XP aşamasına geçilmez.
- XP önce PostgreSQL üzerinde idempotent, append-only ledger olarak uygulanır.
- RabbitMQ daha sonraki ayrı aşamada Outbox/Inbox ile asenkronlaştırma için
  eklenir.
- Leaderboard ürün kuralları ve doğru sonuç önce PostgreSQL üzerinde uygulanır.
- Redis yalnız ölçülmüş ihtiyaç veya açık öğrenme hedefi varsa, yeniden
  oluşturulabilir read model olarak ayrı aşamada eklenir.
- Redis ve RabbitMQ kalıcı doğru kaynak olmaz.

## Gerekçe

- Her aşamada tek bir ana problem öğrenilir ve test edilir.
- İş kuralı hatası ile broker/cache hatası birbirinden ayrılır.
- PostgreSQL constraint ve transaction davranışı önce anlaşılır.
- Redis ve RabbitMQ'nun sisteme kattığı gerçek değer önce/sonra ölçülebilir.
- Çekirdek sistem broker veya cache olmadan da güvenilir çalışır.

## Sonuçlar

- Yol haritası kimlik, XP, mesajlaşma, PostgreSQL leaderboard ve Redis read
  model için ayrı aşamalara bölünmüştür.
- Aşama 0 ortamında yalnız PostgreSQL bulunur.
- RabbitMQ eklendiğinde senkron ve asenkron XP sonucunun aynı kaldığı regresyon
  testleri gerekir.
- Redis eklendiğinde PostgreSQL ile tutarlılık, rebuild ve kesinti testleri
  gerekir.
- PostgreSQL performansı hedefi karşılıyorsa Redis teknik olarak zorunlu
  değildir.

## Yeniden değerlendirme tetikleyicileri

- Kurum standardı zorunlu bir broker/cache mimarisi belirlerse
- Ölçülen trafik veya gecikme hedefi mevcut sırayı değiştirmeyi gerektirirse
- Ayrı ekip sahipliği ya da bağımsız deployment ihtiyacı oluşursa
- Eğitim hedefi belirli altyapının daha erken izole bir laboratuvarda
  incelenmesini gerektirirse
