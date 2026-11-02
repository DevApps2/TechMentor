package com.techmentor.api.service;

import com.techmentor.api.email.LogoDoEmail;
import com.techmentor.api.email.ModeloDeEmail;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.remetente}")
    private String remetente;

    /** Texto simples: ganha o visual padrão do TechMentor (cada bloco separado por linha em branco vira um parágrafo). */
    public void enviar(String para, String assunto, String texto) {
        ModeloDeEmail modelo = ModeloDeEmail.com("Mensagem do TechMentor");
        for (String paragrafo : texto.split("\n\n")) {
            modelo.paragrafo(paragrafo);
        }
        enviar(para, assunto, modelo);
    }

    /** E-mail padrão: HTML com logo, e uma versão em texto puro para quem não mostra HTML. */
    public void enviar(String para, String assunto, ModeloDeEmail modelo) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, "UTF-8");

            helper.setFrom(remetente, "TechMentor");
            helper.setTo(para);
            helper.setSubject(assunto);

            boolean comLogo = LogoDoEmail.disponivel();
            helper.setText(modelo.texto(), modelo.html(comLogo));

            // A imagem precisa ser anexada depois do texto
            if (comLogo) {
                helper.addInline(LogoDoEmail.CID, new ByteArrayResource(LogoDoEmail.png()), "image/png");
            }

            mailSender.send(mensagem);

        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new IllegalStateException("Não foi possível montar o e-mail.", e);
        }
    }
}