package com.techmentor.api.model;

/** O que aconteceu, para o histórico de auditoria. O texto em português é o que o app mostra. */
public enum AcaoAuditada {
    CURSO_CRIADO("Curso criado"),
    CURSO_EDITADO("Dados do curso editados"),
    CURSO_PUBLICADO("Curso publicado"),
    CURSO_ARQUIVADO("Curso arquivado"),
    CURSO_VOLTOU_PARA_RASCUNHO("Curso voltou para rascunho"),
    CURSO_ENVIADO_PARA_REVISAO("Curso enviado para revisão"),
    CURSO_DEVOLVIDO("Curso devolvido com comentário"),
    CURSO_RECUSADO("Curso recusado"),
    MODULO_CRIADO("Módulo criado"),
    MODULO_EDITADO("Módulo editado"),
    MODULO_APAGADO("Módulo apagado"),
    MODULOS_REORDENADOS("Módulos reordenados"),
    AULA_CRIADA("Aula criada"),
    AULA_EDITADA("Aula editada"),
    AULA_APAGADA("Aula apagada"),
    AULAS_REORDENADAS("Aulas reordenadas"),
    PARCERIA_CONVIDADA("Parceira convidada"),
    PARCERIA_CONVITE_REENVIADO("Convite reenviado"),
    PARCERIA_ACEITOU_TERMOS("Convite aceito e termos de parceria aceitos"),
    PARCERIA_VERIFICADA("Parceira verificada"),
    PARCERIA_SUSPENSA("Parceira suspensa"),
    PARCERIA_REATIVADA("Parceira reativada");

    private final String descricao;

    AcaoAuditada(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}
