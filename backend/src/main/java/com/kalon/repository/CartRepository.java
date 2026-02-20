package com.kalon.repository;

import com.kalon.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
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
}
