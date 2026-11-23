package br.univille.odontocare.web;

import br.univille.odontocare.config.UsuarioAtual;
import br.univille.odontocare.dto.ProcedimentoDto;
import br.univille.odontocare.service.ProcedimentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procedimentos")
public class ProcedimentoController {

    private final ProcedimentoService service;
    private final UsuarioAtual atual;

    public ProcedimentoController(ProcedimentoService service, UsuarioAtual atual) {
        this.service = service;
        this.atual = atual;
    }

    @GetMapping
    public List<ProcedimentoDto> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ProcedimentoDto buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProcedimentoDto criar(@Valid @RequestBody ProcedimentoDto dto) {
        atual.exigirAdmin();
        return service.criar(dto);
    }

    @PutMapping("/{id}")
    public ProcedimentoDto atualizar(@PathVariable Long id, @Valid @RequestBody ProcedimentoDto dto) {
        atual.exigirAdmin();
        return service.atualizar(id, dto);
    }

    /** 409 se houver agendamento usando o procedimento. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        atual.exigirAdmin();
        service.excluir(id);
    }
}
