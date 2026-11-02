# Şema sözleşmesi ve Flyway migrasyon planı

## Tablo/kolon sözleşmeleri
| Tablo | Önemli kolonlar | Bütünlük |
|---|---|---|
| `organization` | id UUID, name, active | name boş olamaz |
| `branch` | id, organization_id, name, time_zone, address, active | FK organization |
| `branch_opening_hours` | id, branch_id, weekday, start_time, end_time | start < end; gerekirse gün içinde birden fazla aralık |
| `department` | id, branch_id, name, active | unique(branch_id,name) |
| `service_definition` | id, organization_id, name, description, active | org-scoped unique name |
| `branch_service` | id, branch_id, service_definition_id, duration_minutes, fee_amount DECIMAL(12,2), currency CHAR(3), active | duration>0; fee>=0 |
| `user_account` | id, email, password_hash, enabled, created_at | unique email (normalize), hash not plaintext |
| `membership` | id, user_id, organization_id, branch_id nullable, role | permitted scope; 1 membership can span org |
| `patient_profile` | id, user_id, full_name, phone | user FK unique |
| `staff_profile` | id, user_id, full_name, phone, staff_type, specialty, active | type required |
| `staff_assignment` | id, staff_id, branch_id, department_id, active | branch/department scope |
| `staff_service_eligibility` | id, assignment_id, branch_service_id | unique(assignment_id,branch_service_id) |
| `weekly_availability` | id, assignment_id, weekday, start_time, end_time | start<end |
| `availability_exception` | id, assignment_id, local_date, kind, start_time nullable, end_time nullable | leave full day or bounded interval |
| `appointment` | id, patient_id, staff_id, branch_id, branch_service_id, start_at_utc TIMESTAMPTZ, end_at_utc TIMESTAMPTZ, status, service_name_snapshot, duration_minutes_snapshot, fee_amount_snapshot, currency_snapshot, version, created_at | end>start; status enum check; FK |
| `appointment_status_history` | id, appointment_id, from_status, to_status, actor_user_id, reason, occurred_at | append-only |
| `booking_request` | id, patient_id, request_key, body_hash, appointment_id | optional SHOULD; unique(patient_id,request_key) |

## Önerilen migration sırası
- `V1__create_organization_and_identity.sql` : organization, branch, department, account, membership, profiles.
- `V2__create_catalog_staff_and_availability.sql` : branch services, assignment, eligibility, schedules/exceptions.
- `V3__create_appointments.sql` : appointment/history, index ve check.
- `V4__add_idempotency.sql` : SHOULD; yalnız uygulanıyorsa.
- `V5__add_postgres_exclusion_guard.sql` : opsiyonel ve sadece testle ispatlandıysa.

## Migration kuralları
- Mevcut uygulanmış migration düzenlenmez; yeni migration eklenir.
- `spring.jpa.hibernate.ddl-auto=validate`; şema Flyway tarafından yönetilir.
- Varsayılan prod profile demo kullanıcı eklenmez; sahte demo seed yalnız dev/test profile.
- `TIMESTAMPTZ` mutlak an saklar; şubenin `time_zone` alanı yerel gösterim için kullanılır. Tarih okuma yazma JDBC/ORM tarafında UTC olarak yapılandırılır.
- `@Version` ile status update yarışı kontrol edilir; commit sırasında unique/FK ihlalleri anlamlı uygulama hatasına çevrilir.

## Önemli veri bütünlüğü uyarısı
FK'ler aynı şube eşleşmesini otomatik garanti etmez; `branch_service` ve `staff_assignment` kimliklerinin şubeye ait olmasını servis seviyesinde yeniden doğrula, gerekiyorsa kompozit unique + foreign key ekle. Böylece sadece UI doğrulamasına güvenilmez.
