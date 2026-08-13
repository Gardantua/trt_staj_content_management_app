# ContentEngagementApplication.java

Gerçek kaynak: `src/main/java/com/trt/contentengagement/ContentEngagementApplication.java`

## Bu sınıf neden var?

Bu sınıf monolith backend'in başlangıç düğmesidir. İçinde iş kuralı yoktur. Java
programını Spring Boot uygulamasına dönüştürür ve Spring'in bileşenleri bulup
birbirine bağlamasını başlatır.

## Üstten alta açıklama

### `@SpringBootApplication`

Spring'e üç ana işi yapmasını söyler:

1. Bu uygulamanın yapılandırma başlangıç noktası olduğunu kabul et.
2. Gerekli Spring Boot ayarlarını bağımlılıklara göre otomatik kur.
3. `com.trt.contentengagement` paketi ve altındaki controller, service, repository
   gibi Spring bileşenlerini tara.

### `ContentEngagementApplication` sınıfı

Uygulamanın kimliğini ve başlangıç konumunu taşır. Nesnesi oluşturulup iş kuralı
çalıştırılmaz.

### `main(String[] arguments)`

- Amaç: Java süreci başladığında Spring Boot'u çalıştırmak.
- Girdi: Terminalden uygulamaya verilen başlangıç argümanları.
- Çıktı: Doğrudan bir değer döndürmez; Spring uygulama context'ini ve web
  sunucusunu ayağa kaldırır.
- Bağımlılık: `SpringApplication`.
- Hata davranışı: Yapılandırma, bean oluşturma, port veya veritabanı bağlantısı
  gibi başlangıç sorunları varsa uygulama açılmaz ve hata loguyla kapanır.
- Akıştaki yeri: Bütün HTTP isteklerinden önce yalnız bir kez çalışır.

Yalancı kod:

```text
PROGRAM BAŞLAYINCA
  Spring'e "ana uygulama sınıfı budur" de
  komut satırı argümanlarını Spring'e ver
  bileşenleri oluştur
  yapılandırmayı yükle
  web sunucusunu başlat
```
