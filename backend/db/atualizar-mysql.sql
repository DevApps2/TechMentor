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

-- Usuários criados antes da coluna "ativo" ficam com ativo = 0 no MySQL; reativa todos.
-- (Confira antes se existe algum usuário que foi desativado de propósito.)
-- UPDATE usuarios SET ativo = 1;
