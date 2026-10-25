package br.edu.fatec.saarprf.model;

import br.edu.fatec.saarprf.AcidenteFixture;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValidacaoAcidenteTest {

    private static Validator validator;

    @BeforeAll
    static void criarValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private List<String> mensagens(Acidente a) {
        return validator.validate(a).stream().map(ConstraintViolation::getMessage).toList();
    }

    @Test
    void acidenteValidoNaoTemViolacoes() {
        assertThat(mensagens(AcidenteFixture.valido())).isEmpty();
    }

    @Test
    void ufForaDaListaEhRejeitada() {
        Acidente a = AcidenteFixture.valido();
        a.setUf("XX");
        assertThat(mensagens(a)).containsExactly("UF inválida");
    }

    @Test
    void classificacaoForaDoDominioEhRejeitada() {
        Acidente a = AcidenteFixture.valido();
        a.setClassificacao("Grave");
        assertThat(mensagens(a)).containsExactly("classificação inválida");
    }

    @Test
    void classificacaoIgnoradoEhAceita() {
        Acidente a = AcidenteFixture.valido();
        a.setClassificacao("Ignorado");
        assertThat(mensagens(a)).isEmpty();
    }

    @Test
    void rodoviaSemFormatoBrEhRejeitada() {
        Acidente a = AcidenteFixture.valido();
        a.setRodovia("116");
        assertThat(mensagens(a)).containsExactly("rodovia inválida: use o formato BR-000 (ex.: BR-116)");
    }

    @Test
    void rodoviaZeroEhRejeitada() {
        Acidente a = AcidenteFixture.valido();
        a.setRodovia("BR-000");
        assertThat(mensagens(a)).containsExactly("rodovia inválida: use o formato BR-000 (ex.: BR-116)");
    }

    @Test
    void quilometroAcimaDoLimiteEhRejeitado() {
        Acidente a = AcidenteFixture.valido();
        a.setQuilometro(new BigDecimal("10000"));
        assertThat(mensagens(a)).containsExactly("quilômetro fora do intervalo válido");
    }

    @Test
    void quilometroZeroEhAceito() {
        Acidente a = AcidenteFixture.valido();
        a.setQuilometro(BigDecimal.ZERO);
        assertThat(mensagens(a)).isEmpty();
    }

    @Test
    void quilometroNegativoEhRejeitado() {
        Acidente a = AcidenteFixture.valido();
        a.setQuilometro(new BigDecimal("-1"));
        assertThat(mensagens(a)).containsExactly("quilômetro não pode ser negativo");
    }

    @Test
    void coordenadasForaDoBrasilSaoRejeitadas() {
        Acidente a = AcidenteFixture.valido();
        a.setLatitude(new BigDecimal("10"));
        a.setLongitude(new BigDecimal("-20"));
        assertThat(mensagens(a)).containsExactlyInAnyOrder(
                "latitude fora do território brasileiro",
                "longitude fora do território brasileiro");
    }

    @Test
    void dataFuturaEhRejeitada() {
        Acidente a = AcidenteFixture.valido();
        a.setDataOcorrencia(LocalDate.now().plusDays(1));
        assertThat(mensagens(a)).containsExactly("data da ocorrência não pode ser futura");
    }

    @Test
    void camposObrigatoriosAusentes() {
        Acidente a = AcidenteFixture.valido();
        a.setMunicipio(null);
        a.setMortos(null);
        assertThat(mensagens(a)).containsExactlyInAnyOrder(
                "município é obrigatório",
                "número de mortos é obrigatório");
    }
}
