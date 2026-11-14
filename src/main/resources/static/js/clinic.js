(function () {
    "use strict";

    function addMinutesToTimeString(timeStr, minutesToAdd) {
        if (!timeStr || !minutesToAdd || isNaN(minutesToAdd)) {
            return timeStr || "-";
        }
        const parts = timeStr.split(":");
        if (parts.length < 2) {
            return timeStr;
        }
        const hours = parseInt(parts[0], 10);
        const mins = parseInt(parts[1], 10);
        if (isNaN(hours) || isNaN(mins)) {
            return timeStr;
        }
        const total = hours * 60 + mins + parseInt(minutesToAdd, 10);
        const endHours = Math.floor(total / 60) % 24;
        const endMins = total % 60;
        return String(endHours).padStart(2, "0") + ":" + String(endMins).padStart(2, "0");
    }

    function initPatientBookingWizard() {
        const form = document.getElementById("booking-form");
        if (!form) {
            return;
        }

        const branchSelect = document.getElementById("branchId");
        const serviceSelect = document.getElementById("branchServiceId");
        const staffSelect = document.getElementById("staffId");
        const dateInput = document.getElementById("appointmentDate");
        const slotsContainer = document.getElementById("slots-container");
        const slotsLoading = document.getElementById("slots-loading");

        const bookBaseUrl = form.getAttribute("data-book-url") || "/patient/book";
        const slotsBaseUrl = form.getAttribute("data-slots-url") || "/patient/slots";
        const initialSlot = form.getAttribute("data-selected-slot") || "";

        function selectedOptionAttr(selectEl, attrName, fallback) {
            if (!selectEl || selectEl.selectedIndex < 0) {
                return fallback;
            }
            const opt = selectEl.options[selectEl.selectedIndex];
            if (!opt || !opt.value) {
                return fallback;
            }
            return opt.getAttribute(attrName) || opt.text || fallback;
        }

        function updateBookingSummary() {
            const branchSummary = document.getElementById("summary-branch");
            const serviceSummary = document.getElementById("summary-service");
            const staffSummary = document.getElementById("summary-staff");
            const dateSummary = document.getElementById("summary-date");
            const timeSummary = document.getElementById("summary-time");
            const durationSummary = document.getElementById("summary-duration");
            const feeSummary = document.getElementById("summary-fee");

            if (branchSummary) {
                branchSummary.textContent = selectedOptionAttr(branchSelect, "data-branch-name", "Seçilmedi");
            }
            if (serviceSummary) {
                serviceSummary.textContent = selectedOptionAttr(serviceSelect, "data-service-name", "Seçilmedi");
            }
            if (staffSummary) {
                staffSummary.textContent = selectedOptionAttr(staffSelect, "data-staff-name", "Seçilmedi");
            }
            if (dateSummary) {
                dateSummary.textContent = (dateInput && dateInput.value) ? dateInput.value : "-";
            }

            const durationMinutes = serviceSelect && serviceSelect.selectedIndex >= 0
                ? serviceSelect.options[serviceSelect.selectedIndex].getAttribute("data-duration")
                : null;
            const feeDisplay = serviceSelect && serviceSelect.selectedIndex >= 0
                ? serviceSelect.options[serviceSelect.selectedIndex].getAttribute("data-fee")
                : null;

            if (durationSummary) {
                durationSummary.textContent = durationMinutes ? durationMinutes + " dk" : "-";
            }
            if (feeSummary) {
                feeSummary.textContent = feeDisplay || "-";
            }

            const checkedSlot = form.querySelector("input[name='startTime']:checked");
            if (timeSummary) {
                if (checkedSlot && checkedSlot.value) {
                    const endStr = addMinutesToTimeString(checkedSlot.value, durationMinutes);
                    timeSummary.textContent = checkedSlot.value + " - " + endStr;
                } else {
                    timeSummary.textContent = "-";
                }
            }
        }

        function renderSlotOptions(slots, preservedSlot) {
            if (!slotsContainer) {
                return;
            }
            slotsContainer.innerHTML = "";

            if (!Array.isArray(slots) || slots.length === 0) {
                const empty = document.createElement("div");
                empty.id = "no-slots-msg";
                empty.className = "text-muted small text-center py-2";
                empty.textContent = "Seçilen tarih için uygun randevu saati bulunamadı (kapalı gün, izin veya dolu slot). Lütfen başka bir tarih seçiniz.";
                slotsContainer.appendChild(empty);
                updateBookingSummary();
                return;
            }

            const list = document.createElement("div");
            list.id = "slots-list";
            list.className = "d-flex flex-wrap gap-2";

            slots.forEach(function (slot) {
                const wrapper = document.createElement("div");

                const input = document.createElement("input");
                input.type = "radio";
                input.className = "btn-check slot-radio-input";
                input.name = "startTime";
                input.id = "slot-" + slot;
                input.value = slot;
                input.required = true;
                if (preservedSlot && preservedSlot === slot) {
                    input.checked = true;
                }
                input.addEventListener("change", updateBookingSummary);

                const label = document.createElement("label");
                label.className = "slot-badge-btn";
                label.setAttribute("for", "slot-" + slot);
                label.textContent = slot;

                wrapper.appendChild(input);
                wrapper.appendChild(label);
                list.appendChild(wrapper);
            });

            slotsContainer.appendChild(list);
            updateBookingSummary();
        }

        function fetchAvailableSlots(preservedSlot) {
            if (!staffSelect || !serviceSelect || !dateInput || !slotsContainer) {
                updateBookingSummary();
                return;
            }
            const staffId = staffSelect.value;
            const branchServiceId = serviceSelect.value;
            const date = dateInput.value;

            if (!staffId || !branchServiceId || !date) {
                updateBookingSummary();
                return;
            }

            if (slotsLoading) {
                slotsLoading.classList.remove("d-none");
            }

            const url = slotsBaseUrl
                + "?staffId=" + encodeURIComponent(staffId)
                + "&branchServiceId=" + encodeURIComponent(branchServiceId)
                + "&date=" + encodeURIComponent(date);

            fetch(url, {
                method: "GET",
                headers: { "Accept": "application/json", "X-Requested-With": "XMLHttpRequest" }
            })
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error("Slot response HTTP " + response.status);
                    }
                    return response.json();
                })
                .then(function (data) {
                    renderSlotOptions(data, preservedSlot || "");
                })
                .catch(function () {
                    slotsContainer.innerHTML = "";
                    const errDiv = document.createElement("div");
                    errDiv.className = "text-danger small text-center py-2";
                    errDiv.textContent = "Müsait saat dilimleri yüklenemedi. Lütfen seçimleri kontrol edip tekrar deneyiniz.";
                    slotsContainer.appendChild(errDiv);
                    updateBookingSummary();
                })
                .finally(function () {
                    if (slotsLoading) {
                        slotsLoading.classList.add("d-none");
                    }
                });
        }

        function navigateWithParams(params) {
            const url = new URL(bookBaseUrl, window.location.origin);
            Object.keys(params).forEach(function (key) {
                if (params[key]) {
                    url.searchParams.set(key, params[key]);
                }
            });
            window.location.href = url.toString();
        }

        if (branchSelect) {
            branchSelect.addEventListener("change", function () {
                if (!branchSelect.value) {
                    return;
                }
                navigateWithParams({
                    branchId: branchSelect.value,
                    date: dateInput ? dateInput.value : ""
                });
            });
        }

        if (serviceSelect) {
            serviceSelect.addEventListener("change", function () {
                if (!branchSelect || !branchSelect.value) {
                    return;
                }
                navigateWithParams({
                    branchId: branchSelect.value,
                    branchServiceId: serviceSelect.value,
                    date: dateInput ? dateInput.value : ""
                });
            });
        }

        if (staffSelect) {
            staffSelect.addEventListener("change", function () {
                fetchAvailableSlots("");
            });
        }

        if (dateInput) {
            dateInput.addEventListener("change", function () {
                fetchAvailableSlots("");
            });
        }

        form.querySelectorAll("input[name='startTime']").forEach(function (radio) {
            radio.addEventListener("change", updateBookingSummary);
        });

        updateBookingSummary();
        if (staffSelect && staffSelect.value && serviceSelect && serviceSelect.value && dateInput && dateInput.value) {
            const existingRadios = form.querySelectorAll("input[name='startTime']");
            if (existingRadios.length === 0) {
                fetchAvailableSlots(initialSlot);
            }
        }
    }

    function initPatientCancellationModal() {
        const cancelModal = document.getElementById("cancelAppointmentModal");
        if (!cancelModal) {
            return;
        }
        cancelModal.addEventListener("show.bs.modal", function (event) {
            const trigger = event.relatedTarget;
            if (!trigger) {
                return;
            }
            const appointmentId = trigger.getAttribute("data-appointment-id");
            const summaryText = trigger.getAttribute("data-appointment-summary");
            const actionTemplate = cancelModal.getAttribute("data-action-prefix") || "/patient/appointments/";
            const modalForm = document.getElementById("cancelAppointmentForm");
            const summaryEl = document.getElementById("cancelAppointmentSummary");
            const reasonEl = document.getElementById("cancelReason");

            if (modalForm && appointmentId) {
                modalForm.action = actionTemplate + appointmentId + "/cancel";
            }
            if (summaryEl) {
                summaryEl.textContent = summaryText || "";
            }
            if (reasonEl) {
                reasonEl.value = "";
            }
        });
    }

    function initStaffRejectModal() {
        const rejectModal = document.getElementById("rejectModal");
        if (!rejectModal) {
            return;
        }
        rejectModal.addEventListener("show.bs.modal", function (event) {
            const trigger = event.relatedTarget;
            if (!trigger) {
                return;
            }
            const appointmentId = trigger.getAttribute("data-appointment-id");
            const patientName = trigger.getAttribute("data-patient-name");
            const form = document.getElementById("rejectForm");
            const patientLabel = document.getElementById("rejectPatientName");
            const reasonInput = document.getElementById("rejectReason");

            if (form && appointmentId) {
                form.action = "/staff/appointments/" + appointmentId + "/reject";
            }
            if (patientLabel) {
                patientLabel.textContent = patientName || "";
            }
            if (reasonInput) {
                reasonInput.value = "";
            }
        });
    }

    function initAdminModalsAndFilters() {
        // Branch edit modal
        const editBranchModal = document.getElementById("editBranchModal");
        if (editBranchModal) {
            editBranchModal.addEventListener("show.bs.modal", function (event) {
                const btn = event.relatedTarget;
                if (!btn) {
                    return;
                }
                const branchId = btn.getAttribute("data-branch-id");
                const form = document.getElementById("editBranchForm");
                if (form && branchId) {
                    form.action = "/admin/branches/" + branchId + "/edit";
                }
                const nameInput = document.getElementById("editBranchName");
                const tzInput = document.getElementById("editBranchTimeZone");
                const addrInput = document.getElementById("editBranchAddress");
                if (nameInput) nameInput.value = btn.getAttribute("data-branch-name") || "";
                if (tzInput) tzInput.value = btn.getAttribute("data-branch-tz") || "Europe/Istanbul";
                if (addrInput) addrInput.value = btn.getAttribute("data-branch-address") || "";
            });
        }

        // Organization edit modal
        const editOrgModal = document.getElementById("editOrganizationModal");
        if (editOrgModal) {
            editOrgModal.addEventListener("show.bs.modal", function (event) {
                const btn = event.relatedTarget;
                if (!btn) {
                    return;
                }
                const orgId = btn.getAttribute("data-org-id");
                const form = document.getElementById("editOrganizationForm");
                if (form && orgId) {
                    form.action = "/admin/organizations/" + orgId + "/edit";
                }
                const nameInput = document.getElementById("editOrgName");
                if (nameInput) nameInput.value = btn.getAttribute("data-org-name") || "";
            });
        }

        // Department edit modal
        const editDeptModal = document.getElementById("editDepartmentModal");
        if (editDeptModal) {
            editDeptModal.addEventListener("show.bs.modal", function (event) {
                const btn = event.relatedTarget;
                if (!btn) {
                    return;
                }
                const deptId = btn.getAttribute("data-dept-id");
                const form = document.getElementById("editDepartmentForm");
                if (form && deptId) {
                    form.action = "/admin/departments/" + deptId + "/edit";
                }
                const nameInput = document.getElementById("editDeptName");
                if (nameInput) nameInput.value = btn.getAttribute("data-dept-name") || "";
            });
        }

        // Service definition edit modal
        const editDefModal = document.getElementById("editServiceDefModal");
        if (editDefModal) {
            editDefModal.addEventListener("show.bs.modal", function (event) {
                const btn = event.relatedTarget;
                if (!btn) {
                    return;
                }
                const defId = btn.getAttribute("data-def-id");
                const form = document.getElementById("editServiceDefForm");
                if (form && defId) {
                    form.action = "/admin/services/definitions/" + defId + "/edit";
                }
                const nameInput = document.getElementById("editDefName");
                const descInput = document.getElementById("editDefDescription");
                if (nameInput) nameInput.value = btn.getAttribute("data-def-name") || "";
                if (descInput) descInput.value = btn.getAttribute("data-def-description") || "";
            });
        }

        // Filter departments by selected branch in staff assignment form
        const assignBranch = document.getElementById("assignBranch");
        const assignDept = document.getElementById("assignDepartment");
        if (assignBranch && assignDept) {
            function filterDepartments() {
                const targetBranch = assignBranch.value;
                let firstVisibleValue = null;
                let currentVisible = false;
                Array.from(assignDept.options).forEach(function (opt) {
                    const deptBranch = opt.getAttribute("data-branch-id");
                    const visible = !targetBranch || deptBranch === targetBranch;
                    opt.hidden = !visible;
                    opt.disabled = !visible;
                    if (visible) {
                        if (firstVisibleValue === null) firstVisibleValue = opt.value;
                        if (opt.value === assignDept.value) currentVisible = true;
                    }
                });
                if (!currentVisible && firstVisibleValue !== null) {
                    assignDept.value = firstVisibleValue;
                }
            }
            assignBranch.addEventListener("change", filterDepartments);
            filterDepartments();
        }

        // Filter branch service checkboxes by selected assignment's branch in staff eligibility form
        const eligAssignment = document.getElementById("eligAssignment");
        const eligServices = document.querySelectorAll(".elig-service-item");
        if (eligAssignment && eligServices.length > 0) {
            function filterEligibilities() {
                const selectedOpt = eligAssignment.options[eligAssignment.selectedIndex];
                const branchId = selectedOpt ? selectedOpt.getAttribute("data-branch-id") : "";
                eligServices.forEach(function (item) {
                    const itemBranch = item.getAttribute("data-branch-id");
                    const visible = !branchId || itemBranch === branchId;
                    item.classList.toggle("d-none", !visible);
                    const cb = item.querySelector("input[type='checkbox']");
                    if (cb && !visible) {
                        cb.checked = false;
                        cb.disabled = true;
                    } else if (cb) {
                        cb.disabled = false;
                    }
                });
            }
            eligAssignment.addEventListener("change", filterEligibilities);
            filterEligibilities();
        }

        // Toggle time range requirement/hint on staff availability exception form
        const exceptionKind = document.getElementById("exceptionKind");
        const excStart = document.getElementById("excStart");
        const excEnd = document.getElementById("excEnd");
        const excTimeFields = document.getElementById("exceptionTimeFields");
        if (exceptionKind && excStart && excEnd) {
            function updateExceptionTimeState() {
                const isFullDay = exceptionKind.value === "FULL_DAY_LEAVE";
                excStart.disabled = isFullDay;
                excEnd.disabled = isFullDay;
                excStart.required = !isFullDay;
                excEnd.required = !isFullDay;
                if (excTimeFields) {
                    excTimeFields.classList.toggle("opacity-50", isFullDay);
                }
                if (isFullDay) {
                    excStart.value = "";
                    excEnd.value = "";
                }
            }
            exceptionKind.addEventListener("change", updateExceptionTimeState);
            updateExceptionTimeState();
        }

        // Toggle specialty field guidance by staffType
        const staffTypeSelect = document.getElementById("staffType");
        const specialtyInput = document.getElementById("staffSpecialty");
        const specialtyRequiredMarker = document.getElementById("specialtyRequiredMarker");
        if (staffTypeSelect && specialtyInput) {
            function updateSpecialtyHint() {
                const isDoctor = staffTypeSelect.value === "DOCTOR";
                specialtyInput.required = isDoctor;
                if (specialtyRequiredMarker) {
                    specialtyRequiredMarker.classList.toggle("d-none", !isDoctor);
                }
            }
            staffTypeSelect.addEventListener("change", updateSpecialtyHint);
            updateSpecialtyHint();
        }
    }

    document.addEventListener("DOMContentLoaded", function () {
        initPatientBookingWizard();
        initPatientCancellationModal();
        initStaffRejectModal();
        initAdminModalsAndFilters();
    });
})();
