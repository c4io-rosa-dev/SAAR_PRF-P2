package br.edu.fatec.saarprf.dto;

import java.time.LocalDate;

public record TotaisAcidentes(Long total, Long mortos, Long feridos, LocalDate dataInicial, LocalDate dataFinal) {
}
