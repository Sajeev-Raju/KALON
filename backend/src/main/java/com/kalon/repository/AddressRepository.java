package com.kalon.repository;

import com.kalon.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserIdOrderByIsDefaultDescIdAsc(Long userId);
    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);
    long countByUserId(Long userId);
}
