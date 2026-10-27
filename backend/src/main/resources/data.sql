-- =====================================================================
-- Carga inicial de dados de teste do TechMentor (MySQL).
--
-- Idempotente: cada INSERT só grava o que ainda não existe (chave natural:
-- e-mail, título, nome...), então pode ser executado várias vezes sem duplicar
-- e convive com dados que você já cadastrou pela API.
--
-- Todos os usuários abaixo usam a senha de teste: Techmentor@123
-- (o hash BCrypt abaixo foi gerado com BCryptPasswordEncoder, o mesmo da API).
-- =====================================================================

-- ---------------------------------------------------------------------
-- Usuários (1 admin, 3 mentoras, 5 alunas)
-- ---------------------------------------------------------------------
INSERT INTO usuarios (nome, email, senha_hash, tipo, xp_total, nivel, sequencia_dias, ultima_atividade, criado_em, ativo)
SELECT v.nome, v.email, '$2a$10$T1vKvajnbNRTSoUxL6KSeu./ZxACOCfBJYfRRmrCyZklDHuq12p.m', v.tipo,
       v.xp_total, v.nivel, v.sequencia_dias, v.ultima_atividade, '2026-10-01 09:00:00', TRUE
FROM (
    SELECT 'Administradora TechMentor' AS nome, 'admin@techmentor.com' AS email, 'ADMIN' AS tipo, 0 AS xp_total, 1 AS nivel, 0 AS sequencia_dias, NULL AS ultima_atividade
    UNION ALL SELECT 'Maria Silva',      'maria.silva@techmentor.com',   'MENTORA', 0, 1, 0, NULL
    UNION ALL SELECT 'Juliana Costa',    'juliana.costa@techmentor.com', 'MENTORA', 0, 1, 0, NULL
    UNION ALL SELECT 'Fernanda Alves',   'fernanda.alves@techmentor.com','MENTORA', 0, 1, 0, NULL
    UNION ALL SELECT 'Beatriz Santos',   'beatriz.santos@email.com',     'ALUNA', 140, 2, 3, '2026-10-25'
    UNION ALL SELECT 'Camila Rocha',     'camila.rocha@email.com',       'ALUNA',  90, 1, 2, '2026-10-24'
    UNION ALL SELECT 'Daniela Martins',  'daniela.martins@email.com',    'ALUNA', 360, 3, 5, '2026-10-25'
    UNION ALL SELECT 'Elisa Ferreira',   'elisa.ferreira@email.com',     'ALUNA',   0, 1, 0, NULL
    UNION ALL SELECT 'Perfil Suspeito',  'suspeito@email.com',           'ALUNA',   0, 1, 0, NULL
) v
WHERE NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.email = v.email);

-- ---------------------------------------------------------------------
-- Áreas de atuação, perfis de mentora e vínculo mentora x área
-- ---------------------------------------------------------------------
INSERT INTO areas_atuacao (nome)
SELECT v.nome
FROM (
    SELECT 'Back-end' AS nome
    UNION ALL SELECT 'Front-end'
    UNION ALL SELECT 'Dados'
    UNION ALL SELECT 'UX/UI'
    UNION ALL SELECT 'Carreira'
    UNION ALL SELECT 'DevOps'
) v
WHERE NOT EXISTS (SELECT 1 FROM areas_atuacao a WHERE a.nome = v.nome);

INSERT INTO perfis_mentora (usuario_id, bio, cargo, empresa, anos_experiencia, nota_media, total_avaliacoes)
SELECT u.id, v.bio, v.cargo, v.empresa, v.anos, v.nota, v.total
FROM (
    SELECT 'maria.silva@techmentor.com' AS email,
           'Engenheira de software apaixonada por ensinar e por ajudar mulheres a entrarem em tecnologia.' AS bio,
           'Engenheira de Software Sênior' AS cargo, 'Acme Tecnologia' AS empresa, 8 AS anos, 5.0 AS nota, 1 AS total
    UNION ALL SELECT 'juliana.costa@techmentor.com',
           'Cientista de dados com foco em análise exploratória, machine learning e storytelling com dados.',
           'Cientista de Dados', 'DataWorks', 6, 0.0, 0
    UNION ALL SELECT 'fernanda.alves@techmentor.com',
           'Designer de produto com experiência em pesquisa com usuárias, design systems e prototipação.',
           'Designer de Produto Sênior', 'Pixel Studio', 10, 0.0, 0
) v
JOIN usuarios u ON u.email = v.email
WHERE NOT EXISTS (SELECT 1 FROM perfis_mentora p WHERE p.usuario_id = u.id);

