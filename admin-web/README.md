# Yönetim ve kullanıcı arayüzleri (Aşama 10A–10F)

Bu uygulama ayrı **İçerik yönetimi** sekmesinde dizi/film oluşturma, yayımdaki
metadata ve kapak düzenleme, güvenli kalıcı silme ve quiz yazarlığını içerir. Kapak
ve soru görselleri ilgili formdan doğrudan yüklenir. Quiz
yayın kontrolünü içerir. XP/leaderboard operasyonları ayrı ürün dilimleridir.

Dizilerde editör sezon sayısını ve her sezonun bölüm sayısını birlikte girer;
admin tek toplu API isteğiyle başlangıç kayıtlarını oluşturur. Yayımlanmış dizide
yalnız son sezon/bölümden sonra ekleme yapılabilir; eski kayıtlar salt okunurdur.
Taslak dizide bölüm adı ve açıklaması **Bölümleri düzenle** alanından değiştirilebilir.

Production'da admin gerçek e-posta/şifre oturumuyla `/admin` adresinden giriş yapar.
Geçici aktör header'ları yalnız yerel geliştirme kolaylığıdır; tarayıcıdaki rol
kontrolü güvenlik sınırı değildir, gerçek yetkilendirme API tarafından uygulanır.

```powershell
npm ci
npm run dev
```

Varsayılan API yolu `/api` olduğundan Vite geliştirme sunucusu istekleri yerel
backend'e (`8081`) iletir. Ayrı bir istemci API adresi için `VITE_API_BASE_URL`,
farklı local proxy hedefi için `VITE_PROXY_TARGET` verin.

Doğrulama komutları:

```powershell
npm run test
npm run build
```

İçerik detayında **Kapak görseli** alanından JPEG veya PNG dosyası seçilir,
önce yüklenir ve ardından alternatif metinle kapağa bağlanır. Sunucu 5 MB,
4096 × 4096 piksel, dosya imzası ve geçerli görsel içeriğini tekrar doğrular.
Yeni draft publish önkoşullarını karşılamıyorsa backend'in hata kodu ve trace
ID'si arayüzde gösterilir. Bu davranış başarılı production authentication
anlamına gelmez.

Bağımsız görsel kütüphanesi gösterilmez. Yeni kapak ve soru görselleri ilgili
düzenleme alanından yüklenip doğrudan kayda bağlanır. İçerik detayındaki
**Quiz çalışma alanı**, dizi geneli, belirli sezon veya belirli bölüm kapsamlı
taslak oluşturur; sürüm metnini, soru sırasını, tam dört şıkkı, doğru cevabı ve
erişilebilir görsel alanlarını yönetir. Sorular arasında yatay gezinilir; sıralı
alt önizlemeden herhangi bir soru yeniden seçilip kaydedilir. Zorluk otomatik
normaldir ve kullanıcıya ayrıca gösterilmez. Taslak silinebilir veya yayınlanır;
yayınlanmış sürümde değişiklik yeni bir draft sürümü açar.

Quiz on sorudan azsa sekme veya sayfa kapatılırken tarayıcı uyarı gösterir. Bu
editör kaybını önleyen bir UX korumasıdır; backend yayın minimumu değildir.
İçerik silme fiziksel silmedir. Daha önce oynanmış içerik, attempt ve XP geçmişini
korumak için `CONTENT_DELETE_HAS_GAMEPLAY_HISTORY` kodlu `409` ile reddedilir.

Admin ana menüsündeki **Quizler** sayfası hazırlanmakta olan ve kullanıma açık
quizleri; **Quiz geçmişi** sayfası kaldırılan quizleri ve eski yayın sürümlerini
gösterir. Hiç yayınlanmamış quiz fiziksel silinir. Kullanıma açılmış quiz
geçmişe kaldırılır; kazanılmış XP değişmez.

**Görselli cevap anahtarı PDF** düğmesi içerik adı, açıklaması, quiz kimliği/sürümü,
kapsamı ve doğru seçenekleri işaretlenmiş gerçek bir `.pdf` dosyası indirir. Her
soru çözme ekranına yakın ayrı sayfada gösterilir; soruya özel görsel yoksa içerik
kapağı kullanılır. PDF
kütüphanesi yalnız düğmeye basıldığında yüklendiği için ilk admin ekran paketini
büyütmez.

## Kullanıcı kataloğu

Bu aynı Vite uygulamasının kullanıcı giriş noktasıdır. Yayınlanmış içeriği,
sezon/bölüm bilgisini, quiz çözme akışını, sonucu, XP özetini ve global
sıralamayı gösterir. Süre, soru sırası, doğruluk ve skor backend tarafından
belirlenir.

`npm run dev` sonrasında kullanıcı için `http://localhost:5173/`, admin için
`http://localhost:5173/admin.html` adresini açın. **Hesap oluştur** ile e-posta,
görünen ad ve en az sekiz karakterli şifre
girin. Sonraki ziyaretlerde aynı hesapla giriş yapılır; quiz, XP ve sıralama
verisi bu hesabın kalıcı UUID'sine bağlanır.

Herkese açık kayıt yalnız `USER` rolü üretir. İlk yönetici production başlangıç
ayarından bir kez oluşturulur; admin ekranında kayıt olma seçeneği yoktur. Kurumsal
tabii SSO/OIDC ileride geldiğinde bu yerel hesap sınırı ayrıca değiştirilebilir.
