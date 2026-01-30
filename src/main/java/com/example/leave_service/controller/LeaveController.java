package com.example.leave_service.controller;

import com.example.leave_service.entity.LeaveBalance;
import com.example.leave_service.entity.LeaveRequest;
import com.example.leave_service.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping("/apply/{userId}")
    public LeaveRequest applyLeave(@PathVariable Long userId, @RequestBody LeaveRequest request) {
        return leaveService.applyLeave(userId, request);
    }

    @PostMapping("/{id}/manager-approve/{managerId}")
    public LeaveRequest managerApprove(@PathVariable Long id, @PathVariable Long managerId) {
        return leaveService.managerApprove(id, managerId);
    }

    @PostMapping("/{id}/hr-approve/{hrId}")
    public LeaveRequest hrApprove(@PathVariable Long id, @PathVariable Long hrId) {
        return leaveService.hrApprove(id, hrId);
    }

    @PostMapping("/{id}/reject")
    public LeaveRequest reject(@PathVariable Long id,
                               @RequestParam String reason,
                               @RequestParam Long approverId) {
        return leaveService.rejectLeave(id, reason, approverId);
    }


    @GetMapping("/balance/{userId}")
    public LeaveBalance getBalance(@PathVariable Long userId) {
        return leaveService.getBalance(userId);
    }

    @GetMapping("/history/{userId}")
    public List<LeaveRequest> getLeaveHistory(@PathVariable Long userId) {
        return leaveService.getLeaveHistory(userId);
    }

}