INSERT INTO mentora_areas (perfil_mentora_id, area_id)
SELECT p.id, a.id
FROM (
    SELECT 'maria.silva@techmentor.com' AS email, 'Back-end' AS area
    UNION ALL SELECT 'maria.silva@techmentor.com',    'Carreira'
    UNION ALL SELECT 'juliana.costa@techmentor.com',  'Dados'
    UNION ALL SELECT 'juliana.costa@techmentor.com',  'Carreira'
    UNION ALL SELECT 'fernanda.alves@techmentor.com', 'UX/UI'
    UNION ALL SELECT 'fernanda.alves@techmentor.com', 'Front-end'
) v
JOIN usuarios u ON u.email = v.email
JOIN perfis_mentora p ON p.usuario_id = u.id
JOIN areas_atuacao a ON a.nome = v.area
WHERE NOT EXISTS (SELECT 1 FROM mentora_areas ma WHERE ma.perfil_mentora_id = p.id AND ma.area_id = a.id);

-- ---------------------------------------------------------------------
-- Horários de disponibilidade, mentorias e avaliação
-- ---------------------------------------------------------------------
INSERT INTO horarios_disponiveis (mentora_id, inicio, fim, reservado)
SELECT p.id, v.inicio, v.fim, v.reservado
FROM (
    SELECT 'maria.silva@techmentor.com' AS email, '2026-10-20 14:00:00' AS inicio, '2026-10-20 15:00:00' AS fim, 1 AS reservado
    UNION ALL SELECT 'maria.silva@techmentor.com',    '2026-11-10 14:00:00', '2026-11-10 15:00:00', 1
    UNION ALL SELECT 'maria.silva@techmentor.com',    '2026-11-12 10:00:00', '2026-11-12 11:00:00', 0
    UNION ALL SELECT 'juliana.costa@techmentor.com',  '2026-11-11 15:00:00', '2026-11-11 16:00:00', 0
    UNION ALL SELECT 'juliana.costa@techmentor.com',  '2026-11-13 09:00:00', '2026-11-13 10:00:00', 0
    UNION ALL SELECT 'fernanda.alves@techmentor.com', '2026-11-10 10:00:00', '2026-11-10 11:00:00', 0
    UNION ALL SELECT 'fernanda.alves@techmentor.com', '2026-11-14 16:00:00', '2026-11-14 17:00:00', 1
) v
JOIN usuarios u ON u.email = v.email
JOIN perfis_mentora p ON p.usuario_id = u.id
WHERE NOT EXISTS (SELECT 1 FROM horarios_disponiveis h WHERE h.mentora_id = p.id AND h.inicio = v.inicio);

INSERT INTO mentorias (aluna_id, horario_id, status, assunto, criada_em)
SELECT al.id, h.id, v.status, v.assunto, v.criada_em
FROM (
    SELECT 'beatriz.santos@email.com' AS aluna, 'maria.silva@techmentor.com' AS mentora, '2026-10-20 14:00:00' AS inicio,
           'CONCLUIDA' AS status, 'Como me preparar para a primeira vaga em back-end' AS assunto, '2026-10-15 10:00:00' AS criada_em
    UNION ALL SELECT 'camila.rocha@email.com',    'maria.silva@techmentor.com',    '2026-11-10 14:00:00',
           'AGENDADA',   'Dúvidas sobre transição de carreira para tecnologia', '2026-10-24 16:30:00'
    UNION ALL SELECT 'daniela.martins@email.com', 'fernanda.alves@techmentor.com', '2026-11-14 16:00:00',
           'CONFIRMADA', 'Revisão do meu portfólio de UX', '2026-10-25 11:15:00'
) v
JOIN usuarios al ON al.email = v.aluna
JOIN usuarios um ON um.email = v.mentora
JOIN perfis_mentora p ON p.usuario_id = um.id
JOIN horarios_disponiveis h ON h.mentora_id = p.id AND h.inicio = v.inicio
WHERE NOT EXISTS (SELECT 1 FROM mentorias m WHERE m.horario_id = h.id AND m.aluna_id = al.id);

