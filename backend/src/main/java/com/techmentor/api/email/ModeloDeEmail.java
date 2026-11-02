package com.techmentor.api.email;

import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Monta o e-mail padrão do TechMentor (logo, cartão, botão e rodapé) a partir de blocos, na ordem em que forem adicionados.
 * Todo texto é escapado: nome de usuária ou relato de denúncia nunca viram HTML.
 *
 * Exemplo: ModeloDeEmail.com("Recuperação de senha").saudacao("Olá, Ana!").paragrafo("...").destaque("Seu código", "123456")
 */
public final class ModeloDeEmail {

    private enum Tipo { PARAGRAFO, DETALHE, DESTAQUE, BOTAO, NOTA }

    private record Bloco(Tipo tipo, String a, String b) { }

    private static final String ROXO = "#7C3AED";
    private static final String TEXTO = "#3F3D4A";
    private static final String MUDO = "#8C8792";

    private final String titulo;
    private String saudacao;
    private final List<Bloco> blocos = new ArrayList<>();

    private ModeloDeEmail(String titulo) {
        this.titulo = titulo;
    }

    public static ModeloDeEmail com(String titulo) {
        return new ModeloDeEmail(titulo);
    }

    public ModeloDeEmail saudacao(String texto) {
        this.saudacao = texto;
        return this;
    }

    public ModeloDeEmail paragrafo(String texto) {
        blocos.add(new Bloco(Tipo.PARAGRAFO, texto, null));
        return this;
    }

    /** Linha de uma pequena tabela (ex.: "Quando" / "seg, 10 de nov, 14:00"). Linhas seguidas formam uma tabela só. */
    public ModeloDeEmail detalhe(String rotulo, String valor) {
        blocos.add(new Bloco(Tipo.DETALHE, rotulo, valor));
        return this;
    }

    /** Caixa grande no meio do e-mail, boa para um código. */
    public ModeloDeEmail destaque(String rotulo, String valor) {
        blocos.add(new Bloco(Tipo.DESTAQUE, rotulo, valor));
        return this;
    }

    /** Botão roxo. Só aceita endereços http(s). */
    public ModeloDeEmail botao(String texto, String url) {
        if (url != null && (url.startsWith("https://") || url.startsWith("http://"))) {
            blocos.add(new Bloco(Tipo.BOTAO, texto, url));
        }
        return this;
    }

    /** Texto pequeno e cinza, para avisos. */
    public ModeloDeEmail nota(String texto) {
        blocos.add(new Bloco(Tipo.NOTA, texto, null));
        return this;
    }

    // ---------------------------------------------------------------- HTML

    public String html(boolean comLogo) {

        StringBuilder h = new StringBuilder();

        h.append("<!DOCTYPE html><html lang=\"pt-BR\"><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>").append(esc(titulo)).append("</title></head>")
                .append("<body style=\"margin:0;padding:0;background:#F4F3F8;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#F4F3F8;padding:24px 12px;\">")
                .append("<tr><td align=\"center\">")
                .append("<table role=\"presentation\" width=\"560\" cellpadding=\"0\" cellspacing=\"0\" ")
                .append("style=\"width:100%;max-width:560px;background:#FFFFFF;border:1px solid #EBE8EF;border-radius:16px;font-family:Arial,Helvetica,sans-serif;\">");

        // Cabeçalho: logo + nome
        h.append("<tr><td style=\"padding:24px 28px 18px 28px;border-bottom:1px solid #EFEDF4;\">")
                .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\"><tr>");
        if (comLogo) {
            h.append("<td style=\"padding-right:10px;\"><img src=\"cid:").append(LogoDoEmail.CID)
                    .append("\" width=\"40\" height=\"40\" alt=\"TechMentor\" style=\"display:block;border:0;border-radius:11px;\"></td>");
        }
        h.append("<td style=\"font-size:21px;font-weight:800;color:#20202A;letter-spacing:-0.5px;\">Tech<span style=\"color:")
                .append(ROXO).append(";\">Mentor</span></td>")
                .append("</tr></table></td></tr>");

        // Conteúdo
        h.append("<tr><td style=\"padding:26px 28px 8px 28px;color:").append(TEXTO).append(";\">")
                .append("<h1 style=\"margin:0 0 16px 0;font-size:22px;line-height:1.3;color:#20202A;\">").append(esc(titulo)).append("</h1>");

        if (saudacao != null) {
            h.append("<p style=\"margin:0 0 14px 0;font-size:15px;line-height:1.6;font-weight:bold;color:#20202A;\">")
                    .append(esc(saudacao)).append("</p>");
        }

