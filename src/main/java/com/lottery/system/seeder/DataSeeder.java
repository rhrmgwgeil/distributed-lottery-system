package com.lottery.system.seeder;

import com.lottery.system.entity.User;
import com.lottery.system.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            User defaultAdmin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin"))
                    .scope("ADMIN")
                    .isPasswordChanged(false)
                    .build();
            userRepository.save(defaultAdmin);
        }
    }
}
