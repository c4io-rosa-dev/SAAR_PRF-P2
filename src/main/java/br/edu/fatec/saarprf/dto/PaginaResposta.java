package br.edu.fatec.saarprf.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PaginaResposta<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos, int totalPaginas) {

    public static <T> PaginaResposta<T> de(Page<T> page) {
        return new PaginaResposta<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
