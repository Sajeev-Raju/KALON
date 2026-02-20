package com.kalon.repository;

import com.kalon.entity.LowStockAlertSent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface LowStockAlertSentRepository extends JpaRepository<LowStockAlertSent, Long> {

    @Query("SELECT a.variantId FROM LowStockAlertSent a")
    Set<Long> findAllAlertedVariantIds();

    @Modifying
    @Query("DELETE FROM LowStockAlertSent a WHERE a.variantId IN :variantIds")
    void deleteByVariantIdIn(@Param("variantIds") List<Long> variantIds);

    boolean existsByVariantId(Long variantId);
}
