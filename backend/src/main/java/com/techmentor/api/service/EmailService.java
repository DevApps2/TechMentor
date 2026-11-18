package com.techmentor.api.service;

import com.techmentor.api.email.LogoDoEmail;
import com.techmentor.api.email.ModeloDeEmail;
import com.techmentor.api.model.RegistroEnvioEmail;
import com.techmentor.api.repository.RegistroEnvioEmailRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final int MAXIMO_DE_TENTATIVAS = 4;

    private final JavaMailSender mailSender;
    private final RegistroEnvioEmailRepository registros;

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
        boolean comLogo = LogoDoEmail.disponivel();
        enviarConteudo(para, assunto, modelo.texto(), modelo.html(comLogo), comLogo);
    }

    /** Envio idempotente para eventos de parceria, registrado e confirmado só após mailSender.send retornar. */
    public void enviarRastreado(String chaveEvento, String para, String assunto, ModeloDeEmail modelo,
                                boolean guardarConteudoParaRetentativa) {
        if (registros.existsByChaveEvento(chaveEvento)) return;

        RegistroEnvioEmail registro = new RegistroEnvioEmail();
        registro.setChaveEvento(chaveEvento);
        registro.setDestinatario(para == null ? "" : para.trim());
        registro.setAssunto(assunto);
        registro.setSituacao(RegistroEnvioEmail.Situacao.PENDENTE);
        registro.setProximaTentativaEm(LocalDateTime.now());

        boolean valido = destinatarioValido(para);
        boolean comLogo = LogoDoEmail.disponivel();
        registro.setComLogo(comLogo);
        if (guardarConteudoParaRetentativa && valido) {
            registro.setTextoRetentativa(modelo.texto());
            registro.setHtmlRetentativa(modelo.html(comLogo));
        }
        try {
            registro = registros.saveAndFlush(registro);
        } catch (DataIntegrityViolationException duplicado) {
            return;
        }

        if (!valido) {
            log.warn("E-mail do evento {} não enviado: destinatário inválido.", chaveEvento);
            falhou(registro, null, "Destinatário inválido.");
            return;
        }
        tentar(registro, RegistroEnvioEmail.Situacao.PENDENTE, modelo.texto(), modelo.html(comLogo), comLogo);
    }

    /** Reenvio automático limitado a quatro tentativas, apenas para mensagens sem código ou credencial. */
    @Scheduled(fixedDelayString = "60000", initialDelayString = "60000")
    public void reenviarFalhasDeParceria() {
        List<RegistroEnvioEmail> pendentes = registros.encontrarRetentativas(
                List.of(RegistroEnvioEmail.Situacao.PENDENTE, RegistroEnvioEmail.Situacao.FALHOU),
                LocalDateTime.now(), PageRequest.of(0, 20));
        for (RegistroEnvioEmail registro : pendentes) {
            if (registro.getTentativas() >= MAXIMO_DE_TENTATIVAS || !destinatarioValido(registro.getDestinatario())) continue;
            RegistroEnvioEmail.Situacao anterior = registro.getSituacao();
            if (registros.reservarRetentativa(registro.getId(), anterior, RegistroEnvioEmail.Situacao.ENVIANDO) == 1) {
                realizarEnvio(registro, registro.getTextoRetentativa(), registro.getHtmlRetentativa(), registro.isComLogo());
            }
        }
    }

    private void tentar(RegistroEnvioEmail registro, RegistroEnvioEmail.Situacao situacaoAnterior,
                        String texto, String html, boolean comLogo) {
        if (registros.reservarRetentativa(registro.getId(), situacaoAnterior, RegistroEnvioEmail.Situacao.ENVIANDO) != 1) return;
        realizarEnvio(registro, texto, html, comLogo);
    }

    private void realizarEnvio(RegistroEnvioEmail registro, String texto, String html, boolean comLogo) {
        try {
            enviarConteudo(registro.getDestinatario(), registro.getAssunto(), texto, html, comLogo);
            registro.setTentativas(registro.getTentativas() + 1);
            registro.setSituacao(RegistroEnvioEmail.Situacao.ENVIADO);
            registro.setProximaTentativaEm(null);
            registro.setUltimoErro(null);
            registros.save(registro);
        } catch (Exception e) {
            falhou(registro, e, "Falha no serviço de e-mail.");
        }
    }

    private void falhou(RegistroEnvioEmail registro, Exception e, String mensagem) {
        if (e != null) {
            registro.setTentativas(registro.getTentativas() + 1);
            log.warn("Falha no envio de e-mail do evento {} ({}).", registro.getChaveEvento(), e.getClass().getSimpleName());
        }
        registro.setSituacao(RegistroEnvioEmail.Situacao.FALHOU);
        registro.setUltimoErro(mensagem);
        if (registro.getTextoRetentativa() != null && registro.getTentativas() < MAXIMO_DE_TENTATIVAS) {
            long[] esperaMinutos = {1, 5, 15};
            int indice = Math.max(0, registro.getTentativas() - 1);
            registro.setProximaTentativaEm(LocalDateTime.now().plusMinutes(esperaMinutos[Math.min(indice, esperaMinutos.length - 1)]));
        } else {
            registro.setProximaTentativaEm(null);
        }
        registros.save(registro);
    }

    private static boolean destinatarioValido(String para) {
        if (para == null || para.isBlank()) return false;
        try {
            InternetAddress endereco = new InternetAddress(para.trim(), true);
            endereco.validate();
            return true;
        } catch (Exception ignorado) {
            return false;
        }
    }

    private void enviarConteudo(String para, String assunto, String texto, String html, boolean comLogo) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, "UTF-8");

            helper.setFrom(remetente, "TechMentor");
            helper.setTo(para);
            helper.setSubject(assunto);

            helper.setText(texto, html);

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
