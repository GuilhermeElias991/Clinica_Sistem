package br.univille.odontocare.service;

import br.univille.odontocare.domain.Configuracao;
import br.univille.odontocare.dto.ConfiguracaoDto;
import br.univille.odontocare.exception.ApiException;
import br.univille.odontocare.repository.ConfiguracaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class ConfiguracaoService {

    private static final Set<Integer> INTERVALOS = Set.of(15, 30, 60);

    private final ConfiguracaoRepository repo;

    public ConfiguracaoService(ConfiguracaoRepository repo) {
        this.repo = repo;
    }

    /** A migração V1 cria a linha id = 1; se alguém apagou, recria com o padrão. */
    @Transactional
    public Configuracao atual() {
        return repo.findById(Configuracao.ID_UNICO).orElseGet(() -> repo.save(new Configuracao()));
    }

    @Transactional
    public ConfiguracaoDto obter() {
        return ConfiguracaoDto.from(atual());
    }

    @Transactional
    public ConfiguracaoDto atualizar(ConfiguracaoDto dto) {
        if (!INTERVALOS.contains(dto.intervalo())) {
            throw ApiException.campo("intervalo", "Intervalo deve ser 15, 30 ou 60 minutos");
        }
        if (!dto.abertura().isBefore(dto.fechamento())) {
            throw ApiException.campo("abertura", "Abertura deve ser antes do fechamento");
        }
        Configuracao c = atual();
        c.setNomeClinica(dto.clinica().trim());
        c.setAbertura(dto.abertura());
        c.setFechamento(dto.fechamento());
        c.setIntervaloMin(dto.intervalo());
        c.setAtendeSabado(Boolean.TRUE.equals(dto.sabado()));
        return ConfiguracaoDto.from(c);
    }
}
