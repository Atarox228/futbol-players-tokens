package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.exception.ValidationException;
import com.desapp.futbolplayerstokens.modelo.User;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(String username, String password, String email) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new ValidationException("Username already exists");
        }
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .role(User.Role.USER)
                .build();
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}