package br.edu.fatec.saarprf.repository;

import br.edu.fatec.saarprf.dto.ContagemCausa;
import br.edu.fatec.saarprf.dto.ContagemMensal;
import br.edu.fatec.saarprf.dto.ContagemRodovia;
import br.edu.fatec.saarprf.dto.TotaisAcidentes;
import br.edu.fatec.saarprf.model.Acidente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AcidenteRepository extends JpaRepository<Acidente, Long> {

    // filtro nulo = critério ignorado, assim os filtros podem ser combinados livremente
    // o PostgreSQL precisa saber o tipo do parâmetro de data no "is null"
    String FILTRO = """
             (:uf is null or a.uf = :uf)
             and (:classificacao is null or a.classificacao = :classificacao)
             and (cast(:dataInicio as LocalDate) is null or a.dataOcorrencia >= :dataInicio)
             and (cast(:dataFim as LocalDate) is null or a.dataOcorrencia <= :dataFim)
            """;

    @Query("select a from Acidente a where" + FILTRO)
    Page<Acidente> filtrar(@Param("uf") String uf,
                           @Param("classificacao") String classificacao,
                           @Param("dataInicio") LocalDate dataInicio,
                           @Param("dataFim") LocalDate dataFim,
                           Pageable pageable);

    @Query("select new br.edu.fatec.saarprf.dto.ContagemCausa(a.causaAcidente, count(a))"
            + " from Acidente a where" + FILTRO
            + " group by a.causaAcidente order by count(a) desc, a.causaAcidente")
    List<ContagemCausa> contarPorCausa(@Param("uf") String uf,
                                       @Param("classificacao") String classificacao,
                                       @Param("dataInicio") LocalDate dataInicio,
                                       @Param("dataFim") LocalDate dataFim,
                                       Pageable limite);

    @Query("select new br.edu.fatec.saarprf.dto.ContagemMensal(year(a.dataOcorrencia), month(a.dataOcorrencia), count(a))"
            + " from Acidente a where" + FILTRO
            + " group by year(a.dataOcorrencia), month(a.dataOcorrencia)"
            + " order by year(a.dataOcorrencia), month(a.dataOcorrencia)")
    List<ContagemMensal> contarPorMes(@Param("uf") String uf,
                                      @Param("classificacao") String classificacao,
                                      @Param("dataInicio") LocalDate dataInicio,
                                      @Param("dataFim") LocalDate dataFim);

    @Query("select new br.edu.fatec.saarprf.dto.TotaisAcidentes(count(a), coalesce(sum(a.mortos), 0L),"
            + " coalesce(sum(a.feridos), 0L), min(a.dataOcorrencia), max(a.dataOcorrencia))"
            + " from Acidente a where" + FILTRO)
    TotaisAcidentes totalizar(@Param("uf") String uf,
                              @Param("classificacao") String classificacao,
                              @Param("dataInicio") LocalDate dataInicio,
                              @Param("dataFim") LocalDate dataFim);

    @Query("select new br.edu.fatec.saarprf.dto.ContagemRodovia(a.rodovia, count(a))"
            + " from Acidente a where" + FILTRO
            + " group by a.rodovia order by count(a) desc, a.rodovia")
    List<ContagemRodovia> contarPorRodovia(@Param("uf") String uf,
                                           @Param("classificacao") String classificacao,
                                           @Param("dataInicio") LocalDate dataInicio,
                                           @Param("dataFim") LocalDate dataFim,
                                           Pageable limite);

    @Query("select a.idPrf from Acidente a where a.idPrf is not null")
    List<Long> buscarIdsPrf();
}
