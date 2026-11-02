# Gherkin biçimli ana kabul senaryoları

```gherkin
Feature: Uygun saatten çakışmasız randevu
  Scenario: Hasta müsait bir randevu talep eder
    Given "Merkez" şubesi Pazartesi 09:00-17:00 açıktır
    And "Dr. Ada" 09:00-12:00 çalışır ve "Genel Muayene" yapabilir
    And "Genel Muayene" süresi 30 dakika ve ücreti 800 TRY'dir
    When hasta Pazartesi 09:00 için randevu talep eder
    Then randevu PENDING durumunda oluşturulur
    And bitiş saati 09:30 olur
    And ücret 800 TRY olarak sabitlenir

  Scenario: Zaman aralığı çakışır
    Given aynı doktorda 09:00-09:30 PENDING randevu vardır
    When başka hasta 09:15-09:45 için randevu talep eder
    Then sistem BOOKING_CONFLICT döndürür
    And ikinci randevu oluşturulmaz

  Scenario: Yarı açık aralık ardışıklığı
    Given aynı doktorda 09:00-09:30 randevu vardır
    When başka hasta 09:30-10:00 için randevu talep eder
    Then yeni randevu oluşturulabilir

  Scenario: Yetki kontrolü
    Given şube A personeli kullanıcıya giriş yapmıştır
    When şube B'ye ait randevuyu onaylamaya çalışır
    Then sistem yetkisiz erişimi engeller

  Scenario: İzin
    Given doktorun 10:00-11:00 izni vardır
    When hasta 10:15-10:45 seçer
    Then slot kullanılamaz

  Scenario: Eşzamanlı talep
    Given aynı doktor aynı saat için iki eşzamanlı istek alır
    When iki işlemin transaction'ı yarışır
    Then en fazla bir PENDING randevu commit edilir
    And diğer istek kontrollü çakışma yanıtı alır
```

## Kanıt
Senaryolar otomasyonla (`BookingServiceIT`, `AuthorizationIT`), ayrıca UAT ekran görüntüleriyle belgelenir. Sadece sözlü beyan kabul edilmez.
