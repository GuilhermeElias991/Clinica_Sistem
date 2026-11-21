package br.univille.odontocare.domain;

import java.util.Set;

/** Valores aceitos pela coluna agendamentos.status (CHECK ck_agendamentos_status). */
public final class StatusAgendamento {

    public static final String AGENDADO = "agendado";
    public static final String CONFIRMADO = "confirmado";
    public static final String CONCLUIDO = "concluido";
    public static final String CANCELADO = "cancelado";
    public static final String FALTOU = "faltou";

    public static final Set<String> TODOS = Set.of(AGENDADO, CONFIRMADO, CONCLUIDO, CANCELADO, FALTOU);

    private StatusAgendamento() {}

    public static boolean valido(String status) {
        return status != null && TODOS.contains(status);
    }
}
