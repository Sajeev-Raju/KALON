package com.kalon.repository;

import com.kalon.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"items", "items.product", "items.product.images", "items.variant"})
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
    Optional<Cart> findByUserIdWithItems(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"items", "items.product", "items.product.images", "items.variant"})
    @Query("SELECT c FROM Cart c WHERE c.id = :id")
    Optional<Cart> findByIdWithItems(@Param("id") Long id);

    @Query("SELECT c FROM Cart c JOIN FETCH c.items ci JOIN FETCH c.user u " +
           "WHERE c.updatedAt BETWEEN :from AND :to " +
           "AND SIZE(c.items) > 0 AND u.isActive = true")
    List<Cart> findAbandonedCarts(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
