package com.techmentor.api.service;

import com.techmentor.api.dto.ParceiraDtos.AceiteRequest;
import com.techmentor.api.dto.ParceiraDtos.ConviteRequest;
import com.techmentor.api.dto.ParceiraDtos.ParceiraResponse;
import com.techmentor.api.dto.ParceiraDtos.TermosResponse;
import com.techmentor.api.dto.ParceiraDtos.VerificacaoRequest;
import com.techmentor.api.evento.EventosDeNotificacao.ParceiraAceitouConvite;
import com.techmentor.api.evento.EventosDeNotificacao.ParceiraConvidada;
import com.techmentor.api.evento.EventosDeNotificacao.SituacaoDaParceiraMudou;
import com.techmentor.api.exceptions.ApiException;
import com.techmentor.api.exceptions.BadRequestException;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.ForbiddenException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.exceptions.TooManyRequestsException;
import com.techmentor.api.model.AcaoAuditada;
import com.techmentor.api.model.ConviteParceira;
import com.techmentor.api.model.Parceira;
import com.techmentor.api.model.RegistroAuditoria;
import com.techmentor.api.model.SituacaoParceira;
import com.techmentor.api.model.TipoParceira;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.ConviteParceiraRepository;
import com.techmentor.api.repository.CursoRepository;
import com.techmentor.api.repository.ParceiraRepository;
import com.techmentor.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * O ciclo de vida da parceira: convite da administradora, aceite com os termos, verificação com checklist,
 * suspensão e reativação. Quem pode chamar cada ação é decidido nas rotas (/api/admin é só da administradora)
 * e conferido de novo aqui.
 */
@Service
@RequiredArgsConstructor
public class ParceiraService {

    private static final int VALIDADE_DO_CONVITE_DIAS = 7;
    private static final int MAXIMO_TENTATIVAS = 5;
    private static final int INTERVALO_ENTRE_ENVIOS_SEGUNDOS = 60;
    private static final String CODIGO_INVALIDO = "Código inválido ou expirado.";

    // Sem 0/O e 1/I, para ninguém errar ao digitar o que leu no e-mail
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    // Quem tem e-mail destes provedores não prova que é da instituição
    private static final Set<String> PROVEDORES_PESSOAIS = Set.of(
            "gmail.com", "hotmail.com", "outlook.com", "yahoo.com", "yahoo.com.br", "icloud.com", "live.com",
            "bol.com.br", "uol.com.br", "terra.com.br", "protonmail.com");

    private final ParceiraRepository parceiras;
    private final ConviteParceiraRepository convites;
    private final UsuarioRepository usuarios;
    private final CursoRepository cursos;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoria;
    private final ApplicationEventPublisher eventos;

    // ------------------------------------------------------------------ administradora

