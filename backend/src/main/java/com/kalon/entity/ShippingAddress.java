package com.kalon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingAddress {

    @Column(name = "shipping_full_name")
    private String fullName;

    @Column(name = "shipping_phone")
    private String phoneNumber;

    @Column(name = "shipping_address_line_1")
    private String addressLine1;

    @Column(name = "shipping_address_line_2")
    private String addressLine2;

    @Column(name = "shipping_city")
    private String city;

    @Column(name = "shipping_state")
    private String state;

    @Column(name = "shipping_postal_code")
    private String postalCode;

    @Column(name = "shipping_country")
    private String country;
}
