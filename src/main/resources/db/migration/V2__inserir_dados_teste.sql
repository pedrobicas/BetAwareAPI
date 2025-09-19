-- dados de teste para usuarios
INSERT INTO usuario (username, nome, cpf, cep, endereco, senha, email, perfil) VALUES
('admin', 'Administrador', '12345678901', '12345678', 'Rua Exemplo, 123', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'admin@betaware.com', 'ADMIN'),
('usuario1', 'Usuário Teste', '98765432109', '87654321', 'Av Principal, 456', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'usuario@betaware.com', 'USUARIO');

-- dados de teste para apostas
INSERT INTO apostas (categoria, jogo, valor, resultado, data, usuario_id) VALUES
('Futebol', 'Flamengo x Vasco', 100.0, 'GANHOU', '2025-09-15 18:00:00', 2),
('Basquete', 'Lakers x Bulls', 50.0, 'PERDEU', '2025-09-16 20:00:00', 2),
('Futebol', 'Brasil x Argentina', 200.0, 'PENDENTE', '2025-09-20 16:00:00', 2);
