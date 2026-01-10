package com.example.grazy_back.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.grazy_back.enums.RoleEnum;
import com.example.grazy_back.model.Usuario;
import com.example.grazy_back.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuração para inicializar o Super Admin no primeiro boot.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer 
{
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String superAdminEmail;

    @Value("${app.admin.password:admin}")
    private String superAdminPassword;

    @Value("${app.superadmin.name:Super Admin}")
    private String superAdminName;

    @Bean
    public CommandLineRunner initSuperAdmin() 
    {
        return args -> {
            // Verifica se já existe um SUPER_ADMIN
            boolean existeSuperAdmin = usuarioRepository.findByRole(RoleEnum.SUPER_ADMIN)
                .stream()
                .findFirst()
                .isPresent();

            if (!existeSuperAdmin) 
            {
                // Verifica se o email já está em uso
                if (usuarioRepository.existsByEmail(superAdminEmail)) 
                {
                    log.warn("Email {} já está em uso. Super Admin não criado.", superAdminEmail);
                    return;
                }

                // Cria o Super Admin
                Usuario superAdmin = Usuario.builder()
                    .email(superAdminEmail)
                    .senha(passwordEncoder.encode(superAdminPassword))
                    .nome(superAdminName)
                    .role(RoleEnum.SUPER_ADMIN)
                    .ativo(true)
                    .primeiroAcesso(false)
                    .tenant(null) // Super Admin não pertence a nenhum tenant
                    .build();

                usuarioRepository.save(superAdmin);
                log.info("Super Admin criado com sucesso: {}", superAdminEmail);
                log.info("==================================================");
                log.info("SUPER ADMIN CREDENTIALS:");
                log.info("Email: {}", superAdminEmail);
                log.info("Password: {}", superAdminPassword);
                log.info("==================================================");
                log.warn("IMPORTANTE: Troque a senha do Super Admin em produção!");
            } 
            else 
            {
                log.info("Super Admin já existe. Pulando inicialização.");
            }
        };
    }
}
