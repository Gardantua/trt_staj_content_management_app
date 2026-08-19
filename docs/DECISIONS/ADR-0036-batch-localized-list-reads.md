# ADR-0036: Yerelleştirilmiş listelerde toplu çeviri okuması

## Durum

Accepted — 19.08.2026

## Bağlam

İngilizce içerik ve yayınlanmış quiz listelerinde önce ana kayıtlar okunuyor, ardından
her kayıt için çeviri repository'si yeniden çağrılıyordu. Her çağrı kök ve alt öğeler
için birden fazla SQL çalıştırdığı için liste büyüdükçe sorgu sayısı doğrusal artıyordu.
Bu `N+1` sorgu biçimi son arayüz yavaşlamasının somut nedeniydi.

## Karar

- Liste servisleri önce sayfadaki içerik veya yayın sürümü kimliklerini toplar.
- Çeviri repository portları tek kimlik okumasına ek olarak toplu `findAll` işlemi sunar.
- JDBC adapter'ları kök çevirileri, alt öğeleri ve en alt öğeleri ayrı `IN` sorgularıyla
  topluca okur ve bellekte kimliğe göre birleştirir.
- Türkçe ana/fallback dil davranışı, tekil detay endpoint'leri ve kalıcı veri modeli
  değişmez.
- Bu aşamada Redis, uygulama içi cache veya yeni bir altyapı bileşeni eklenmez.

## Gerekçe

Toplu okuma, sorgu sayısını listedeki kayıt miktarından ayırır ve mevcut PostgreSQL
transaction sınırı içinde güncel sonucu döndürür. Yeni bir cache eklemeden asıl sorgu
şekli düzeltilir; böylece invalidation ve eski veri riski oluşmaz.

## Değerlendirilen alternatifler

- Uygulama/Redis önbelleği: tekrar okumalarda hızlıdır; fakat çeviri güncellemelerinde
  invalidation, ek gözlemlenebilirlik ve eski veri riski getirir.
- Tek büyük join sorgusu: round-trip sayısını azaltabilir; fakat sezon, bölüm, soru ve
  seçeneklerin kartezyen çoğalması sonucu aktarılan satır miktarını büyütür.
- Mevcut tekil sorguları korumak: en küçük kod değişikliğidir; ancak liste büyüdükçe
  yavaşlama devam eder.

## Sonuçlar

- İngilizce bir liste için çeviri SQL sayısı kayıt sayısına göre artmaz; çeviri varsa
  en fazla üç toplu sorgu çalışır.
- Sonuçların kimliğe göre bellekte birleştirilmesi adapter sorumluluğudur.
- Çok büyük sayfa boyutları ileride kullanılacaksa `IN` parametre sınırı ve bellek
  tüketimi yeniden ölçülmelidir; mevcut sayfalama bu riski sınırlar.

## Yeniden değerlendirme tetikleyicileri

- Ölçümlerde toplu sorguların hâlâ hedef gecikmeyi aşması
- Sayfa boyutlarının belirgin şekilde büyümesi
- Birden fazla uygulama örneğinde yoğun ve çoğunlukla değişmeyen çeviri trafiği oluşması
