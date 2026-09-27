package com.example.newcotizador.dto;
import java.util.List;
import org.springframework.data.domain.Page;
public record PaginaResponse<T>(List<T> content, long totalElements, int totalPages, int number) {
    public static <T> PaginaResponse<T> of(Page<T> page) {
        return new PaginaResponse<>(page.getContent(), page.getTotalElements(), page.getTotalPages(), page.getNumber());
    }
}
