# Operasyon ve production hazırlığı

Bu belge Aşama 9'un çalıştırılabilir kontrollerini ve ilk mühendislik hedeflerini
tek yerde toplar. Gerçek trafik, kurum altyapısı ve hukuk onayı gelmeden aşağıdaki
sayısal hedefler production garantisi sayılmaz.

## Tek sunucuda ilk canlı kurulum

Hedef Linux sunucuda Docker Engine, Compose eklentisi, domainin sunucu IP'sine DNS
kaydı ve firewall'da yalnız `22`, `80`, `443` gerekir. `22` mümkünse yalnız yönetici
IP'sine sınırlandırılır. Veritabanı, RabbitMQ, Redis ve iki Java servisi internete
port yayımlamaz.

1. `.env.production.example` dosyasını `.env.production` adıyla kopyala; farklı,
   uzun parolaları ve MailerSend'in doğrulanmış domain bilgilerini gir.
2. `INITIAL_ADMIN_ENABLED=true` bırakıp güçlü, benzersiz ilk admin parolası belirle.
3. Uygulamayı başlat:

```text
docker compose --env-file .env.production -f compose.production.yaml up -d --build
```

4. `https://DOMAIN/actuator/health` yerine dışarıdan yalnız uygulama akışlarını;
   sunucuda ise `docker compose ... ps` ve backend healthcheck durumunu kontrol et.
   Kullanıcı sayfası `https://DOMAIN/`, yönetici sayfası `https://DOMAIN/admin` olur.
5. İlk admin girişi başarılı olunca `INITIAL_ADMIN_ENABLED=false` yap,
   `INITIAL_ADMIN_PASSWORD` değerini dosyadan sil ve backend'i yeniden oluştur:

```text
docker compose --env-file .env.production -f compose.production.yaml up -d backend
```

İlk admin mekanizması sistemde bir `ADMIN` varsa ikinci admin oluşturmaz. Yeni admin
yetkilendirme ihtiyacı için ileride denetimli bir yönetici ekranı/API'si tasarlanır;
doğrudan veritabanı güncellemesi normal operasyon değildir. `.env.production`
Git'e eklenmez, dosya erişimi yalnız sunucu yöneticisiyle sınırlandırılır.

### Canlıya çıkmadan önce zorunlu smoke kontrolleri

- Kullanıcı kayıt, giriş, çıkış ve şifre sıfırlama e-postası
- `/admin` girişinde USER'ın reddi, ADMIN'in içerik ekranına erişimi
- Quiz tamamlama, XP olayı ve leaderboard'un beklenen kısa gecikmeyle güncellenmesi
- Backend/leaderboard health, Caddy HTTPS yenilemesi ve container restart davranışı
- İki PostgreSQL ile medya volume'unun ayrı konuma şifreli yedeği ve boş hedefe
  gerçek restore provası

Bu Compose tek sunucu maliyetini düşürür fakat yüksek erişilebilir değildir. Backend
yeniden başlarsa bellek içi oturumlar kaybolur ve kullanıcı yeniden giriş yapar.
Redis kaybı kabul edilir; PostgreSQL ve medya kaybı kabul edilmez.

## SLI ve ilk SLO sınırları

SLI ölçülen göstergedir; SLO bu göstergenin hedeflenen sınırıdır. HTTP istekleri
için başlangıç SLI'ları başarı oranı, p95/p99 gecikme ve sürdürülebilen istek
hızıdır. İlk MVP koruma sınırları şöyledir:

- Başarısız istek oranı `%1` altında.
- p95 gecikme `500 ms`, p99 gecikme `1 s` altında.
- Baseline kapasite varsayımı tek uygulama örneğinde `10 istek/s`.
- Ramp ve spike profilleri sırasıyla `50` ve `150 istek/s` sınırını arar; bunlar
  vaat değil, darboğaz bulma yükleridir.

04.08.2026 yerel baseline sonucu: gerçek PostgreSQL ve tek uygulama örneğinde
bir dakika boyunca 10 istek/s, toplam 601 istek, `%0` hata ve `0` dropped
iteration; ortalama `18,72 ms`, p95 `15,75 ms`, p99 `332,82 ms`, maksimum
`933,40 ms`. Eşiklerin tamamı geçti. Tek yerel koşu production kapasite kanıtı
değildir; ramp, spike ve soak profilleri hedef ortamda release öncesi koşturulur.

