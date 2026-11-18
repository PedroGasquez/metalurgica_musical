IF DB_ID('aula_bd') IS NULL
    CREATE DATABASE aula_bd;
GO

USE aula_bd;
GO

IF OBJECT_ID('tb_musica', 'U') IS NULL
    CREATE TABLE tb_musica (
        codigo INT IDENTITY(1,1) PRIMARY KEY,
        nome   VARCHAR(100),
        genero VARCHAR(50),
        autor  VARCHAR(100)
    );
GO
