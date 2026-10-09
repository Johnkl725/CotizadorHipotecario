USE Cotizador;
GO
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF NOT EXISTS (SELECT 1 FROM Cotizador.producto_hipotecario WHERE nombre_producto='Hipotecario Tradicional')
    INSERT INTO Cotizador.producto_hipotecario(nombre_producto,tasa_interes_referencial) VALUES('Hipotecario Tradicional',9.00);
IF NOT EXISTS (SELECT 1 FROM Cotizador.producto_hipotecario WHERE nombre_producto='MiVivienda Verde')
    INSERT INTO Cotizador.producto_hipotecario(nombre_producto,tasa_interes_referencial) VALUES('MiVivienda Verde',7.50);
IF NOT EXISTS (SELECT 1 FROM Cotizador.producto_hipotecario WHERE nombre_producto='Hipotecario Joven')
    INSERT INTO Cotizador.producto_hipotecario(nombre_producto,tasa_interes_referencial) VALUES('Hipotecario Joven',8.25);
-- Synthetic demonstration records. Existing customer data is never overwritten.
IF NOT EXISTS (SELECT 1 FROM Cotizador.cliente WHERE tipo_documento='DNI' AND numero_documento='70000001')
    INSERT INTO Cotizador.cliente(tipo_documento,numero_documento,nombres,apellidos,ingreso_mensual_neto)
    VALUES('DNI','70000001','Juan','Perez',4500.00);
IF NOT EXISTS (SELECT 1 FROM Cotizador.cliente WHERE tipo_documento='DNI' AND numero_documento='70000002')
    INSERT INTO Cotizador.cliente(tipo_documento,numero_documento,nombres,apellidos,ingreso_mensual_neto)
    VALUES('DNI','70000002','Maria','Gomez',3200.00);
COMMIT;
GO
