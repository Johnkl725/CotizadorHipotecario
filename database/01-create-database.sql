USE master;
GO
IF DB_ID(N'Cotizador') IS NULL
    EXEC(N'CREATE DATABASE [Cotizador]');
GO
