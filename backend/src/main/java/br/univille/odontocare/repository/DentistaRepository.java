package br.univille.odontocare.repository;

import br.univille.odontocare.domain.Dentista;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DentistaRepository extends JpaRepository<Dentista, Long> {

    List<Dentista> findAllByOrderByIdAsc();

    boolean existsByCroIgnoreCase(String cro);

    boolean existsByCroIgnoreCaseAndIdNot(String cro, Long id);

    /** Trava a agenda do dentista durante o agendamento (evita duas reservas simultâneas). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Dentista d where d.id = :id")
    Optional<Dentista> findByIdForUpdate(@Param("id") Long id);
}
