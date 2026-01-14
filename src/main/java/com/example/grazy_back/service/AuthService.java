package com.example.grazy_back.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.grazy_back.dto.LoginResponseV2;
import com.example.grazy_back.dto.OnboardingRequest;
import com.example.grazy_back.dto.ServicoRequest;
import com.example.grazy_back.enums.RoleEnum;
import com.example.grazy_back.model.Tenant;
import com.example.grazy_back.model.Usuario;
import com.example.grazy_back.repository.UsuarioRepository;
import com.example.grazy_back.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service para autenticação de usuários (tenants e super admin).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService 
{
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TenantService tenantService;
    private final ServicoService servicoService;

    /**
     * Autentica um usuário do sistema.
     * @param email Email do usuário
     * @param senha Senha do usuário
     * @param tenantIdFromHost ID do tenant identificado pelo Host (pode ser null)
     */
    public Optional<LoginResponseV2> autenticar(String email, String senha, Long tenantIdFromHost) 
    {
        return usuarioRepository.findByEmail(email)
            .filter(usuario -> passwordEncoder.matches(senha, usuario.getSenha()))
            .filter(Usuario::isAtivo)
            .filter(usuario -> {
                // VALIDAÇÃO CRÍTICA: Verificar se o usuário pertence ao tenant correto
                if (tenantIdFromHost != null) 
                {
                    // Se há um tenant identificado pelo Host, o usuário DEVE pertencer a ele
                    if (usuario.getTenant() == null) 
                    {
                        log.warn("Tentativa de login de usuário sem tenant em domínio com tenant: {} (tenant esperado: {})", 
                            email, tenantIdFromHost);
                        return false;
                    }
                    
                    if (!usuario.getTenant().getId().equals(tenantIdFromHost)) 
                    {
                        log.warn("Tentativa de login com tenant incorreto: {} (tenant do usuário: {}, tenant do host: {})", 
                            email, usuario.getTenant().getId(), tenantIdFromHost);
                        return false;
                    }
                }
                // Se não há tenant do host (ex: localhost, admin master), permitir qualquer usuário
                return true;
            })
            .map(usuario -> {
                // Verifica se tenant está ativo (para não super admin)
                if (usuario.getTenant() != null && !usuario.getTenant().isAtivo()) 
                {
                    log.warn("Tentativa de login em tenant inativo: {}", email);
                    return null;
                }

                // Atualiza último login
                usuario.setUltimoLogin(Instant.now());
                usuarioRepository.save(usuario);

                // Gera token JWT com informações adicionais
                String token = jwtUtil.generateToken(usuario.getEmail(), usuario.getRole().name(), 
                    usuario.getTenant() != null ? usuario.getTenant().getId() : null);

                Tenant tenant = usuario.getTenant();
                
                log.info("Login bem-sucedido: {} (tenant: {})", email, tenant != null ? tenant.getId() : "nenhum");
                
                return LoginResponseV2.builder()
                    .token(token)
                    .usuarioId(usuario.getId())
                    .nome(usuario.getNome())
                    .email(usuario.getEmail())
                    .role(usuario.getRole())
                    .tenantId(tenant != null ? tenant.getId() : null)
                    .tenantNome(tenant != null ? tenant.getNomeNegocio() : null)
                    .tenantSubdominio(tenant != null ? tenant.getSubdominio() : null)
                    .primeiroAcesso(usuario.isPrimeiroAcesso())
                    .onboardingCompleto(tenant != null ? tenant.isOnboardingCompleto() : true)
                    .expiresAt(Instant.now().plusMillis(jwtUtil.getExpirationMs()))
                    .build();
            });
    }

    /**
     * Processa o onboarding do primeiro acesso.
     */
    @Transactional
    public void processarOnboarding(Long usuarioId, OnboardingRequest request) 
    {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (usuario.getTenant() == null) 
        {
            throw new IllegalArgumentException("Usuário não pertence a nenhum tenant");
        }

        Long tenantId = usuario.getTenant().getId();

        // 1. Atualiza configurações visuais
        com.example.grazy_back.dto.ConfiguracaoTenantRequest configRequest = 
            com.example.grazy_back.dto.ConfiguracaoTenantRequest.builder()
                .corPrimaria(request.getCorPrimaria())
                .corSecundaria(request.getCorSecundaria())
                .logoUrl(request.getLogoUrl())
                .telefone(request.getTelefone())
                .whatsapp(request.getWhatsapp())
                .build();

        tenantService.atualizarConfiguracao(tenantId, configRequest);

        // 2. Cria primeiro serviço se informado
        if (request.getNomeServico() != null && !request.getNomeServico().isBlank()) 
        {
            ServicoRequest servicoRequest = new ServicoRequest();
            servicoRequest.setNome(request.getNomeServico());
            servicoRequest.setDescricao(request.getDescricaoServico());
            servicoRequest.setPreco(request.getPrecoServico());
            servicoRequest.setDuracaoMinutos(request.getDuracaoServico() != null ? request.getDuracaoServico() : 30);
            
            servicoService.salvarServico(servicoRequest);
            log.info("Primeiro serviço criado para tenant {}", tenantId);
        }

        // 3. Atualiza senha se informada
        if (request.getNovaSenha() != null && !request.getNovaSenha().isBlank()) 
        {
            usuario.setSenha(passwordEncoder.encode(request.getNovaSenha()));
        }

        // 4. Marca primeiro acesso como concluído
        usuario.setPrimeiroAcesso(false);
        usuarioRepository.save(usuario);

        // 5. Marca onboarding do tenant como completo
        tenantService.completarOnboarding(tenantId);

        log.info("Onboarding completo para usuário {} do tenant {}", usuarioId, tenantId);
    }

    /**
     * Troca a senha do usuário.
     */
    @Transactional
    public void trocarSenha(Long usuarioId, String senhaAtual, String novaSenha) 
    {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) 
        {
            throw new IllegalArgumentException("Senha atual incorreta");
        }

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuario.setPrimeiroAcesso(false);
        usuarioRepository.save(usuario);
        
        log.info("Senha alterada para usuário: {}", usuario.getEmail());
    }

    /**
     * Verifica se usuário é Super Admin.
     */
    public boolean isSuperAdmin(String email) 
    {
        return usuarioRepository.findByEmail(email)
            .map(u -> u.getRole() == RoleEnum.SUPER_ADMIN)
            .orElse(false);
    }

    /**
     * Busca usuário por email.
     */
    public Optional<Usuario> buscarPorEmail(String email) 
    {
        return usuarioRepository.findByEmail(email);
    }
}
