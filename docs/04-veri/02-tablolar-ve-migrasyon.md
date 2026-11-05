# Şema sözleşmesi ve Flyway migrasyon planı

## Tablo/kolon sözleşmeleri
| Tablo | Önemli kolonlar | Bütünlük |
|---|---|---|
| `organization` | id UUID, name, active, created_at | name boş olamaz (`chk_organization_name_not_blank`) |
| `branch` | id, organization_id, name, time_zone, address, active, created_at | FK organization (`fk_branch_organization`), composite `uq_branch_id_org_id` |
| `branch_opening_hours` | id, branch_id, weekday, start_time, end_time | FK branch, `start_time < end_time`, weekday 1..7 |
| `department` | id, branch_id, name, active, created_at | FK branch, unique(branch_id, name) |
| `service_definition` | id, organization_id, name, description, active | org-scoped unique name (S03 / V2) |
| `branch_service` | id, branch_id, service_definition_id, duration_minutes, fee_amount DECIMAL(12,2), currency CHAR(3), active | duration>0; fee>=0 (S03 / V2) |
| `user_account` | id, email, password_hash, enabled, created_at | unique email, hash not plaintext, not blank |
| `membership` | id, user_id, organization_id, branch_id nullable, role, created_at | FK user, FK org, composite FK branch+org (`fk_membership_branch_org`) |
| `patient_profile` | id, user_id, full_name, phone, created_at | user FK unique (`uq_patient_profile_user_id`) |
| `staff_profile` | id, user_id, full_name, phone, staff_type, specialty, active, created_at | user FK unique (`uq_staff_profile_user_id`), type required |
| `staff_assignment` | id, staff_id, branch_id, department_id, active | branch/department scope (S05 / V2) |
| `staff_service_eligibility` | id, assignment_id, branch_service_id | unique(assignment_id, branch_service_id) (S05 / V2) |
| `weekly_availability` | id, assignment_id, weekday, start_time, end_time | start<end (S05 / V2) |
| `availability_exception` | id, assignment_id, local_date, kind, start_time nullable, end_time nullable | leave full day or bounded interval (S05 / V2) |
| `appointment` | id, patient_id, staff_id, branch_id, branch_service_id, start_at_utc TIMESTAMPTZ, end_at_utc TIMESTAMPTZ, status, service_name_snapshot, duration_minutes_snapshot, fee_amount_snapshot, currency_snapshot, version, created_at | end>start; status enum check; FK (S06 / V3) |
| `appointment_status_history` | id, appointment_id, from_status, to_status, actor_user_id, reason, occurred_at | append-only (S07 / V3) |
| `booking_request` | id, patient_id, request_key, body_hash, appointment_id | optional SHOULD; unique(patient_id,request_key) (S06 / V4) |

## Gerçekleşen ve Önerilen Migration Sırası
- `V1__create_organization_and_identity.sql` : **(S01'de uygulandı)** `organization`, `branch`, `branch_opening_hours`, `department`, `user_account`, `membership`, `patient_profile`, `staff_profile`.
- `V2__create_catalog_tables.sql` : **(S03'te uygulandı)** `service_definition`, `branch_service` (kompozit kiracı izolasyonu ve kontroller).
- `V3__create_staff_and_availability.sql` : *(S05)* `staff_assignment`, `staff_service_eligibility`, `weekly_availability`, `availability_exception`.
- `V4__create_appointments.sql` : *(S06/S07)* `appointment`, `appointment_status_history`, index ve check.
- `V5__add_idempotency.sql` : *(S06)* SHOULD; yalnız uygulanıyorsa.
- `V6__add_postgres_exclusion_guard.sql` : *(S06)* opsiyonel ve sadece testle ispatlandıysa.

## Migration kuralları
- Mevcut uygulanmış migration düzenlenmez; yeni migration eklenir.
- `spring.jpa.hibernate.ddl-auto=validate`; şema Flyway tarafından yönetilir.
- `spring.flyway.clean-disabled=true`; üretim ve test güvenliği için temizleme engellenir.
- Varsayılan prod profile demo kullanıcı eklenmez; sahte demo seed yalnız dev/test profile.
- `TIMESTAMPTZ` mutlak an saklar; şubenin `time_zone` alanı yerel gösterim için kullanılır. Tarih okuma yazma JDBC/ORM tarafında UTC olarak yapılandırılır.
- `@Version` ile status update yarışı kontrol edilir; commit sırasında unique/FK ihlalleri anlamlı uygulama hatasına çevrilir.

## Çok Kiracılı Veri Bütünlüğü Kuralı
Yalnızca bağımsız tekil yabancı anahtarlar (`branch_id` ve `organization_id`) kiracı izolasyonunu garanti etmez. V1 migrasyonunda `branch` tablosuna `uq_branch_id_org_id (id, organization_id)` tanımlanmış ve `membership` tablosuna `fk_membership_branch_org FOREIGN KEY (branch_id, organization_id) REFERENCES branch(id, organization_id)` kompozit kısıtı eklenmiştir.
S03'te bu kural katalog katmanına genişletilmiştir: `service_definition` tablosuna `uq_service_definition_id_org_id (id, organization_id)` kısıtı eklenmiş; `branch_service` tablosuna hem `fk_branch_service_branch_org FOREIGN KEY (branch_id, organization_id) REFERENCES branch(id, organization_id)` hem de `fk_branch_service_def_org FOREIGN KEY (service_definition_id, organization_id) REFERENCES service_definition(id, organization_id)` kompozit yabancı anahtarları bağlanmıştır. Böylece A Kurumunun şubesine B Kurumunun hizmetinin atanması veritabanı motoru seviyesinde fiziksel olarak engellenmiştir.
