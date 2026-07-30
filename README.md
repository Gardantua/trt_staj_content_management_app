# TRT İçerik Etkileşim Backend Platformu

TRT ve tabii içerikleri için geliştirilecek, API-first bir içerik etkileşim
platformudur. İlk kullanım senaryosu; kullanıcıların dizi ve bölüm bazlı
quizleri çözmesi, sunucu tarafında puanlanması, XP kazanması ve sıralamasını
görmesidir.

Bu repo öncelikle backend mühendisliği çalışmasıdır. Frontend yalnızca API'leri
gösterecek kadar geliştirilebilir.

GitHub deposu:
[Gardantua/trt_staj_content_management_app](https://github.com/Gardantua/trt_staj_content_management_app)

## Projenin öğrenme amacı

Bu proje yalnızca çalışan endpoint'ler üretmek için değil, bir backend
sisteminin neden bu şekilde tasarlandığını uçtan uca öğrenmek için
geliştirilecektir. Her aşamada:

1. Çözülen iş problemi ve ilgili domain kavramları açıklanır.
2. Teknoloji veya desen seçilmeden önce alternatifler ve trade-off'lar konuşulur.
3. Kod küçük adımlarla yazılır; önemli sınıfların ve transaction sınırlarının
   görevi açıklanır.
4. Değişken, metot ve sınıf adları yaptığı işi anlatacak şekilde seçilir.
5. Yeni aşamaya geçmeden önce yazılan kodun akışı, iş kuralları, hata davranışı
   ve testlerinin neyi kanıtladığı kullanıcıya açıklanır.
6. Önce risk ve beklenen davranış tanımlanır, sonra uygun test yazılır.
7. Çalıştırma ve hata ayıklama adımları kullanıcı tarafından tekrar edilebilir
   biçimde belgelenir.
8. Aşama sonunda öğrenilenler, alınan kararlar ve açık sorular kaydedilir.

Amaç, kodu yalnızca üretmek değil; veri modeli, API, transaction, eşzamanlılık,
güvenlik, test ve operasyon davranışlarına birlikte hâkim olmaktır.

## İlk sürüm

İlk sürümde:

- İçerik, sezon ve bölüm yönetimi
- Quiz, soru ve cevap seçeneği yönetimi
- Quiz attempt başlatma
- Tek seferlik ve süre kontrollü cevap gönderme
- Sunucu taraflı puanlama
- Kullanıcı ve yönetici yetkilendirmesi
- XP işlemleri
- Global ve içerik bazlı leaderboard
- OpenAPI dokümantasyonu
- Otomatik testler

bulunacaktır.

İlk sürümde canlı TV, İngilizce öğrenme, arkadaş sistemi, yapay zekâyla soru
üretme, Kubernetes ve mikroservis ayrıştırması yapılmayacaktır.

## Önerilen başlangıç teknolojileri

Teknoloji seçimi kurum standardı öğrenilene kadar tavsiye niteliğindedir:

- Java 21
- Spring Boot
- PostgreSQL
- Flyway
- Docker Compose
- OpenAPI
- Testcontainers

RabbitMQ, stajdan sorumlu mühendisin bildirdiği kurum/proje gereksinimi olarak
kullanılacaktır. Ancak çekirdek quiz sistemi PostgreSQL üzerinde güvenilir
biçimde tamamlanmadan eklenmeyecektir. Redis ise ölçülmüş ihtiyaç oluştuğunda
değerlendirilecektir:

- XP önce PostgreSQL üzerinde idempotent bir işlem defteri olarak doğru
  çalıştırılır. RabbitMQ daha sonra Outbox ile quiz tamamlama yan etkilerini
  asenkronlaştırmak için eklenir.
- Leaderboard kuralları ve doğru sonuç önce PostgreSQL üzerinde kanıtlanır.
  Redis ancak ölçülmüş okuma/gecikme ihtiyacı varsa yeniden oluşturulabilir bir
  read model olarak eklenir.

## Sadeleştirilmiş çekirdek akış

İlk sürümün omurgası şudur:

```text
Yönetici içeriği ve quizi yayınlar
        ↓
Kullanıcı yayınlanmış quizi başlatır
        ↓
Sunucu süreyi ve cevapları doğrular
        ↓
Attempt tek kez tamamlanır ve skor kesinleşir
        ↓
XP oluşur ve sıralama okunabilir
```

Bu akışı kanıtlamayan teknoloji veya özellik MVP'ye eklenmez.

## Dokümantasyon

- [Proje özeti](docs/PROJECT_BRIEF.md)
- [Dokümantasyon ve diyagram dizini](docs/README.md)
- [Resmî proje tanımı ve teknik isterler](docs/PROJECT_SPECIFICATION.md)
- [PDF proje dokümanı](output/pdf/TRT_Icerik_Etkilesim_Backend_Proje_Dokumani.pdf)
- [Backend mühendisliği çalışma defteri](docs/bwl.md)
- [Mimari](docs/ARCHITECTURE.md)
- [PlantUML sistem diyagramı](docs/diagrams/system-architecture.puml)
- [Yol haritası](docs/ROADMAP.md)
- [Güncel durum](docs/CURRENT_STATE.md)
- [Mimari kararlar](docs/DECISIONS/README.md)

Uzun teknik referans:
`TRT_Etkilesimli_Quiz_Platformu_Backend_Mimari_Raporu.docx`

## Çalışma yöntemi

Her geliştirme görevinin başında `AGENTS.md` ve `docs/` altındaki belgeler
okunmalıdır. Bir görevde yalnızca yol haritasındaki tek aşama ele alınmalıdır.

Her aşamanın sonunda:

1. İlgili testler çalıştırılır.
2. Alınan kalıcı kararlar ADR olarak kaydedilir.
3. `docs/CURRENT_STATE.md` güncellenir.
4. Kullanıcıya değişikliklerin nedeni, sistemdeki yeri ve test sonucu anlatılır.
5. Bir sonraki aşamaya kullanıcı onayı olmadan geçilmez.

Eksik veya taşınmış dosyalar kullanıcı izni olmadan yeniden oluşturulmaz. PDF
sayfalarını görsele dönüştürme, OCR veya görsel kalite incelemesi gibi image
processing işlemleri de ayrıca kullanıcı onayı gerektirir.

## Yerel geliştirme

### Gereksinimler

- Java 21
- Çalışan Docker Desktop
- Git

Kurulumdan sonra yeni bir terminal aç ve sürümleri doğrula:

```powershell
java -version
docker version
docker compose version
```

`java -version` çıktısı Java 21 göstermelidir.

### PostgreSQL'i başlatma

```powershell
docker compose up -d --wait
docker compose ps
```

Proje PostgreSQL'i host üzerinde `5433`, container içinde `5432` portunu
kullanır. Bunun nedeni geliştirme bilgisayarında `5432` portunu kullanan başka
bir PostgreSQL sürecinin bulunmasıdır.

PostgreSQL'i durdurmak için:

```powershell
docker compose stop
```

`docker compose down` container'ı kaldırır ancak isimlendirilmiş veritabanı
volume'unu silmez. `down -v` veriyi de sileceği için bilinçli karar olmadan
kullanılmamalıdır.

### Testleri çalıştırma

Windows:

```powershell
.\mvnw.cmd verify
```

Linux, macOS veya CI:

```text
bash ./mvnw verify
```

Bu komut kodu derler, Testcontainers ile geçici gerçek PostgreSQL başlatır,
Flyway migration'ını uygular, API testlerini çalıştırır ve çalıştırılabilir JAR
üretir.

### Uygulamayı çalıştırma

Önce PostgreSQL açık olmalıdır:

```powershell
docker compose up -d --wait
.\mvnw.cmd spring-boot:run
```

Health adresi:

```text
http://localhost:8081/actuator/health
```

Beklenen cevap:

```json
{"status":"UP"}
```

Yerel varsayılanlar `application.yml` içindedir. Gerçek ortamlarda bağlantı
bilgileri `DATABASE_URL`, `DATABASE_USERNAME` ve `DATABASE_PASSWORD` environment
değişkenleriyle dışarıdan verilmelidir.

## Sıradaki çalışma

Aşama 0 yerel ortamda tamamlandı. Kullanıcı çalıştırma ve test akışını tekrar
edip temel parçaları anladıktan sonra, ayrı kullanıcı onayıyla Aşama 1 olan
kimlik ve erişim temeline geçilebilir.
