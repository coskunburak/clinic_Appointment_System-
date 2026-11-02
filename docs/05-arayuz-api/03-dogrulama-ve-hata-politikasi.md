# Input validation, hata eşleme ve tarih/saat politikası

## Form validation
- E-posta: normalize/trimming; unique, format kontrollü; parolalar sadece güvenli hash.
- İsim: 2..120 karakter; telefon örnek verisi için opsiyonel; HTML/template escaping.
- Hizmet: ad 3..150; süre 1..480 dakika; ücret 0..9999999999.99 iki decimal; currency 3 karakter.
- Saat: `HH:mm`, end > start; iki overlap haftalık saat girişi için birleştirme/reddetme kuralı belirlenir.
- Tarih: server `Clock` kaynağı ile gelecek; geçmiş veya çok ileri (>180 gün varsayımı) istek reddedilir.
- UUID: URL'de tip doğrulaması, scope kontrolü ve kaynak yok/hata mesajı.

## Güven sınırı
HTML seçim listesi doğrudur varsayımı YANLIŞTIR. Formda başka branch service/staff UUID gönderilebilir. `BookingService`, authoritative repo'dan org/branch/eligible staff ilişkisini tekrar doğrular.

## Zaman dönüşümü
1. `LocalDate + LocalTime`, `ZoneId` ve kurala göre `ZonedDateTime` elde et.
2. DST gap'te local saat geçersizse 422; overlap'te offset açık belirtilmediyse 422; kullanıcıya yeni seçim sun.
3. Kalıcı `Instant startAt/endAt` UTC anıdır; yerel görüntüleme seçili şube zone'uyla yapılır.
4. Haftalık saatler şubenin **yerel** zamanına göredir. İstisna günler tarih olarak yereldir.
5. Rezervasyon süresini önce gerçek `Duration` ile ekle; başlangıç/bitiş instant kontrol edilir.

## Error mapping
Domain exception -> controller advice -> güvenli kod + anlaşılır Türkçe metin. ConstraintViolation, stale version ve data conflict kontrollü, hassas SQL/stacktrace ancak loglarda (maskeli) görünür. Loglarda `requestId` bulunması önerilir.
