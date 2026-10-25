package br.edu.fatec.saarprf.service;

import br.edu.fatec.saarprf.model.Acidente;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversorLinhaCsvTest {

    private final ConversorLinhaCsv conversor = new ConversorLinhaCsv();

    private Map<String, String> linhaValida() {
        Map<String, String> campos = new HashMap<>();
        campos.put("id", "652493");
        campos.put("data_inversa", "2025-01-01");
        campos.put("horario", "06:20:00");
        campos.put("uf", "SP");
        campos.put("br", "116");
        campos.put("km", "225");
        campos.put("municipio", "GUARULHOS");
        campos.put("causa_acidente", "Reação tardia ou ineficiente do condutor");
        campos.put("tipo_acidente", "Tombamento");
        campos.put("classificacao_acidente", "Com Vítimas Feridas");
        campos.put("mortos", "0");
        campos.put("feridos", "1");
        campos.put("latitude", "-23,48586772");
        campos.put("longitude", "-46,54075317");
        return campos;
    }

    private void assertRejeita(Map<String, String> campos, String motivo) {
        assertThatThrownBy(() -> conversor.converter(campos))
                .isInstanceOf(LinhaInvalidaException.class)
                .hasMessage(motivo);
    }

    @Test
    void converteLinhaValida() {
        Acidente a = conversor.converter(linhaValida());

        assertThat(a.getIdPrf()).isEqualTo(652493L);
        assertThat(a.getDataOcorrencia()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(a.getHorario()).isEqualTo(LocalTime.of(6, 20));
        assertThat(a.getUf()).isEqualTo("SP");
        assertThat(a.getRodovia()).isEqualTo("BR-116");
        assertThat(a.getQuilometro()).isEqualByComparingTo("225");
        assertThat(a.getMunicipio()).isEqualTo("GUARULHOS");
        assertThat(a.getClassificacao()).isEqualTo("Com Vítimas Feridas");
        assertThat(a.getMortos()).isZero();
        assertThat(a.getFeridos()).isEqualTo(1);
        assertThat(a.getLatitude()).isEqualByComparingTo(new BigDecimal("-23.48586772"));
        assertThat(a.getLongitude()).isEqualByComparingTo(new BigDecimal("-46.54075317"));
    }

    @Test
    void rodoviaComDoisDigitosGanhaZeroAEsquerda() {
        Map<String, String> campos = linhaValida();
        campos.put("br", "40");
        assertThat(conversor.converter(campos).getRodovia()).isEqualTo("BR-040");
    }

    @Test
    void quilometroComVirgulaDecimal() {
        Map<String, String> campos = linhaValida();
        campos.put("km", "546,2");
        assertThat(conversor.converter(campos).getQuilometro()).isEqualByComparingTo("546.2");
    }

    @Test
    void quilometroComPontoDecimal() {
        Map<String, String> campos = linhaValida();
        campos.put("km", "546.2");
        assertThat(conversor.converter(campos).getQuilometro()).isEqualByComparingTo("546.2");
    }

    @Test
    void aceitaDataNoFormatoDoDicionario() {
        Map<String, String> campos = linhaValida();
        campos.put("data_inversa", "01/01/2025");
        assertThat(conversor.converter(campos).getDataOcorrencia()).isEqualTo(LocalDate.of(2025, 1, 1));
    }

    @Test
    void municipioComEspacoDuploEhNormalizado() {
        Map<String, String> campos = linhaValida();
        campos.put("municipio", "SAO SEBASTIAO DE LAGOA DE  ROCA");
        assertThat(conversor.converter(campos).getMunicipio()).isEqualTo("SAO SEBASTIAO DE LAGOA DE ROCA");
    }

    @Test
    void espacosNasPontasSaoRemovidosEUfVaiParaMaiusculo() {
        Map<String, String> campos = linhaValida();
        campos.put("uf", " sp ");
        campos.put("tipo_acidente", "  Tombamento ");
        Acidente a = conversor.converter(campos);
        assertThat(a.getUf()).isEqualTo("SP");
        assertThat(a.getTipoAcidente()).isEqualTo("Tombamento");
    }

    @Test
    void idEmNotacaoCientificaEhRejeitado() {
        Map<String, String> campos = linhaValida();
        campos.put("id", "8e+05");
        assertRejeita(campos, "id PRF inválido");
    }

    @Test
    void classificacaoNaEhRejeitada() {
        Map<String, String> campos = linhaValida();
        campos.put("classificacao_acidente", "NA");
        assertRejeita(campos, "classificação ausente");
    }

    @Test
    void brZeroEhRejeitada() {
        Map<String, String> campos = linhaValida();
        campos.put("br", "0");
        assertRejeita(campos, "rodovia não identificada");
    }

    @Test
    void brNaoNumericaEhRejeitada() {
        Map<String, String> campos = linhaValida();
        campos.put("br", "abc");
        assertRejeita(campos, "rodovia inválida");
    }

    @Test
    void dataInvalidaEhRejeitada() {
        Map<String, String> campos = linhaValida();
        campos.put("data_inversa", "2025-13-45");
        assertRejeita(campos, "data inválida");
    }

    @Test
    void horarioInvalidoEhRejeitado() {
        Map<String, String> campos = linhaValida();
        campos.put("horario", "25:99");
        assertRejeita(campos, "horário inválido");
    }

    @Test
    void latitudeNaoNumericaEhRejeitada() {
        Map<String, String> campos = linhaValida();
        campos.put("latitude", "abc");
        assertRejeita(campos, "latitude inválida");
    }

    @Test
    void mortosNaoNumericoEhRejeitado() {
        Map<String, String> campos = linhaValida();
        campos.put("mortos", "NA");
        assertRejeita(campos, "número de mortos inválido");
    }

    @Test
    void idComMuitosDigitosEhRejeitado() {
        Map<String, String> campos = linhaValida();
        campos.put("id", "99999999999999999999");
        assertRejeita(campos, "id PRF inválido");
    }

    @Test
    void brComMuitosDigitosEhRejeitada() {
        Map<String, String> campos = linhaValida();
        campos.put("br", "99999999999");
        assertRejeita(campos, "rodovia inválida");
    }
}
