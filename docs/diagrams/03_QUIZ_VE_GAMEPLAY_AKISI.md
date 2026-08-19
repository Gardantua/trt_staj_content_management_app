# 03 - Quiz ve Gameplay İş Akışı (Şekil 3)

Bu diyagram, **TRT tabii İçerik Etkileşim Platformu**'ndaki sunucu otoriter (Server-Authoritative) puanlama mekanizmasını, süre/deadline üretimini, `AWAITING_NEXT_QUESTION` durum geçişini ve görsel önyükleme (image preloading) sürecini gösterir.

---

## ⏱️ Şekil 3. Sunucu Otoriteli Quiz ve Gameplay Sıralama Diyagramı (Mermaid Sequence)

```mermaid
sequenceDiagram
    autonumber
    actor User as Kullanıcı (Tarayıcı)
    participant UI as React SPA (Web İstemcisi)
    participant Caddy as Caddy Reverse Proxy
    participant Backend as Spring Boot Monolith (/api)
    participant DB as Monolith PostgreSQL

    %% 1. BAŞLATMA
    rect rgb(245, 248, 255)
        note right of User: 1. Quiz Başlatma & Deadline Üretimi
        User->>UI: "Quiz'e Başla" tıklar
        UI->>Caddy: POST /api/v1/quizzes/{quizId}/start-attempt
        Caddy->>Backend: İstek iletilir (Cookie + CSRF)
        Backend->>DB: Aktif QuizVersion ve Soru 1'i getir
        Backend->>Backend: Sunucu Saatiyle Deadline Üret (now + 30sn)
        Backend->>DB: INSERT gameplay_attempts (status='IN_PROGRESS', deadline=...)
        Backend-->>UI: Soru 1 DTO (Doğru şık GİZLİ, deadline açık)
        UI->>UI: Geri sayımı 30 saniyeden başlatır
    end

    %% 2. CEVAPLAMA VE SÜRE DURDURMA
    rect rgb(255, 250, 245)
        note right of User: 2. Cevaplama, Doğrulama & Süre Durdurma
        User->>UI: B şıkkını seçer
        UI->>UI: Görünür sayacı ANINDA dondurur (Pause)
        UI->>Caddy: POST /api/v1/attempts/{attemptId}/answers (optionId=B)
        Caddy->>Backend: İstek iletilir
        Backend->>Backend: Süre Doğrulaması: server_now <= question_deadline ?
        alt Zamanında Cevap
            Backend->>DB: INSERT attempt_answers (score_awarded=10)
            Backend->>DB: UPDATE gameplay_attempts SET score+=10, status='AWAITING_NEXT_QUESTION'
        else Timeout
            Backend->>DB: INSERT attempt_answers (outcome='TIMEOUT', score_awarded=0)
            Backend->>DB: UPDATE gameplay_attempts SET status='AWAITING_NEXT_QUESTION'
        end
        Backend-->>UI: AnswerFeedback (isCorrect, earnedScore, correctOptionText)
        UI->>UI: Sonuç kartını açar (Puan ve Doğru Şık metni)
        UI-)UI: Arka Planda Sessizce Soru 2 Görselini İndirir (Preload)
    end

    %% 3. SONRAKİ SORUYA GEÇİŞ (SÜRE KORUMASI)
    rect rgb(245, 255, 245)
        note right of User: 3. Sonraki Soruya Geçiş & Süre Yenileme
        User->>UI: "Sonraki Soruya Geç" tıklar
        UI->>Caddy: POST /api/v1/attempts/{attemptId}/next-question
        Caddy->>Backend: İstek iletilir
        Backend->>Backend: Soru 2 için YENİ Deadline Üret (now + 30sn)
        Backend->>DB: UPDATE gameplay_attempts SET status='IN_PROGRESS', question_deadline=...
        Backend-->>UI: Soru 2 DTO (Temiz yeni deadline ile)
        UI->>UI: Sayacı temiz 30 saniyeden başlatır
    end

    %% 4. TAMAMLAMA
    rect rgb(245, 245, 245)
        note right of User: 4. Tamamlama & Atomik Outbox Kaydı
        User->>UI: Son soruyu tamamlar
        UI->>Caddy: POST /api/v1/attempts/{attemptId}/complete
        Caddy->>Backend: İstek iletilir
        Backend->>DB: BEGIN TX: UPDATE gameplay_attempts (COMPLETED) + INSERT outbox_events
        Backend-->>UI: Final Tebrik & Özet Kartı
    end
```

---

## 🎯 Temel Kazanımlar

- **`AWAITING_NEXT_QUESTION` Durumu:** Kullanıcı sonuç ekranında cevabı incelerken geçen süre sonraki sorudan çalınmaz; yeni soruya geçişte sunucu sıfırdan temiz 30 saniyelik deadline üretir.
- **Doğru Cevabın Gizliliği:** Doğru şık bilgisi (`correctOptionText`), soru çözülene kadar istemciye kesinlikle iletilmez.
- **Görsel Önyükleme (Preload):** Ağ gecikmesini önlemek için sonuç ekranı incelenirken sıradaki görsel arka planda indirilir.
