package br.edu.fatec.saarprf.dto;

import br.edu.fatec.saarprf.service.Normalizacao;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record FiltroAcidente(
        String uf,
        String classificacao,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {

    public FiltroAcidente {
        uf = Normalizacao.maiusculo(uf);
        classificacao = Normalizacao.texto(classificacao);
    }
}
