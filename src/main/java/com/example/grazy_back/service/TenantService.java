package com.example.grazy_back.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.grazy_back.dto.CriarTenantRequest;
import com.example.grazy_back.dto.ConfiguracaoTenantRequest;
import com.example.grazy_back.dto.ConfiguracaoTenantResponse;
import com.example.grazy_back.dto.TenantResponse;
import com.example.grazy_back.dto.EmailRequest;
import com.example.grazy_back.enums.PlanoEnum;
import com.example.grazy_back.enums.RoleEnum;
import com.example.grazy_back.enums.StatusTenantEnum;
import com.example.grazy_back.model.ConfiguracaoTenant;
import com.example.grazy_back.model.Tenant;
import com.example.grazy_back.model.Usuario;
import com.example.grazy_back.repository.ConfiguracaoTenantRepository;
import com.example.grazy_back.repository.TenantRepository;
import com.example.grazy_back.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service para gerenciamento de tenants (provisionamento, configuração, etc).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService 
{
    private final TenantRepository tenantRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConfiguracaoTenantRepository configuracaoTenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.domain:seusistema.com}")
    private String appDomain;

    @Value("${app.url.base:https://seusistema.com}")
    private String baseUrl;

    /**
     * Cria um novo tenant (Passo 2 do plano - Provisionamento).
     * Este é o método chamado quando o Super Admin cadastra um novo cliente.
     */
    @Transactional
    public TenantResponse criarTenant(CriarTenantRequest request) 
    {
        log.info("Criando novo tenant: {}", request.getNomeNegocio());

        // Validações
        if (tenantRepository.existsBySubdominio(request.getSubdominio())) 
            throw new IllegalArgumentException("Subdomínio já está em uso: " + request.getSubdominio());

        if (usuarioRepository.existsByEmail(request.getEmailAdmin())) 
            throw new IllegalArgumentException("Email já cadastrado: " + request.getEmailAdmin());

        // Gera senha provisória se não informada
        String senhaProvisoria = request.getSenhaProvisoria();
        if (senhaProvisoria == null || senhaProvisoria.isBlank()) 
            senhaProvisoria = gerarSenhaProvisoria();

        // 1. Criar o Tenant
        Tenant tenant = Tenant.builder()
            .nomeNegocio(request.getNomeNegocio())
            .subdominio(request.getSubdominio().toLowerCase())
            .dominioCustomizado(request.getDominioCustomizado())
            .emailAdmin(request.getEmailAdmin())
            .plano(request.getPlano() != null ? request.getPlano() : PlanoEnum.BASICO)
            .status(StatusTenantEnum.ATIVO)
            .ativo(true)
            .onboardingCompleto(false)
            .limiteAgendamentosMes(request.getPlano() != null ? request.getPlano().getLimiteAgendamentosMes() : 200)
            .databaseSchema("tenant_" + request.getSubdominio().toLowerCase().replace("-", "_"))
            .build();

        tenant = tenantRepository.save(tenant);
        log.info("Tenant criado com ID: {}", tenant.getId());

        // 2. Criar o usuário admin do tenant
        Usuario adminUsuario = Usuario.builder()
            .email(request.getEmailAdmin())
            .senha(passwordEncoder.encode(senhaProvisoria))
            .nome(request.getNomeAdmin())
            .telefone(request.getTelefoneAdmin())
            .role(RoleEnum.TENANT_ADMIN)
            .tenant(tenant)
            .ativo(true)
            .primeiroAcesso(true)
            .build();

        usuarioRepository.save(adminUsuario);
        log.info("Usuário admin criado para tenant: {}", adminUsuario.getEmail());

        // 3. Criar configuração padrão do tenant
        ConfiguracaoTenant config = ConfiguracaoTenant.builder()
            .tenant(tenant)
            .nomeExibicao(request.getNomeNegocio())
            .email(request.getEmailAdmin())
            .corPrimaria("#3B82F6")
            .corSecundaria("#10B981")
            .corFundo("#FFFFFF")
            .corTexto("#1F2937")
            .build();

        configuracaoTenantRepository.save(config);

        // 4. Enviar email de boas-vindas
        String urlAcesso = String.format("https://%s.%s", request.getSubdominio(), appDomain);
        enviarEmailBoasVindas(request.getEmailAdmin(), request.getNomeAdmin(), urlAcesso, senhaProvisoria);

        log.info("Tenant provisionado com sucesso: {}", urlAcesso);

        return TenantResponse.builder()
            .id(tenant.getId())
            .nomeNegocio(tenant.getNomeNegocio())
            .subdominio(tenant.getSubdominio())
            .dominioCustomizado(tenant.getDominioCustomizado())
            .emailAdmin(tenant.getEmailAdmin())
            .nomeAdmin(request.getNomeAdmin())
            .plano(tenant.getPlano())
            .status(tenant.getStatus())
            .ativo(tenant.isAtivo())
            .onboardingCompleto(tenant.isOnboardingCompleto())
            .agendamentosNoMes(tenant.getAgendamentosNoMes())
            .limiteAgendamentosMes(tenant.getLimiteAgendamentosMes())
            .criadoEm(tenant.getCriadoEm())
            .urlAcesso(urlAcesso)
            .senhaProvisoria(senhaProvisoria)
            .build();
    }

    /**
     * Lista todos os tenants (para Super Admin).
     */
    public List<TenantResponse> listarTenants() 
    {
        return tenantRepository.findByAtivoTrueOrderByCriadoEmDesc().stream()
            .map(this::toTenantResponse)
            .collect(Collectors.toList());
    }

    /**
     * Busca tenant por subdomínio.
     */
    public Optional<Tenant> buscarPorSubdominio(String subdominio) 
    {
        return tenantRepository.findBySubdominio(subdominio.toLowerCase());
    }

    /**
     * Busca tenant por subdomínio (alias).
     */
    public Optional<Tenant> buscarTenantPorSubdominio(String subdominio) 
    {
        return buscarPorSubdominio(subdominio);
    }

    /**
     * Busca tenant por domínio customizado.
     */
    public Optional<Tenant> buscarPorDominioCustomizado(String dominio) 
    {
        return tenantRepository.findByDominioCustomizado(dominio);
    }

    /**
     * Busca tenant por ID.
     */
    public Optional<TenantResponse> buscarPorId(Long id) 
    {
        return tenantRepository.findById(id).map(this::toTenantResponse);
    }

    /**
     * Atualiza configurações do tenant.
     */
    @Transactional
    public ConfiguracaoTenantResponse atualizarConfiguracao(Long tenantId, ConfiguracaoTenantRequest request) 
    {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant não encontrado: " + tenantId));

        ConfiguracaoTenant config = configuracaoTenantRepository.findByTenantId(tenantId)
            .orElseGet(() -> ConfiguracaoTenant.builder().tenant(tenant).build());

        // Atualiza apenas campos não nulos
        if (request.getCorPrimaria() != null) config.setCorPrimaria(request.getCorPrimaria());
        if (request.getCorSecundaria() != null) config.setCorSecundaria(request.getCorSecundaria());
        if (request.getCorFundo() != null) config.setCorFundo(request.getCorFundo());
        if (request.getCorTexto() != null) config.setCorTexto(request.getCorTexto());
        if (request.getLogoUrl() != null) config.setLogoUrl(request.getLogoUrl());
        if (request.getFaviconUrl() != null) config.setFaviconUrl(request.getFaviconUrl());
        if (request.getNomeExibicao() != null) config.setNomeExibicao(request.getNomeExibicao());
        if (request.getSlogan() != null) config.setSlogan(request.getSlogan());
        if (request.getTelefone() != null) config.setTelefone(request.getTelefone());
        if (request.getWhatsapp() != null) config.setWhatsapp(request.getWhatsapp());
        if (request.getEmail() != null) config.setEmail(request.getEmail());
        if (request.getEndereco() != null) config.setEndereco(request.getEndereco());
        if (request.getInstagram() != null) config.setInstagram(request.getInstagram());
        if (request.getFacebook() != null) config.setFacebook(request.getFacebook());
        if (request.getTiktok() != null) config.setTiktok(request.getTiktok());
        if (request.getHorarioFuncionamento() != null) config.setHorarioFuncionamento(request.getHorarioFuncionamento());
        if (request.getIntervaloAgendamentoMinutos() != null) config.setIntervaloAgendamentoMinutos(request.getIntervaloAgendamentoMinutos());
        if (request.getAntecedenciaMinimaHoras() != null) config.setAntecedenciaMinimaHoras(request.getAntecedenciaMinimaHoras());
        if (request.getAntecedenciaMaximaDias() != null) config.setAntecedenciaMaximaDias(request.getAntecedenciaMaximaDias());
        if (request.getNotificacoesEmailAtivas() != null) config.setNotificacoesEmailAtivas(request.getNotificacoesEmailAtivas());
        if (request.getNotificacoesWhatsappAtivas() != null) config.setNotificacoesWhatsappAtivas(request.getNotificacoesWhatsappAtivas());
        if (request.getWebhookUrl() != null) config.setWebhookUrl(request.getWebhookUrl());

        config = configuracaoTenantRepository.save(config);
        tenant.setAtualizadoEm(Instant.now());
        tenantRepository.save(tenant);

        return toConfiguracaoResponse(tenant, config);
    }

    /**
     * Busca configurações do tenant (para frontend aplicar white-label).
     */
    public Optional<ConfiguracaoTenantResponse> buscarConfiguracao(String subdominio) 
    {
        return tenantRepository.findBySubdominio(subdominio.toLowerCase())
            .map(tenant -> {
                ConfiguracaoTenant config = configuracaoTenantRepository.findByTenantId(tenant.getId())
                    .orElse(ConfiguracaoTenant.builder().tenant(tenant).build());
                return toConfiguracaoResponse(tenant, config);
            });
    }

    /**
     * Busca configurações por ID do tenant.
     */
    public Optional<ConfiguracaoTenantResponse> buscarConfiguracaoPorId(Long tenantId) 
    {
        return tenantRepository.findById(tenantId)
            .map(tenant -> {
                ConfiguracaoTenant config = configuracaoTenantRepository.findByTenantId(tenant.getId())
                    .orElse(ConfiguracaoTenant.builder().tenant(tenant).build());
                return toConfiguracaoResponse(tenant, config);
            });
    }

    /**
     * Marca o onboarding como completo.
     */
    @Transactional
    public void completarOnboarding(Long tenantId) 
    {
        tenantRepository.findById(tenantId).ifPresent(tenant -> {
            tenant.setOnboardingCompleto(true);
            tenant.setAtualizadoEm(Instant.now());
            tenantRepository.save(tenant);
            log.info("Onboarding completo para tenant: {}", tenant.getSubdominio());
        });
    }

    /**
     * Suspende um tenant (por falta de pagamento).
     */
    @Transactional
    public void suspenderTenant(Long tenantId, String motivo) 
    {
        tenantRepository.findById(tenantId).ifPresent(tenant -> {
            tenant.setStatus(StatusTenantEnum.SUSPENSO);
            tenant.setAtivo(false);
            tenant.setSuspensaoEm(Instant.now());
            tenant.setMotivoSuspensao(motivo);
            tenantRepository.save(tenant);
            log.info("Tenant suspenso: {} - Motivo: {}", tenant.getSubdominio(), motivo);
        });
    }

    /**
     * Reativa um tenant.
     */
    @Transactional
    public void reativarTenant(Long tenantId) 
    {
        tenantRepository.findById(tenantId).ifPresent(tenant -> {
            tenant.setStatus(StatusTenantEnum.ATIVO);
            tenant.setAtivo(true);
            tenant.setSuspensaoEm(null);
            tenant.setMotivoSuspensao(null);
            tenantRepository.save(tenant);
            log.info("Tenant reativado: {}", tenant.getSubdominio());
        });
    }

    /**
     * Incrementa contador de agendamentos do mês.
     */
    @Transactional
    public boolean incrementarAgendamento(Long tenantId) 
    {
        return tenantRepository.findById(tenantId).map(tenant -> {
            if (tenant.getLimiteAgendamentosMes() == -1) 
            {
                // Ilimitado (Enterprise)
                tenant.setAgendamentosNoMes(tenant.getAgendamentosNoMes() + 1);
                tenantRepository.save(tenant);
                return true;
            }

            if (tenant.getAgendamentosNoMes() >= tenant.getLimiteAgendamentosMes()) 
            {
                log.warn("Tenant {} atingiu limite de agendamentos: {}/{}", 
                    tenant.getSubdominio(), tenant.getAgendamentosNoMes(), tenant.getLimiteAgendamentosMes());
                return false;
            }

            tenant.setAgendamentosNoMes(tenant.getAgendamentosNoMes() + 1);
            tenantRepository.save(tenant);
            return true;
        }).orElse(false);
    }

    /**
     * Reseta contadores de agendamento (executar todo início de mês).
     */
    @Transactional
    public void resetarContadoresMensais() 
    {
        tenantRepository.findByAtivoTrue().forEach(tenant -> {
            tenant.setAgendamentosNoMes(0);
            tenantRepository.save(tenant);
        });
        log.info("Contadores de agendamento resetados");
    }

    // === Métodos privados ===

    private String gerarSenhaProvisoria() 
    {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) 
        {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void enviarEmailBoasVindas(String email, String nome, String urlAcesso, String senha) 
    {
        try 
        {
            String subject = "🎉 Bem-vindo ao seu novo sistema de agendamentos!";
            String body = String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #3B82F6;">Olá, %s!</h2>
                    
                    <p>Seu sistema de agendamentos está pronto para uso! 🚀</p>
                    
                    <div style="background-color: #F3F4F6; padding: 20px; border-radius: 8px; margin: 20px 0;">
                        <p><strong>🔗 Acesse seu sistema:</strong></p>
                        <p><a href="%s" style="color: #3B82F6; font-size: 18px;">%s</a></p>
                        
                        <p><strong>📧 Email:</strong> %s</p>
                        <p><strong>🔑 Senha provisória:</strong> %s</p>
                    </div>
                    
                    <p>⚠️ <strong>Importante:</strong> Troque sua senha no primeiro acesso.</p>
                    
                    <p>No primeiro acesso, você será guiado por um passo-a-passo para personalizar seu sistema com as cores da sua marca e sua logo.</p>
                    
                    <p>Precisa de ajuda? Responda este email ou entre em contato conosco.</p>
                    
                    <p>Sucesso! 💪</p>
                </body>
                </html>
                """, nome, urlAcesso, urlAcesso, email, senha);

            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(List.of(email));
            emailRequest.setSubject(subject);
            emailRequest.setBody(body);
            emailRequest.setHtml(true);

            emailService.send(emailRequest);
            log.info("Email de boas-vindas enviado para: {}", email);
        } 
        catch (Exception e) 
        {
            log.error("Erro ao enviar email de boas-vindas para {}: {}", email, e.getMessage());
        }
    }

    private TenantResponse toTenantResponse(Tenant tenant) 
    {
        Usuario admin = usuarioRepository.findByTenant(tenant).stream()
            .filter(u -> u.getRole() == RoleEnum.TENANT_ADMIN)
            .findFirst()
            .orElse(null);

        return TenantResponse.builder()
            .id(tenant.getId())
            .nomeNegocio(tenant.getNomeNegocio())
            .subdominio(tenant.getSubdominio())
            .dominioCustomizado(tenant.getDominioCustomizado())
            .emailAdmin(tenant.getEmailAdmin())
            .nomeAdmin(admin != null ? admin.getNome() : null)
            .plano(tenant.getPlano())
            .status(tenant.getStatus())
            .ativo(tenant.isAtivo())
            .onboardingCompleto(tenant.isOnboardingCompleto())
            .agendamentosNoMes(tenant.getAgendamentosNoMes())
            .limiteAgendamentosMes(tenant.getLimiteAgendamentosMes())
            .criadoEm(tenant.getCriadoEm())
            .urlAcesso(String.format("https://%s.%s", tenant.getSubdominio(), appDomain))
            .build();
    }

    private ConfiguracaoTenantResponse toConfiguracaoResponse(Tenant tenant, ConfiguracaoTenant config) 
    {
        return ConfiguracaoTenantResponse.builder()
            .tenantId(tenant.getId())
            .nomeNegocio(tenant.getNomeNegocio())
            .subdominio(tenant.getSubdominio())
            .corPrimaria(config.getCorPrimaria())
            .corSecundaria(config.getCorSecundaria())
            .corFundo(config.getCorFundo())
            .corTexto(config.getCorTexto())
            .logoUrl(config.getLogoUrl())
            .faviconUrl(config.getFaviconUrl())
            .nomeExibicao(config.getNomeExibicao())
            .slogan(config.getSlogan())
            .telefone(config.getTelefone())
            .whatsapp(config.getWhatsapp())
            .email(config.getEmail())
            .endereco(config.getEndereco())
            .instagram(config.getInstagram())
            .facebook(config.getFacebook())
            .tiktok(config.getTiktok())
            .horarioFuncionamento(config.getHorarioFuncionamento())
            .intervaloAgendamentoMinutos(config.getIntervaloAgendamentoMinutos())
            .antecedenciaMinimaHoras(config.getAntecedenciaMinimaHoras())
            .antecedenciaMaximaDias(config.getAntecedenciaMaximaDias())
            .onboardingCompleto(tenant.isOnboardingCompleto())
            .plano(tenant.getPlano().getNome())
            .build();
    }
}
