package com.kalon.controller;

import com.kalon.dto.AddressDTO;
import com.kalon.dto.ApiResponse;
import com.kalon.entity.User;
import com.kalon.exception.ResourceNotFoundException;
import com.kalon.repository.UserRepository;
import com.kalon.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressDTO>>> getAddresses(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(addressService.getAddressesByUserId(userId)));
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressDTO>> getAddressById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long addressId) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(addressService.getAddressById(addressId, userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressDTO>> createAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddressDTO dto) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Address added", addressService.createAddress(userId, dto)));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressDTO>> updateAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressDTO dto) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Address updated", addressService.updateAddress(addressId, userId, dto)));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long addressId) {
        Long userId = getUserId(userDetails);
        addressService.deleteAddress(addressId, userId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted", null));
    }

    private Long getUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getId();
    }
}