`ops/load/k6/stage9-load.js` baseline, ramp, spike ve on dakikalık kısa soak
profillerini aynı public content-list yolunda çalıştırır. Ham kapasite ölçümünde
rate limiter kapatılır; abuse kontrolü ayrı integration testiyle doğrulanır.
Prometheus hata/gecikme eğilimini, Tempo tek isteğin HTTP → Outbox → RabbitMQ
consumer span zincirini, loglar ise `requestTraceId`, `traceId` ve `spanId`
alanlarını gösterir.

## Rate limiting ve güven sınırı

Her uygulama örneği, uzak IP başına sürekli doldurulan token bucket kullanır.
Varsayılan sınır dakikada 300 istek ve 300 token burst kapasitesidir. Bu değer,
aynı IP kotasını paylaşan admin liste/detay ve medya isteklerinin normal editör
akışını kesmemesi için 10.08.2026'da 60'tan 300'e yükseltildi. Limit aşımı
kararlı `RATE_LIMIT_EXCEEDED` hata kodu, `429`, `Retry-After` ve RateLimit
header'ları üretir. Uygulama proxy header'larına doğrudan güvenmez; production
load balancer gerçek istemci adresini güvenilir ağ sınırında normalize etmelidir.

Bu bellek içi mekanizma tek-instance MVP korumasıdır. Birden çok instance'ta
global kota gerekiyorsa dağıtık limiter değerlendirilecektir. Redis alternatifi
ortak kota sağlar fakat abuse trafiğini Redis erişilebilirliğine bağlar ve her
isteğe ağ maliyeti ekler; bu nedenle gerçek çok-instance gereksinimi oluşmadan
seçilmemiştir.

## Kesinti ve geri dönüş matrisi

| Bağımlılık | Beklenen davranış | Veri güvenliği kanıtı |
|---|---|---|
| PostgreSQL | Health `DOWN/503` veya sınırlı probe timeout; kalıcı yazma yapılmaz | Container pause/unpause sonrası önceden commit edilmiş marker okunur |
| RabbitMQ | Attempt ve Outbox commit olur; publish retry bekler | Broker dönüşünde aynı Outbox yayımlanır, tek XP oluşur |
| Redis | Leaderboard PostgreSQL fallback döner | XP ledger PostgreSQL'de kalır, Redis read model yeniden üretilebilir |

Uyarılar en az health durumu, HTTP hata oranı, Outbox bekleyen satır/yaş,
consumer hata/DLQ ve Redis fallback oranını kapsamalıdır. Kurum alarm kanalı ve
eşikleri production topolojisi belirlendiğinde kesinleştirilecektir.
Hard PostgreSQL pause provasında mevcut JDBC bağlantısı health timeout'una kadar
askıda kalabilir; production readiness/liveness ayrımı ve altyapı probe timeout'u
deployment platformunda uygulama timeout'undan daha uzun ayarlanmalıdır.

DLQ replay otomatik değildir. Operatör önce hata nedenini düzeltir, mesajın
`eventId` değerini ve payload sürümünü doğrular, yedeği alır, sonra mesajı özgün
routing key ile ana exchange'e tekrar yayımlar. Inbox ve XP unique constraint'i
tekrarı güvenli kılar. Payload veya doğru cevap gibi hassas içerik loglanmaz.

## PostgreSQL backup, restore, RPO ve RTO

İlk mühendislik hedefi `RPO ≤ 15 dakika` ve `RTO ≤ 60 dakika`dır. RPO, felakette
kabul edilen en fazla veri kaybı penceresi; RTO servisin geri dönmesi için hedef
süredir. Bu hedef için production'da günlük full backup, sürekli WAL arşivleme,
şifreli ve ayrı failure-domain storage ile düzenli restore provası gerekir.
Mevcut Compose volume tek başına backup değildir.

Stage 9 testi çalışan şemayı `pg_dump --format=custom` ile alır, ayrı bir geçici
veritabanına `pg_restore` eder ve hem Flyway geçmişini hem marker verisini okur.
Yerel manuel prova yalnız disposable hedefte şu akışı izler:

```powershell
docker exec content-engagement-postgres pg_dump -U content_engagement -Fc -f /tmp/content.backup content_engagement
docker cp content-engagement-postgres:/tmp/content.backup .\build\content.backup
docker exec content-engagement-postgres createdb -U content_engagement content_engagement_restore_probe
docker cp .\build\content.backup content-engagement-postgres:/tmp/content.backup
docker exec content-engagement-postgres pg_restore -U content_engagement -d content_engagement_restore_probe /tmp/content.backup
```

