package com.repairshop.repair_ticket_system.config;

import com.repairshop.repair_ticket_system.entity.Role;
import com.repairshop.repair_ticket_system.entity.User;
import com.repairshop.repair_ticket_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmail("admin@wikirepair.tn").isEmpty()) {
            userRepository.save(User.builder()
                    .fullName("Administrateur")
                    .email("admin@wikirepair.tn")
                    .password(passwordEncoder.encode("Admin@1234"))
                    .role(Role.ADMIN)
                    .build());
            log.info("Default admin created — email: admin@wikirepair.tn | password: Admin@1234");
        }
    }
}