INSERT INTO avaliacoes_mentoria (mentoria_id, nota, comentario, criada_em)
SELECT m.id, 5, 'A Maria foi super atenciosa e me deu um plano de estudos claro. Recomendo muito!', '2026-10-20 16:00:00'
FROM mentorias m
JOIN usuarios al ON al.id = m.aluna_id AND al.email = 'beatriz.santos@email.com'
JOIN horarios_disponiveis h ON h.id = m.horario_id AND h.inicio = '2026-10-20 14:00:00'
WHERE NOT EXISTS (SELECT 1 FROM avaliacoes_mentoria a WHERE a.mentoria_id = m.id);

-- ---------------------------------------------------------------------
-- Cursos, módulos e aulas ("Java para iniciantes" reaproveita o que já existir)
-- ---------------------------------------------------------------------
INSERT INTO cursos (titulo, descricao, categoria, icone_url, ativo)
SELECT v.titulo, v.descricao, v.categoria, v.icone_url, 1
FROM (
    SELECT 'Java para iniciantes' AS titulo, 'Aprenda os fundamentos de Java do zero.' AS descricao,
           'PROGRAMACAO' AS categoria, 'https://cdn.techmentor.com/icones/java.png' AS icone_url
    UNION ALL SELECT 'Python para Análise de Dados', 'Manipule e visualize dados com Python, pandas e matplotlib.',
           'DADOS', 'https://cdn.techmentor.com/icones/python.png'
    UNION ALL SELECT 'Fundamentos de UX Design', 'Pesquisa com usuárias, wireframes e prototipação de interfaces.',
           'DESIGN', 'https://cdn.techmentor.com/icones/ux.png'
    UNION ALL SELECT 'Carreira em Tecnologia', 'Currículo, LinkedIn, entrevistas e planejamento de carreira.',
           'CARREIRA', 'https://cdn.techmentor.com/icones/carreira.png'
) v
WHERE NOT EXISTS (SELECT 1 FROM cursos c WHERE c.titulo = v.titulo);

INSERT INTO modulos (curso_id, titulo, ordem)
SELECT c.id, v.titulo, v.ordem
FROM (
    SELECT 'Java para iniciantes' AS curso, 'Introdução ao Java' AS titulo, 1 AS ordem
    UNION ALL SELECT 'Java para iniciantes',           'Orientação a Objetos', 2
    UNION ALL SELECT 'Python para Análise de Dados',   'Primeiros passos com Python', 1
    UNION ALL SELECT 'Python para Análise de Dados',   'Análise com pandas', 2
    UNION ALL SELECT 'Fundamentos de UX Design',       'Entendendo a usuária', 1
    UNION ALL SELECT 'Fundamentos de UX Design',       'Do wireframe ao protótipo', 2
    UNION ALL SELECT 'Carreira em Tecnologia',         'Currículo e LinkedIn', 1
    UNION ALL SELECT 'Carreira em Tecnologia',         'Entrevistas técnicas e comportamentais', 2
) v
JOIN cursos c ON c.titulo = v.curso
WHERE NOT EXISTS (SELECT 1 FROM modulos m WHERE m.curso_id = c.id AND m.ordem = v.ordem);

