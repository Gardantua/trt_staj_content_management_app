# Admin Web İhtiyaç ve Teslim Planı

## Amaç ve sınır

Admin, editörün kullanıcıya gösterilecek içeriği güvenli biçimde hazırladığı
çalışma alanıdır. Kullanıcı profilini, skoru veya doğru cevabı tarayıcıdan
belirlemez; her karar mevcut backend sözleşmesi ve rol kontrolüyle uygulanır.

Bu plan ihtiyaçların tamamını görünür kılar; hepsinin bir anda geliştirilmesi
anlamına gelmez. Her sürüm tek bir doğrulanabilir dikey dilim olarak ele alınır.

## İhtiyaç haritası

| Alan | Editörün ihtiyacı | Durum | Planlanan dilim |
| --- | --- | --- | --- |
| Katalog | Dizi/film taslağı oluşturma, başlık-açıklama düzenleme, sezon ve bölüm sıralama | Var | 10A |
| Görsel ve erişilebilirlik | JPEG/PNG doğrudan yükleme, alternatif metin ve kapak bağlama; kütüphane UI'si kaldırıldı | Var | 10C + 10D bakım |
| Quiz yazarlığı | İçerik, sezon veya bölüm kapsamlı quiz; taslak/sürüm, soru-şık-doğru cevap ve erişilebilir soru metni | Var | 10D |
| Yayın kontrolü | Eksik öğeleri tek ekranda görme, yayınlama, arşivleme ve değişmez yayın sürümünü ayırt etme | Sonraki | 10E önerisi |
| Operasyon | Yetkili XP düzeltmesi, leaderboard yeniden kurma, audit kaydını filtreleme | Sonraki | 10F önerisi |
| Ekip çalışması | Taslak sahipliği, değişiklik geçmişi, çakışma uyarısı ve onay akışı | Karar gerekli | Gerçek çok-editör ihtiyacından sonra |
| Production erişimi | OIDC oturumu, rol eşlemesi, CSRF/CORS ve kurumun medya depolama/CDN kararı | Karar gerekli | Hosting topolojisi netleşince |

## Geliştirme sırası

1. **10C — Medya ve kapak bağlama:** Editör, doğrulanmış görseli yükler,
   açıklayıcı alternatif metni girer ve taslağa kapak olarak bağlar. Bu sayede
   mevcut publish önkoşulu arayüzden tamamlanır.
2. **10D — Quiz yazarlığı (tamamlandı):** İçerik, sezon veya bölüm kapsamlı quiz
   taslağı; soru, şık, doğru cevap ve doğrudan yüklenen soru görseli
   yönetimi eklendi. Doğru cevap bilgisi yalnız admin endpoint'inde kalır;
   kullanıcı tarafına sızmaz.
3. **10E — Yayın kontrolü:** İçerik ve quiz için eksik/uygun durum kontrolü,
   yayınlama ve arşivleme sunulur. Yayın sonrası düzenleme kuralı backend'in
   otoritesinde kalır.
4. **10F — Operasyon araçları:** Sadece ADMIN için XP düzeltmesi, leaderboard
   yeniden kurma ve audit araması uygulanır. Her işlem gerekçe ve izlenebilir
   kayıtla gider.

## Her dilim için değişmeyen kalite eşiği

- Native etiketli form alanları, klavye odağı ve renk dışı durum mesajları.
- Backend hata kodu ve trace ID'nin destek için korunması.
- Yetkinin tarayıcıda değil backend'de denetlenmesi.
- Frontend unit testleri ile TypeScript production buildinin geçmesi.
- Yeni veya kalıcı mimari seçim varsa ADR; tamamlanan iş, test ve sıradaki tek
  aday için `CURRENT_STATE` güncellemesi.

## Bilinçli olarak bekletilenler

Canlı TV, eğitim modu, sosyal özellikler, otomatik yapay zekâ soru üretimi ve
mikroservisleşme MVP admin planında değildir. Bunlar ayrı ürün kararı,
yetkilendirme ve test stratejisi gerektirir.
