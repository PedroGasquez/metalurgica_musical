IF DB_ID('aula_bd') IS NULL
    CREATE DATABASE aula_bd;
GO

USE aula_bd;
GO

IF OBJECT_ID('tb_musica', 'U') IS NULL
    CREATE TABLE tb_musica (
        codigo        INT IDENTITY(1,1) PRIMARY KEY,
        nome          VARCHAR(100),
        genero        VARCHAR(50),
        autor         VARCHAR(100),
        soundcloud_id BIGINT NULL,
        link          VARCHAR(500) NULL
    );
GO

-- Atualiza bancos criados antes da integração com o SoundCloud
IF COL_LENGTH('tb_musica', 'soundcloud_id') IS NULL
    ALTER TABLE tb_musica ADD soundcloud_id BIGINT NULL, link VARCHAR(500) NULL;
GO
