-- Solo desarrollo: identidades ficticias; los scores no representan una escala oficial.
USE Cotizador;
GO
SET XACT_ABORT ON;
BEGIN TRANSACTION;
INSERT INTO Cotizador.Clientes (dni, nombres, apellidos, score_crediticio, ingresos_mensuales)
SELECT d.dni, d.nombres, d.apellidos, d.score, d.ingresos
FROM (VALUES
    ('99000001', 'Ana Demo', 'Perfil Alto', 850, 12000.00),
    ('99000002', 'Luis Demo', 'Perfil Medio', 650, 6500.00),
    ('99000003', 'Rosa Demo', 'Perfil Bajo', 400, 2800.00),
    ('99000004', 'Pedro Demo', 'Sin Historial', NULL, 5000.00)
) d(dni, nombres, apellidos, score, ingresos)
WHERE NOT EXISTS (SELECT 1 FROM Cotizador.Clientes c WITH (UPDLOCK, HOLDLOCK) WHERE c.dni = d.dni);
COMMIT;
GO
