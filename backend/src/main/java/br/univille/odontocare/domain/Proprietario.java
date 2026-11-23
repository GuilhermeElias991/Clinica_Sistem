package br.univille.odontocare.domain;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

/** Registro que pertence a um usuário (a médica que o cadastrou). */
@MappedSuperclass
public abstract class Proprietario extends Auditavel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dono_id")
    private Usuario dono;

    public Usuario getDono() { return dono; }
    public void setDono(Usuario dono) { this.dono = dono; }

    /** Nome de quem cadastrou (para o administrador saber de quem é o registro). */
    public String nomeDoDono() { return dono == null ? "" : dono.getNome(); }
}
