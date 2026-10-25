package br.edu.fatec.saarprf.dto;

import java.util.List;
import java.util.Map;

public record ResultadoImportacao(
        String arquivo,
        int linhasLidas,
        int importadas,
        int rejeitadas,
        int duplicadas,
        Map<String, Integer> motivosRejeicao,
        List<RejeicaoLinha> exemplosRejeicao,
        long tempoMs,
        String mensagem) {
}
