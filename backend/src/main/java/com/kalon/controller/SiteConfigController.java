package com.kalon.controller;

import com.kalon.dto.ApiResponse;
import com.kalon.dto.SiteConfigDTO;
import com.kalon.service.SiteConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/site-config")
@RequiredArgsConstructor
public class SiteConfigController {

    private final SiteConfigService siteConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SiteConfigDTO>>> getActiveConfigs() {
        return ResponseEntity.ok(ApiResponse.success(siteConfigService.getActiveConfigs()));
    }

    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<SiteConfigDTO>> getConfigByKey(@PathVariable String key) {
        SiteConfigDTO config = siteConfigService.getConfigByKey(key);
        if (config == null) {
            return ResponseEntity.ok(ApiResponse.error("Config not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(config));
    }
}



