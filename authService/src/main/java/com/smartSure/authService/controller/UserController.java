package com.smartSure.authService.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartSure.authService.dto.AddressRequestDto;
import com.smartSure.authService.dto.AddressResponseDto;
import com.smartSure.authService.dto.UserRequestDto;
import com.smartSure.authService.dto.UserResponseDto;
import com.smartSure.authService.service.AddressService;
import com.smartSure.authService.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Tag(name = "Smart Sure User Controller", description = "Backend API Testing for User and associated fields")
public class UserController {

	private final UserService service;
	private final AddressService addService;

	// Returns the userId and role extracted from gateway headers — useful for debugging
	@GetMapping("/profile")
	public String getProfile(HttpServletRequest request) {
		String userId = request.getHeader("X-User-Id");
		String role   = request.getHeader("X-User-Role");
		return "UserId: " + userId + ", Role: " + role;
	}

	@PostMapping("/addInfo")
	@Operation(summary = "Adding information", description = "Adding information to registered user row")
	@ApiResponse(responseCode = "202", description = "Information added successfully")
	public ResponseEntity<UserResponseDto> addInfo(@RequestBody @Valid UserRequestDto reqDto) {
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.add(reqDto));
	}

	@GetMapping("/getInfo/{userId}")
	@Operation(summary = "Get User", description = "Get verified user with user id")
	@ApiResponse(responseCode = "200", description = "User fetched successfully")
	public ResponseEntity<UserResponseDto> getUser(@PathVariable Long userId) {
		return ResponseEntity.status(HttpStatus.OK).body(service.get(userId));
	}

	@PutMapping("/update/{userId}")
	@Operation(summary = "Update User", description = "Updating information to registered user row")
	@ApiResponse(responseCode = "202", description = "Information updated successfully")
	public ResponseEntity<UserResponseDto> updateUser(@RequestBody @Valid UserRequestDto reqDto, @PathVariable Long userId) {
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.update(reqDto, userId));
	}

	@DeleteMapping("/delete/{userId}")
	@Operation(summary = "Delete User", description = "Removing existing user from database")
	@ApiResponse(responseCode = "200", description = "User removed successfully")
	public ResponseEntity<UserResponseDto> deleteUser(@PathVariable Long userId) {
		return ResponseEntity.status(HttpStatus.OK).body(service.delete(userId));
	}

	@PostMapping("/addAddress/{userId}")
	@Operation(summary = "Adding User's Address", description = "Adding address to registered user row")
	@ApiResponse(responseCode = "202", description = "Address added successfully")
	public ResponseEntity<AddressResponseDto> addAddress(@RequestBody @Valid AddressRequestDto reqDto, @PathVariable Long userId) {
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(addService.create(reqDto, userId));
	}

	@GetMapping("/getAddress/{userId}")
	@Operation(summary = "Get User's Address", description = "Get verified user's address with user id")
	@ApiResponse(responseCode = "200", description = "Address fetched successfully")
	public ResponseEntity<AddressResponseDto> getAddress(@PathVariable Long userId) {
		return ResponseEntity.status(HttpStatus.OK).body(addService.get(userId));
	}

	@PutMapping("/updateAddress/{userId}")
	@Operation(summary = "Update User's Address", description = "Updating address information to registered user row")
	@ApiResponse(responseCode = "202", description = "Address updated successfully")
	public ResponseEntity<AddressResponseDto> updateAddress(@RequestBody @Valid AddressRequestDto reqDto, @PathVariable Long userId) {
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(addService.update(reqDto, userId));
	}

	@DeleteMapping("/deleteAddress/{userId}")
	@Operation(summary = "Delete User's address", description = "Removing existing user's address from database")
	@ApiResponse(responseCode = "200", description = "Address removed successfully")
	public ResponseEntity<AddressResponseDto> deleteAddress(@PathVariable Long userId) {
		return ResponseEntity.status(HttpStatus.OK).body(addService.delete(userId));
	}

	// Called by adminService via Feign — returns all users
	@GetMapping("/all")
	public ResponseEntity<List<UserResponseDto>> getAllUsers() {
		return ResponseEntity.ok(service.getAll());
	}
}
