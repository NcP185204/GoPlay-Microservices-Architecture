package com.caophuc.court.repository;

import com.caophuc.court.model.Court;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtRepository extends JpaRepository<Court, Integer>, JpaSpecificationExecutor<Court> {
    // JpaSpecificationExecutor giúp tìm kiếm nâng cao
}