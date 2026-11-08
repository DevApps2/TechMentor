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
--   ALTER TABLE denuncias ADD COLUMN avaliacao_id BIGINT NULL,
--                         ADD CONSTRAINT fk_denuncias_avaliacao FOREIGN KEY (avaliacao_id) REFERENCES avaliacoes (id);
