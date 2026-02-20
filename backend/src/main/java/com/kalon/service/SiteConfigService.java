package com.kalon.service;

import com.kalon.dto.SiteConfigDTO;
import com.kalon.entity.SiteConfig;
import com.kalon.repository.SiteConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SiteConfigService {

    private final SiteConfigRepository siteConfigRepository;

    public List<SiteConfigDTO> getAllConfigs() {
        return siteConfigRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<SiteConfigDTO> getActiveConfigs() {
        return siteConfigRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public SiteConfigDTO getConfigByKey(String key) {
        SiteConfig config = siteConfigRepository.findByConfigKey(key)
                .orElse(null);
        return config != null ? toDTO(config) : null;
    }

    @Transactional
    public SiteConfigDTO createOrUpdateConfig(SiteConfigDTO dto) {
        SiteConfig config = siteConfigRepository.findByConfigKey(dto.getConfigKey())
                .orElse(SiteConfig.builder()
                        .configKey(dto.getConfigKey())
                        .configType(SiteConfig.ConfigType.TEXT)
                        .isActive(true)
                        .displayOrder(0)
                        .build());

        config.setConfigValue(dto.getConfigValue());
        if (dto.getConfigType() != null) {
            config.setConfigType(SiteConfig.ConfigType.valueOf(dto.getConfigType()));
        }
        if (dto.getDescription() != null) {
            config.setDescription(dto.getDescription());
        }
        if (dto.getDisplayOrder() != null) {
            config.setDisplayOrder(dto.getDisplayOrder());
        }
        config.setActive(dto.isActive());

        config = siteConfigRepository.save(config);
        return toDTO(config);
    }

    @Transactional
    public void deleteConfig(Long id) {
        siteConfigRepository.deleteById(id);
    }

    // ─── Stock Management Helpers ───

    public int getLowStockThreshold() {
        SiteConfigDTO config = getConfigByKey("low_stock_threshold");
        if (config != null && config.getConfigValue() != null) {
            try {
                return Integer.parseInt(config.getConfigValue());
            } catch (NumberFormatException e) {
                // fall through to default
            }
        }
        return 10;
    }

    public String getLowStockAlertEmail() {
        SiteConfigDTO config = getConfigByKey("low_stock_alert_email");
        if (config != null && config.getConfigValue() != null && !config.getConfigValue().isBlank()) {
            return config.getConfigValue();
        }
        return null;
    }

    private SiteConfigDTO toDTO(SiteConfig config) {
        return SiteConfigDTO.builder()
                .id(config.getId())
                .configKey(config.getConfigKey())
                .configValue(config.getConfigValue())
                .configType(config.getConfigType().name())
                .description(config.getDescription())
                .isActive(config.isActive())
                .displayOrder(config.getDisplayOrder())
                .build();
    }
}

