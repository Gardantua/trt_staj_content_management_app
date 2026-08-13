# ADR-0025: Sezon ve bölümlerin atomik toplu planla oluşturulması

## Durum

Accepted — 10.08.2026

## Bağlam

Bir dizinin sezon ve bölümlerini tek tek admin istekleriyle oluşturmak, 29–30
bölümlük sezonlarda onlarca form doldurma ve ağ isteği gerektiriyordu. İsteklerden
biri başarısız olduğunda içerik hiyerarşisi yarım kalabiliyor, editör de hangi
bölümde kaldığını takip etmek zorunda kalıyordu.

## Karar

- Diziye bir veya daha fazla sezon, her sezonun bölüm sayısıyla birlikte
  `POST /api/v1/admin/contents/{contentId}/season-plan` komutuyla eklenir.
- Komut `Content` aggregate'ini bir kez yükler, bütün sezon ve bölümleri aynı
  transaction içinde üretir ve aggregate'i bir kez kaydeder.
- Sezon ve bölüm numaraları sırayla belirlenir; varsayılan başlıklar `1. Sezon`
  ve `1. Bölüm` biçimindedir. Özel başlık/açıklama mevcut tekil güncelleme
  endpoint'leriyle sonradan değiştirilebilir.
- Tek bir sezon tanımı bile domain kuralını ihlal ederse bütün transaction geri
  alınır. Kısmi sezon veya bölüm ağacı kalmaz.
- Bir istekte en fazla 100 sezon, sezonda 1.000 bölüm ve toplamda 10.000 bölüm
  kabul edilir.
- Admin arayüzü tek tek sezon/bölüm ekleme formları yerine sezon sayısı ve her
  sezon için bölüm sayısı alanlarını gösterir. Oluşan uzun bölüm listeleri yalnız
  editör istediğinde render edilir.

## Alternatifler ve trade-off

Frontend'in mevcut sezon ve bölüm endpoint'lerini döngüyle çağırması backend
değişikliğini azaltırdı; fakat onlarca HTTP isteği, rate-limit tüketimi ve kısmi
başarı riski üretirdi. Tek toplu endpoint daha güçlü transaction garantisi sağlar.

İçerik oluşturma isteğine sezon planını doğrudan eklemek tek ekranlı bir sihirbaz
sağlayabilirdi; ancak kapak, metadata ve hiyerarşi hatalarını aynı büyük sözleşmede
birleştirirdi. Mevcut iki adımlı içerik oluşturma akışı korunarak toplu plan içerik
detayında ayrı ve tekrar kullanılabilir bir komut olarak tutuldu.

## Sonuçlar

- 2 sezon ve 59 bölüm tek admin isteğiyle oluşturulabilir.
- Audit her bölüm için onlarca kayıt yerine tek `SEASON_PLAN_CREATED` işlemi tutar.
- Mevcut PostgreSQL unique ve foreign key constraint'leri değişmediği için yeni
  migration gerekmez.
- ADR-0027 ile toplu plan yayımlanmış dizide de çalışır; yalnız mevcut son sezon
  numarasından sonra ekleme yapabilir.
