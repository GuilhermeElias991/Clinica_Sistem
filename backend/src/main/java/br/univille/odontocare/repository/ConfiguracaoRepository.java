package br.univille.odontocare.repository;

import br.univille.odontocare.domain.Configuracao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoRepository extends JpaRepository<Configuracao, Integer> {
}
