package com.example.newcotizador.domain.model;
import java.util.List;
public record Pagina<T>(List<T> content, long totalElements, int totalPages, int number) {}