Restore sırası: olayı ilan et, yazmayı durdur, backup bütünlüğünü ve hedefi
doğrula, yeni/boş veritabanına restore et, Flyway ve smoke kontrollerini çalıştır,
uygulama bağlantısını değiştir, gözlemle. Kaynak production veritabanına doğrudan
`--clean` uygulanmaz. Geri dönüş, bağlantıyı dokunulmamış eski veritabanına geri
çevirmektir.

## Güvenli migration

Migration'lar expand → migrate/backfill → contract sırasını izler:

1. Eski ve yeni kodla uyumlu nullable kolon/indeks ekle.
2. Yeni kod iki biçimi okuyabilsin; gerekiyorsa küçük, yeniden başlatılabilir
   batch'lerle backfill yap.
3. Metriklerle eski kullanımın bittiğini kanıtla.
4. `NOT NULL`, kolon silme veya ad değiştirme gibi contract adımını sonraki
   sürümde uygula.

V9 bu yaklaşımın provasıdır: `trace_parent` ve `trace_state` nullable eklenir;
eski Outbox satırının yeni publisher ile hâlâ yayımlandığı test edilir. Büyük
tabloda `ALTER TABLE ... DEFAULT` ile rewrite, tek transaction'da büyük backfill,
eşzamanlı olmayan indeks ve uzun constraint validation kilit riskidir.
`ops/runbooks/migration-preflight.sql` tablo boyutu, aktif transaction ve
bloklanan sorguları dağıtımdan önce görünür kılar.

Migration başarısızsa uygulama rollout'u durdurulur. Expand değişikliği eski
uygulamayla uyumlu bırakılır; veri kaybettirecek ters migration çalıştırılmaz.
Gerekirse uygulama eski sürüme alınır, sorun ileri yönlü yeni migration ile
düzeltilir. Contract adımı için ayrıca doğrulanmış backup ve bakım penceresi
zorunludur.

## Veri saklama, silme, KVKK ve audit

Aşağıdaki süreler asgari veri ilkesi için başlangıç mühendislik politikasıdır;
production açılışından önce veri sorumlusu/hukuk tarafından onaylanmalıdır:

| Veri | Başlangıç retention | Silme/anonimleştirme sınırı |
|---|---:|---|
| HTTP uygulama logları | 30 gün | Body, token, doğru cevap ve kişisel veri loglanmaz |
| Dağıtık trace | 7 gün | UUID/tag cardinality ve payload kaydı yasak |
| Prometheus metric | 90 gün | Yalnız düşük cardinality teknik label |
| Outbox (başarılı) | 30 gün | Replay penceresi sonrası batch purge |
| Inbox deduplication | 90 gün | En uzun olası replay penceresinden kısa olamaz |
| DLQ / başarısız Outbox | Çözüm + 90 gün | Operatör onayı olmadan silinmez |
| Admin audit | 2 yıl | Append-only; hukuk daha uzun süre isterse güncellenir |
| Attempt, answer ve XP ledger | Hesap ömrü + 30 gün | Yasal saklama yoksa doğrulanmış talepte anonimleştirme/silme |
| Sahipsiz yerel medya | 7 gün | Referans kontrolünden sonra temizlenir |

Kullanıcı UUID'si takma adlı olsa da kişisel veridir. Erişim ve silme talebi
kimlik sağlayıcısında doğrulanmadan uygulanmaz. Mevcut MVP'de merkezi kullanıcı
kaydı ve bütün event payload'larını kapsayan anonimleştirme iş akışı yoktur;
dolayısıyla production kimlik entegrasyonu bu akış sağlanmadan tamamlanmış
sayılmaz. Audit kaydı silme talebinin kim tarafından, ne zaman ve hangi hukuki
dayanakla işlendiğini kaydeder; secret veya silinen verinin kendisini içermez.

## Güvenlik kapıları

CI normal `verify` işine ek olarak Maven'in ürettiği 164 bileşenli transitive
CycloneDX SBOM'u OSV-Scanner ile bilinen açıklara, Gitleaks ile bütün Git
geçmişindeki secret'lara karşı tarar.
Dependabot Maven ve GitHub Actions güncellemelerini haftalık açar. Tarama sonucu
yanlış pozitifse suppression dosyası ancak CVE, etkilenen yol, gerekçe, sahip ve
son kullanma tarihi kaydedilerek eklenebilir.