INSERT INTO aulas (modulo_id, titulo, ordem, duracao_minutos, xp_recompensa, conteudo_url)
SELECT m.id, v.titulo, v.ordem, v.duracao, 20, v.url
FROM (
    SELECT 'Java para iniciantes' AS curso, 1 AS modulo, 'O que é Java e como instalar' AS titulo, 1 AS ordem, 15 AS duracao, 'https://cdn.techmentor.com/aulas/java-01.mp4' AS url
    UNION ALL SELECT 'Java para iniciantes', 1, 'Variáveis e tipos de dados',       2, 20, 'https://cdn.techmentor.com/aulas/java-02.mp4'
    UNION ALL SELECT 'Java para iniciantes', 2, 'Classes e objetos',                1, 25, 'https://cdn.techmentor.com/aulas/java-03.mp4'
    UNION ALL SELECT 'Java para iniciantes', 2, 'Herança e polimorfismo',           2, 30, 'https://cdn.techmentor.com/aulas/java-04.mp4'
    UNION ALL SELECT 'Python para Análise de Dados', 1, 'Instalando o ambiente Python',   1, 12, 'https://cdn.techmentor.com/aulas/python-01.mp4'
    UNION ALL SELECT 'Python para Análise de Dados', 1, 'Listas, dicionários e funções',  2, 22, 'https://cdn.techmentor.com/aulas/python-02.mp4'
    UNION ALL SELECT 'Python para Análise de Dados', 2, 'Lendo arquivos CSV com pandas',  1, 20, 'https://cdn.techmentor.com/aulas/python-03.mp4'
    UNION ALL SELECT 'Python para Análise de Dados', 2, 'Limpeza e visualização de dados',2, 28, 'https://cdn.techmentor.com/aulas/python-04.mp4'
    UNION ALL SELECT 'Fundamentos de UX Design', 1, 'O que é UX e por que importa',     1, 10, 'https://cdn.techmentor.com/aulas/ux-01.mp4'
    UNION ALL SELECT 'Fundamentos de UX Design', 1, 'Entrevistas e personas',           2, 18, 'https://cdn.techmentor.com/aulas/ux-02.mp4'
    UNION ALL SELECT 'Fundamentos de UX Design', 2, 'Wireframes de baixa fidelidade',   1, 20, 'https://cdn.techmentor.com/aulas/ux-03.mp4'
    UNION ALL SELECT 'Fundamentos de UX Design', 2, 'Protótipos navegáveis no Figma',   2, 25, 'https://cdn.techmentor.com/aulas/ux-04.mp4'
    UNION ALL SELECT 'Carreira em Tecnologia', 1, 'Montando um currículo que se destaca', 1, 15, 'https://cdn.techmentor.com/aulas/carreira-01.mp4'
    UNION ALL SELECT 'Carreira em Tecnologia', 1, 'Otimizando seu LinkedIn',              2, 12, 'https://cdn.techmentor.com/aulas/carreira-02.mp4'
    UNION ALL SELECT 'Carreira em Tecnologia', 2, 'Como se preparar para entrevistas técnicas', 1, 22, 'https://cdn.techmentor.com/aulas/carreira-03.mp4'
    UNION ALL SELECT 'Carreira em Tecnologia', 2, 'Perguntas comportamentais',           2, 18, 'https://cdn.techmentor.com/aulas/carreira-04.mp4'
) v
JOIN cursos c ON c.titulo = v.curso
JOIN modulos m ON m.curso_id = c.id AND m.ordem = v.modulo
WHERE NOT EXISTS (SELECT 1 FROM aulas a WHERE a.modulo_id = m.id AND a.ordem = v.ordem);

-- ---------------------------------------------------------------------
-- Matrículas e progresso nas aulas
-- ---------------------------------------------------------------------
INSERT INTO matriculas (usuario_id, curso_id, matriculada_em, concluido_em)
SELECT u.id, c.id, v.matriculada_em, v.concluido_em
FROM (
    SELECT 'beatriz.santos@email.com' AS email, 'Java para iniciantes' AS curso, '2026-10-18 10:00:00' AS matriculada_em, NULL AS concluido_em
    UNION ALL SELECT 'camila.rocha@email.com',    'Java para iniciantes',         '2026-10-19 11:00:00', NULL
    UNION ALL SELECT 'camila.rocha@email.com',    'Python para Análise de Dados', '2026-10-21 09:30:00', NULL
    UNION ALL SELECT 'daniela.martins@email.com', 'Java para iniciantes',         '2026-10-19 08:00:00', '2026-10-24 21:00:00'
    UNION ALL SELECT 'daniela.martins@email.com', 'Fundamentos de UX Design',     '2026-10-24 21:30:00', NULL
    UNION ALL SELECT 'elisa.ferreira@email.com',  'Carreira em Tecnologia',       '2026-10-25 15:00:00', NULL
) v
JOIN usuarios u ON u.email = v.email
JOIN cursos c ON c.titulo = v.curso
WHERE NOT EXISTS (SELECT 1 FROM matriculas m WHERE m.usuario_id = u.id AND m.curso_id = c.id);

