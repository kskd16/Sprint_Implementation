package com.smartSure.authService.service;

import com.smartSure.authService.dto.UserRequestDto;
import com.smartSure.authService.dto.UserResponseDto;
import com.smartSure.authService.entity.Role;
import com.smartSure.authService.entity.User;
import com.smartSure.authService.exception.UserNotFoundException;
import com.smartSure.authService.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 * Tests add, get, update, delete, and getAll operations.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository repo;
    @Mock private ModelMapper modelMapper;

    @InjectMocks private UserService userService;

    private User mockUser;
    private UserResponseDto mockResponse;
    private UserRequestDto mockRequest;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserId(1L);
        mockUser.setFirstName("Rahul");
        mockUser.setLastName("Sharma");
        mockUser.setEmail("rahul@example.com");
        mockUser.setPhone(9876543210L);
        mockUser.setRole(Role.CUSTOMER);

        mockResponse = new UserResponseDto();
        mockResponse.setUserId(1L);
        mockResponse.setFirstName("Rahul");
        mockResponse.setEmail("rahul@example.com");

        mockRequest = new UserRequestDto();
        mockRequest.setFirstName("Rahul");
        mockRequest.setLastName("Sharma");
        mockRequest.setEmail("rahul@example.com");
        mockRequest.setPassword("Test@1234");
        mockRequest.setPhone(9876543210L);
    }

    @Test
    @DisplayName("Add user info - updates existing user by email")
    void add_updatesExistingUser() {
        when(repo.findByEmail("rahul@example.com")).thenReturn(Optional.of(mockUser));
        when(repo.save(any(User.class))).thenReturn(mockUser);
        when(modelMapper.map(mockUser, UserResponseDto.class)).thenReturn(mockResponse);

        UserResponseDto result = userService.add(mockRequest);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("rahul@example.com");
        verify(repo).save(mockUser);
    }

    @Test
    @DisplayName("Add user info - throws when email not registered")
    void add_throwsWhenEmailNotFound() {
        when(repo.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        mockRequest.setEmail("unknown@example.com");

        assertThatThrownBy(() -> userService.add(mockRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("not registered");
    }

    @Test
    @DisplayName("Get user - returns user when found")
    void get_returnsUserWhenFound() {
        when(repo.findById(1L)).thenReturn(Optional.of(mockUser));
        when(modelMapper.map(mockUser, UserResponseDto.class)).thenReturn(mockResponse);

        UserResponseDto result = userService.get(1L);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Get user - throws when user not found")
    void get_throwsWhenNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.get(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Update user - updates and returns updated user")
    void update_updatesUser() {
        when(repo.findById(1L)).thenReturn(Optional.of(mockUser));
        when(repo.save(any(User.class))).thenReturn(mockUser);
        when(modelMapper.map(mockUser, UserResponseDto.class)).thenReturn(mockResponse);

        UserResponseDto result = userService.update(mockRequest, 1L);

        assertThat(result).isNotNull();
        verify(repo).save(mockUser);
    }

    @Test
    @DisplayName("Update user - throws when user not found")
    void update_throwsWhenNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(mockRequest, 99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Delete user - deletes and returns deleted user")
    void delete_deletesUser() {
        when(repo.findById(1L)).thenReturn(Optional.of(mockUser));
        when(modelMapper.map(mockUser, UserResponseDto.class)).thenReturn(mockResponse);

        UserResponseDto result = userService.delete(1L);

        assertThat(result).isNotNull();
        verify(repo).deleteById(1L);
    }

    @Test
    @DisplayName("Delete user - throws when user not found")
    void delete_throwsWhenNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Get all users - returns all users")
    void getAll_returnsAllUsers() {
        when(repo.findAll()).thenReturn(List.of(mockUser));
        when(modelMapper.map(mockUser, UserResponseDto.class)).thenReturn(mockResponse);

        List<UserResponseDto> result = userService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("rahul@example.com");
    }
}
