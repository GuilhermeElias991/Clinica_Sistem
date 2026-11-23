package br.univille.odontocare.service;

import br.univille.odontocare.config.UsuarioAtual;
import br.univille.odontocare.domain.Paciente;
import br.univille.odontocare.dto.PacienteDto;
import br.univille.odontocare.exception.ApiException;
import br.univille.odontocare.repository.AgendamentoRepository;
import br.univille.odontocare.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static br.univille.odontocare.service.Documentos.*;

@Service
public class PacienteService {

    private final PacienteRepository pacientes;
    private final AgendamentoRepository agendamentos;
    private final UsuarioAtual atual;

    public PacienteService(PacienteRepository pacientes, AgendamentoRepository agendamentos, UsuarioAtual atual) {
        this.pacientes = pacientes;
        this.agendamentos = agendamentos;
        this.atual = atual;
    }

    @Transactional(readOnly = true)
    public List<PacienteDto> listar() {
        Long dono = atual.filtroDono();
        List<Paciente> lista = dono == null ? pacientes.findAllByOrderByNomeAsc() : pacientes.findAllByDonoIdOrderByNomeAsc(dono);
        return lista.stream().map(PacienteDto::from).toList();
    }

    @Transactional(readOnly = true)
    public PacienteDto buscar(Long id) {
        return PacienteDto.from(obter(id));
    }

    @Transactional
    public PacienteDto criar(PacienteDto dto) {
        Paciente p = new Paciente();
        p.setDono(atual.entidade());
        aplicar(p, dto, null);
        return PacienteDto.from(pacientes.save(p));
    }

    @Transactional
    public PacienteDto atualizar(Long id, PacienteDto dto) {
        Paciente p = obter(id);
        aplicar(p, dto, id);
        return PacienteDto.from(p);
    }

    /** Exclui o paciente e, em cascata, todos os agendamentos dele. */
    @Transactional
    public void excluir(Long id) {
        Paciente p = obter(id);
        agendamentos.deleteByPacienteId(id);
        pacientes.delete(p);
    }

    Paciente obter(Long id) {
        Paciente p = pacientes.findById(id).orElse(null);
        return atual.permitir(p, "Paciente não encontrado");
    }

    private void aplicar(Paciente p, PacienteDto dto, Long idAtual) {
        if (!cpfValido(dto.cpf())) throw ApiException.campo("cpf", "CPF inválido");
        String cpf = formatarCpf(dto.cpf());
        Long dono = p.getDono() == null ? null : p.getDono().getId();
        boolean duplicado = idAtual == null ? pacientes.existsByDonoIdAndCpf(dono, cpf) : pacientes.existsByDonoIdAndCpfAndIdNot(dono, cpf, idAtual);
        if (duplicado) throw ApiException.conflitoCampo("cpf", "CPF já cadastrado");
        if (digitos(dto.telefone()).length() < 10) throw ApiException.campo("telefone", "Telefone inválido");

        p.setNome(dto.nome().trim());
        p.setCpf(cpf);
        p.setTelefone(formatarTelefone(dto.telefone()));
        p.setEmail(vazioParaNull(dto.email()));
        p.setDataNascimento(dto.nascimento());
        p.setConvenio(dto.convenio() == null || dto.convenio().isBlank() ? "Particular" : dto.convenio().trim());
        p.setObservacoes(vazioParaNull(dto.obs()));
    }
}
