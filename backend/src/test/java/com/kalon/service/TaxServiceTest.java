package com.kalon.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TaxServiceTest {

    private TaxService taxService;

    @BeforeEach
    void setUp() {
        taxService = new TaxService();
        ReflectionTestUtils.setField(taxService, "gstRate", new BigDecimal("18"));
        ReflectionTestUtils.setField(taxService, "taxEnabled", true);
    }

    @Test
    void calculateInclusiveTax_standardAmount() {
        BigDecimal subtotal = new BigDecimal("1180");
        BigDecimal tax = taxService.calculateInclusiveTax(subtotal);

        // 1180 * 18 / 118 = 180
        assertThat(tax).isEqualByComparingTo(new BigDecimal("180.00"));
    }

    @Test
    void calculateInclusiveTax_zeroAmount() {
        BigDecimal tax = taxService.calculateInclusiveTax(BigDecimal.ZERO);
        assertThat(tax).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculateInclusiveTax_whenDisabled() {
        ReflectionTestUtils.setField(taxService, "taxEnabled", false);
        BigDecimal tax = taxService.calculateInclusiveTax(new BigDecimal("1000"));
        assertThat(tax).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculateExclusiveTax_standardAmount() {
        BigDecimal subtotal = new BigDecimal("1000");
        BigDecimal tax = taxService.calculateExclusiveTax(subtotal);

        // 1000 * 18 / 100 = 180
        assertThat(tax).isEqualByComparingTo(new BigDecimal("180.00"));
    }
}
