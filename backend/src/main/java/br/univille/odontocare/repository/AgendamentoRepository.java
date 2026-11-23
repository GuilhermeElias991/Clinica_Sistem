package br.univille.odontocare.repository;

import br.univille.odontocare.domain.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long>, JpaSpecificationExecutor<Agendamento> {

    /**
     * Regra de conflito (docs/BANCO_DE_DADOS.md §5): mesma data, status diferente de cancelado,
     * mesmo dentista OU mesmo paciente, e horários sobrepostos.
     * {@code ignorarId} = id do agendamento sendo editado (use -1 na criação).
     */
    @Query("""
            select a from Agendamento a
             join fetch a.paciente
             where a.data = :data
               and a.status <> 'cancelado'
               and (a.dentista.id = :dentistaId or a.paciente.id = :pacienteId)
               and a.horaInicio < :fim
               and :inicio < a.horaFim
               and a.id <> :ignorarId
             order by a.horaInicio
            """)
    List<Agendamento> findConflitos(@Param("data") LocalDate data,
                                    @Param("inicio") LocalTime inicio,
                                    @Param("fim") LocalTime fim,
                                    @Param("dentistaId") Long dentistaId,
                                    @Param("pacienteId") Long pacienteId,
                                    @Param("ignorarId") Long ignorarId);

    List<Agendamento> findByDataBetween(LocalDate inicio, LocalDate fim);

    List<Agendamento> findByDonoIdAndDataBetween(Long donoId, LocalDate inicio, LocalDate fim);

    boolean existsByProcedimentoId(Long procedimentoId);

    @Modifying
    @Query("delete from Agendamento a where a.paciente.id = :id")
    int deleteByPacienteId(@Param("id") Long id);

    @Modifying
    @Query("delete from Agendamento a where a.dentista.id = :id")
    int deleteByDentistaId(@Param("id") Long id);
}
