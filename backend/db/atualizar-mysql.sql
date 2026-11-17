-- =====================================================================
-- Atualização de um banco MySQL que já existia antes das mudanças de API completa.
--
-- O Hibernate (ddl-auto=update) cria tabelas e colunas novas sozinho, mas NÃO altera
-- colunas ENUM já existentes. Estes dois ALTER acrescentam os valores novos:
--   - cursos.categoria: SEGURANCA, IA e REDES
--   - denuncias.motivo: COMPORTAMENTO_INADEQUADO
--
-- Rode uma vez no banco "techmentor", com a API já iniciada pelo menos uma vez
-- (para as colunas novas existirem). Banco H2 não precisa disto.
-- =====================================================================

ALTER TABLE cursos
    MODIFY categoria ENUM('CARREIRA','DADOS','DESIGN','OUTROS','PROGRAMACAO','SEGURANCA','IA','REDES') NOT NULL;

ALTER TABLE denuncias
    MODIFY motivo ENUM('ASSEDIO','DISCURSO_DE_ODIO','OUTRO','PERFIL_FALSO','SPAM','COMPORTAMENTO_INADEQUADO') NOT NULL;

-- O Hibernate cria a coluna conquistas.secreta ao iniciar a API atualizada.
-- Marca duas medalhas de descoberta como secretas também em bancos já existentes.
UPDATE conquistas
SET secreta = TRUE
WHERE nome IN ('Primeiro desafio', 'Explorando oportunidades');

-- Usuários criados antes da coluna "ativo" ficam com ativo = 0 no MySQL; reativa todos.
-- (Confira antes se existe algum usuário que foi desativado de propósito.)
-- UPDATE usuarios SET ativo = 1;

-- Colunas novas desta versão. Com ddl-auto=update (desenvolvimento) o Hibernate as cria sozinho.
-- Em produção, com o perfil "prod" (ddl-auto=validate), rode antes de subir a API (só se ainda não existirem):
--   ALTER TABLE usuarios  ADD COLUMN token_versao INT NULL;
--   ALTER TABLE usuarios  ADD COLUMN senha_provisoria BOOLEAN NULL;   -- null conta como "não é provisória"
--   ALTER TABLE denuncias ADD COLUMN avaliacao_id BIGINT NULL,
--                         ADD CONSTRAINT fk_denuncias_avaliacao FOREIGN KEY (avaliacao_id) REFERENCES avaliacoes (id);

-- Vídeos das aulas: o data.sql traz 53 aulas com vídeos do YouTube (Curso em Vídeo). Um banco que já tinha aulas
-- com os links antigos (inventados ou clipes de teste) não é corrigido pelo data.sql, que só insere o que falta.
-- Para recomeçar só o conteúdo dos cursos, apague aulas, módulos, matrículas e progresso e rode o data.sql de novo.

-- Gestão de cursos (instituição, situação, posição única). Com ddl-auto=update (desenvolvimento) o Hibernate cria as
-- colunas e as restrições sozinho. Em produção, com o perfil "prod" (ddl-auto=validate), rode antes de subir a API:
--   ALTER TABLE cursos ADD COLUMN instituicao VARCHAR(120) NULL;
--   ALTER TABLE cursos ADD COLUMN situacao VARCHAR(20) NULL;
--   UPDATE cursos SET situacao = CASE WHEN ativo = 1 THEN 'PUBLICADO' ELSE 'ARQUIVADO' END WHERE situacao IS NULL;
--   ALTER TABLE modulos ADD CONSTRAINT uk_modulo_ordem UNIQUE (curso_id, ordem);
--   ALTER TABLE aulas   ADD CONSTRAINT uk_aula_ordem   UNIQUE (modulo_id, ordem);
-- As duas últimas falham se já existirem duas aulas (ou dois módulos) na mesma posição: corrija as posições antes.
-- A coluna "ativo" continua existindo: é o espelho da situação (ativo = PUBLICADO).

