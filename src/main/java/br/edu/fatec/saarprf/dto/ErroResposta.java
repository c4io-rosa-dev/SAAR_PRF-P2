package br.edu.fatec.saarprf.dto;

import java.util.Map;

public record ErroResposta(String mensagem, Map<String, String> erros) {
}
