# ADR-0003: Aşama 0 Teknoloji Temeli

## Durum

Accepted

## Bağlam

Projenin ilk çalıştırılabilir iskeleti için Java ile uyumlu, güncel destek alan
bir Spring Boot sürümü ve tekrarlanabilir bir build aracı seçilmelidir.
Kurumun kesin teknoloji standardı henüz öğrenilmemiştir.

Spring'in 30.07.2026 tarihindeki resmî proje bilgisi Spring Boot 4.1.0'ı güncel
kararlı sürüm olarak göstermektedir. Spring Boot 3.5 serisinin açık kaynak
desteği sona ermiştir. Yerel ortamda Java 21.0.11 LTS ve Docker Engine
doğrulanmıştır.

## Karar

- Dil seviyesi Java 21 olacaktır.
- Başlangıç framework sürümü Spring Boot 4.1.0 olacaktır.
- Build aracı Maven ve proje içindeki Maven Wrapper olacaktır.
- Kalıcı veri için PostgreSQL 17 kullanılacaktır.
- Yerel bağımlılık olarak yalnız PostgreSQL, Docker Compose ile çalıştırılacaktır.
- Integration testleri gerçek PostgreSQL'i Testcontainers ile başlatacaktır.
- Redis ve RabbitMQ Aşama 0'a eklenmeyecektir.

## Gerekçe

- Java 21 LTS öğrenme ve uzun süreli kullanım için dengeli bir tabandır.
- Spring Boot 4.1.0 güncel açık kaynak destek hattındadır ve Java 21'i destekler.
- Maven'ın convention tabanlı yaşam döngüsü başlangıçta Gradle'ın esnek DSL'ine
  göre daha az ek karar gerektirir.
- Maven Wrapper, geliştirici ve CI ortamının aynı Maven sürümünü kullanmasını
  sağlar.
- Gerçek PostgreSQL testi, H2 gibi farklı davranabilen bir sahte veritabanının
  constraint ve migration farklarını gizlemesini önler.

## Alternatifler

- Gradle geçerli bir alternatiftir ve daha programlanabilir build tanımı sunar;
  fakat bu aşamada ek DSL öğrenme maliyeti getirir.
- Spring Boot 3.5 daha eski ekosistemlerle uyumlu olabilir; ancak açık kaynak
  destek süresi sona erdiği için kurum zorunluluğu olmadan tercih edilmemiştir.
- PostgreSQL'in makineye doğrudan kurulması mümkündür; Docker Compose kadar
  tekrarlanabilir ve izole değildir.

## Sonuçlar

- Proje Java 21 çalıştırma ortamı gerektirir.
- Uygulamanın yerel çalışması için PostgreSQL container'ı açık olmalıdır.
- Integration testleri için çalışan bir Docker Engine gerekir.
- Framework ana sürümü nedeniyle eski Spring Boot 3 örneklerinin paket adları
  birebir kullanılamayabilir.
- Geliştirme bilgisayarındaki mevcut port çakışmalarına dokunmamak için uygulama
  varsayılan olarak `8081`, Docker PostgreSQL ise host üzerinde `5433` kullanır.
  Container içindeki PostgreSQL portu standart `5432` olarak kalır.

## Yeniden değerlendirme tetikleyicileri

- Backend lead veya kurum farklı bir dil, framework, sürüm ya da build aracı
  standardı bildirirse
- Spring Boot 4 uyumsuz bir kurumsal bağımlılık ortaya çıkarsa
- Java 21 dışında zorunlu bir çalışma ortamı belirlenirse
- Maven build yaşam döngüsü ekip ihtiyaçlarını karşılamazsa
