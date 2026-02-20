package com.kalon.repository;

import com.kalon.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);

    // ─── Analytics queries ───

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate AND u.createdAt < :endDate")
    Long countNewUsersInRange(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT CAST(u.created_at AS DATE) AS reg_date, COUNT(*) " +
           "FROM users u WHERE u.created_at >= :startDate AND u.created_at < :endDate " +
           "GROUP BY CAST(u.created_at AS DATE) ORDER BY 1", nativeQuery = true)
    List<Object[]> registrationsByDateRange(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT u.provider, COUNT(*) FROM users u GROUP BY u.provider", nativeQuery = true)
    List<Object[]> countByProvider();

    @Query(value = "SELECT COUNT(DISTINCT o.user_id) FROM orders o " +
           "JOIN users u ON o.user_id = u.id " +
           "WHERE u.created_at < :periodStart " +
           "AND o.created_at >= :periodStart AND o.created_at < :periodEnd " +
           "AND o.status != 'CANCELLED'", nativeQuery = true)
    Long countReturningCustomersInRange(@Param("periodStart") LocalDateTime periodStart,
                                         @Param("periodEnd") LocalDateTime periodEnd);

    @Query(value = "SELECT COUNT(DISTINCT o.user_id) FROM orders o " +
           "JOIN users u ON o.user_id = u.id " +
           "WHERE u.created_at >= :periodStart AND u.created_at < :periodEnd " +
           "AND o.created_at >= :periodStart AND o.created_at < :periodEnd " +
           "AND o.status != 'CANCELLED'", nativeQuery = true)
    Long countNewOrderingCustomersInRange(@Param("periodStart") LocalDateTime periodStart,
                                           @Param("periodEnd") LocalDateTime periodEnd);

    @Query(value = "SELECT u.id, COALESCE(SUM(o.total_amount), 0) AS total_spent " +
           "FROM users u LEFT JOIN orders o ON u.id = o.user_id AND o.status = 'DELIVERED' " +
           "WHERE u.role = 'USER' " +
           "GROUP BY u.id", nativeQuery = true)
    List<Object[]> customerLifetimeValues();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = com.kalon.entity.User.Role.USER AND u.isActive = true")
    Long countActiveUsers();
}
