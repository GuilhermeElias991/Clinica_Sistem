package br.univille.odontocare.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "dentistas")
public class Dentista extends Auditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 30, unique = true)
    private String cro;

    @Column(nullable = false, length = 80)
    private String especialidade;

    @Column(length = 20)
    private String telefone;

    @Column(nullable = false, length = 7)
    private String cor = "#0ea5e9";

    @Column(nullable = false)
    private boolean ativo = true;

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCro() { return cro; }
    public void setCro(String cro) { this.cro = cro; }
    public String getEspecialidade() { return especialidade; }
    public void setEspecialidade(String especialidade) { this.especialidade = especialidade; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = cor; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
