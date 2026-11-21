package br.univille.odontocare.repository;

import br.univille.odontocare.domain.Procedimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcedimentoRepository extends JpaRepository<Procedimento, Long> {

    List<Procedimento> findAllByOrderByIdAsc();
}
