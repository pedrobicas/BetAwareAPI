-- tabela de usuários
CREATE TABLE IF NOT EXISTS usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    nome VARCHAR(255) NOT NULL,
    cpf VARCHAR(11) NOT NULL UNIQUE,
    cep VARCHAR(8) NOT NULL,
    endereco VARCHAR(255),
    senha VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    perfil VARCHAR(20) NOT NULL
);

-- tabela de apostas
CREATE TABLE IF NOT EXISTS apostas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    categoria VARCHAR(100) NOT NULL,
    jogo VARCHAR(255) NOT NULL,
    valor DOUBLE NOT NULL,
    resultado VARCHAR(50) NOT NULL,
    data TIMESTAMP NOT NULL,
    usuario_id BIGINT NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);
