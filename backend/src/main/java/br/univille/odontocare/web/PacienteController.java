package br.univille.odontocare.web;

import br.univille.odontocare.dto.PacienteDto;
import br.univille.odontocare.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService service;

    public PacienteController(PacienteService service) {
        this.service = service;
    }

    @GetMapping
    public List<PacienteDto> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public PacienteDto buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PacienteDto criar(@Valid @RequestBody PacienteDto dto) {
        return service.criar(dto);
    }

    @PutMapping("/{id}")
    public PacienteDto atualizar(@PathVariable Long id, @Valid @RequestBody PacienteDto dto) {
        return service.atualizar(id, dto);
    }

    /** Remove também os agendamentos do paciente. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
