package com.moneymanager.service;

import com.moneymanager.entity.User;
import com.moneymanager.repository.UserRepository;
import com.moneymanager.config.SecurityUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @PostConstruct
    public void seedAdmin() {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                .name("System Admin")
                .email("admin@moneymanager.com")
                .phone("1234567890")
                .password(SecurityUtil.hashPassword("admin123"))
                .role("ADMIN")
                .active(true)
                .build();
            userRepository.save(admin);
        }
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByRole(String role) {
        return userRepository.findByRole(role);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User registerUser(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
        user.setPassword(SecurityUtil.hashPassword(user.getPassword()));
        user.setRole("USER");
        user.setActive(true);
        return userRepository.save(user);
    }

    @Transactional
    public User saveUserDirectly(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User updateUserProfile(Long id, String name, String email, String phone) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Email already in use by another account");
        }

        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPassword(SecurityUtil.hashPassword(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void toggleUserActiveStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
