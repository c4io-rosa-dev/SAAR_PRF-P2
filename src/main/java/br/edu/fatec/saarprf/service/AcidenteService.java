package br.edu.fatec.saarprf.service;

import br.edu.fatec.saarprf.dto.ContagemCausa;
import br.edu.fatec.saarprf.dto.ContagemMensal;
import br.edu.fatec.saarprf.dto.FiltroAcidente;
import br.edu.fatec.saarprf.dto.PaginaResposta;
import br.edu.fatec.saarprf.model.Acidente;
import br.edu.fatec.saarprf.repository.AcidenteRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AcidenteService {

    private static final int TAMANHO_MAXIMO_PAGINA = 200;
    private static final int QUANTIDADE_TOP_CAUSAS = 10;
    private static final Sort ORDEM_PADRAO = Sort.by(
            Sort.Order.desc("dataOcorrencia"), Sort.Order.desc("horario"), Sort.Order.desc("id"));

    private final AcidenteRepository repository;

    public AcidenteService(AcidenteRepository repository) {
        this.repository = repository;
    }

    public PaginaResposta<Acidente> listar(int pagina, int tamanho) {
        return PaginaResposta.de(repository.findAll(paginacao(pagina, tamanho)));
    }

    public Optional<Acidente> buscarPorId(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Acidente criar(Acidente acidente) {
        acidente.setId(null);
        // registros cadastrados manualmente não têm identificador de origem na PRF
        acidente.setIdPrf(null);
        normalizar(acidente);
        return repository.save(acidente);
    }

    @Transactional
    public Optional<Acidente> atualizar(Long id, Acidente dados) {
        return repository.findById(id).map(existente -> {
            copiarDados(dados, existente);
            normalizar(existente);
            return repository.save(existente);
        });
    }

    @Transactional
    public boolean excluir(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    public PaginaResposta<Acidente> filtrar(FiltroAcidente filtro, int pagina, int tamanho) {
        return PaginaResposta.de(repository.filtrar(filtro.uf(), filtro.classificacao(),
                filtro.dataInicio(), filtro.dataFim(), paginacao(pagina, tamanho)));
    }

    public List<ContagemCausa> topCausas(FiltroAcidente filtro) {
        return repository.contarPorCausa(filtro.uf(), filtro.classificacao(),
                filtro.dataInicio(), filtro.dataFim(), PageRequest.of(0, QUANTIDADE_TOP_CAUSAS));
    }

    public List<ContagemMensal> evolucaoMensal(FiltroAcidente filtro) {
        return repository.contarPorMes(filtro.uf(), filtro.classificacao(),
                filtro.dataInicio(), filtro.dataFim());
    }

    private Pageable paginacao(int pagina, int tamanho) {
        int tamanhoAjustado = Math.clamp(tamanho, 1, TAMANHO_MAXIMO_PAGINA);
        int paginaMaxima = Integer.MAX_VALUE / tamanhoAjustado;
        return PageRequest.of(Math.clamp(pagina, 0, paginaMaxima), tamanhoAjustado, ORDEM_PADRAO);
    }

    private void normalizar(Acidente acidente) {
        acidente.setUf(Normalizacao.maiusculo(acidente.getUf()));
        acidente.setMunicipio(Normalizacao.maiusculo(acidente.getMunicipio()));
        acidente.setCausaAcidente(Normalizacao.texto(acidente.getCausaAcidente()));
        acidente.setTipoAcidente(Normalizacao.texto(acidente.getTipoAcidente()));
    }

    private void copiarDados(Acidente origem, Acidente destino) {
        destino.setDataOcorrencia(origem.getDataOcorrencia());
        destino.setHorario(origem.getHorario());
        destino.setUf(origem.getUf());
        destino.setMunicipio(origem.getMunicipio());
        destino.setRodovia(origem.getRodovia());
        destino.setQuilometro(origem.getQuilometro());
        destino.setCausaAcidente(origem.getCausaAcidente());
        destino.setTipoAcidente(origem.getTipoAcidente());
        destino.setClassificacao(origem.getClassificacao());
        destino.setMortos(origem.getMortos());
        destino.setFeridos(origem.getFeridos());
        destino.setLatitude(origem.getLatitude());
        destino.setLongitude(origem.getLongitude());
    }
}
