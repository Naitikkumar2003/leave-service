package com.example.leave_service.repository;

import com.example.leave_service.entity.LeaveRequest;
import com.example.leave_service.entity.LeaveStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByUserId(Long userId);

    // Overlapping leave check
    List<LeaveRequest> findByUserIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long userId,
            List<LeaveStatus> statuses,
            LocalDate endDate,
            LocalDate startDate
    );
}
