package br.edu.fatec.saarprf.controller;

import br.edu.fatec.saarprf.AcidenteFixture;
import br.edu.fatec.saarprf.dto.FiltroAcidente;
import br.edu.fatec.saarprf.dto.PaginaResposta;
import br.edu.fatec.saarprf.dto.RejeicaoLinha;
import br.edu.fatec.saarprf.dto.ResumoAcidentes;
import br.edu.fatec.saarprf.dto.ResultadoImportacao;
import br.edu.fatec.saarprf.exception.ImportacaoException;
import br.edu.fatec.saarprf.model.Acidente;
import br.edu.fatec.saarprf.service.AcidenteService;
import br.edu.fatec.saarprf.service.ImportacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AcidenteController.class)
class AcidenteControllerTest {

    private static final String JSON_VALIDO = """
            {"dataOcorrencia":"2025-01-01","horario":"06:20:00","uf":"SP","municipio":"GUARULHOS",
             "rodovia":"BR-116","quilometro":225.0,"causaAcidente":"Reação tardia ou ineficiente do condutor",
             "tipoAcidente":"Tombamento","classificacao":"Com Vítimas Feridas","mortos":0,"feridos":1,
             "latitude":-23.48586772,"longitude":-46.54075317}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AcidenteService service;

    @MockitoBean
    private ImportacaoService importacaoService;

    private static Acidente comId(long id) {
        Acidente a = AcidenteFixture.valido();
        a.setId(id);
        return a;
    }

    @Test
    void listarRetornaPagina() throws Exception {
        when(service.listar(0, 50)).thenReturn(new PaginaResposta<>(List.of(comId(1)), 0, 50, 1, 1));

        mvc.perform(get("/api/acidentes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(1));
    }

    @Test
    void buscarExistenteRetorna200() throws Exception {
        when(service.buscarPorId(1L)).thenReturn(Optional.of(comId(1)));

        mvc.perform(get("/api/acidentes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rodovia").value("BR-116"))
                .andExpect(jsonPath("$.dataOcorrencia").value("2025-01-01"));
    }

    @Test
    void buscarInexistenteRetorna404() throws Exception {
        when(service.buscarPorId(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/acidentes/99")).andExpect(status().isNotFound());
    }

    @Test
    void criarValidoRetorna201ComLocation() throws Exception {
        when(service.criar(any(Acidente.class))).thenAnswer(inv -> {
            Acidente a = inv.getArgument(0);
            a.setId(10L);
            return a;
        });

        mvc.perform(post("/api/acidentes").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/acidentes/10"))
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void criarInvalidoRetorna400ComMensagemDoCampo() throws Exception {
        String json = JSON_VALIDO.replace("\"uf\":\"SP\"", "\"uf\":\"XX\"");

        mvc.perform(post("/api/acidentes").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros.uf").value("UF inválida"));
        verify(service, never()).criar(any());
    }

    @Test
    void jsonMalformadoRetorna400() throws Exception {
        mvc.perform(post("/api/acidentes").contentType(MediaType.APPLICATION_JSON).content("{\"dataOcorrencia\":\"ontem\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(
                        "JSON inválido ou ausente: verifique os formatos de data (aaaa-mm-dd), horário (hh:mm ou hh:mm:ss) e números."));
    }

    @Test
    void atualizarExistenteRetorna200() throws Exception {
        when(service.atualizar(eq(1L), any(Acidente.class))).thenReturn(Optional.of(comId(1)));

        mvc.perform(put("/api/acidentes/1").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isOk());
    }

    @Test
    void atualizarInexistenteRetorna404() throws Exception {
        when(service.atualizar(eq(99L), any(Acidente.class))).thenReturn(Optional.empty());

        mvc.perform(put("/api/acidentes/99").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    void excluirExistenteRetorna204() throws Exception {
        when(service.excluir(1L)).thenReturn(true);

        mvc.perform(delete("/api/acidentes/1")).andExpect(status().isNoContent());
    }

    @Test
    void excluirInexistenteRetorna404() throws Exception {
        when(service.excluir(99L)).thenReturn(false);

        mvc.perform(delete("/api/acidentes/99")).andExpect(status().isNotFound());
    }

    @Test
    void importarComErroEstruturalRetorna400() throws Exception {
        when(importacaoService.importar(any())).thenThrow(new ImportacaoException("Extensão inválida: envie um arquivo .csv."));
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "dados.txt", "text/plain", "abc".getBytes());

        mvc.perform(multipart("/api/acidentes/importar").file(arquivo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Extensão inválida: envie um arquivo .csv."));
    }

    @Test
    void filtroRepassaParametros() throws Exception {
        when(service.filtrar(eq(new FiltroAcidente("SP", null, LocalDate.of(2025, 1, 1), null)), eq(0), eq(50))).thenReturn(new PaginaResposta<>(List.of(), 0, 50, 0, 0));

        mvc.perform(get("/api/acidentes/filtro").param("uf", "sp").param("dataInicio", "2025-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void filtroComDataInvalidaRetorna400() throws Exception {
        mvc.perform(get("/api/acidentes/filtro").param("dataInicio", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.dataInicio").value("valor em formato inválido (datas no formato aaaa-mm-dd)"));
    }

    @Test
    void importarValidoRetorna200() throws Exception {
        when(importacaoService.importar(any())).thenReturn(new ResultadoImportacao("dados.csv", 3, 2, 1, 0,
                Map.of("rodovia não identificada", 1), List.of(new RejeicaoLinha(4, "rodovia não identificada")),
                10, "Importação concluída: 2 registros importados."));
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "dados.csv", "text/csv", "abc".getBytes());

        mvc.perform(multipart("/api/acidentes/importar").file(arquivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importadas").value(2));
    }

    @Test
    void resumoRetornaTotaisDoFiltro() throws Exception {
        when(service.resumo(eq(new FiltroAcidente("SP", null, null, null))))
                .thenReturn(new ResumoAcidentes(120L, 9L, 150L, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 8, 31), "BR-116", 40L));

        mvc.perform(get("/api/acidentes/estatisticas/resumo").param("uf", "SP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(120))
                .andExpect(jsonPath("$.mortos").value(9))
                .andExpect(jsonPath("$.rodoviaMaisCritica").value("BR-116"))
                .andExpect(jsonPath("$.dataInicial").value("2025-01-01"));
    }
}