        boolean tabelaAberta = false;
        for (Bloco bloco : blocos) {

            if (tabelaAberta && bloco.tipo() != Tipo.DETALHE) {
                h.append("</table>");
                tabelaAberta = false;
            }

            switch (bloco.tipo()) {
                case PARAGRAFO -> h.append("<p style=\"margin:0 0 14px 0;font-size:15px;line-height:1.6;\">")
                        .append(esc(bloco.a())).append("</p>");

                case DETALHE -> {
                    if (!tabelaAberta) {
                        h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" ")
                                .append("style=\"margin:4px 0 16px 0;background:#F8F7FC;border-radius:12px;padding:6px 16px;\">");
                        tabelaAberta = true;
                    }
                    h.append("<tr><td style=\"padding:8px 0;font-size:13px;color:").append(MUDO)
                            .append(";width:110px;vertical-align:top;\">").append(esc(bloco.a())).append("</td>")
                            .append("<td style=\"padding:8px 0;font-size:14px;font-weight:bold;color:#20202A;\">")
                            .append(esc(bloco.b())).append("</td></tr>");
                }

                case DESTAQUE -> h.append("<div style=\"margin:18px 0;padding:18px;text-align:center;background:#F3EEFF;border-radius:12px;\">")
                        .append("<div style=\"font-size:12px;font-weight:bold;letter-spacing:1px;color:#6D28D9;text-transform:uppercase;\">")
                        .append(esc(bloco.a())).append("</div>")
                        .append("<div style=\"margin-top:6px;padding-left:8px;font-size:34px;font-weight:bold;letter-spacing:8px;color:#4C1D95;font-family:'Courier New',monospace;\">")
                        .append(esc(bloco.b())).append("</div></div>");

                case BOTAO -> h.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:20px 0 8px 0;\"><tr>")
                        .append("<td bgcolor=\"").append(ROXO).append("\" style=\"border-radius:10px;\">")
                        .append("<a href=\"").append(esc(bloco.b())).append("\" style=\"display:inline-block;padding:14px 28px;")
                        .append("font-size:15px;font-weight:bold;color:#FFFFFF;text-decoration:none;border-radius:10px;\">")
                        .append(esc(bloco.a())).append("</a></td></tr></table>")
                        .append("<p style=\"margin:0 0 14px 0;font-size:12px;line-height:1.5;color:").append(MUDO)
                        .append(";\">Se o botão não funcionar, copie este endereço no navegador:<br>")
                        .append("<span style=\"word-break:break-all;\">").append(esc(bloco.b())).append("</span></p>");

                case NOTA -> h.append("<p style=\"margin:6px 0 14px 0;font-size:12px;line-height:1.6;color:").append(MUDO).append(";\">")
                        .append(esc(bloco.a())).append("</p>");
            }
        }
        if (tabelaAberta) {
            h.append("</table>");
        }

        h.append("</td></tr>");

        // Rodapé
        h.append("<tr><td style=\"padding:18px 28px;background:#FAF9FC;border-top:1px solid #EFEDF4;border-radius:0 0 16px 16px;")
                .append("font-size:12px;line-height:1.6;color:").append(MUDO).append(";\">")
                .append("<strong style=\"color:#20202A;\">TechMentor</strong> &middot; Mentoria e aprendizado em tecnologia<br>")
                .append("Este é um e-mail automático. Por favor, não responda.")
                .append("</td></tr></table></td></tr></table></body></html>");

        return h.toString();
    }

    //  Texto puro (clientes que não mostram HTML)

    public String texto() {

        StringBuilder t = new StringBuilder();
        t.append(titulo).append("\n\n");
        if (saudacao != null) {
            t.append(saudacao).append("\n\n");
        }

        for (int i = 0; i < blocos.size(); i++) {
            Bloco bloco = blocos.get(i);
            switch (bloco.tipo()) {
                case PARAGRAFO, NOTA -> t.append(bloco.a()).append("\n\n");
                case DETALHE -> {
                    t.append(bloco.a()).append(": ").append(bloco.b()).append("\n");
                    boolean ultimo = i + 1 >= blocos.size() || blocos.get(i + 1).tipo() != Tipo.DETALHE;
                    if (ultimo) {
                        t.append("\n");
                    }
                }
                case DESTAQUE, BOTAO -> t.append(bloco.a()).append(": ").append(bloco.b()).append("\n\n");
            }
        }

        t.append("--\nTechMentor - Mentoria e aprendizado em tecnologia\nEste é um e-mail automático. Por favor, não responda.");
        return t.toString();
    }

    /** Escapa o HTML e mantém as quebras de linha. */
    private static String esc(String texto) {
        return HtmlUtils.htmlEscape(texto == null ? "" : texto).replace("\n", "<br>");
    }
}