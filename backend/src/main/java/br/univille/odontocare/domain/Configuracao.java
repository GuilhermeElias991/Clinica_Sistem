package br.univille.odontocare.domain;

import jakarta.persistence.*;

import java.time.LocalTime;

/** Linha única (id = 1) com o expediente da clínica. */
@Entity
@Table(name = "configuracoes")
public class Configuracao {

    public static final int ID_UNICO = 1;

    @Id
    private Integer id = ID_UNICO;

    @Column(name = "nome_clinica", nullable = false, length = 120)
    private String nomeClinica = "OdontoCare";

    @Column(nullable = false)
    private LocalTime abertura = LocalTime.of(8, 0);

    @Column(nullable = false)
    private LocalTime fechamento = LocalTime.of(18, 0);

    @Column(name = "intervalo_min", nullable = false)
    private int intervaloMin = 30;

    @Column(name = "atende_sabado", nullable = false)
    private boolean atendeSabado = false;

    public Integer getId() { return id; }
    public String getNomeClinica() { return nomeClinica; }
    public void setNomeClinica(String nomeClinica) { this.nomeClinica = nomeClinica; }
    public LocalTime getAbertura() { return abertura; }
    public void setAbertura(LocalTime abertura) { this.abertura = abertura; }
    public LocalTime getFechamento() { return fechamento; }
    public void setFechamento(LocalTime fechamento) { this.fechamento = fechamento; }
    public int getIntervaloMin() { return intervaloMin; }
    public void setIntervaloMin(int intervaloMin) { this.intervaloMin = intervaloMin; }
    public boolean isAtendeSabado() { return atendeSabado; }
    public void setAtendeSabado(boolean atendeSabado) { this.atendeSabado = atendeSabado; }
}
