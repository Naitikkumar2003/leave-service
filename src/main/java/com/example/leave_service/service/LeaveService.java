package com.example.leave_service.service;

import com.example.leave_service.entity.*;
import com.example.leave_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepo;
    private final LeaveBalanceRepository leaveBalanceRepo;
    private final RestTemplate restTemplate;

    private final String USER_SERVICE_URL = "http://localhost:8081";
    private final String PROJECT_SERVICE_URL = "http://localhost:8082";

    // 🟢 APPLY LEAVE
    public LeaveRequest applyLeave(Long userId, LeaveRequest request) {

        // Leave type required
        if (request.getLeaveType() == null) {
            throw new RuntimeException("Leave type is required");
        }

        // Overlapping leave check
        List<LeaveRequest> overlappingLeaves =
                leaveRequestRepo.findByUserIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        userId,
                        List.of(LeaveStatus.PENDING, LeaveStatus.HR_APPROVED),
                        request.getEndDate(),
                        request.getStartDate()
                );

        if (!overlappingLeaves.isEmpty()) {
            throw new RuntimeException("You already have leave for overlapping dates");
        }
        if (request.getLeaveType() == LeaveType.WFH) {
            Boolean onBench = restTemplate.getForObject(
                    PROJECT_SERVICE_URL + "/projects/" + userId + "/bench-status",
                    Boolean.class);

            if (Boolean.TRUE.equals(onBench)) {
                throw new RuntimeException("WFH not allowed while on bench");
            }
        }


        // Comment length check
        if (request.getComments() != null && request.getComments().length() > 200) {
            throw new RuntimeException("Comments cannot exceed 200 characters");
        }

        // Half-day rule
        if (Boolean.TRUE.equals(request.getHalfDay()) &&
                !request.getStartDate().equals(request.getEndDate())) {
            throw new RuntimeException("Half-day leave must be for a single day");
        }

        LeaveBalance balance = leaveBalanceRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        double days = calculateWorkingDays(
                request.getStartDate(),
                request.getEndDate(),
                Boolean.TRUE.equals(request.getHalfDay())
        );

        // Allow up to -3 negative
        if ((balance.getRemainingLeaves() - days) < -3) {
            throw new RuntimeException("Leave limit exceeded. Max -3 allowed");
        }

        request.setUserId(userId);
        request.setStatus(LeaveStatus.PENDING);
        request.setAppliedAt(LocalDateTime.now());
        request.setHrId(null);
        request.setRejectionReason(null);
        request.setApprovedManagerIds(new HashSet<>());

        return leaveRequestRepo.save(request);
    }

    // 🟡 MANAGER APPROVAL
    public LeaveRequest managerApprove(Long leaveId, Long managerId) {
        LeaveRequest leave = leaveRequestRepo.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        if (!isValidManager(leave.getUserId(), managerId)) {
            throw new RuntimeException("Manager not authorized for this employee");
        }


        if (leave.getUserId().equals(managerId)) {
            throw new RuntimeException("Self approval not allowed");
        }

        if (leave.getStatus() == LeaveStatus.HR_APPROVED || leave.getStatus() == LeaveStatus.REJECTED) {
            throw new RuntimeException("Leave already finalized by HR");
        }

        if (leave.getApprovedManagerIds().contains(managerId)) {
            throw new RuntimeException("Manager already approved");
        }

        leave.getApprovedManagerIds().add(managerId);
        return leaveRequestRepo.save(leave);
    }

    // 🔵 HR APPROVAL (FINAL)

    public LeaveRequest hrApprove(Long leaveId, Long hrId) {

        LeaveRequest leave = leaveRequestRepo.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (leave.getStatus() == LeaveStatus.REJECTED)
            throw new RuntimeException("Rejected leave cannot be approved");

        LeaveBalance balance = leaveBalanceRepo.findByUserId(leave.getUserId())
                .orElseThrow(() -> new RuntimeException("Balance not found"));

        double days = calculateWorkingDays(
                leave.getStartDate(),
                leave.getEndDate(),
                Boolean.TRUE.equals(leave.getHalfDay()));

        deductLeaveBalance(balance, days);

        leave.setStatus(LeaveStatus.HR_APPROVED);
        leave.setHrId(hrId);

        leaveBalanceRepo.save(balance);
        return leaveRequestRepo.save(leave);
    }


    // 🔴 REJECT
    public LeaveRequest rejectLeave(Long leaveId, String reason, Long approverId) {
        LeaveRequest leave = leaveRequestRepo.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));

        if (leave.getUserId().equals(approverId)) {
            throw new RuntimeException("Self rejection not allowed");
        }

        if (reason == null || reason.isBlank()) {
            throw new RuntimeException("Rejection reason is required");
        }

        if (leave.getStatus() == LeaveStatus.REJECTED) {
            throw new RuntimeException("Leave already rejected");
        }

        if (leave.getStatus() == LeaveStatus.HR_APPROVED && !leave.getHrId().equals(approverId)) {
            throw new RuntimeException("Only HR can modify approved leave");
        }

        leave.setStatus(LeaveStatus.REJECTED);
        leave.setRejectionReason(reason);

        return leaveRequestRepo.save(leave);
    }
    private boolean isValidManager(Long employeeId, Long managerId) {
        Boolean valid = restTemplate.getForObject(
                PROJECT_SERVICE_URL + "/projects/manager-check?empId=" + employeeId + "&managerId=" + managerId,
                Boolean.class);
        return Boolean.TRUE.equals(valid);
    }


    // 🟣 BALANCE
    public LeaveBalance getBalance(Long userId) {
        return leaveBalanceRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));
    }

    // 📜 HISTORY
    public List<LeaveRequest> getLeaveHistory(Long userId) {
        return leaveRequestRepo.findByUserId(userId);
    }

    // 🧮 WORKING DAYS CALCULATION (Excludes weekends)
    private double calculateWorkingDays(LocalDate start, LocalDate end, boolean halfDay) {
        double days = 0;
        LocalDate date = start;

        while (!date.isAfter(end)) {
            if (date.getDayOfWeek().getValue() < 6) { // Mon–Fri
                days++;
            }
            date = date.plusDays(1);
        }

        return halfDay ? 0.5 : days;
    }
    private void deductLeaveBalance(LeaveBalance balance, double days) {

        double remaining = days;

        if (balance.getPrimaryLeave() >= remaining) {
            balance.setPrimaryLeave(balance.getPrimaryLeave() - remaining);
            remaining = 0;
        } else {
            remaining -= balance.getPrimaryLeave();
            balance.setPrimaryLeave(0);
        }

        if (remaining > 0 && balance.getSecondaryLeave() >= remaining) {
            balance.setSecondaryLeave(balance.getSecondaryLeave() - remaining);
            remaining = 0;
        } else if (remaining > 0) {
            remaining -= balance.getSecondaryLeave();
            balance.setSecondaryLeave(0);
        }

        if (remaining > 0 && balance.getCarryForwardLeave() >= remaining) {
            balance.setCarryForwardLeave(balance.getCarryForwardLeave() - remaining);
            remaining = 0;
        } else if (remaining > 0) {
            remaining -= balance.getCarryForwardLeave();
            balance.setCarryForwardLeave(0);
        }

        // Negative allowed up to -3
        balance.setRemainingLeaves(balance.getRemainingLeaves() - days);

        if (balance.getRemainingLeaves() < -3) {
            throw new RuntimeException("Leave limit exceeded (-3 max)");
        }
    }
    public LeaveRequest updateLeave(Long leaveId, LeaveRequest updated, Long userId) {

        LeaveRequest leave = leaveRequestRepo.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (!leave.getUserId().equals(userId))
            throw new RuntimeException("Not your leave");

        if (leave.getStatus() == LeaveStatus.HR_APPROVED)
            throw new RuntimeException("Cannot edit after HR approval");

        leave.setStartDate(updated.getStartDate());
        leave.setEndDate(updated.getEndDate());
        leave.setComments(updated.getComments());

        return leaveRequestRepo.save(leave);
    }
    @Scheduled(cron = "0 0 0 1 * ?") // 1st of every month
    public void creditMonthlyLeaves() {

        List<LeaveBalance> balances = leaveBalanceRepo.findAll();

        for (LeaveBalance balance : balances) {

            if ("INTERN".equals(balance.getUserType())) continue;

            int month = LocalDate.now().getMonthValue();

            if (month <= 7) {
                balance.setPrimaryLeave(balance.getPrimaryLeave() + 1.5);
            } else {
                balance.setSecondaryLeave(balance.getSecondaryLeave() + 1.5);
            }

            balance.setRemainingLeaves(balance.getRemainingLeaves() + 1.5);
            leaveBalanceRepo.save(balance);
        }
    }
    public LeaveBalance awardLeave(Long userId, double days, String adminPassword) {

        if (!"ADMIN123".equals(adminPassword))
            throw new RuntimeException("Invalid admin password");

        LeaveBalance balance = leaveBalanceRepo.findByUserId(userId)
                .orElseThrow();

        balance.setSecondaryLeave(balance.getSecondaryLeave() + days);
        balance.setRemainingLeaves(balance.getRemainingLeaves() + days);

        return leaveBalanceRepo.save(balance);
    }
    public Page<LeaveBalance> getNegativeBalanceReport(Pageable pageable) {
        return leaveBalanceRepo.findByRemainingLeavesLessThan(0, pageable);
    }





}
