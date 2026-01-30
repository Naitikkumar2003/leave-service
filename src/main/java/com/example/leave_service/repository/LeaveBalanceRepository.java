package com.example.leave_service.repository;

import com.example.leave_service.entity.LeaveBalance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    Optional<LeaveBalance> findByUserId(Long userId);

    Page<LeaveBalance> findByRemainingLeavesLessThan(int i, Pageable pageable);
}

