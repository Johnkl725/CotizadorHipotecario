package com.example.newcotizador.dto;
import com.example.newcotizador.domain.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record RegistroSolicitudRequest(
    @NotNull @Min(1) @Max(32767) Integer productoId,
    @NotNull @DecimalMin("0.01") @Digits(integer=10, fraction=2) BigDecimal montoSolicitado,
    @NotNull @Min(1) @Max(360) Short plazoMeses,
    @NotEmpty @Size(max=5) List<@NotNull @Valid Persona> participantes,
    @NotEmpty @Size(max=5) List<@NotNull @Valid Garantia> inmuebles
) {
    public record Persona(@NotNull @Min(1) Integer clienteId,
        @NotNull @Pattern(regexp="TITULAR|CODEUDOR") String tipoParticipacion) {}
    public record Garantia(
        @NotNull @Pattern(regexp="DEPARTAMENTO|CASA|TERRENO") String tipoInmueble,
        @NotBlank @Size(max=150) String direccion,
        @Size(max=30) String partidaRegistral,
        @NotNull @DecimalMin("0.01") @Digits(integer=10, fraction=2) BigDecimal valorComercial,
        @DecimalMin("0.01") @Digits(integer=10, fraction=2) BigDecimal valorTasacion
    ) {}
    public SolicitudCredito toDomain() {
        return SolicitudCredito.builder()
            .producto(ProductoHipotecario.builder().productoId(productoId.shortValue()).build())
            .montoSolicitado(montoSolicitado).plazoMeses(plazoMeses)
            .participantes(participantes.stream().map(p -> Participante.builder()
                .cliente(Cliente.builder().clienteId(p.clienteId()).build()).tipoParticipacion(p.tipoParticipacion()).build()).toList())
            .inmuebles(inmuebles.stream().map(i -> Inmueble.builder().tipoInmueble(i.tipoInmueble())
                .direccion(i.direccion().trim()).partidaRegistral(i.partidaRegistral())
                .valorComercial(i.valorComercial()).valorTasacion(i.valorTasacion()).build()).toList())
            .build();
    }
}
