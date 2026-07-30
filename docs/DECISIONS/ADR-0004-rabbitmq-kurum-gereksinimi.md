# ADR-0004: RabbitMQ'nun Hedef Mimaride Kullanılması

## Durum

Accepted

## Bağlam

Stajdan sorumlu mühendis, projede RabbitMQ kullanılacağını bildirmiştir. Önceki
plan RabbitMQ'yu gerçek ihtiyaç oluştuğunda değerlendirilecek bir aday olarak
tutuyordu. Yol haritasında güvenilir mesajlaşma zaten çekirdek gameplay ve
PostgreSQL tabanlı XP davranışından sonraki ayrı aşama olarak tanımlanmıştı.

RabbitMQ'nun kurum gereksinimi olması, çekirdek iş kurallarının broker'a bağımlı
tasarlanmasını veya broker'ın ilk aşamaya eklenmesini gerektirmez.

## Karar

- RabbitMQ hedef mimaride kullanılacaktır.
- RabbitMQ, yol haritasındaki Aşama 6'da eklenecektir.
- Önce gameplay ve XP davranışı PostgreSQL üzerinde güvenilir ve idempotent
  biçimde tamamlanacaktır.
- Quiz tamamlama olayı Transactional Outbox üzerinden yayımlanacaktır.
- Consumer tarafında Inbox/deduplication, retry ve DLQ davranışları
  tanımlanacaktır.
- RabbitMQ kalıcı iş verisinin doğru kaynağı olmayacaktır.

## Gerekçe

- Kurum/proje standardına uyulur.
- İş kuralı ile mesajlaşma altyapısının sorumlulukları ayrılır.
- Outbox, veritabanı işlemi başarılıyken mesaj gönderiminin kaybolması riskini
  azaltır.
- Idempotent consumer, aynı mesajın tekrar teslim edilmesinin ikinci XP işlemi
  üretmesini engeller.
- Ayrı aşama, RabbitMQ'nun sisteme kattığı davranışın test edilmesini ve
  öğrenilmesini kolaylaştırır.

## Sonuçlar

- Aşama 6 opsiyonel değerlendirme olmaktan çıkar ve planlanan zorunlu aşama
  olur.
- Aşama 0'a RabbitMQ eklenmez; mevcut PostgreSQL temeli değişmez.
- RabbitMQ gerçek container ile Testcontainers integration testlerine tabi
  tutulur.
- Broker kesintisi quiz sonucunu veya Outbox kaydını kaybettirmemelidir.
- Redis hakkındaki ihtiyaç temelli karar değişmez.

## Yeniden değerlendirme tetikleyicileri

- Kurumun broker standardını değiştirmesi
- RabbitMQ yerine yönetilen başka bir mesajlaşma hizmetinin zorunlu tutulması
- Trafik, replay veya stream gereksinimlerinin farklı bir broker gerektirmesi
