package com.lottery.system.repository;

import com.lottery.system.entity.UserActivityCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserActivityCounterRepository extends JpaRepository<UserActivityCounter, Long> {
    Optional<UserActivityCounter> findByUserIdAndActivityId(Long userId, Long activityId);
}
