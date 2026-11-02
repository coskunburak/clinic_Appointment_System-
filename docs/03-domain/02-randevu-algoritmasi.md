# Booking algoritması, çakışma ve transaction doğruluğu

## Arayüz boş saat sorgusu (read-only)
1. Şube/hizmet/personel ilişkisinin aktifliğini ve hizmet uyumunu doğrula.
2. Şubenin `ZoneId` değeriyle istenen yerel günün açılış ve çalışma aralıklarını bul.
3. Düzenli mesaiyi, izin/özel gün istisnalarını ve şube açıklığını kesiştir.
4. Sıralı 15 dakika başlangıç adaylarını, **hizmetin süre uzunluğu kadar** interval halinde oluştur.
5. Çakışan aktif randevulara göre filtrele; müsait adayları yerel saat + offset ile sun.
6. Bu liste **anlık fotoğraftır**, rezervasyon garantisi değildir; nihai doğrulama yazma transaction'ında tekrar yapılır.

## Kesin rezervasyon (transaction)
```text
@Transactional BookingService.requestAppointment(command, actor):
  1. authenticate actor; derive patientId from session
  2. validate UUID, branch/service/staff ownership and active flags
  3. acquire PESSIMISTIC_WRITE lock on stable staff row (staffId)
  4. re-read authoritative branch service duration/fee and staff eligibility
  5. resolve local date/time against branch ZoneId -> Instant start, Instant end
  6. apply all SchedulingConstraint.validate(context)
       OpeningHoursConstraint
       StaffAvailabilityConstraint (exceptions)
       StaffServiceConstraint
       NoOverlapConstraint (read under staff lock)
       Optional PatientOverlapConstraint (patient lock needed if enabled)
  7. create PENDING Appointment + immutable price/duration/name snapshot
  8. insert Appointment + status history + idempotency mapping when enabled
  9. commit; release lock
```

**Kritik:** Çakışma kontrolünü transaction dışındaki boş saat ekranından devralma. Kilit alınmadan yapılan "SELECT COUNT ... sonra INSERT" yarıştır ve double-booking doğurur. Aynı personele ait her BOOK/RESCHEDULE (gelecekte) ve uygunluğu etkileyen güncelleme aynı kilit protokolünü kullanır. Personel çalışma saatini değiştirmeye karar veren servis mevcut randevuları kontrol eder veya aktif onaylı randevulara dokunmadığını garanti eder.

## Lock grafiği (önerilen sade akış)
```mermaid
sequenceDiagram
    participant UI as Hasta / Web
    participant B as BookingService
    participant DB as PostgreSQL
    UI->>B: POST create
    B->>DB: BEGIN; lock staff row FOR UPDATE
    DB-->>B: lock granted
    B->>DB: mevcut aktif randevuları oku
    B->>B: uygunluk + overlap + snapshot
    alt Uygun
      B->>DB: appointment + history INSERT
      B->>DB: COMMIT
      B-->>UI: 303 / detail PENDING
    else Çakışma
      B->>DB: ROLLBACK
      B-->>UI: 409 BOOKING_CONFLICT
    end
```

## Interval hesabı
```
existing.start < proposed.end && existing.end > proposed.start
```
Örnekler: `09:00–09:30` ile `09:30–10:00` **çakışmaz**; `09:00–09:30` ile `09:29–10:00` **çakışır**. Sınırlar `[start,end)` olarak yorumlanır.

## Postgres savunma katmanı (isteğe bağlı)
Postgres `btree_gist` ile `EXCLUDE USING GIST (staff_id WITH =, tstzrange(start_at_utc, end_at_utc, '[)') WITH &&) WHERE (status IN ('PENDING','CONFIRMED','COMPLETED'))` kısıtı eklenebilir. Ancak Sprint 6 kabulü için **çalışan kilit protokolü ve eşzamanlı entegrasyon testi** önce zorunludur. Migration uyumluluğu ve DB error mapping ayrıca test edilmeli; aynı transaction'da deadlock'a yol açan ters kilit sırası kullanılmamalı.

## Eşzamanlı test kabulü
- İki ayrı transaction ve gerçek PostgreSQL bağlantısı; iki thread başlangıcı barrier ile eşzamanlı.
- Her ikisi de aynı staff+interval'i talep eder.
- Bir tanesi oluşturulur; diğeri kontrollü hata alır; DB'de tek aktif appointment vardır.
- Aynı anda **farklı personel** için iki istek başarılı olabilir.
- Lock timeout/deadlock DB exception'ları iç detay göstermeden deterministik kullanıcı mesajına dönüştürülür.

## Idempotency (SHOULD)
İstemci tekrar gönderim anahtarı (`requestKey`) oluşturur; `(patient_id,request_key)` unique. Aynı anahtar aynı komut gövdesi ile tekrarlandığında önceki sonuç dönülür, farklı gövdede 409; anahtar mevcut transaction'da saklanır. Duplicate click UI üzerinden engellense bile server güvenliği sürdürülür.
