package com.example.clinic.organization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "branch_opening_hours")
public class BranchOpeningHours {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "weekday", nullable = false)
    private short weekday;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    protected BranchOpeningHours() {
    }

    public BranchOpeningHours(Branch branch, short weekday, LocalTime startTime, LocalTime endTime) {
        this(UUID.randomUUID(), branch, weekday, startTime, endTime);
    }

    public BranchOpeningHours(UUID id, Branch branch, short weekday, LocalTime startTime, LocalTime endTime) {
        this.id = Objects.requireNonNull(id, "BranchOpeningHours id must not be null");
        this.branch = Objects.requireNonNull(branch, "Branch must not be null");
        this.weekday = weekday;
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
    }

    public UUID getId() {
        return id;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = Objects.requireNonNull(branch, "branch must not be null");
    }

    public short getWeekday() {
        return weekday;
    }

    public void setWeekday(short weekday) {
        this.weekday = weekday;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BranchOpeningHours that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "BranchOpeningHours{" +
                "id=" + id +
                ", weekday=" + weekday +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
