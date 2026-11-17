package com.techmentor.api.service;

import com.techmentor.api.model.InscricaoOportunidade;
import com.techmentor.api.model.Oportunidade;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.dto.OportunidadeDtos.*;
import com.techmentor.api.repository.InscricaoOportunidadeRepository;
import com.techmentor.api.repository.OportunidadeRepository;
import com.techmentor.api.evento.EventosDeNotificacao;
import com.techmentor.api.exceptions.BadRequestException;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.ForbiddenException;
import com.techmentor.api.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OportunidadeService {
    private final OportunidadeRepository oportunidades;
    private final InscricaoOportunidadeRepository inscricoes;
    private final ConquistaService conquistas;
    private final ApplicationEventPublisher eventos;

    // Fora da pasta de fotos de propósito: as fotos são públicas, os currículos não
    @Value("${app.pasta-curriculos:curriculos}")
    private String pastaCurriculos;

    private static final long LIMITE_DO_CURRICULO = 5L * 1024 * 1024;

    /** O PDF guardado: onde está e com que nome mostrar. */
    public record CurriculoGuardado(Path caminho, String nome) { }

    public OportunidadeService(OportunidadeRepository oportunidades,
                               InscricaoOportunidadeRepository inscricoes,
                               ConquistaService conquistas,
                               ApplicationEventPublisher eventos) {
        this.oportunidades = oportunidades;
        this.inscricoes = inscricoes;
        this.conquistas = conquistas;
        this.eventos = eventos;
    }

    /** Oportunidades ativas e com prazo em aberto, pelo prazo mais próximo; filtros opcionais por tipo e por área. */
    @Transactional(readOnly = true)
    public List<OportunidadeResponse> listar(Oportunidade.Tipo tipo, String area) {
        return listar(tipo, area, null);
    }

    /** Igual à lista acima, e a busca (opcional) procura no título, na organização, na área e na descrição. */
    @Transactional(readOnly = true)
    public List<OportunidadeResponse> listar(Oportunidade.Tipo tipo, String area, String busca) {
        String termo = busca == null ? "" : busca.trim().toLowerCase();
        return oportunidades.listarAbertas(LocalDate.now()).stream()
                .filter(o -> tipo == null || o.getTipo() == tipo)
                .filter(o -> area == null || area.isBlank() || area.trim().equalsIgnoreCase(o.getArea()))
                .filter(o -> termo.isEmpty() || contem(o, termo))
                .map(this::toResponse).toList();
    }

    private static boolean contem(Oportunidade o, String termo) {
        return java.util.stream.Stream.of(o.getTitulo(), o.getOrganizacao(), o.getArea(), o.getDescricao())
                .anyMatch(texto -> texto != null && texto.toLowerCase().contains(termo));
    }

    @Transactional(readOnly = true)
    public OportunidadeResponse obter(Long id) {
        Oportunidade oportunidade = buscar(id);
        if (!oportunidade.isAtiva()) throw new NotFoundException("Oportunidade não encontrada.");
        return toResponse(oportunidade);
    }

    public OportunidadeResponse criar(Usuario actor, OportunidadeRequest request) {
        RegraAcesso.exigirAdmin(actor);
        validar(request);
        return toResponse(oportunidades.save(aplicar(new Oportunidade(), request)));
    }

    public OportunidadeResponse atualizar(Usuario actor, Long id, OportunidadeRequest request) {
        RegraAcesso.exigirAdmin(actor);
        validar(request);
        Oportunidade oportunidade = buscar(id);
        if (!oportunidade.isAtiva()) throw new NotFoundException("Oportunidade não encontrada.");
        return toResponse(aplicar(oportunidade, request));
    }

    public void excluir(Usuario actor, Long id) {
        RegraAcesso.exigirAdmin(actor);
        buscar(id).setAtiva(false);
    }

    public InscricaoResponse candidatar(Usuario actor, Long oportunidadeId) {
        return inscrever(actor, oportunidadeId, false, null);
    }

    /** Candidatura com os dados do formulário (corpo opcional: sem dados, só registra). */
    public InscricaoResponse candidatar(Usuario actor, Long oportunidadeId, CandidaturaRequest dados) {
        return inscrever(actor, oportunidadeId, false, dados);
    }

    public InscricaoResponse inscreverEmEvento(Usuario actor, Long oportunidadeId) {
        return inscrever(actor, oportunidadeId, true, null);
    }

    private InscricaoResponse inscrever(Usuario actor, Long id, boolean evento, CandidaturaRequest dados) {
        Oportunidade oportunidade = buscar(id);
        if (!oportunidade.isAtiva() || (oportunidade.getPrazoInscricao() != null
                && oportunidade.getPrazoInscricao().isBefore(LocalDate.now())))
            throw new ConflictException("Oportunidade encerrada.");
        if ((oportunidade.getTipo() == Oportunidade.Tipo.EVENTO) != evento)
            throw new BadRequestException(evento
                    ? "Inscrições são permitidas apenas para eventos."
                    : "Candidaturas são permitidas apenas para vagas e bolsas.");
        if (inscricoes.findByUsuarioIdAndOportunidadeId(actor.getId(), id).isPresent())
            throw new ConflictException("Usuário já possui inscrição nesta oportunidade.");
        InscricaoOportunidade inscricao = new InscricaoOportunidade();
        inscricao.setUsuario(actor);
        inscricao.setOportunidade(oportunidade);
        if (dados != null) {
            inscricao.setContatoNome(dados.nome().trim());
            inscricao.setContatoEmail(dados.email().trim().toLowerCase());
            inscricao.setTelefone(dados.telefone().trim());
            inscricao.setCidade(vazioComoNulo(dados.cidade()));
            inscricao.setLinkProfissional(vazioComoNulo(dados.linkProfissional()));
            inscricao.setMensagem(vazioComoNulo(dados.mensagem()));
        }
        InscricaoOportunidade salva = inscricoes.save(inscricao);
        conquistas.desbloquear(actor, ConquistaService.PRIMEIRA_CANDIDATURA);
        return toResponse(salva);
    }

    @Transactional(readOnly = true)
    public List<InscricaoResponse> minhasInscricoes(Usuario actor, boolean eventos) {
        return inscricoes.findByUsuarioIdOrderByCriadaEmDesc(actor.getId()).stream()
                .filter(i -> (i.getOportunidade().getTipo() == Oportunidade.Tipo.EVENTO) == eventos)
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InscricaoResponse obterCandidatura(Usuario actor, Long id) {
        InscricaoOportunidade inscricao = inscricoes.findById(id)
                .orElseThrow(() -> new NotFoundException("Candidatura não encontrada."));
        RegraAcesso.exigirProprietarioOuAdmin(actor, inscricao.getUsuario().getId());
        return toResponse(inscricao);
    }

    /**
     * A candidata anexa (ou troca) o currículo em PDF da própria candidatura. Só enquanto a equipe ainda não respondeu.
     * O tipo que o app informa pode ser falso: confere se o arquivo começa mesmo com "%PDF-".
     */
    public InscricaoResponse anexarCurriculo(Usuario actor, Long id, MultipartFile arquivo) {
        InscricaoOportunidade inscricao = inscricoes.findById(id)
                .orElseThrow(() -> new NotFoundException("Candidatura não encontrada."));
        if (!inscricao.getUsuario().getId().equals(actor.getId())) throw new ForbiddenException();
        if (inscricao.getStatus() == InscricaoOportunidade.Status.APROVADA
                || inscricao.getStatus() == InscricaoOportunidade.Status.RECUSADA)
            throw new ConflictException("Esta candidatura já foi respondida e não aceita mais anexos.");

        if (arquivo == null || arquivo.isEmpty()) throw new BadRequestException("Envie o currículo em PDF.");
        if (arquivo.getSize() > LIMITE_DO_CURRICULO) throw new BadRequestException("O PDF deve ter no máximo 5 MB.");

        byte[] inicio;
        try (InputStream entrada = arquivo.getInputStream()) {
            inicio = entrada.readNBytes(5);
        } catch (IOException e) {
            throw new BadRequestException("Não foi possível ler o arquivo.");
        }
        if (!"%PDF-".equals(new String(inicio, StandardCharsets.ISO_8859_1))) {
            throw new BadRequestException("O arquivo não é um PDF válido.");
        }

        String antigo = inscricao.getCurriculoArquivo();
        String nomeGuardado = "curriculo-" + id + "-" + UUID.randomUUID() + ".pdf";
        try {
            Path pasta = Path.of(pastaCurriculos).toAbsolutePath();
            Files.createDirectories(pasta);
            arquivo.transferTo(pasta.resolve(nomeGuardado));
        } catch (IOException e) {
            throw new BadRequestException("Não foi possível salvar o currículo.");
        }

        inscricao.setCurriculoArquivo(nomeGuardado);
        inscricao.setCurriculoNome(nomeOriginal(arquivo.getOriginalFilename()));
        apagarArquivo(antigo);
        return toResponse(inscricao);
    }

    /** Só a admin: o PDF da candidatura, para baixar. 404 se a candidata não anexou. */
    @Transactional(readOnly = true)
    public CurriculoGuardado curriculoDaCandidatura(Usuario actor, Long id) {
        RegraAcesso.exigirAdmin(actor);
        InscricaoOportunidade inscricao = inscricoes.findById(id)
                .orElseThrow(() -> new NotFoundException("Candidatura não encontrada."));
        if (inscricao.getCurriculoArquivo() == null) throw new NotFoundException("Esta candidatura não tem currículo.");

        Path pasta = Path.of(pastaCurriculos).toAbsolutePath().normalize();
        Path caminho = pasta.resolve(inscricao.getCurriculoArquivo()).normalize();
        if (!caminho.startsWith(pasta) || !Files.isRegularFile(caminho)) {
            throw new NotFoundException("O arquivo do currículo não foi encontrado.");
        }
        return new CurriculoGuardado(caminho, inscricao.getCurriculoNome() == null ? "curriculo.pdf" : inscricao.getCurriculoNome());
    }

    /** Só a admin: quem se candidatou a esta oportunidade, da mais nova para a mais antiga. */
    @Transactional(readOnly = true)
    public List<CandidaturaAdminResponse> candidaturasDaOportunidade(Usuario actor, Long oportunidadeId) {
        RegraAcesso.exigirAdmin(actor);
        buscar(oportunidadeId);
        return inscricoes.findByOportunidadeIdOrderByCriadaEmDesc(oportunidadeId).stream()
                .map(this::toAdminResponse).toList();
    }

    /** Só a admin: muda a situação (ENVIADA, EM_ANALISE, APROVADA ou RECUSADA) e avisa quem se candidatou. */
    public CandidaturaAdminResponse atualizarStatus(Usuario actor, Long id, InscricaoOportunidade.Status novo) {
        RegraAcesso.exigirAdmin(actor);
        if (novo == null) throw new BadRequestException("Informe a nova situação.");

        InscricaoOportunidade inscricao = inscricoes.findById(id)
                .orElseThrow(() -> new NotFoundException("Candidatura não encontrada."));

        if (inscricao.getStatus() != novo) {
            inscricao.setStatus(novo);
            Oportunidade oportunidade = inscricao.getOportunidade();
            eventos.publishEvent(new EventosDeNotificacao.CandidaturaAtualizada(
                    inscricao.getUsuario(), oportunidade.getTitulo(),
                    oportunidade.getTipo() == Oportunidade.Tipo.EVENTO, novo));
        }
        return toAdminResponse(inscricao);
    }

    /** A própria usuária retira a candidatura (ou a inscrição), enquanto a equipe ainda não respondeu. */
    public void desistir(Usuario actor, Long id) {
        InscricaoOportunidade inscricao = inscricoes.findById(id)
                .orElseThrow(() -> new NotFoundException("Candidatura não encontrada."));
        if (!inscricao.getUsuario().getId().equals(actor.getId())) throw new ForbiddenException();
        if (inscricao.getStatus() == InscricaoOportunidade.Status.APROVADA
                || inscricao.getStatus() == InscricaoOportunidade.Status.RECUSADA)
            throw new ConflictException("Esta candidatura já foi respondida e não pode ser retirada.");
        String curriculo = inscricao.getCurriculoArquivo();
        inscricoes.delete(inscricao);
        apagarArquivo(curriculo);   // quem desiste leva os dados embora: o PDF não fica guardado
    }

    private void apagarArquivo(String nomeGuardado) {
        if (nomeGuardado == null) return;
        try {
            Path pasta = Path.of(pastaCurriculos).toAbsolutePath().normalize();
            Path caminho = pasta.resolve(nomeGuardado).normalize();
            if (caminho.startsWith(pasta)) Files.deleteIfExists(caminho);
        } catch (IOException e) {
            // arquivo que não dá para apagar agora não deve impedir a desistência
        }
    }

    /** Só o nome do arquivo (sem pastas), limpo e curto, para mostrar à equipe. */
    private static String nomeOriginal(String enviado) {
        String nome = enviado == null ? "" : enviado.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (nome.isEmpty()) nome = "curriculo.pdf";
        return nome.length() <= 120 ? nome : nome.substring(0, 116) + ".pdf";
    }

    private static String vazioComoNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private Oportunidade buscar(Long id) {
        return oportunidades.findById(id).orElseThrow(() -> new NotFoundException("Oportunidade não encontrada."));
    }

    private void validar(OportunidadeRequest request) {
        if (request == null) throw new BadRequestException("Dados da oportunidade são obrigatórios.");
        if (request.tipo() == null || request.titulo() == null || request.titulo().isBlank())
            throw new BadRequestException("Tipo e título são obrigatórios.");
        if (request.prazoInscricao() != null && request.prazoInscricao().isBefore(LocalDate.now()))
            throw new BadRequestException("Prazo não pode estar no passado.");
    }

    private Oportunidade aplicar(Oportunidade oportunidade, OportunidadeRequest request) {
        oportunidade.setTipo(request.tipo());
        oportunidade.setTitulo(request.titulo().trim());
        oportunidade.setOrganizacao(request.organizacao());
        oportunidade.setDescricao(request.descricao());
        oportunidade.setRequisitos(request.requisitos());
        oportunidade.setBeneficios(request.beneficios());
        oportunidade.setArea(request.area());
        oportunidade.setNivel(request.nivel());
        oportunidade.setLocalidade(request.localidade());
        oportunidade.setLinkExterno(request.linkExterno());
        oportunidade.setPrazoInscricao(request.prazoInscricao());
        oportunidade.setAtiva(true);
        return oportunidade;
    }

    private OportunidadeResponse toResponse(Oportunidade oportunidade) {
        return new OportunidadeResponse(oportunidade.getId(), oportunidade.getTipo(), oportunidade.getTitulo(),
                oportunidade.getOrganizacao(), oportunidade.getDescricao(), oportunidade.getRequisitos(),
                oportunidade.getLocalidade(), oportunidade.getLinkExterno(), oportunidade.getPrazoInscricao(),
                oportunidade.isAtiva(), oportunidade.getBeneficios(), oportunidade.getArea(), oportunidade.getNivel());
    }

    private CandidaturaAdminResponse toAdminResponse(InscricaoOportunidade inscricao) {
        Usuario usuario = inscricao.getUsuario();
        return new CandidaturaAdminResponse(inscricao.getId(), usuario.getId(), usuario.getNome(), usuario.getEmail(),
                inscricao.getOportunidade().getId(), inscricao.getOportunidade().getTitulo(),
                inscricao.getStatus(), inscricao.getCriadaEm(),
                inscricao.getContatoNome(), inscricao.getContatoEmail(), inscricao.getTelefone(), inscricao.getCidade(),
                inscricao.getLinkProfissional(), inscricao.getMensagem(),
                inscricao.getCurriculoArquivo() != null, inscricao.getCurriculoNome());
    }

    private InscricaoResponse toResponse(InscricaoOportunidade inscricao) {
        return new InscricaoResponse(inscricao.getId(), inscricao.getUsuario().getId(), inscricao.getOportunidade().getId(),
                inscricao.getOportunidade().getTitulo(), inscricao.getStatus(), inscricao.getCriadaEm());
    }
}
