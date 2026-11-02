package com.techmentor.api.email;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * Desenha o logo do app (quadrado roxo e azul com os símbolos de código) em PNG, para ir dentro dos e-mails.
 * As medidas seguem o ícone do app, então não precisa de arquivo de imagem no projeto.
 */
public final class LogoDoEmail {

    /** Nome usado no HTML (src="cid:...") para apontar para a imagem anexada. */
    public static final String CID = "logo-techmentor";

    private static final byte[] PNG = desenhar();

    private LogoDoEmail() { }

    /** Falso se o desenho não pôde ser gerado; o e-mail sai igual, só com o nome em texto. */
    public static boolean disponivel() {
        return PNG.length > 0;
    }

    public static byte[] png() {
        return PNG;
    }

    private static byte[] desenhar() {
        try {
            int tamanho = 192;
            // O ícone do app usa uma área de 72 unidades; aqui ela vira os 192 pixels
            double escala = tamanho / 72.0;

            BufferedImage imagem = new BufferedImage(tamanho, tamanho, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = imagem.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                g.setPaint(new GradientPaint(0, 0, new Color(0x8B5CF6), tamanho, tamanho, new Color(0x2563EB)));
                g.fill(new RoundRectangle2D.Double(0, 0, tamanho, tamanho, 58, 58));

                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke((float) (6 * escala), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(angulo(escala, 43, 41, 30, 54, 43, 67));
                g.draw(angulo(escala, 65, 41, 78, 54, 65, 67));

                g.setColor(new Color(0xBDE3FF));
                g.setStroke(new BasicStroke((float) (5.5 * escala), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(x(58, escala), y(38, escala), x(50, escala), y(70, escala)));
            } finally {
                g.dispose();
            }

            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            ImageIO.write(imagem, "png", saida);
            return saida.toByteArray();

        } catch (Throwable e) {
            // Servidor sem suporte a imagens: o e-mail continua funcionando, só sem o desenho
            return new byte[0];
        }
    }

    private static Path2D angulo(double escala, double x1, double y1, double x2, double y2, double x3, double y3) {
        Path2D caminho = new Path2D.Double();
        caminho.moveTo(x(x1, escala), y(y1, escala));
        caminho.lineTo(x(x2, escala), y(y2, escala));
        caminho.lineTo(x(x3, escala), y(y3, escala));
        return caminho;
    }

    // A área do ícone começa em 18 nas duas direções
    private static double x(double valor, double escala) {
        return (valor - 18) * escala;
    }

    private static double y(double valor, double escala) {
        return (valor - 18) * escala;
    }
}