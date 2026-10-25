package br.edu.fatec.saarprf.service;

import br.edu.fatec.saarprf.model.Acidente;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Component
public class ConversorLinhaCsv {

    private static final DateTimeFormatter DATA_BRASILEIRA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Acidente converter(Map<String, String> campos) {
        Acidente a = new Acidente();
        a.setIdPrf(converterIdPrf(valor(campos, "id")));
        a.setDataOcorrencia(converterData(valor(campos, "data_inversa")));
        a.setHorario(converterHorario(valor(campos, "horario")));
        a.setUf(Normalizacao.maiusculo(valor(campos, "uf")));
        a.setRodovia(converterRodovia(valor(campos, "br")));
        a.setQuilometro(converterDecimal(valor(campos, "km"), "quilômetro inválido"));
        a.setMunicipio(Normalizacao.maiusculo(valor(campos, "municipio")));
        a.setCausaAcidente(valor(campos, "causa_acidente"));
        a.setTipoAcidente(valor(campos, "tipo_acidente"));
        a.setClassificacao(converterClassificacao(valor(campos, "classificacao_acidente")));
        a.setMortos(converterInteiro(valor(campos, "mortos"), "número de mortos inválido"));
        a.setFeridos(converterInteiro(valor(campos, "feridos"), "número de feridos inválido"));
        a.setLatitude(converterDecimal(valor(campos, "latitude"), "latitude inválida"));
        a.setLongitude(converterDecimal(valor(campos, "longitude"), "longitude inválida"));
        return a;
    }

    // a PRF usa o texto "NA" para marcar valor ausente
    private String valor(Map<String, String> campos, String coluna) {
        String valor = Normalizacao.texto(campos.get(coluna));
        return valor == null || valor.equalsIgnoreCase("NA") ? null : valor;
    }

    // ids corrompidos (ex.: "8e+05", notação científica gerada por planilha) não são recuperáveis
    private Long converterIdPrf(String valor) {
        if (valor == null || !valor.matches("\\d+")) {
            throw new LinhaInvalidaException("id PRF inválido");
        }
        try {
            return Long.valueOf(valor);
        } catch (NumberFormatException e) {
            throw new LinhaInvalidaException("id PRF inválido");
        }
    }

    // o arquivo usa aaaa-mm-dd, mas o dicionário oficial documenta dd/mm/aaaa: aceitamos os dois
    private LocalDate converterData(String valor) {
        if (valor == null) {
            throw new LinhaInvalidaException("data ausente");
        }
        try {
            return valor.contains("/") ? LocalDate.parse(valor, DATA_BRASILEIRA) : LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            throw new LinhaInvalidaException("data inválida");
        }
    }

    private LocalTime converterHorario(String valor) {
        if (valor == null) {
            throw new LinhaInvalidaException("horário ausente");
        }
        try {
            return LocalTime.parse(valor);
        } catch (DateTimeParseException e) {
            throw new LinhaInvalidaException("horário inválido");
        }
    }

    // br = 0 é o marcador da PRF para rodovia não identificada
    private String converterRodovia(String valor) {
        if (valor == null) {
            throw new LinhaInvalidaException("rodovia não identificada");
        }
        if (!valor.matches("\\d+")) {
            throw new LinhaInvalidaException("rodovia inválida");
        }
        int numero;
        try {
            numero = Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new LinhaInvalidaException("rodovia inválida");
        }
        if (numero == 0) {
            throw new LinhaInvalidaException("rodovia não identificada");
        }
        return String.format("BR-%03d", numero);
    }

    private String converterClassificacao(String valor) {
        if (valor == null) {
            throw new LinhaInvalidaException("classificação ausente");
        }
        return valor;
    }

    // o arquivo usa vírgula decimal, o dicionário documenta ponto: aceitamos os dois
    private BigDecimal converterDecimal(String valor, String motivo) {
        if (valor == null) {
            throw new LinhaInvalidaException(motivo);
        }
        try {
            return new BigDecimal(valor.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new LinhaInvalidaException(motivo);
        }
    }

    private Integer converterInteiro(String valor, String motivo) {
        if (valor == null) {
            throw new LinhaInvalidaException(motivo);
        }
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException e) {
            throw new LinhaInvalidaException(motivo);
        }
    }
}
