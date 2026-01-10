package com.example.grazy_back.enums;


public enum StatusTenantEnum 
{
    PENDENTE,      // Aguardando pagamento/ativação
    ATIVO,         // Funcionando normalmente
    TRIAL,         // Período de teste gratuito
    SUSPENSO,      // Pagamento atrasado
    CANCELADO,     // Cancelou o serviço
    BLOQUEADO      // Bloqueado por violação de termos
}
