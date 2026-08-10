# Kullanıcı Web Deneyimi Planı

## Amaç

Yayınlanmış içeriklerden kullanıcıyı güvenli quiz akışına taşıyan, sonuç ve
ilerlemeyi görünür kılan tek bir mobil-duyarlı web deneyimi oluşturmak.

## Tamamlanan ekranlar

## Görsel kimlik: Hikâye İzi (10.08.2026)

Kullanıcı deneyiminin adı **Hikâye İzi**dir: kullanıcı yayınlanan hikâyeyi
keşfeder, ayrıntısına iner, hatırlama meydan okumasını tamamlar ve XP ilerlemesini
görür. Bu, başka bir ürünün ekranını kopyalamaz; ürünün kendi iz sürme anlatımını
görünür kılar.

- Koyu gece zemini, mint ana eylem ve gök mavisi ikincil bilgi rengi kullanılır;
  turuncu vurgu ve gradyen yoktur.
- Kullanıcının sağladığı `gorseller/` varlıkları platform bağlamı için işlenir;
  ürün adı, ekran hiyerarşisi ve etkileşim dili özgündür.
- Keşfet alanı doğrudan “iz sürme” vaadiyle başlar; numaralı yönlendirme veya
  boş dekoratif şekil kullanılmaz. Kart, quiz ve profil alanları aynı görsel
  sözlüğü sürdürür.
- Hareket yalnız odak/hover ve cevap geçişlerinde kısa durum geri bildirimi
  verir; süre, doğru cevap ve skor yine backend response'undan okunur.

1. **Yerel oturum kapısı:** Yalnız `local` backend profilinde kullanılabilen
   UUID tabanlı USER deneme oturumu. Gerçek login olarak sunulmaz.
2. **Keşfet:** Yayınlanmış katalog ve sayfalama.
3. **İçerik detayı:** Kapak, sezon/bölüm ve yayınlanmış quiz listesi.
4. **Quiz:** Standart veya uzatılmış süre seçimi, sunucu deadline sayacı,
   sıradaki soru, erişilebilir görsel ve cevap geri bildirimi.
5. **Sonuç ve profil:** Sunucu skoru, beklenen XP, toplam XP ve kullanıcının
   global sırası.
6. **Sıralama:** Global Top 10 ve kullanıcının Top 10 dışındaki sırası.

## Sunucu otoritesi

İstemci doğru cevabı, skoru veya süreyi üretmez. Her cevap yalnız question ID
ve option ID taşır; backend sonucu, sıradaki soruyu ve kesin skoru döndürür.
Gönderim anahtarı kullanıcı arayüzünde saklanır; ağ hatasında aynı anahtarla
tekrar denenmesi duplicate cevap oluşturmaz.

## Mobil sınırlar

Katalog küçük ekranlarda iki kolona, quiz/profil/sıralama tek kolona iner.
Birincil dokunma hedefleri en az 44 px'e yakındır; süre, ilerleme, doğru/yanlış
durumu renk dışında metinle de verilir. Bu ekran sınırları, ileride native
mobil istemcinin aynı API sözleşmesini kullanabilmesi için korunur.

## Bilinçli olarak dışarıda kalanlar

- Production OIDC/OAuth login, kayıt, parola ve profil düzenleme
- Sosyal profil, görünen ad/avatar ve arkadaş sıralaması
- Offline quiz veya gerçek push bildirimi

Bu alanlar için kurum kimlik ve hosting sözleşmeleri geldikten sonra ayrı karar,
güvenlik testi ve mobil uygulama planı gerekir.
