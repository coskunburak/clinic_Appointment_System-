package com.example.clinic.web.form;

import com.example.clinic.identity.domain.StaffType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Form object capturing new staff registration by administrators.
 */
public class StaffRegistrationForm {

    @NotNull(message = "Lütfen bir şube seçiniz.")
    private UUID branchId;

    @NotBlank(message = "E-posta adresi boş bırakılamaz.")
    @Email(message = "Lütfen geçerli bir e-posta adresi giriniz.")
    private String email;

    @NotBlank(message = "Şifre boş bırakılamaz.")
    @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır.")
    private String password;

    @NotBlank(message = "Ad soyad boş bırakılamaz.")
    @Size(max = 100, message = "Ad soyad en fazla 100 karakter olabilir.")
    private String fullName;

    @Size(max = 20, message = "Telefon numarası en fazla 20 karakter olabilir.")
    private String phone;

    @NotNull(message = "Personel türü zorunludur.")
    private StaffType staffType = StaffType.DOCTOR;

    @Size(max = 100, message = "Uzmanlık alanı en fazla 100 karakter olabilir.")
    private String specialty;

    public StaffRegistrationForm() {
    }

    public StaffRegistrationForm(UUID branchId, String email, String password, String fullName, String phone, StaffType staffType, String specialty) {
        this.branchId = branchId;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.phone = phone;
        this.staffType = staffType;
        this.specialty = specialty;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public StaffType getStaffType() {
        return staffType;
    }

    public void setStaffType(StaffType staffType) {
        this.staffType = staffType;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }
}