INSERT INTO progresso_aulas (usuario_id, aula_id, concluida_em)
SELECT u.id, a.id, v.concluida_em
FROM (
    SELECT 'beatriz.santos@email.com' AS email, 'Java para iniciantes' AS curso, 1 AS modulo, 1 AS aula, '2026-10-21 10:00:00' AS concluida_em
    UNION ALL SELECT 'beatriz.santos@email.com', 'Java para iniciantes', 1, 2, '2026-10-23 10:00:00'
    UNION ALL SELECT 'beatriz.santos@email.com', 'Java para iniciantes', 2, 1, '2026-10-25 10:00:00'
    UNION ALL SELECT 'camila.rocha@email.com',   'Java para iniciantes', 1, 1, '2026-10-22 19:00:00'
    UNION ALL SELECT 'camila.rocha@email.com',   'Python para Análise de Dados', 1, 1, '2026-10-24 19:00:00'
    UNION ALL SELECT 'daniela.martins@email.com','Java para iniciantes', 1, 1, '2026-10-20 08:30:00'
    UNION ALL SELECT 'daniela.martins@email.com','Java para iniciantes', 1, 2, '2026-10-21 08:30:00'
    UNION ALL SELECT 'daniela.martins@email.com','Java para iniciantes', 2, 1, '2026-10-23 08:30:00'
    UNION ALL SELECT 'daniela.martins@email.com','Java para iniciantes', 2, 2, '2026-10-24 20:30:00'
) v
JOIN usuarios u ON u.email = v.email
JOIN cursos c ON c.titulo = v.curso
JOIN modulos m ON m.curso_id = c.id AND m.ordem = v.modulo
JOIN aulas a ON a.modulo_id = m.id AND a.ordem = v.aula
WHERE NOT EXISTS (SELECT 1 FROM progresso_aulas p WHERE p.usuario_id = u.id AND p.aula_id = a.id);

-- ---------------------------------------------------------------------
-- Gamificação: conquistas, desafios, participações, histórico de XP
-- ---------------------------------------------------------------------
INSERT INTO conquistas (nome, descricao, icone_url, xp_bonus)
SELECT v.nome, v.descricao, v.icone_url, v.xp_bonus
FROM (
    SELECT 'Primeira aula' AS nome, 'Concluiu a primeira aula na plataforma.' AS descricao,
           'https://cdn.techmentor.com/conquistas/primeira-aula.png' AS icone_url, 50 AS xp_bonus
    UNION ALL SELECT 'Sequência de 3 dias', 'Estudou por 3 dias seguidos.',
           'https://cdn.techmentor.com/conquistas/sequencia-3.png', 30
    UNION ALL SELECT 'Curso concluído', 'Concluiu todas as aulas de um curso.',
           'https://cdn.techmentor.com/conquistas/curso-concluido.png', 100
    UNION ALL SELECT 'Mentoria agendada', 'Agendou a primeira mentoria.',
           'https://cdn.techmentor.com/conquistas/mentoria.png', 20
    UNION ALL SELECT 'Explorando oportunidades', 'Candidatou-se à primeira oportunidade.',
           'https://cdn.techmentor.com/conquistas/oportunidade.png', 20
) v
WHERE NOT EXISTS (SELECT 1 FROM conquistas c WHERE c.nome = v.nome);

INSERT INTO usuario_conquistas (usuario_id, conquista_id, desbloqueada_em)
SELECT u.id, c.id, v.desbloqueada_em
FROM (
    SELECT 'beatriz.santos@email.com' AS email, 'Primeira aula' AS conquista, '2026-10-21 10:00:00' AS desbloqueada_em
    UNION ALL SELECT 'beatriz.santos@email.com',  'Sequência de 3 dias', '2026-10-25 10:00:00'
    UNION ALL SELECT 'camila.rocha@email.com',    'Primeira aula',       '2026-10-22 19:00:00'
    UNION ALL SELECT 'daniela.martins@email.com', 'Primeira aula',       '2026-10-20 08:30:00'
    UNION ALL SELECT 'daniela.martins@email.com', 'Sequência de 3 dias', '2026-10-22 08:30:00'
    UNION ALL SELECT 'daniela.martins@email.com', 'Curso concluído',     '2026-10-24 21:00:00'
) v
JOIN usuarios u ON u.email = v.email
JOIN conquistas c ON c.nome = v.conquista
WHERE NOT EXISTS (SELECT 1 FROM usuario_conquistas uc WHERE uc.usuario_id = u.id AND uc.conquista_id = c.id);

INSERT INTO desafios (titulo, descricao, xp_recompensa, inicio, fim, meta_aulas)
SELECT v.titulo, v.descricao, v.xp, v.inicio, v.fim, v.meta
FROM (
    SELECT 'Primeiros passos em Java' AS titulo, 'Conclua 4 aulas de Java até o fim do mês.' AS descricao,
           100 AS xp, '2026-10-19' AS inicio, '2026-10-31' AS fim, 4 AS meta
    UNION ALL SELECT 'Semana de Dados', 'Conclua 3 aulas de Python para Análise de Dados em uma semana.',
           80, '2026-11-09', '2026-11-15', 3
    UNION ALL SELECT 'Desafio UX 5 dias', 'Conclua 2 aulas de UX Design antes do fim do mês.',
           60, '2026-10-20', '2026-10-31', 2
) v
WHERE NOT EXISTS (SELECT 1 FROM desafios d WHERE d.titulo = v.titulo);

