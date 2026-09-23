package com.recipehub.service;

import com.recipehub.dto.RegistrationForm;
import com.recipehub.model.User;
import com.recipehub.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegistrationForm form) {
        String name = form.getName().trim();
        String email = normalizeEmail(form.getEmail());

        if (!form.getPassword().equals(form.getPasswordConfirmation())) {
            throw new RegistrationException("passwordConfirmation", "Passwords do not match");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RegistrationException("email", "Email is already registered");
        }

        String passwordHash = passwordEncoder.encode(form.getPassword());
        return userRepository.save(new User(name, email, passwordHash));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static class RegistrationException extends RuntimeException {

        private final String fieldName;

        public RegistrationException(String fieldName, String message) {
            super(message);
            this.fieldName = fieldName;
        }

        public String getFieldName() {
            return fieldName;
        }
    }
}
