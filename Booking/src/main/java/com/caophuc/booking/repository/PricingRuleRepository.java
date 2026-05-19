package com.caophuc.booking.repository;

import com.caophuc.booking.model.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Integer> {

    /**
     * Tìm tất cả các quy tắc giá của một sân.
     */
    List<PricingRule> findByCourtId(Integer courtId);

    /**
     * Tìm quy tắc giá phù hợp cho một sân, vào một ngày và giờ cụ thể.
     */
    @Query("SELECT pr FROM PricingRule pr WHERE pr.courtId = :courtId " +
           "AND pr.dayOfWeek = :dayOfWeek " +
           "AND pr.startTime <= :time AND pr.endTime > :time")
    List<PricingRule> findApplicableRule(
            @Param("courtId") Integer courtId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("time") LocalTime time

    );
}