INSERT INTO usuario_desafios (usuario_id, desafio_id, status, concluido_em)
SELECT u.id, d.id, v.status, v.concluido_em
FROM (
    SELECT 'daniela.martins@email.com' AS email, 'Primeiros passos em Java' AS desafio, 'CONCLUIDO' AS status, '2026-10-24 20:30:00' AS concluido_em
    UNION ALL SELECT 'beatriz.santos@email.com', 'Primeiros passos em Java', 'EM_ANDAMENTO', NULL
    UNION ALL SELECT 'daniela.martins@email.com', 'Desafio UX 5 dias',       'EM_ANDAMENTO', NULL
) v
JOIN usuarios u ON u.email = v.email
JOIN desafios d ON d.titulo = v.desafio
WHERE NOT EXISTS (SELECT 1 FROM usuario_desafios ud WHERE ud.usuario_id = u.id AND ud.desafio_id = d.id);

INSERT INTO historico_xp (usuario_id, pontos, motivo, criado_em)
SELECT u.id, v.pontos, v.motivo, v.criado_em
FROM (
    SELECT 'beatriz.santos@email.com' AS email, 20 AS pontos, 'Aula concluída: O que é Java e como instalar' AS motivo, '2026-10-21 10:00:00' AS criado_em
    UNION ALL SELECT 'beatriz.santos@email.com', 50, 'Conquista desbloqueada: Primeira aula',             '2026-10-21 10:00:00'
    UNION ALL SELECT 'beatriz.santos@email.com', 20, 'Aula concluída: Variáveis e tipos de dados',        '2026-10-23 10:00:00'
    UNION ALL SELECT 'beatriz.santos@email.com', 20, 'Aula concluída: Classes e objetos',                 '2026-10-25 10:00:00'
    UNION ALL SELECT 'beatriz.santos@email.com', 30, 'Conquista desbloqueada: Sequência de 3 dias',       '2026-10-25 10:00:00'
    UNION ALL SELECT 'camila.rocha@email.com',   20, 'Aula concluída: O que é Java e como instalar',      '2026-10-22 19:00:00'
    UNION ALL SELECT 'camila.rocha@email.com',   50, 'Conquista desbloqueada: Primeira aula',             '2026-10-22 19:00:00'
    UNION ALL SELECT 'camila.rocha@email.com',   20, 'Aula concluída: Instalando o ambiente Python',      '2026-10-24 19:00:00'
    UNION ALL SELECT 'daniela.martins@email.com', 20, 'Aula concluída: O que é Java e como instalar',     '2026-10-20 08:30:00'
    UNION ALL SELECT 'daniela.martins@email.com', 50, 'Conquista desbloqueada: Primeira aula',            '2026-10-20 08:30:00'
    UNION ALL SELECT 'daniela.martins@email.com', 20, 'Aula concluída: Variáveis e tipos de dados',       '2026-10-21 08:30:00'
    UNION ALL SELECT 'daniela.martins@email.com', 30, 'Conquista desbloqueada: Sequência de 3 dias',      '2026-10-22 08:30:00'
    UNION ALL SELECT 'daniela.martins@email.com', 20, 'Aula concluída: Classes e objetos',                '2026-10-23 08:30:00'
    UNION ALL SELECT 'daniela.martins@email.com', 20, 'Aula concluída: Herança e polimorfismo',           '2026-10-24 20:30:00'
    UNION ALL SELECT 'daniela.martins@email.com',100, 'Desafio concluído: Primeiros passos em Java',      '2026-10-24 20:30:00'
    UNION ALL SELECT 'daniela.martins@email.com',100, 'Conquista desbloqueada: Curso concluído',          '2026-10-24 21:00:00'
) v
JOIN usuarios u ON u.email = v.email
WHERE NOT EXISTS (SELECT 1 FROM historico_xp h WHERE h.usuario_id = u.id AND h.motivo = v.motivo);

