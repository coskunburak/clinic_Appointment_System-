# Booking algoritması, çakışma ve transaction doğruluğu

## Arayüz boş saat sorgusu (read-only)
1. Şube/hizmet/personel ilişkisinin aktifliğini ve hizmet uyumunu doğrula.
2. Şubenin `ZoneId` değeriyle istenen yerel günün açılış ve çalışma aralıklarını bul.
3. Düzenli mesaiyi, izin/özel gün istisnalarını ve şube açıklığını kesiştir.
4. Hizmet süresi adımlı aday aralıkları (`cursor = slotEnd`) oluştur (S05 motoru davranışıyla uyumlu).
5. Çakışan aktif randevulara göre filtrele; müsait adayları yerel saat ile sun.
6. Bu liste **anlık fotoğraftır**, rezervasyon garantisi değildir; nihai doğrulama yazma transaction'ında tekrar yapılır.

## Kesin rezervasyon (transaction)
```text
@Transactional BookingService.bookAppointment(command):
  1. Authenticate actor; derive patientId from session / AccessPolicy
  2. Validate UUIDs, branch/service/staff ownership and active flags
  3. Ordered row locking (Deadlock prevention):
       a. Acquire PESSIMISTIC_WRITE lock on patient row (patientId)
       b. Acquire PESSIMISTIC_WRITE lock on stable staff row (staffId)
  4. Re-read authoritative branch service duration/fee and staff eligibility
  5. Resolve local date/time against branch ZoneId (validate DST gap & overlap) -> Instant start, Instant end
  6. Apply all SchedulingConstraint & business rules:
       - OpeningHoursConstraint (full interval containment)
       - StaffAvailabilityConstraint & Exceptions (full-day leave, partial leave, overtime)
       - StaffServiceEligibilityConstraint
       - Staff overlap query under staff lock [start, end)
       - Patient overlap query under patient lock [start, end) (FR-029)
  7. Create PENDING Appointment + immutable ServiceSnapshot (name, duration, fee, currency)
  8. Insert Appointment + appointment_status_history creation event in same transaction
  9. Commit; release locks
```

**Kritik:** Çakışma kontrolünü transaction dışındaki boş saat ekranından devralma. Kilit alınmadan yapılan "SELECT COUNT ... sonra INSERT" yarıştır ve double-booking doğurur. Aynı personele ait her BOOK/RESCHEDULE (gelecekte) ve uygunluğu etkileyen güncelleme aynı kilit protokolünü kullanır. Personel çalışma saatini değiştirmeye karar veren servis mevcut randevuları kontrol eder veya aktif onaylı randevulara dokunmadığını garanti eder.

## Lock grafiği (önerilen sade akış)
```mermaid
sequenceDiagram
    participant UI as Hasta / Web
    participant B as BookingService
    participant DB as PostgreSQL
    UI->>B: bookAppointment(cmd)
    B->>DB: BEGIN; lock patient FOR UPDATE
    DB-->>B: patient lock granted
    B->>DB: lock staff FOR UPDATE
    DB-->>B: staff lock granted
    B->>DB: hekim ve hasta aktif randevularını oku
    B->>B: uygunluk + overlap + snapshot
    alt Uygun
      B->>DB: appointment + history INSERT
      B->>DB: COMMIT
      B-->>UI: Appointment PENDING
    else Çakışma
      B->>DB: ROLLBACK
      B-->>UI: 409 AppointmentConflictException / PatientOverlapException
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