-- Parcerias, revisão de cursos, histórico (auditoria) e notificações com destino.
-- Com ddl-auto=update (desenvolvimento) o Hibernate cria as tabelas parceiras, convites_parceira e auditoria e as colunas novas
-- (notificacoes.tipo/destino/referencia e cursos.parceira_id/comentario_revisao). O que ele NÃO faz é ampliar um ENUM que já
-- existe no MySQL: sem os dois comandos abaixo, criar uma parceira (tipo PARCEIRA) e enviar um curso para revisão (EM_REVISAO) falham.
-- Rode uma vez, com a API já iniciada pelo menos uma vez:
ALTER TABLE usuarios MODIFY tipo ENUM('ALUNA','MENTORA','ADMIN','PARCEIRA') NOT NULL;
ALTER TABLE cursos   MODIFY situacao ENUM('RASCUNHO','EM_REVISAO','PUBLICADO','ARQUIVADO') NULL;
-- (se a coluna cursos.situacao já for VARCHAR(20), o segundo comando continua valendo e não faz mal)

-- Em produção, com o perfil "prod" (ddl-auto=validate), rode antes de subir a API:
--   ALTER TABLE notificacoes ADD COLUMN tipo VARCHAR(30) NULL, ADD COLUMN destino VARCHAR(30) NULL, ADD COLUMN referencia VARCHAR(150) NULL;
--   CREATE TABLE parceiras (
--       id BIGINT AUTO_INCREMENT PRIMARY KEY,
--       usuario_id BIGINT NOT NULL UNIQUE,
--       nome VARCHAR(120) NOT NULL UNIQUE,
--       tipo VARCHAR(20) NOT NULL,
--       documento VARCHAR(14) NULL UNIQUE,
--       site VARCHAR(200) NULL,
--       responsavel VARCHAR(120) NOT NULL,
--       situacao VARCHAR(20) NOT NULL,
--       criada_em DATETIME(6) NOT NULL,
--       termos_versao VARCHAR(20) NULL,
--       termos_aceitos_em DATETIME(6) NULL,
--       site_conferido BIT NOT NULL,
--       dominio_do_email_conferido BIT NOT NULL,
--       identificacao_conferida BIT NOT NULL,
--       verificada_em DATETIME(6) NULL,
--       verificada_por_id BIGINT NULL,
--       observacao_da_verificacao VARCHAR(500) NULL,
--       motivo_da_suspensao VARCHAR(500) NULL,
--       CONSTRAINT fk_parceiras_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
--       CONSTRAINT fk_parceiras_verificada_por FOREIGN KEY (verificada_por_id) REFERENCES usuarios (id)
--   );
--   CREATE TABLE convites_parceira (
--       id BIGINT AUTO_INCREMENT PRIMARY KEY,
--       parceira_id BIGINT NOT NULL,
--       codigo_hash VARCHAR(255) NOT NULL,
--       expira_em DATETIME(6) NOT NULL,
--       criado_em DATETIME(6) NOT NULL,
--       tentativas INT NOT NULL,
--       usado BIT NOT NULL,
--       CONSTRAINT fk_convites_parceira FOREIGN KEY (parceira_id) REFERENCES parceiras (id)
--   );
--   CREATE TABLE auditoria (
--       id BIGINT AUTO_INCREMENT PRIMARY KEY,
--       quando DATETIME(6) NOT NULL,
--       autor_id BIGINT NULL,
--       autor_nome VARCHAR(120) NULL,
--       autor_papel VARCHAR(20) NULL,
--       acao VARCHAR(40) NOT NULL,
--       entidade VARCHAR(20) NOT NULL,
--       entidade_id BIGINT NOT NULL,
--       detalhe VARCHAR(500) NULL
--   );
--   CREATE INDEX ix_auditoria_entidade ON auditoria (entidade, entidade_id);
--   ALTER TABLE cursos ADD COLUMN parceira_id BIGINT NULL, ADD COLUMN comentario_revisao VARCHAR(500) NULL,
--                      ADD CONSTRAINT fk_cursos_parceira FOREIGN KEY (parceira_id) REFERENCES parceiras (id);