INSERT INTO notificacoes (usuario_id, titulo, mensagem, lida, criada_em)
SELECT u.id, v.titulo, v.mensagem, v.lida, v.criada_em
FROM (
    SELECT 'beatriz.santos@email.com' AS email, 'Nova conquista!' AS titulo, 'Você desbloqueou a conquista Sequência de 3 dias.' AS mensagem, 0 AS lida, '2026-10-25 10:00:00' AS criada_em
    UNION ALL SELECT 'beatriz.santos@email.com',  'Mentoria concluída',  'Que tal avaliar sua mentoria com a Maria Silva?',                      1, '2026-10-20 15:30:00'
    UNION ALL SELECT 'camila.rocha@email.com',    'Mentoria agendada',   'Sua mentoria com Maria Silva está marcada para 10/11 às 14h.',          0, '2026-10-24 16:30:00'
    UNION ALL SELECT 'daniela.martins@email.com', 'Curso concluído!',    'Parabéns! Você concluiu o curso Java para iniciantes.',                 1, '2026-10-24 21:00:00'
    UNION ALL SELECT 'daniela.martins@email.com', 'Mentoria confirmada', 'Sua mentoria com Fernanda Alves foi confirmada para 14/11 às 16h.',     0, '2026-10-25 12:00:00'
    UNION ALL SELECT 'elisa.ferreira@email.com',  'Bem-vinda ao TechMentor', 'Explore os cursos e comece sua jornada em tecnologia.',              0, '2026-10-25 15:00:00'
) v
JOIN usuarios u ON u.email = v.email
WHERE NOT EXISTS (SELECT 1 FROM notificacoes n WHERE n.usuario_id = u.id AND n.titulo = v.titulo AND n.criada_em = v.criada_em);

-- ---------------------------------------------------------------------
-- Oportunidades e inscrições/candidaturas
-- ---------------------------------------------------------------------
INSERT INTO oportunidades (tipo, titulo, organizacao, descricao, requisitos, localidade, link_externo, prazo_inscricao, ativa)
SELECT v.tipo, v.titulo, v.organizacao, v.descricao, v.requisitos, v.localidade, v.link, v.prazo, 1
FROM (
    SELECT 'ESTAGIO' AS tipo, 'Estágio em Desenvolvimento Java' AS titulo, 'Acme Tecnologia' AS organizacao,
           'Atuação no time de back-end com Java e Spring Boot.' AS descricao,
           'Cursando Ciência da Computação ou áreas afins.' AS requisitos,
           'São Paulo - SP (híbrido)' AS localidade, 'https://acme.com/carreiras/estagio-java' AS link, '2026-12-15' AS prazo
    UNION ALL SELECT 'EMPREGO', 'Analista de Dados Júnior', 'DataWorks',
           'Construção de dashboards e análises para o time de produto.',
           'Conhecimento de SQL e Python; vivência com BI é um diferencial.',
           'Remoto', 'https://dataworks.com/vagas/analista-dados-jr', '2026-11-30'
    UNION ALL SELECT 'BOLSA', 'Bolsa Mulheres em Tecnologia', 'Instituto Código Feminino',
           'Bolsa integral para formação em desenvolvimento de software com mentoria.',
           'Ser mulher, maior de 18 anos e estar fora do mercado de tecnologia.',
           'Online', 'https://codigofeminino.org/bolsas', '2026-12-01'
    UNION ALL SELECT 'EVENTO', 'TechMentor Day 2026', 'TechMentor',
           'Evento online com palestras, painéis e rodadas de mentoria relâmpago.',
           'Aberto ao público; inscrição gratuita.',
           'Online', 'https://techmentor.com/eventos/techmentor-day-2026', '2026-11-20'
    UNION ALL SELECT 'EVENTO', 'Workshop de Portfólio em UX', 'Pixel Studio',
           'Workshop prático para montar e apresentar seu portfólio de UX.',
           'Interesse em design de produto.',
           'Rio de Janeiro - RJ', 'https://pixelstudio.com/workshops/portfolio-ux', '2026-11-25'
) v
WHERE NOT EXISTS (SELECT 1 FROM oportunidades o WHERE o.titulo = v.titulo);

INSERT INTO inscricoes_oportunidade (usuario_id, oportunidade_id, status, criada_em)
SELECT u.id, o.id, v.status, v.criada_em
FROM (
    SELECT 'beatriz.santos@email.com' AS email, 'Estágio em Desenvolvimento Java' AS oportunidade, 'ENVIADA' AS status, '2026-10-25 11:00:00' AS criada_em
    UNION ALL SELECT 'camila.rocha@email.com',    'Bolsa Mulheres em Tecnologia', 'EM_ANALISE', '2026-10-23 14:00:00'
    UNION ALL SELECT 'daniela.martins@email.com', 'Analista de Dados Júnior',     'RECUSADA',   '2026-10-10 09:00:00'
    UNION ALL SELECT 'elisa.ferreira@email.com',  'TechMentor Day 2026',          'APROVADA',   '2026-10-25 16:00:00'
    UNION ALL SELECT 'daniela.martins@email.com', 'Workshop de Portfólio em UX',  'APROVADA',   '2026-10-25 12:30:00'
) v
JOIN usuarios u ON u.email = v.email
JOIN oportunidades o ON o.titulo = v.oportunidade
WHERE NOT EXISTS (SELECT 1 FROM inscricoes_oportunidade i WHERE i.usuario_id = u.id AND i.oportunidade_id = o.id);

