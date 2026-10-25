package br.edu.fatec.saarprf.service;

import br.edu.fatec.saarprf.dto.RejeicaoLinha;
import br.edu.fatec.saarprf.dto.ResultadoImportacao;
import br.edu.fatec.saarprf.exception.ImportacaoException;
import br.edu.fatec.saarprf.model.Acidente;
import br.edu.fatec.saarprf.repository.AcidenteRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ImportacaoServiceTest {

    private static final String CABECALHO = "\"id\";\"data_inversa\";\"horario\";\"uf\";\"br\";\"km\";\"municipio\";"
            + "\"causa_acidente\";\"tipo_acidente\";\"classificacao_acidente\";\"mortos\";\"feridos\";"
            + "\"latitude\";\"longitude\"";

    @Mock
    private AcidenteRepository repository;

    @Mock
    private EntityManager entityManager;

    private ImportacaoService service;

    @BeforeEach
    void criarServico() {
        service = new ImportacaoService(repository, new ConversorLinhaCsv(),
                Validation.buildDefaultValidatorFactory().getValidator(), entityManager);
    }

    private static String linha(String id, String br, String classificacao) {
        return id + ";2025-01-01;\"06:20:00\";\"SP\";" + br + ";225;\"GUARULHOS\";"
                + "\"Reação tardia ou ineficiente do condutor\";\"Tombamento\";" + classificacao
                + ";0;1;-23,48586772;-46,54075317";
    }

    private static MockMultipartFile csv(String nome, String... linhas) {
        return new MockMultipartFile("arquivo", nome, "text/csv",
                String.join("\r\n", linhas).getBytes(StandardCharsets.ISO_8859_1));
    }

    @Test
    void contabilizaImportadasRejeitadasEDuplicadas() {
        when(repository.buscarIdsPrf()).thenReturn(List.of(1000L));
        MockMultipartFile arquivo = csv("dados.csv", CABECALHO,
                linha("1", "116", "\"Com Vítimas Feridas\""),
                linha("1", "116", "\"Com Vítimas Feridas\""),
                linha("1000", "116", "\"Com Vítimas Feridas\""),
                linha("2", "0", "\"Com Vítimas Feridas\""),
                linha("3", "116", "NA"),
                linha("8e+05", "277", "\"Sem Vítimas\""));

        ResultadoImportacao r = service.importar(arquivo);

        assertThat(r.linhasLidas()).isEqualTo(6);
        assertThat(r.importadas()).isEqualTo(1);
        assertThat(r.duplicadas()).isEqualTo(2);
        assertThat(r.rejeitadas()).isEqualTo(3);
        assertThat(r.motivosRejeicao())
                .containsEntry("rodovia não identificada", 1)
                .containsEntry("classificação ausente", 1)
                .containsEntry("id PRF inválido", 1);
        assertThat(r.exemplosRejeicao()).extracting(RejeicaoLinha::linha).containsExactly(5L, 6L, 7L);
        assertThat(r.mensagem()).isEqualTo("Importação concluída: 1 registros importados.");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Acidente>> lote = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(lote.capture());
        assertThat(lote.getValue()).hasSize(1);
        assertThat(lote.getValue().getFirst().getIdPrf()).isEqualTo(1L);
    }

    @Test
    void reimportacaoSemNovosRegistros() {
        when(repository.buscarIdsPrf()).thenReturn(List.of(1L));
        MockMultipartFile arquivo = csv("dados.csv", CABECALHO, linha("1", "116", "\"Sem Vítimas\""));

        ResultadoImportacao r = service.importar(arquivo);

        assertThat(r.importadas()).isZero();
        assertThat(r.duplicadas()).isEqualTo(1);
        assertThat(r.mensagem()).isEqualTo("Nenhum registro novo importado (1 duplicados, 0 rejeitados).");
        verify(repository, never()).saveAll(any());
    }

    @Test
    void arquivoVazioEhRecusado() {
        MockMultipartFile vazio = new MockMultipartFile("arquivo", "vazio.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> service.importar(vazio))
                .isInstanceOf(ImportacaoException.class)
                .hasMessageStartingWith("Arquivo vazio");
    }

    @Test
    void extensaoDiferenteDeCsvEhRecusada() {
        MockMultipartFile txt = csv("dados.txt", CABECALHO, linha("1", "116", "\"Sem Vítimas\""));

        assertThatThrownBy(() -> service.importar(txt))
                .isInstanceOf(ImportacaoException.class)
                .hasMessageStartingWith("Extensão inválida");
    }

    @Test
    void separadorVirgulaEhRecusado() {
        MockMultipartFile virgula = csv("dados.csv", CABECALHO.replace(';', ','), "1,2025-01-01");

        assertThatThrownBy(() -> service.importar(virgula))
                .isInstanceOf(ImportacaoException.class)
                .hasMessageStartingWith("Separador incorreto");
    }

    @Test
    void arquivoSemCabecalhoEhRecusado() {
        MockMultipartFile semCabecalho = csv("dados.csv", linha("1", "116", "\"Sem Vítimas\""));

        assertThatThrownBy(() -> service.importar(semCabecalho))
                .isInstanceOf(ImportacaoException.class)
                .hasMessageStartingWith("Cabeçalho ausente ou incompleto");
    }

    @Test
    void arquivoSoComCabecalhoEhRecusado() {
        MockMultipartFile soCabecalho = csv("dados.csv", CABECALHO);

        assertThatThrownBy(() -> service.importar(soCabecalho))
                .isInstanceOf(ImportacaoException.class)
                .hasMessageStartingWith("O arquivo não contém registros");
    }

    @Test
    void cabecalhoComColunaSemNomeEhRecusado() {
        MockMultipartFile semNome = csv("dados.csv", CABECALHO.replace("\"id\";", "\"id\";;"),
                linha("1", "116", "\"Sem Vítimas\""));

        assertThatThrownBy(() -> service.importar(semNome))
                .isInstanceOf(ImportacaoException.class)
                .hasMessageStartingWith("Cabeçalho ausente ou incompleto");
    }

    @Test
    void arquivoComBomUtf8EhRecusado() {
        byte[] bytes = ("﻿" + CABECALHO + "\r\n" + linha("1", "116", "\"Sem Vítimas\""))
                .getBytes(StandardCharsets.UTF_8);
        MockMultipartFile comBom = new MockMultipartFile("arquivo", "dados.csv", "text/csv", bytes);

        assertThatThrownBy(() -> service.importar(comBom))
                .isInstanceOf(ImportacaoException.class)
                .hasMessageStartingWith("Codificação incorreta");
    }

    @Test
    void linhaComAspasQuebradasEhRecusada() {
        MockMultipartFile aspas = csv("dados.csv", CABECALHO,
                linha("1", "116", "\"Sem Vítimas\"").replace("\"GUARULHOS\"", "\"GUAR\"ULHOS\""));

        assertThatThrownBy(() -> service.importar(aspas))
                .isInstanceOf(ImportacaoException.class)
                .hasMessageStartingWith("Arquivo CSV malformado");
    }

    @Test
    void gravaEmLotesDeMil() {
        List<String> linhas = new ArrayList<>();
        linhas.add(CABECALHO);
        for (int i = 1; i <= 2001; i++) {
            linhas.add(linha(String.valueOf(i), "116", "\"Sem Vítimas\""));
        }

        ResultadoImportacao r = service.importar(csv("dados.csv", linhas.toArray(String[]::new)));

        assertThat(r.importadas()).isEqualTo(2001);
        verify(repository, times(3)).saveAll(any());
        verify(entityManager, times(3)).flush();
        verify(entityManager, times(3)).clear();
    }
}
