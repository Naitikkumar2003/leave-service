package com.example.leave_service.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Employee who applied
    @Column(nullable = false)
    private Long userId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(nullable = false)
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(nullable = false)
    private LocalDate endDate;

    @Column(length = 200)
    private String comments;
// comment added
    @Column(length = 255)
    private String reason;

    // LEAVE / WFH / COMP_OFF
    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false)
    private LeaveType leaveType;

    // PENDING / HR_APPROVED / REJECTED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status;

    // Managers who approved
    @ElementCollection
    @CollectionTable(name = "leave_manager_approvals",
            joinColumns = @JoinColumn(name = "leave_request_id"))
    @Column(name = "manager_id")
    private Set<Long> approvedManagerIds = new HashSet<>();

    // HR who finalized
    private Long hrId;

    @Column(length = 300)
    private String rejectionReason;

    private Boolean halfDay;          // true = half day leave
    private String attachmentPath;    // file reference
    private Boolean autoApproved;     // auto-approved if manager didn't act

    private LocalDateTime appliedAt;
}
