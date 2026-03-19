package com.smartSure.authService.repository;

import com.smartSure.authService.entity.Role;
import com.smartSure.authService.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by email — used during login and JWT loading
    Optional<User> findByEmail(String email);

	/*
	 * // Check if email already registered — used during registration boolean
	 * existsByEmail(String email);
	 * 
	 * // Find all users by role — used by admin to list customers List<User>
	 * findByRole(Role role);
	 * 
	 * // Search users by name (case-insensitive partial match)
	 * 
	 * @Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :name, '%'))"
	 * ) List<User> searchByName(@Param("name") String name);
	 * 
	 * // Find user by phone number Optional<User> findByPhone(String phone);
	 */
}
