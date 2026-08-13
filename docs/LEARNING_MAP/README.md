# Kod Öğrenme Haritası

Bu klasör, çalışan kodun yerine geçmeyen açıklamalı bir eşlikçidir. Amaç, Java veya
Spring ayrıntılarına takılmadan bir isteğin sistemde hangi sınıflardan geçtiğini
izleyebilmektir.

## Neden birebir yalancı kod değil?

Gerçek kodun tamamını ikinci kez yalancı kodla yazmak iki kopyanın zamanla
birbirinden ayrılmasına yol açar. Bu harita bunun yerine şu düzeni kullanır:

- Klasör yolu gerçek kaynak kod yolunu aynalar.
- Açıklama dosyasının adı gerçek dosya adının sonuna `.md` eklenmiş halidir.
- Metotlar gerçek sınıftaki sırayla anlatılır.
- Her metot için amaç, girdi, çıktı, bağımlılık, hata davranışı ve akıştaki yer verilir.
- Her bölümün başında gerçek kaynak dosyasının yolu yazılır.

Örnek:

```text
Gerçek kod:
src/main/java/com/trt/contentengagement/gamification/api/XpController.java

Öğrenme karşılığı:
docs/LEARNING_MAP/src/main/java/com/trt/contentengagement/gamification/api/
XpController.java.md
```

## Bu ilk dilimin kapsamı

Bu dilim iki konuyu kapsar:

1. `ContentEngagementApplication`: Spring Boot uygulamasının başlangıç noktası.
2. `gamification`: XP API'si, kullanım senaryoları, domain kuralları, portlar ve
   PostgreSQL adapter'ı.

## Önerilen okuma sırası

1. `ContentEngagementApplication.java.md`
2. `gamification/README.md`
3. `gamification/api/XpController.java.md`
4. `gamification/application/XpService.java.md`
5. `gamification/domain/XpTransaction.java.md`
6. `gamification/infrastructure/persistence/JdbcXpLedgerRepositoryAdapter.java.md`
7. `gamification/SUPPORTING_TYPES.md`
8. `gamification/TEST_MAP.md`

## Harita nasıl okunmalı?

Önce yalnız “Kısa akış” bölümünü oku. Sonra gerçek dosyayı yanında açıp her metodu
açıklamadaki karşılığıyla eşleştir. Son olarak kendine şunları sor:

- Bu sınıf hangi katmanda ve neden burada?
- Girdi nereden geliyor, çıktı nereye gidiyor?
- İş kuralını hangi sınıf koruyor?
- Aynı istek iki kez gelirse ne oluyor?
- Hangi test bu davranışı kanıtlıyor?

## Güncellik kuralı

Gerçek dosyada public metot eklenir, silinir veya davranışı değişirse aynı görevde
öğrenme karşılığı da güncellenmelidir. Açıklama ile kod çelişirse doğru kaynak her
zaman çalışan kod ve testlerdir.
