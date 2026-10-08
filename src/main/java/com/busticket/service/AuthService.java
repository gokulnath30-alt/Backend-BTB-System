package com.busticket.service;

import com.busticket.dto.AuthRequest;
import com.busticket.dto.AuthResponse;
import com.busticket.dto.RegisterRequest;
import com.busticket.entity.Role;
import com.busticket.entity.User;
import com.busticket.repository.UserRepository;
import com.busticket.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        String roleStr = request.getRole() != null ? request.getRole().trim().toUpperCase() : "USER";
        if ("ADMIN".equals(roleStr)) {
            throw new IllegalArgumentException("Registration with Administrator privileges is restricted. Only one system administrator is allowed.");
        }
        Role role;
        if ("OPERATOR".equals(roleStr)) {
            role = Role.OPERATOR;
        } else {
            role = Role.USER;
        }

        String fullName = request.getName();
        if ((fullName == null || fullName.trim().isEmpty()) && request.getFirstName() != null) {
            fullName = (request.getFirstName() + " " + (request.getLastName() != null ? request.getLastName() : "")).trim();
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            fullName = request.getEmail().split("@")[0];
        }

        String phone = request.getPhone() != null ? request.getPhone() : request.getPhoneNumber();

        var user = User.builder()
                .name(fullName)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(phone)
                .role(role)
                .build();
        User saved = userRepository.save(user);
        var jwtToken = jwtService.generateToken(saved);

        java.util.Map<String, Object> userMap = new java.util.HashMap<>();
        userMap.put("id", saved.getId());
        userMap.put("email", saved.getEmail());
        userMap.put("name", saved.getName());
        userMap.put("firstName", saved.getFirstName());
        userMap.put("lastName", saved.getLastName());
        userMap.put("phoneNumber", saved.getPhone());
        userMap.put("role", saved.getRole().name());

        return AuthResponse.builder()
                .token(jwtToken)
                .email(saved.getEmail())
                .role(saved.getRole().name())
                .id(saved.getId())
                .user(userMap)
                .build();
    }

    public AuthResponse authenticate(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Invalid email or password"));
        var jwtToken = jwtService.generateToken(user);

        java.util.Map<String, Object> userMap = new java.util.HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("email", user.getEmail());
        userMap.put("name", user.getName());
        userMap.put("firstName", user.getFirstName());
        userMap.put("lastName", user.getLastName());
        userMap.put("phoneNumber", user.getPhone());
        userMap.put("role", user.getRole().name());

        return AuthResponse.builder()
                .token(jwtToken)
                .email(user.getEmail())
                .role(user.getRole().name())
                .id(user.getId())
                .user(userMap)
                .build();
    }
}
