package com.techmentor.api.service;

/**
 * Os Termos de Parceria. A VERSAO é gravada junto com o aceite de cada parceira: quando o texto mudar,
 * suba a versão (as já aceitas continuam valendo para a versão em que aceitaram).
 *
 * ATENÇÃO: este é um texto-base. Antes de convidar parceiras de verdade, peça a revisão de quem cuida do jurídico do projeto.
 */
public final class TermosDeParceria {
    private TermosDeParceria() { }

    public static final String VERSAO = "2026-11-14";

    public static final String TITULO = "Termos de Parceria TechMentor";

    public static final String TEXTO = """
            1. Conteúdo. A parceira declara ter o direito de usar e divulgar todo o conteúdo que cadastrar (vídeos, textos e imagens) e responde por ele, inclusive por direitos autorais.

            2. Vídeos. Os vídeos ficam hospedados onde a parceira os publicou (por exemplo, YouTube); o TechMentor guarda apenas o link. Se o vídeo sair do ar, a aula deixa de funcionar.

            3. Revisão. Todo curso novo é analisado pela equipe do TechMentor antes de aparecer para as alunas. O TechMentor pode devolver, recusar, arquivar ou tirar do ar qualquer curso que não respeite estes termos ou a segurança das alunas.

            4. Edições. Depois de publicado, o curso pode ser corrigido pela parceira. Toda alteração fica registrada no histórico, com a data e quem a fez.

            5. Alunas. A parceira recebe apenas números agregados sobre o curso (quantas alunas começaram, concluíram e onde pararam). Nenhuma aluna é identificada, e a parceira não deve tentar contatá-las pelo aplicativo.

            6. Crédito. O nome informado no cadastro aparece como crédito do curso para as alunas.

            7. Suspensão. O TechMentor pode suspender a parceria se estes termos forem descumpridos. A parceira pode pedir o encerramento a qualquer momento.
            """;
}
