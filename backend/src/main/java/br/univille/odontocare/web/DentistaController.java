package br.univille.odontocare.web;

import br.univille.odontocare.dto.DentistaDto;
import br.univille.odontocare.exception.ApiException;
import br.univille.odontocare.service.DentistaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dentistas")
public class DentistaController {

    private final DentistaService service;

    public DentistaController(DentistaService service) {
        this.service = service;
    }

    @GetMapping
    public List<DentistaDto> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public DentistaDto buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DentistaDto criar(@Valid @RequestBody DentistaDto dto) {
        return service.criar(dto);
    }

    @PutMapping("/{id}")
    public DentistaDto atualizar(@PathVariable Long id, @Valid @RequestBody DentistaDto dto) {
        return service.atualizar(id, dto);
    }

    /** Ativar / desativar. Corpo: {"ativo": false} */
    @PatchMapping("/{id}")
    public DentistaDto ativar(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!(body.get("ativo") instanceof Boolean ativo)) throw ApiException.campo("ativo", "Informe ativo: true/false");
        return service.alterarAtivo(id, ativo);
    }

    /** Remove também os agendamentos do dentista. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
