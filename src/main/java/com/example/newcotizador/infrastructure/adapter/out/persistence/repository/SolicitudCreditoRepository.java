package com.example.newcotizador.infrastructure.adapter.out.persistence.repository;
import com.example.newcotizador.infrastructure.adapter.out.persistence.entity.SolicitudCreditoJpaEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
public interface SolicitudCreditoRepository extends JpaRepository<SolicitudCreditoJpaEntity, Integer> {
    @Query("""
        select s from SolicitudCreditoJpaEntity s
        where (:ejecutivoId is null or s.ejecutivo.empleadoId = :ejecutivoId)
        and (:estado = '' or s.estadoActual = :estado)
        and (:query = '' or lower(s.numeroExpediente) like lower(concat('%', :query, '%'))
            or exists (select p from SolicitudClienteJpaEntity p where p.solicitud = s
                and (lower(concat(p.cliente.nombres, ' ', p.cliente.apellidos)) like lower(concat('%', :query, '%'))
                    or p.cliente.numeroDocumento like concat('%', :query, '%'))))
        """)
    Page<SolicitudCreditoJpaEntity> buscar(Integer ejecutivoId, String estado, String query, Pageable page);
}
