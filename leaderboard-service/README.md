# Leaderboard Service

Leaderboard, monolith'in `xp.changed.v1` olaylarını tüketen ayrı Spring Boot
servisidir. Monolith veritabanına erişmez.

## Sahip olduğu veriler

- PostgreSQL `leaderboard_xp_entries`: idempotent ve kalıcı XP projeksiyonu
- Redis: PostgreSQL'den yeniden kurulabilen hızlı sıralama görünümü

## Yerel çalışma

Kök dizinde PostgreSQL, RabbitMQ ve Redis bağımlılıklarını başlat:

```powershell
docker compose up -d --wait
```

Compose servisi de 8082'de başlatır. Ayrı Maven süreciyle çalıştırmak istersen:

```powershell
.\mvnw.cmd -f leaderboard-service\pom.xml spring-boot:run
```

Monolith'in dış leaderboard API'sini bu servise yönlendirmek için monolith'i aynı
PowerShell penceresinde şu değişkenle başlat:

```powershell
$env:LEADERBOARD_REMOTE_ENABLED="true"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Mevcut XP ledger'ı ilk kez aktarmak için ADMIN kimliğiyle bir kez `POST
/api/v1/admin/leaderboards/replay-xp-events` çağrılır. İşlem idempotenttir.

## Doğrulama

```powershell
.\mvnw.cmd -f leaderboard-service\pom.xml test
```
