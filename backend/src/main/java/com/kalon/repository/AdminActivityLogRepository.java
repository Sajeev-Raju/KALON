package com.kalon.repository;

import com.kalon.entity.AdminActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AdminActivityLogRepository extends JpaRepository<AdminActivityLog, Long> {

    Page<AdminActivityLog> findByAdminId(Long adminId, Pageable pageable);

    Page<AdminActivityLog> findByAction(AdminActivityLog.Action action, Pageable pageable);

    Page<AdminActivityLog> findByResourceType(AdminActivityLog.ResourceType resourceType, Pageable pageable);

    @Query("SELECT a FROM AdminActivityLog a WHERE " +
           "(:adminId IS NULL OR a.admin.id = :adminId) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:resourceType IS NULL OR a.resourceType = :resourceType) AND " +
           "(:dateFrom IS NULL OR a.createdAt >= :dateFrom) AND " +
           "(:dateTo IS NULL OR a.createdAt <= :dateTo)")
    Page<AdminActivityLog> findWithFilters(
            @Param("adminId") Long adminId,
            @Param("action") AdminActivityLog.Action action,
            @Param("resourceType") AdminActivityLog.ResourceType resourceType,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);

    @Query("SELECT a FROM AdminActivityLog a WHERE " +
           "(LOWER(a.resourceName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(a.details) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<AdminActivityLog> searchLogs(@Param("search") String search, Pageable pageable);

    @Query("SELECT DISTINCT a.admin FROM AdminActivityLog a ORDER BY a.admin.firstName")
    java.util.List<com.kalon.entity.User> findDistinctAdmins();
}
