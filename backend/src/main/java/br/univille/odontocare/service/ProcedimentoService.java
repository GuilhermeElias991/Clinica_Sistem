package br.univille.odontocare.service;

import br.univille.odontocare.domain.Procedimento;
import br.univille.odontocare.dto.ProcedimentoDto;
import br.univille.odontocare.exception.ApiException;
import br.univille.odontocare.repository.AgendamentoRepository;
import br.univille.odontocare.repository.ProcedimentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProcedimentoService {

    private final ProcedimentoRepository procedimentos;
    private final AgendamentoRepository agendamentos;

    public ProcedimentoService(ProcedimentoRepository procedimentos, AgendamentoRepository agendamentos) {
        this.procedimentos = procedimentos;
        this.agendamentos = agendamentos;
    }

    @Transactional(readOnly = true)
    public List<ProcedimentoDto> listar() {
        return procedimentos.findAllByOrderByIdAsc().stream().map(ProcedimentoDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ProcedimentoDto buscar(Long id) {
        return ProcedimentoDto.from(obter(id));
    }

    @Transactional
    public ProcedimentoDto criar(ProcedimentoDto dto) {
        Procedimento p = new Procedimento();
        aplicar(p, dto);
        return ProcedimentoDto.from(procedimentos.save(p));
    }

    /** Alterar o valor não muda agendamentos já feitos: eles guardam o preço da época. */
    @Transactional
    public ProcedimentoDto atualizar(Long id, ProcedimentoDto dto) {
        Procedimento p = obter(id);
        aplicar(p, dto);
        return ProcedimentoDto.from(p);
    }

    /** FK RESTRICT: não exclui procedimento que já tem agendamento. */
    @Transactional
    public void excluir(Long id) {
        Procedimento p = obter(id);
        if (agendamentos.existsByProcedimentoId(id)) {
            throw ApiException.conflict("Há agendamentos usando este procedimento");
        }
        procedimentos.delete(p);
    }

    Procedimento obter(Long id) {
        return procedimentos.findById(id).orElseThrow(() -> ApiException.notFound("Procedimento não encontrado"));
    }

    private void aplicar(Procedimento p, ProcedimentoDto dto) {
        p.setNome(dto.nome().trim());
        p.setDuracaoMin(dto.duracao());
        p.setValor(dto.valor());
        if (dto.ativo() != null) p.setAtivo(dto.ativo());
    }
}