    @Transactional(readOnly = true)
    public List<ParceiraResponse> listar(Usuario ator, SituacaoParceira filtro) {
        RegraAcesso.exigirAdmin(ator);
        List<Parceira> lista = filtro == null ? parceiras.findAll() : parceiras.findBySituacao(filtro);
        return lista.stream()
                .sorted(Comparator.comparing(Parceira::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(this::resposta)
                .toList();
    }

    @Transactional(readOnly = true)
    public ParceiraResponse detalhar(Usuario ator, Long id) {
        RegraAcesso.exigirAdmin(ator);
        return resposta(parceira(id));
    }

    /**
     * Cria a conta (inativa até a parceira aceitar o convite), o perfil e o código do convite, que sai por e-mail.
     * A conta nasce com uma senha aleatória que ninguém conhece: só o código do convite abre o acesso.
     */
    @Transactional
    public ParceiraResponse convidar(Usuario ator, ConviteRequest dados) {
        RegraAcesso.exigirAdmin(ator);

        String email = Emails.normalizar(dados.email());
        if (usuarios.existsByEmail(email)) throw new ConflictException("E-mail já cadastrado.");

        String nome = dados.nome().trim();
        if (parceiras.existsByNomeIgnoreCase(nome)) throw new ConflictException("Já existe uma parceira com esse nome.");

        String documento = Cnpj.soDigitos(dados.documento());
        if (documento == null && dados.tipo() == TipoParceira.EMPRESA) {
            throw new BadRequestException("Empresa precisa informar o CNPJ.");
        }
        if (documento != null) {
            if (!Cnpj.valido(documento)) throw new BadRequestException("CNPJ inválido.");
            if (parceiras.existsByDocumento(documento)) throw new ConflictException("Já existe uma parceira com esse CNPJ.");
        }

        Usuario conta = new Usuario();
        conta.setNome(dados.responsavel().trim());
        conta.setEmail(email);
        conta.setSenhaHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        conta.setTipo(Usuario.Tipo.PARCEIRA);
        conta.setAtivo(false);
        conta = usuarios.save(conta);

        Parceira parceira = new Parceira();
        parceira.setUsuario(conta);
        parceira.setNome(nome);
        parceira.setTipo(dados.tipo());
        parceira.setDocumento(documento);
        parceira.setSite(dados.site() == null || dados.site().isBlank() ? null : dados.site().trim());
        parceira.setResponsavel(dados.responsavel().trim());
        parceira = parceiras.save(parceira);

        emitirConvite(parceira);

        auditoria.registrar(ator, AcaoAuditada.PARCERIA_CONVIDADA, RegistroAuditoria.PARCEIRA, parceira.getId(),
                "Convite para " + email + ".");
        return resposta(parceira);
    }

    /** Manda um código novo (o anterior deixa de valer). Só enquanto a parceira ainda não aceitou. */
    @Transactional
    public void reenviarConvite(Usuario ator, Long id) {
        RegraAcesso.exigirAdmin(ator);
        Parceira parceira = parceira(id);

        if (parceira.aceitouOsTermos()) {
            throw new ConflictException("Esta parceira já aceitou o convite.");
        }
        if (convites.existsByParceiraAndCriadoEmAfter(parceira, LocalDateTime.now().minusSeconds(INTERVALO_ENTRE_ENVIOS_SEGUNDOS))) {
            throw new TooManyRequestsException("Um convite acabou de ser enviado. Aguarde um minuto para reenviar.");
        }

        emitirConvite(parceira);
        auditoria.registrar(ator, AcaoAuditada.PARCERIA_CONVITE_REENVIADO, RegistroAuditoria.PARCEIRA, parceira.getId(), null);
    }

    /**
     * Fecha a verificação. Os três itens do checklist precisam estar conferidos; o que foi marcado, por quem e quando
     * fica guardado. Só vale depois que a parceira aceitou o convite.
     */
    @Transactional
    public ParceiraResponse verificar(Usuario ator, Long id, VerificacaoRequest dados) {
        RegraAcesso.exigirAdmin(ator);
        Parceira parceira = parceira(id);

        if (parceira.getSituacao() != SituacaoParceira.CONVIDADA) {
            throw new ConflictException(parceira.getSituacao() == SituacaoParceira.ATIVA
                    ? "Esta parceira já foi verificada."
                    : "Esta parceira está suspensa: reative-a em vez de verificar.");
        }
        if (!parceira.aceitouOsTermos()) {
            throw new ConflictException("A parceira ainda não aceitou o convite e os termos.");
        }
        if (!dados.siteConferido() || !dados.dominioDoEmailConferido() || !dados.identificacaoConferida()) {
            throw new BadRequestException("Confira os três itens (site, domínio do e-mail e identificação) para verificar.");
        }

        parceira.setSiteConferido(true);
        parceira.setDominioDoEmailConferido(true);
        parceira.setIdentificacaoConferida(true);
        parceira.setVerificadaEm(LocalDateTime.now());
        parceira.setVerificadaPor(ator);
        parceira.setObservacaoDaVerificacao(vazioComoNulo(dados.observacao()));
        parceira.setSituacao(SituacaoParceira.ATIVA);
        parceiras.save(parceira);

        auditoria.registrar(ator, AcaoAuditada.PARCERIA_VERIFICADA, RegistroAuditoria.PARCEIRA, parceira.getId(),
                parceira.getObservacaoDaVerificacao());
        eventos.publishEvent(new SituacaoDaParceiraMudou(parceira.getUsuario(), SituacaoParceira.ATIVA, null));
        return resposta(parceira);
    }

    @Transactional
    public ParceiraResponse suspender(Usuario ator, Long id, String motivo) {
        RegraAcesso.exigirAdmin(ator);
        Parceira parceira = parceira(id);

        if (parceira.getSituacao() != SituacaoParceira.ATIVA) {
            throw new ConflictException("Só uma parceira verificada pode ser suspensa.");
        }

        parceira.setSituacao(SituacaoParceira.SUSPENSA);
        parceira.setMotivoDaSuspensao(motivo.trim());
        parceiras.save(parceira);

        auditoria.registrar(ator, AcaoAuditada.PARCERIA_SUSPENSA, RegistroAuditoria.PARCEIRA, parceira.getId(), motivo.trim());
        eventos.publishEvent(new SituacaoDaParceiraMudou(parceira.getUsuario(), SituacaoParceira.SUSPENSA, motivo.trim()));
        return resposta(parceira);
    }

    @Transactional
    public ParceiraResponse reativar(Usuario ator, Long id) {
        RegraAcesso.exigirAdmin(ator);
        Parceira parceira = parceira(id);

        if (parceira.getSituacao() != SituacaoParceira.SUSPENSA) {
            throw new ConflictException("Esta parceira não está suspensa.");
        }

        parceira.setSituacao(SituacaoParceira.ATIVA);
        parceira.setMotivoDaSuspensao(null);
        parceiras.save(parceira);

        auditoria.registrar(ator, AcaoAuditada.PARCERIA_REATIVADA, RegistroAuditoria.PARCEIRA, parceira.getId(), null);
        eventos.publishEvent(new SituacaoDaParceiraMudou(parceira.getUsuario(), SituacaoParceira.ATIVA, null));
        return resposta(parceira);
    }

    // ------------------------------------------------------------------ a própria parceira

    public TermosResponse termos() {
        return new TermosResponse(TermosDeParceria.VERSAO, TermosDeParceria.TITULO, TermosDeParceria.TEXTO);
    }

    /**
     * A parceira cria a senha e aceita os termos com o código do convite. Sem token: quem chama ainda não tem conta ativa.
     * noRollbackFor: sem isso, o erro desfaria o "tentativas + 1" e o limite de tentativas não funcionaria.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public ParceiraResponse aceitarConvite(AceiteRequest dados) {

        if (!Boolean.TRUE.equals(dados.aceitaTermos())) {
            throw new BadRequestException("É preciso aceitar os Termos de Parceria.");
        }

        Usuario usuario = usuarios.findByEmail(Emails.normalizar(dados.email()))
                .filter(u -> u.getTipo() == Usuario.Tipo.PARCEIRA && !u.isAtivo())
                .orElseThrow(() -> new BadRequestException(CODIGO_INVALIDO));
        Parceira parceira = parceiras.findByUsuario(usuario)
                .filter(p -> !p.aceitouOsTermos())
                .orElseThrow(() -> new BadRequestException(CODIGO_INVALIDO));

        ConviteParceira convite = convites.findFirstByParceiraAndUsadoFalseOrderByCriadoEmDesc(parceira)
                .orElseThrow(() -> new BadRequestException(CODIGO_INVALIDO));

        if (convite.getExpiraEm().isBefore(LocalDateTime.now())) {
            convite.setUsado(true);
            throw new BadRequestException(CODIGO_INVALIDO);
        }
        if (convite.getTentativas() >= MAXIMO_TENTATIVAS) {
            convite.setUsado(true);
            throw new BadRequestException("Muitas tentativas. Peça à equipe do TechMentor um novo convite.");
        }
        if (!passwordEncoder.matches(normalizarCodigo(dados.codigo()), convite.getCodigoHash())) {
            convite.setTentativas(convite.getTentativas() + 1);
            throw new BadRequestException(CODIGO_INVALIDO);
        }

        // O código está certo; só falta ela ter lido a versão vigente dos termos
        if (!TermosDeParceria.VERSAO.equals(dados.versaoTermos())) {
            throw new BadRequestException("Os termos foram atualizados. Leia a versão atual antes de aceitar.");
        }

        usuario.setSenhaHash(passwordEncoder.encode(dados.senha()));
        usuario.setSenhaProvisoria(false);
        usuario.setAtivo(true);
        usuario.invalidarTokens();
        usuarios.save(usuario);

        parceira.setTermosVersao(TermosDeParceria.VERSAO);
        parceira.setTermosAceitosEm(LocalDateTime.now());
        parceiras.save(parceira);
        convite.setUsado(true);

        auditoria.registrar(usuario, AcaoAuditada.PARCERIA_ACEITOU_TERMOS, RegistroAuditoria.PARCEIRA, parceira.getId(),
                "Termos versão " + TermosDeParceria.VERSAO + ".");
        eventos.publishEvent(new ParceiraAceitouConvite(parceira.getId(), parceira.getNome()));
        return resposta(parceira);
    }

    /** O perfil de quem está logada como parceira. */
    @Transactional(readOnly = true)
    public ParceiraResponse meuPerfil(Usuario ator) {
        return resposta(daConta(ator));
    }

    /** A parceira da conta; quem não é parceira não passa. */
    @Transactional(readOnly = true)
    public Parceira daConta(Usuario ator) {
        if (ator.getTipo() != Usuario.Tipo.PARCEIRA) throw new ForbiddenException();
        return parceiras.findByUsuario(ator).orElseThrow(ForbiddenException::new);
    }

    // ------------------------------------------------------------------ apoio

    /** Invalida os códigos anteriores, gera um novo e dispara o e-mail. O texto do código só vive no evento. */
    private void emitirConvite(Parceira parceira) {
        convites.findByParceiraAndUsadoFalse(parceira).forEach(antigo -> antigo.setUsado(true));

        String codigo = novoCodigo();
        ConviteParceira convite = new ConviteParceira();
        convite.setParceira(parceira);
        convite.setCodigoHash(passwordEncoder.encode(codigo));
        convite.setExpiraEm(LocalDateTime.now().plusDays(VALIDADE_DO_CONVITE_DIAS));
        convites.save(convite);

        eventos.publishEvent(new ParceiraConvidada(parceira.getNome(), parceira.getResponsavel(),
                parceira.getUsuario().getEmail(), codigo.substring(0, 4) + "-" + codigo.substring(4), convite.getExpiraEm()));
    }

    private static String novoCodigo() {
        StringBuilder codigo = new StringBuilder();
        for (int i = 0; i < 8; i++) codigo.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
        return codigo.toString();
    }

    /** Aceita "k7m2-9qxa", " K7M2 9QXA "... O código guardado é sempre as 8 letras e números juntos, em maiúsculas. */
    private static String normalizarCodigo(String codigo) {
        return codigo.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private Parceira parceira(Long id) {
        return parceiras.findById(id).orElseThrow(() -> new NotFoundException("Parceira não encontrada!"));
    }

    private static String vazioComoNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private ParceiraResponse resposta(Parceira p) {
        return new ParceiraResponse(p.getId(), p.getUsuario().getId(), p.getNome(), p.getTipo(), p.getDocumento(),
                p.getSite(), p.getResponsavel(), p.getUsuario().getEmail(), p.getSituacao(), p.getCriadaEm(),
                p.aceitouOsTermos(), p.getTermosVersao(), p.getTermosAceitosEm(),
                dominioConfere(p.getUsuario().getEmail(), p.getSite()),
                com.techmentor.api.dto.ParceiraDtos.checklist(p), p.getMotivoDaSuspensao(),
                cursos.findByParceira(p).size());
    }

    /** O e-mail é do mesmo domínio do site? Nulo se não há site; falso para e-mail pessoal (gmail etc.). */
    static Boolean dominioConfere(String email, String site) {
        if (site == null || site.isBlank()) return null;
        try {
            String host = URI.create(site.trim()).getHost();
            if (host == null) return null;
            host = host.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");

            String dominio = email.substring(email.indexOf('@') + 1).toLowerCase(Locale.ROOT);
            if (PROVEDORES_PESSOAIS.contains(dominio)) return false;

            return dominio.equals(host) || dominio.endsWith("." + host) || host.endsWith("." + dominio);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
