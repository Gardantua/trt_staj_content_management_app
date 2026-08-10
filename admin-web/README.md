# Yönetim arayüzü (Aşama 10A)

Bu uygulama içerik kataloğu yönetimini ve taslak içeriğe kapak görseli bağlama
akışını içerir. Quiz yazarlığı, XP ve leaderboard operasyonları ayrı ürün
dilimleri olarak kapsam dışındadır.

Yerel backend'in `local` profiliyle çalışması gerekir. Geçici aktör header'ları
yalnız bu profil için ayarlanır; tarayıcıdaki rol kontrolü güvenlik sınırı
değildir, gerçek yetkilendirme API tarafından uygulanır.

```powershell
npm ci
$env:VITE_LOCAL_ACTOR_ID = "22222222-2222-2222-2222-222222222222"
$env:VITE_LOCAL_ACTOR_ROLES = "EDITOR"
npm run dev
```

Varsayılan API yolu `/api` olduğundan Vite geliştirme sunucusu istekleri yerel
backend'e (`8081`) iletir. Ayrı bir API adresi için `VITE_API_BASE_URL` verin.

Doğrulama komutları:

```powershell
npm run test
npm run build
```

Taslak detayında **Kapak görseli** alanından JPEG veya PNG dosyası seçilir,
önce yüklenir ve ardından alternatif metinle kapağa bağlanır. Sunucu 5 MB,
4096 × 4096 piksel, dosya imzası ve geçerli görsel içeriğini tekrar doğrular.
Yeni draft publish önkoşullarını karşılamıyorsa backend'in hata kodu ve trace
ID'si arayüzde gösterilir. Bu davranış başarılı production authentication
anlamına gelmez.

## Kullanıcı kataloğu (Aşama 10B)

Bu aynı Vite uygulamasının kullanıcı giriş noktasıdır. Yayınlanmış içeriği,
sezon/bölüm bilgisini, quiz çözme akışını, sonucu, XP özetini ve global
sıralamayı gösterir. Süre, soru sırası, doğruluk ve skor backend tarafından
belirlenir.

```powershell
$env:VITE_LOCAL_ACTOR_ID = "11111111-1111-1111-1111-111111111111"
$env:VITE_LOCAL_ACTOR_ROLES = "USER"
npm run dev
```

Tarayıcıda `http://localhost:5173/user.html` adresini açın. Environment değeri
verilmezse sayfadaki yerel deneme kapısına geçerli bir USER UUID'si girilebilir.
Bu değerler yalnız backend'in `local` profiliyle çalışır ve production login
yerine geçmez.
