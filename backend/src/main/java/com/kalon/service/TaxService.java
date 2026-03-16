package com.kalon.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class TaxService {

    @Value("${tax.gst.rate:18}")
    private BigDecimal gstRate;

    @Value("${tax.enabled:true}")
    private boolean taxEnabled;

    /**
     * Calculate GST amount for a given subtotal.
     * GST is assumed to be inclusive in the product price for Indian e-commerce.
     * This method extracts the GST component from the price.
     *
     * If tax is configured as exclusive (added on top), use calculateExclusiveTax instead.
     */
    public BigDecimal calculateInclusiveTax(BigDecimal subtotal) {
        if (!taxEnabled || subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // GST inclusive formula: tax = price - (price * 100 / (100 + rate))
        BigDecimal divisor = new BigDecimal("100").add(gstRate);
        BigDecimal priceWithoutTax = subtotal.multiply(new BigDecimal("100"))
                .divide(divisor, 2, RoundingMode.HALF_UP);
        return subtotal.subtract(priceWithoutTax).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate GST to be added on top of the subtotal.
     */
    public BigDecimal calculateExclusiveTax(BigDecimal subtotal) {
        if (!taxEnabled || subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return subtotal.multiply(gstRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getGstRate() {
        return gstRate;
    }

    public boolean isTaxEnabled() {
        return taxEnabled;
    }
}
