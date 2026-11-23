package br.univille.odontocare.service;

import br.univille.odontocare.config.UsuarioAtual;
import br.univille.odontocare.domain.Dentista;
import br.univille.odontocare.dto.DentistaDto;
import br.univille.odontocare.exception.ApiException;
import br.univille.odontocare.repository.AgendamentoRepository;
import br.univille.odontocare.repository.DentistaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static br.univille.odontocare.service.Documentos.vazioParaNull;

@Service
public class DentistaService {

    private final DentistaRepository dentistas;
    private final AgendamentoRepository agendamentos;
    private final UsuarioAtual atual;

    public DentistaService(DentistaRepository dentistas, AgendamentoRepository agendamentos, UsuarioAtual atual) {
        this.dentistas = dentistas;
        this.agendamentos = agendamentos;
        this.atual = atual;
    }

    @Transactional(readOnly = true)
    public List<DentistaDto> listar() {
        Long dono = atual.filtroDono();
        List<Dentista> lista = dono == null ? dentistas.findAllByOrderByIdAsc() : dentistas.findAllByDonoIdOrderByIdAsc(dono);
        return lista.stream().map(DentistaDto::from).toList();
    }

    @Transactional(readOnly = true)
    public DentistaDto buscar(Long id) {
        return DentistaDto.from(obter(id));
    }

    @Transactional
    public DentistaDto criar(DentistaDto dto) {
        Dentista d = new Dentista();
        d.setDono(atual.entidade());
        aplicar(d, dto, null);
        return DentistaDto.from(dentistas.save(d));
    }

    @Transactional
    public DentistaDto atualizar(Long id, DentistaDto dto) {
        Dentista d = obter(id);
        aplicar(d, dto, id);
        return DentistaDto.from(d);
    }

    @Transactional
    public DentistaDto alterarAtivo(Long id, boolean ativo) {
        Dentista d = obter(id);
        d.setAtivo(ativo);
        return DentistaDto.from(d);
    }

    /** Exclui o dentista e, em cascata, os agendamentos dele. */
    @Transactional
    public void excluir(Long id) {
        Dentista d = obter(id);
        agendamentos.deleteByDentistaId(id);
        dentistas.delete(d);
    }

    Dentista obter(Long id) {
        Dentista d = dentistas.findById(id).orElse(null);
        return atual.permitir(d, "Dentista não encontrado");
    }

    private void aplicar(Dentista d, DentistaDto dto, Long idAtual) {
        String cro = dto.cro().trim();
        Long dono = d.getDono() == null ? null : d.getDono().getId();
        boolean duplicado = idAtual == null ? dentistas.existsByDonoIdAndCroIgnoreCase(dono, cro) : dentistas.existsByDonoIdAndCroIgnoreCaseAndIdNot(dono, cro, idAtual);
        if (duplicado) throw ApiException.conflitoCampo("cro", "CRO já cadastrado");

        d.setNome(dto.nome().trim());
        d.setCro(cro);
        d.setEspecialidade(dto.especialidade().trim());
        d.setTelefone(vazioParaNull(dto.telefone()));
        d.setCor(dto.cor() == null ? "#0ea5e9" : dto.cor().toLowerCase());
        if (dto.ativo() != null) d.setAtivo(dto.ativo());
    }
}
