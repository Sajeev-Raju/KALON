package com.kalon.service;

import com.kalon.dto.AddressDTO;
import com.kalon.entity.Address;
import com.kalon.entity.User;
import com.kalon.exception.AddressAccessException;
import com.kalon.exception.AddressLimitException;
import com.kalon.exception.ResourceNotFoundException;
import com.kalon.repository.AddressRepository;
import com.kalon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

    private static final int MAX_ADDRESSES_PER_USER = 20;

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public List<AddressDTO> getAddressesByUserId(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescIdAsc(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public AddressDTO getAddressById(Long addressId, Long userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(userId)) {
            throw new AddressAccessException("Address does not belong to user");
        }

        return toDTO(address);
    }

    @Transactional
    public AddressDTO createAddress(Long userId, AddressDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        long currentCount = addressRepository.countByUserId(userId);
        if (currentCount >= MAX_ADDRESSES_PER_USER) {
            throw new AddressLimitException("Address limit reached. Maximum " + MAX_ADDRESSES_PER_USER + " addresses allowed.");
        }

        // Auto-set as default if this is the user's first address
        boolean shouldBeDefault = dto.isDefault() || currentCount == 0;

        // If this is set as default, unset other default addresses
        if (shouldBeDefault && currentCount > 0) {
            addressRepository.findByUserIdAndIsDefaultTrue(userId)
                    .ifPresent(address -> {
                        address.setDefault(false);
                        addressRepository.save(address);
                    });
        }

        Address.AddressType addressType = Address.AddressType.HOME;
        if (dto.getAddressType() != null) {
            try {
                addressType = Address.AddressType.valueOf(dto.getAddressType());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid address type '{}', defaulting to HOME", dto.getAddressType());
            }
        }

        Address address = Address.builder()
                .user(user)
                .fullName(dto.getFullName())
                .phoneNumber(dto.getPhoneNumber())
                .addressLine1(dto.getAddressLine1())
                .addressLine2(dto.getAddressLine2())
                .city(dto.getCity())
                .state(dto.getState())
                .postalCode(dto.getPostalCode())
                .country(dto.getCountry() != null ? dto.getCountry() : "India")
                .isDefault(shouldBeDefault)
                .addressType(addressType)
                .build();

        address = addressRepository.save(address);
        log.info("Address created: addressId={}, userId={}", address.getId(), userId);
        return toDTO(address);
    }

    @Transactional
    public AddressDTO updateAddress(Long addressId, Long userId, AddressDTO dto) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(userId)) {
            throw new AddressAccessException("Address does not belong to user");
        }

        // If this is set as default, unset other default addresses
        if (dto.isDefault() && !address.isDefault()) {
            addressRepository.findByUserIdAndIsDefaultTrue(userId)
                    .ifPresent(addr -> {
                        addr.setDefault(false);
                        addressRepository.save(addr);
                    });
        }

        if (dto.getFullName() != null) address.setFullName(dto.getFullName());
        if (dto.getPhoneNumber() != null) address.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getAddressLine1() != null) address.setAddressLine1(dto.getAddressLine1());
        if (dto.getAddressLine2() != null) address.setAddressLine2(dto.getAddressLine2());
        if (dto.getCity() != null) address.setCity(dto.getCity());
        if (dto.getState() != null) address.setState(dto.getState());
        if (dto.getPostalCode() != null) address.setPostalCode(dto.getPostalCode());
        if (dto.getCountry() != null) address.setCountry(dto.getCountry());
        address.setDefault(dto.isDefault());
        if (dto.getAddressType() != null) {
            try {
                address.setAddressType(Address.AddressType.valueOf(dto.getAddressType()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid address type '{}', keeping existing type", dto.getAddressType());
            }
        }

        address = addressRepository.save(address);
        log.info("Address updated: addressId={}, userId={}", addressId, userId);
        return toDTO(address);
    }

    @Transactional
    public void deleteAddress(Long addressId, Long userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(userId)) {
            throw new AddressAccessException("Address does not belong to user");
        }

        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);
        log.info("Address deleted: addressId={}, userId={}", addressId, userId);

        // If deleted address was the default, promote another address
        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserIdOrderByIsDefaultDescIdAsc(userId);
            if (!remaining.isEmpty()) {
                Address newDefault = remaining.get(0);
                newDefault.setDefault(true);
                addressRepository.save(newDefault);
                log.info("Promoted address {} as new default for userId={}", newDefault.getId(), userId);
            }
        }
    }

    private AddressDTO toDTO(Address address) {
        return AddressDTO.builder()
                .id(address.getId())
                .fullName(address.getFullName())
                .phoneNumber(address.getPhoneNumber())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .isDefault(address.isDefault())
                .addressType(address.getAddressType().name())
                .build();
    }
}
