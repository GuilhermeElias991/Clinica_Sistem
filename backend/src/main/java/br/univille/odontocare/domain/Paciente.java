package br.univille.odontocare.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "pacientes")
public class Paciente extends Proprietario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 14)
    private String cpf;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Column(length = 160)
    private String email;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(nullable = false, length = 60)
    private String convenio = "Particular";

    @Column(name = "observacoes")
    private String observacoes;

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public String getConvenio() { return convenio; }
    public void setConvenio(String convenio) { this.convenio = convenio; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
