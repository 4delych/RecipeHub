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
            throw new RegistrationException("passwordConfirmation", "registration.password.mismatch");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RegistrationException("email", "registration.email.exists");
        }

        String passwordHash = passwordEncoder.encode(form.getPassword());
        return userRepository.save(new User(name, email, passwordHash));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static class RegistrationException extends RuntimeException {

        private final String fieldName;
        private final String messageCode;

        public RegistrationException(String fieldName, String messageCode) {
            super(messageCode);
            this.fieldName = fieldName;
            this.messageCode = messageCode;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getMessageCode() {
            return messageCode;
        }
    }
}
