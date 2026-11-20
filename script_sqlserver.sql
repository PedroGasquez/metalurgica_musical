IF DB_ID('aula_bd') IS NULL
    CREATE DATABASE aula_bd;
GO

USE aula_bd;
GO

IF OBJECT_ID('tb_musica', 'U') IS NULL
    CREATE TABLE tb_musica (
        codigo    INT IDENTITY(1,1) PRIMARY KEY,
        nome      VARCHAR(100),
        genero    VARCHAR(50),
        autor     VARCHAR(100),
        deezer_id BIGINT NULL,
        link      VARCHAR(500) NULL
    );
GO

-- Remove a coluna da integração anterior com o SoundCloud
IF COL_LENGTH('tb_musica', 'soundcloud_id') IS NOT NULL
    ALTER TABLE tb_musica DROP COLUMN soundcloud_id;
GO

-- Atualiza bancos criados antes da integração com o Deezer
IF COL_LENGTH('tb_musica', 'deezer_id') IS NULL
    ALTER TABLE tb_musica ADD deezer_id BIGINT NULL;
GO

IF COL_LENGTH('tb_musica', 'link') IS NULL
    ALTER TABLE tb_musica ADD link VARCHAR(500) NULL;
GO
