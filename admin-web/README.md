# Yönetim arayüzü (Aşama 10A)

Bu uygulama yalnız içerik kataloğu yönetim dikey dilimini içerir. Quiz, medya
yükleme, XP ve leaderboard işlemleri bilerek kapsam dışındadır.

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

Bu dilimde medya yükleme ve kapak bağlama ekranı yoktur. Yeni draft publish
önkoşullarını karşılamıyorsa backend'in hata kodu ve trace ID'si arayüzde
gösterilir. Bu davranış başarılı production authentication veya uçtan uca
publish akışı anlamına gelmez.

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
