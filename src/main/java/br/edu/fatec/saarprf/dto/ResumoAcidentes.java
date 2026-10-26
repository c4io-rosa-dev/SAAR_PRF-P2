package br.edu.fatec.saarprf.dto;

import java.time.LocalDate;

public record ResumoAcidentes(
        long total,
        long mortos,
        long feridos,
        LocalDate dataInicial,
        LocalDate dataFinal,
        String rodoviaMaisCritica,
        long ocorrenciasRodovia) {
}
