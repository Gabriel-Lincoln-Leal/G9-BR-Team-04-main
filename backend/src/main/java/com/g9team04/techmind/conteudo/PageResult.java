package com.g9team04.techmind.conteudo;

import org.springframework.data.domain.Page;
import java.util.List;

/** Contrato estável para respostas paginadas da API. */
public record PageResult<T>(List<T> content, long totalElements, int totalPages, int number, int size) {
    public static <T> PageResult<T> from(Page<T> page) {
        return new PageResult<>(page.getContent(), page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize());
    }
}
