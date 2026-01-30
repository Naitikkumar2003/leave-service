package com.example.leave_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leave_balance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    // Earned Jan–Jul
    private double primaryLeave;

    // Earned Aug–Dec + Admin awards
    private double secondaryLeave;

    // Carried from previous year
    private double carryForwardLeave;

    // Total remaining (can go to -3)
    private double remainingLeaves;

    private String userType; // EMPLOYEE / INTERN
}
