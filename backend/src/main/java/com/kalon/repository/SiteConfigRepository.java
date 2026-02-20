package com.kalon.repository;

import com.kalon.entity.SiteConfig;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SiteConfigRepository extends JpaRepository<SiteConfig, Long> {
    Optional<SiteConfig> findByConfigKey(String configKey);
    List<SiteConfig> findByIsActiveTrueOrderByDisplayOrderAsc();
    List<SiteConfig> findByConfigTypeAndIsActiveTrue(SiteConfig.ConfigType configType, Sort sort);
}