-- ---------------------------------------------------------------------
-- Segurança: denúncias, bloqueios e conteúdos de orientação / canais de ajuda
-- ---------------------------------------------------------------------
INSERT INTO denuncias (denunciante_id, denunciado_id, motivo, descricao, status, criada_em)
SELECT du.id, dd.id, v.motivo, v.descricao, v.status, v.criada_em
FROM (
    SELECT 'beatriz.santos@email.com' AS denunciante, 'suspeito@email.com' AS denunciado, 'PERFIL_FALSO' AS motivo,
           'Perfil usa foto e nome de outra pessoa.' AS descricao, 'EM_ANALISE' AS status, '2026-10-22 18:00:00' AS criada_em
    UNION ALL SELECT 'camila.rocha@email.com', 'suspeito@email.com', 'ASSEDIO',
           'Enviou mensagens insistentes e ofensivas após eu recusar uma conversa.', 'ABERTA', '2026-10-24 20:00:00'
) v
JOIN usuarios du ON du.email = v.denunciante
JOIN usuarios dd ON dd.email = v.denunciado
WHERE NOT EXISTS (SELECT 1 FROM denuncias d WHERE d.denunciante_id = du.id AND d.denunciado_id = dd.id AND d.motivo = v.motivo);

-- Denúncia geral (sem usuário denunciado)
INSERT INTO denuncias (denunciante_id, denunciado_id, motivo, descricao, status, criada_em)
SELECT u.id, NULL, 'OUTRO', 'Encontrei um link quebrado na página de oportunidades.', 'RESOLVIDA', '2026-10-18 13:00:00'
FROM usuarios u
WHERE u.email = 'elisa.ferreira@email.com'
  AND NOT EXISTS (SELECT 1 FROM denuncias d WHERE d.denunciante_id = u.id AND d.denunciado_id IS NULL AND d.motivo = 'OUTRO');

INSERT INTO bloqueios_usuario (bloqueador_id, bloqueado_id, criado_em)
SELECT b.id, s.id, v.criado_em
FROM (
    SELECT 'beatriz.santos@email.com' AS bloqueador, 'suspeito@email.com' AS bloqueado, '2026-10-22 18:05:00' AS criado_em
    UNION ALL SELECT 'camila.rocha@email.com', 'suspeito@email.com', '2026-10-24 20:05:00'
) v
JOIN usuarios b ON b.email = v.bloqueador
JOIN usuarios s ON s.email = v.bloqueado
WHERE NOT EXISTS (SELECT 1 FROM bloqueios_usuario x WHERE x.bloqueador_id = b.id AND x.bloqueado_id = s.id);

INSERT INTO conteudos_seguranca (tipo, titulo, descricao, contato)
SELECT v.tipo, v.titulo, v.descricao, v.contato
FROM (
    SELECT 'ORIENTACAO' AS tipo, 'Como identificar um perfil falso' AS titulo,
           'Desconfie de perfis sem histórico, com fotos genéricas ou que pedem dinheiro e dados pessoais. Denuncie pelo próprio app.' AS descricao,
           NULL AS contato
    UNION ALL SELECT 'ORIENTACAO', 'O que fazer ao sofrer assédio online',
           'Não responda, registre prints das conversas, bloqueie o perfil e faça a denúncia na plataforma. Procure apoio de pessoas de confiança.', NULL
    UNION ALL SELECT 'CANAL_AJUDA', 'Central de Atendimento à Mulher',
           'Atendimento gratuito, 24 horas, para orientar e acolher mulheres em situação de violência.', 'Ligue 180'
    UNION ALL SELECT 'CANAL_AJUDA', 'Centro de Valorização da Vida (CVV)',
           'Apoio emocional e prevenção do suicídio, gratuito e 24 horas.', 'Ligue 188'
) v
WHERE NOT EXISTS (SELECT 1 FROM conteudos_seguranca c WHERE c.titulo = v.titulo);
