package com.techmentor.api.service;

import com.techmentor.api.dto.CursoDtos.AulaResponse;
import com.techmentor.api.dto.CursoDtos.CursoRequest;
import com.techmentor.api.dto.CursoDtos.CursoResponse;
import com.techmentor.api.dto.CursoDtos.ModuloResponse;
import com.techmentor.api.dto.GestaoCursoDtos.AulaGestaoRequest;
import com.techmentor.api.dto.GestaoCursoDtos.AulaMetrica;
import com.techmentor.api.dto.GestaoCursoDtos.MetricasCursoResponse;
import com.techmentor.api.dto.GestaoCursoDtos.ModuloGestaoRequest;
import com.techmentor.api.evento.EventosDeNotificacao.CursoEnviadoParaRevisao;
import com.techmentor.api.evento.EventosDeNotificacao.CursoRevisado;
import com.techmentor.api.evento.EventosDeNotificacao.DecisaoDeRevisao;
import com.techmentor.api.dto.AuditoriaDtos.RegistroResponse;
import com.techmentor.api.exceptions.BadRequestException;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.AcaoAuditada;
import com.techmentor.api.model.Aula;
import com.techmentor.api.model.Curso;
import com.techmentor.api.model.Modulo;
import com.techmentor.api.model.Parceira;
import com.techmentor.api.model.RegistroAuditoria;
import com.techmentor.api.model.SituacaoCurso;
import com.techmentor.api.repository.AulaRepository;
import com.techmentor.api.repository.CursoRepository;
import com.techmentor.api.repository.MatriculaRepository;
import com.techmentor.api.repository.ModuloRepository;
import com.techmentor.api.repository.ProgressoAulaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A gestão de cursos, módulos e aulas por id. É o "motor" com as regras de conteúdo; quem chama (hoje a administradora,
 * no futuro também as empresas parceiras, só nos cursos delas) não precisa repeti-las:
 * - um curso novo nasce em RASCUNHO e só vira PUBLICADO se estiver completo;
 * - quem já estudou é protegida: aula ou módulo com progresso não se apaga, e curso com progresso não se reordena;
 * - a posição de módulos e aulas só muda pelo "reordenar" (nunca por edição), e sempre numa transação.
 */
@Service
@RequiredArgsConstructor
public class GestaoCursoService {

    // Posição provisória durante a troca de ordem (o banco não aceita duas aulas na mesma posição, nem por um instante)
    private static final int POSICAO_PROVISORIA = 10_000;

    private final CursoRepository cursos;
    private final ModuloRepository modulos;
    private final AulaRepository aulas;
    private final ProgressoAulaRepository progressos;
    private final MatriculaRepository matriculas;
    private final AuditoriaService auditoria;
    private final ApplicationEventPublisher eventos;

    // ------------------------------------------------------------------ cursos

    @Transactional(readOnly = true)
    public List<CursoResponse> listar(SituacaoCurso filtro) {
        return cursos.findAll().stream()
                .filter(curso -> filtro == null || curso.getSituacao() == filtro)
                .sorted(Comparator.comparing(Curso::getTitulo, String.CASE_INSENSITIVE_ORDER))
                .map(this::resposta)
                .toList();
    }

    /** Os cursos de uma parceira, em qualquer situação. */
    @Transactional(readOnly = true)
    public List<CursoResponse> listarDaParceira(Parceira parceira, SituacaoCurso filtro) {
        return cursos.findByParceira(parceira).stream()
                .filter(curso -> filtro == null || curso.getSituacao() == filtro)
                .sorted(Comparator.comparing(Curso::getTitulo, String.CASE_INSENSITIVE_ORDER))
                .map(this::resposta)
                .toList();
    }

    @Transactional(readOnly = true)
    public CursoResponse detalhar(Long id) {
        return resposta(curso(id));
    }

    @Transactional(readOnly = true)
    public List<RegistroResponse> historico(Long id) {
        curso(id);   // 404 se o curso não existe
        return auditoria.historico(RegistroAuditoria.CURSO, id);
    }

    /** O curso novo sempre nasce em RASCUNHO: as alunas só o veem depois de publicado. */
    @Transactional
    public CursoResponse criar(CursoRequest dados) {
        return criar(dados, null);
    }

    /**
     * Com parceira, o curso fica dela e o crédito é sempre o nome dela: ela não escolhe (nem falsifica) a instituição.
     */
    @Transactional
    public CursoResponse criar(CursoRequest dados, Parceira parceira) {
        String titulo = dados.titulo().trim();
        if (cursos.existsByTituloIgnoreCase(titulo)) {
            throw new ConflictException("Já existe um curso com esse título.");
        }

        Curso curso = dados.toEntity();
        curso.setTitulo(titulo);
        curso.setSituacao(SituacaoCurso.RASCUNHO);
        if (parceira != null) {
            curso.setParceira(parceira);
            curso.setInstituicao(parceira.getNome());
        }

        Curso salvo = cursos.save(curso);
        auditoria.registrar(AcaoAuditada.CURSO_CRIADO, RegistroAuditoria.CURSO, salvo.getId(), "\"" + titulo + "\"");
        return resposta(salvo);
    }

    /** Edita os dados do curso. A situação não muda aqui: tem endpoint próprio, com as suas regras. */
    @Transactional
    public CursoResponse atualizar(Long id, CursoRequest dados) {
        Curso curso = curso(id);
        String titulo = dados.titulo().trim();

        if (!curso.getTitulo().equalsIgnoreCase(titulo) && cursos.existsByTituloIgnoreCase(titulo)) {
            throw new ConflictException("Já existe um curso com esse título.");
        }

        String instituicao = dados.instituicao() == null || dados.instituicao().isBlank() ? null : dados.instituicao().trim();

        List<String> mudou = new ArrayList<>();
        if (!Objects.equals(curso.getTitulo(), titulo)) mudou.add("título");
        if (!Objects.equals(curso.getDescricao(), dados.descricao())) mudou.add("descrição");
        if (curso.getCategoria() != dados.categoria()) mudou.add("categoria");
        if (curso.getNivel() != dados.nivel()) mudou.add("nível");
        if (!Objects.equals(curso.getIconeUrl(), dados.iconeUrl())) mudou.add("ícone");
        if (!Objects.equals(curso.getInstituicao(), instituicao)) mudou.add("instituição");

        curso.setTitulo(titulo);
        curso.setDescricao(dados.descricao());
        curso.setCategoria(dados.categoria());
        curso.setNivel(dados.nivel());
        curso.setIconeUrl(dados.iconeUrl());
        curso.setInstituicao(instituicao);
        Curso salvo = cursos.save(curso);

        if (!mudou.isEmpty()) {
            auditoria.registrar(AcaoAuditada.CURSO_EDITADO, RegistroAuditoria.CURSO, id, "Alterado: " + String.join(", ", mudou) + ".");
        }
        return resposta(salvo);
    }

    /**
     * Troca a situação pela administradora. Publicar um curso em revisão é aprová-lo (e a parceira é avisada).
     * EM_REVISAO não se escolhe aqui (é a parceira que envia) e sair dela para rascunho ou arquivado passa
     * por devolver ou recusar, que exigem um comentário.
     */
    @Transactional
    public CursoResponse mudarSituacao(Long id, SituacaoCurso nova) {
        Curso curso = curso(id);
        SituacaoCurso atual = curso.getSituacao();
        if (atual == nova) return resposta(curso);

        if (nova == SituacaoCurso.EM_REVISAO) {
            throw new BadRequestException("Só a empresa parceira envia o curso para revisão.");
        }
        if (atual == SituacaoCurso.EM_REVISAO && nova != SituacaoCurso.PUBLICADO) {
            throw new BadRequestException("Para tirar um curso da revisão sem publicar, use devolver ou recusar, com um comentário.");
        }
        if (nova == SituacaoCurso.PUBLICADO) {
            exigirCursoCompleto(curso);
        }
        if (nova == SituacaoCurso.RASCUNHO && matriculas.countByCurso(curso) > 0) {
            throw new ConflictException("Este curso já tem alunas matriculadas: arquive-o em vez de voltar para rascunho.");
        }

        curso.setSituacao(nova);
        if (nova == SituacaoCurso.PUBLICADO) curso.setComentarioRevisao(null);
        Curso salvo = cursos.save(curso);

        AcaoAuditada acao = switch (nova) {
            case PUBLICADO -> AcaoAuditada.CURSO_PUBLICADO;
            case ARQUIVADO -> AcaoAuditada.CURSO_ARQUIVADO;
            default -> AcaoAuditada.CURSO_VOLTOU_PARA_RASCUNHO;
        };
        auditoria.registrar(acao, RegistroAuditoria.CURSO, id, "De " + atual + " para " + nova + ".");

        if (atual == SituacaoCurso.EM_REVISAO) avisarParceira(salvo, DecisaoDeRevisao.APROVADO, null);
        return resposta(salvo);
    }

    /** A parceira termina o curso e pede a análise. Só rascunho, e só completo (as mesmas regras de publicar). */
    @Transactional
    public CursoResponse enviarParaRevisao(Long id) {
        Curso curso = curso(id);

        if (curso.getParceira() == null) {
            throw new BadRequestException("Só os cursos de empresas parceiras passam por revisão.");
        }
        switch (curso.getSituacao()) {
            case EM_REVISAO -> throw new ConflictException("Este curso já está em revisão.");
            case PUBLICADO -> throw new ConflictException("Este curso já está publicado.");
            case ARQUIVADO -> throw new ConflictException("Um curso arquivado não pode ser enviado para revisão.");
            default -> { }
        }
        exigirCursoCompleto(curso);

        curso.setSituacao(SituacaoCurso.EM_REVISAO);
        Curso salvo = cursos.save(curso);

        auditoria.registrar(AcaoAuditada.CURSO_ENVIADO_PARA_REVISAO, RegistroAuditoria.CURSO, id, null);
        eventos.publishEvent(new CursoEnviadoParaRevisao(id, salvo.getTitulo(), curso.getParceira().getNome()));
        return resposta(salvo);
    }

    /** A administradora manda o curso de volta para a parceira ajustar. O comentário fica no curso até ele ser publicado. */
    @Transactional
    public CursoResponse devolver(Long id, String comentario) {
        Curso curso = emRevisao(id);
        curso.setSituacao(SituacaoCurso.RASCUNHO);
        curso.setComentarioRevisao(comentario.trim());
        Curso salvo = cursos.save(curso);

        auditoria.registrar(AcaoAuditada.CURSO_DEVOLVIDO, RegistroAuditoria.CURSO, id, comentario.trim());
        avisarParceira(salvo, DecisaoDeRevisao.DEVOLVIDO, comentario.trim());
        return resposta(salvo);
    }

    /** A administradora recusa o curso: ele é arquivado e a parceira lê o motivo. */
    @Transactional
    public CursoResponse recusar(Long id, String comentario) {
        Curso curso = emRevisao(id);
        curso.setSituacao(SituacaoCurso.ARQUIVADO);
        curso.setComentarioRevisao(comentario.trim());
        Curso salvo = cursos.save(curso);

        auditoria.registrar(AcaoAuditada.CURSO_RECUSADO, RegistroAuditoria.CURSO, id, comentario.trim());
        avisarParceira(salvo, DecisaoDeRevisao.RECUSADO, comentario.trim());
        return resposta(salvo);
    }

    private Curso emRevisao(Long id) {
        Curso curso = curso(id);
        if (curso.getSituacao() != SituacaoCurso.EM_REVISAO) {
            throw new ConflictException("Este curso não está em revisão.");
        }
        return curso;
    }

    private void avisarParceira(Curso curso, DecisaoDeRevisao decisao, String comentario) {
        if (curso.getParceira() == null) return;
        eventos.publishEvent(new CursoRevisado(curso.getParceira().getUsuario(), curso.getId(), curso.getTitulo(), decisao, comentario));
    }

    /** Só publica o que as alunas conseguem estudar: descrição, ao menos um módulo e nenhum módulo vazio. */
    private void exigirCursoCompleto(Curso curso) {
        if (curso.getDescricao() == null || curso.getDescricao().isBlank()) {
            throw new BadRequestException("Escreva a descrição do curso antes de publicar.");
        }
        if (curso.getModulos().isEmpty()) {
            throw new BadRequestException("Adicione pelo menos um módulo antes de publicar.");
        }
        for (Modulo modulo : curso.getModulos()) {
            if (modulo.getAulas().isEmpty()) {
                throw new BadRequestException("O módulo \"" + modulo.getTitulo() + "\" está sem aulas. Adicione uma aula ou apague o módulo.");
            }
        }
    }

    // ------------------------------------------------------------------ módulos

    @Transactional
    public ModuloResponse criarModulo(Long cursoId, ModuloGestaoRequest dados) {
        Curso curso = curso(cursoId);
        int proxima = curso.getModulos().stream().mapToInt(Modulo::getOrdem).max().orElse(0) + 1;

        Modulo modulo = new Modulo();
        modulo.setCurso(curso);
        modulo.setTitulo(dados.titulo().trim());
        modulo.setOrdem(proxima);
        modulo = modulos.save(modulo);
        curso.getModulos().add(modulo);

        auditoria.registrar(AcaoAuditada.MODULO_CRIADO, RegistroAuditoria.CURSO, cursoId, "\"" + modulo.getTitulo() + "\"");
        return ModuloResponse.from(modulo);
    }

    @Transactional
    public ModuloResponse atualizarModulo(Long id, ModuloGestaoRequest dados) {
        Modulo modulo = modulo(id);
        String antes = modulo.getTitulo();
        modulo.setTitulo(dados.titulo().trim());
        Modulo salvo = modulos.save(modulo);

        auditoria.registrar(AcaoAuditada.MODULO_EDITADO, RegistroAuditoria.CURSO, modulo.getCurso().getId(),
                "\"" + antes + "\" passou a \"" + salvo.getTitulo() + "\"");
        return ModuloResponse.from(salvo);
    }

    @Transactional
    public void apagarModulo(Long id) {
        Modulo modulo = modulo(id);

        long alunas = progressos.alunasDoModulo(modulo);
        if (alunas > 0) {
            throw new ConflictException(avisoDeProgresso(alunas, "este módulo"));
        }

        Curso curso = modulo.getCurso();
        if (curso.getSituacao() == SituacaoCurso.PUBLICADO && curso.getModulos().size() == 1) {
            throw new ConflictException("Este curso está publicado e ficaria sem módulos. Arquive o curso ou crie outro módulo antes.");
        }

        String titulo = modulo.getTitulo();
        curso.getModulos().remove(modulo);
        modulos.delete(modulo);   // apaga de fato (as aulas dele vão junto); não depende da detecção de "órfão"
        modulos.flush();

        aplicarOrdemDosModulos(new ArrayList<>(curso.getModulos()));   // fecha o buraco que ficou
        auditoria.registrar(AcaoAuditada.MODULO_APAGADO, RegistroAuditoria.CURSO, curso.getId(), "\"" + titulo + "\"");
    }

    @Transactional
    public CursoResponse reordenarModulos(Long cursoId, List<Long> ids) {
        Curso curso = curso(cursoId);
        List<Modulo> atuais = new ArrayList<>(curso.getModulos());

        exigirMesmosIds(atuais.stream().map(Modulo::getId).toList(), ids, "módulos");

        long alunas = progressos.alunasDoCurso(curso);
        if (alunas > 0) {
            throw new ConflictException(avisoDeProgresso(alunas, "este curso")
                    + " Reordenar os módulos mudaria a trilha delas.");
        }

        Map<Long, Modulo> porId = new HashMap<>();
        atuais.forEach(modulo -> porId.put(modulo.getId(), modulo));

        aplicarOrdemDosModulos(ids.stream().map(porId::get).toList());
        curso.getModulos().sort(Comparator.comparingInt(Modulo::getOrdem));

        auditoria.registrar(AcaoAuditada.MODULOS_REORDENADOS, RegistroAuditoria.CURSO, cursoId, null);
        return resposta(curso);
    }

    // ------------------------------------------------------------------ aulas

    @Transactional
    public AulaResponse criarAula(Long moduloId, AulaGestaoRequest dados) {
        Modulo modulo = modulo(moduloId);
        int proxima = modulo.getAulas().stream().mapToInt(Aula::getOrdem).max().orElse(0) + 1;

        Aula aula = new Aula();
        aula.setModulo(modulo);
        aula.setOrdem(proxima);
        preencher(aula, dados);
        aula = aulas.save(aula);
        modulo.getAulas().add(aula);

        auditoria.registrar(AcaoAuditada.AULA_CRIADA, RegistroAuditoria.CURSO, modulo.getCurso().getId(),
                "\"" + aula.getTitulo() + "\" em \"" + modulo.getTitulo() + "\"");
        return AulaResponse.from(aula);
    }

    @Transactional
    public AulaResponse atualizarAula(Long id, AulaGestaoRequest dados) {
        Aula aula = aula(id);
        String antes = aula.getTitulo();
        String linkAntes = aula.getConteudoUrl();
        preencher(aula, dados);
        Aula salva = aulas.save(aula);

        String detalhe = "\"" + antes + "\"" + (Objects.equals(linkAntes, salva.getConteudoUrl()) ? "" : " (link do vídeo trocado)");
        auditoria.registrar(AcaoAuditada.AULA_EDITADA, RegistroAuditoria.CURSO, aula.getModulo().getCurso().getId(), detalhe);
        return AulaResponse.from(salva);
    }

    private void preencher(Aula aula, AulaGestaoRequest dados) {
        aula.setTitulo(dados.titulo().trim());
        aula.setDuracaoMinutos(dados.duracaoMinutos());
        if (dados.xpRecompensa() != null) aula.setXpRecompensa(dados.xpRecompensa());
        aula.setConteudoUrl(dados.conteudoUrl() == null || dados.conteudoUrl().isBlank() ? null : dados.conteudoUrl().trim());
    }

    @Transactional
    public void apagarAula(Long id) {
        Aula aula = aula(id);

        long alunas = progressos.alunasDaAula(aula);
        if (alunas > 0) {
            throw new ConflictException(avisoDeProgresso(alunas, "esta aula"));
        }

        Modulo modulo = aula.getModulo();
        if (modulo.getCurso().getSituacao() == SituacaoCurso.PUBLICADO && modulo.getAulas().size() == 1) {
            throw new ConflictException("Este curso está publicado e o módulo \"" + modulo.getTitulo()
                    + "\" ficaria sem aulas. Adicione outra aula antes de apagar esta.");
        }

        String titulo = aula.getTitulo();
        modulo.getAulas().remove(aula);
        aulas.delete(aula);   // apaga de fato; não depende da detecção de "órfão"
        aulas.flush();

        aplicarOrdemDasAulas(new ArrayList<>(modulo.getAulas()));   // fecha o buraco que ficou
        auditoria.registrar(AcaoAuditada.AULA_APAGADA, RegistroAuditoria.CURSO, modulo.getCurso().getId(),
                "\"" + titulo + "\" de \"" + modulo.getTitulo() + "\"");
    }

    @Transactional
    public ModuloResponse reordenarAulas(Long moduloId, List<Long> ids) {
        Modulo modulo = modulo(moduloId);
        List<Aula> atuais = new ArrayList<>(modulo.getAulas());

        exigirMesmosIds(atuais.stream().map(Aula::getId).toList(), ids, "aulas");

        long alunas = progressos.alunasDoModulo(modulo);
        if (alunas > 0) {
            throw new ConflictException(avisoDeProgresso(alunas, "este módulo")
                    + " Reordenar as aulas mudaria a trilha delas.");
        }

        Map<Long, Aula> porId = new HashMap<>();
        atuais.forEach(aula -> porId.put(aula.getId(), aula));

        aplicarOrdemDasAulas(ids.stream().map(porId::get).toList());
        modulo.getAulas().sort(Comparator.comparingInt(Aula::getOrdem));

        auditoria.registrar(AcaoAuditada.AULAS_REORDENADAS, RegistroAuditoria.CURSO, modulo.getCurso().getId(),
                "Módulo \"" + modulo.getTitulo() + "\"");
        return ModuloResponse.from(modulo);
    }

    // ------------------------------------------------------------------ métricas

    /** Números agregados do curso. Nunca devolve quem são as alunas. */
    @Transactional(readOnly = true)
    public MetricasCursoResponse metricas(Long id) {
        Curso curso = curso(id);

        long matriculadas = matriculas.countByCurso(curso);
        long concluintes = matriculas.countByCursoAndConcluidoEmIsNotNull(curso);

        Map<Long, Long> porAula = new HashMap<>();
        for (Object[] linha : progressos.concluintesPorAula(curso)) {
            porAula.put((Long) linha[0], (Long) linha[1]);
        }

        List<AulaMetrica> lista = new ArrayList<>();
        for (Modulo modulo : curso.getModulos()) {
            for (Aula aula : modulo.getAulas()) {
                lista.add(new AulaMetrica(aula.getId(), modulo.getOrdem(), aula.getOrdem(), aula.getTitulo(),
                        porAula.getOrDefault(aula.getId(), 0L)));
            }
        }

        // Onde mais gente para: a maior queda entre uma aula e a seguinte (quem concluiu a aula, mas não a próxima)
        AulaMetrica ondePararam = null;
        long pararam = 0;
        for (int i = 0; i < lista.size() - 1; i++) {
            long queda = lista.get(i).concluiram() - lista.get(i + 1).concluiram();
            if (queda > pararam) {
                pararam = queda;
                ondePararam = lista.get(i);
            }
        }

        int taxa = matriculadas == 0 ? 0 : (int) (concluintes * 100 / matriculadas);

        return new MetricasCursoResponse(curso.getId(), curso.getTitulo(), curso.getSituacao(), matriculadas,
                progressos.alunasDoCurso(curso), concluintes, taxa, lista, ondePararam, pararam);
    }

    // ------------------------------------------------------------------ apoio

    /** O curso de um módulo ou de uma aula: a camada da parceira usa para conferir de quem é antes de mexer. */
    @Transactional(readOnly = true)
    public Curso cursoPorId(Long id) {
        return curso(id);
    }

    @Transactional(readOnly = true)
    public Curso cursoDoModulo(Long moduloId) {
        return modulo(moduloId).getCurso();
    }

    @Transactional(readOnly = true)
    public Curso cursoDaAula(Long aulaId) {
        return aula(aulaId).getModulo().getCurso();
    }

    private Curso curso(Long id) {
        return cursos.findById(id).orElseThrow(() -> new NotFoundException("Curso não encontrado!"));
    }

    private Modulo modulo(Long id) {
        return modulos.findById(id).orElseThrow(() -> new NotFoundException("Módulo não encontrado!"));
    }

    private Aula aula(Long id) {
        return aulas.findById(id).orElseThrow(() -> new NotFoundException("Aula não encontrada!"));
    }

    private CursoResponse resposta(Curso curso) {
        curso.setTotalAulas(aulas.countByModulo_Curso(curso));
        return CursoResponse.from(curso);
    }

    private static String avisoDeProgresso(long alunas, String oQue) {
        return alunas + (alunas == 1 ? " aluna já estudou " : " alunas já estudaram ") + oQue
                + ". Para tirar o curso do ar, arquive-o; para corrigir, edite o conteúdo em vez de apagar.";
    }

    /** A lista nova precisa ter exatamente os itens atuais, cada um uma vez. */
    private static void exigirMesmosIds(List<Long> atuais, List<Long> novos, String oQue) {
        Set<Long> unicos = new HashSet<>(novos);
        if (novos.size() != atuais.size() || unicos.size() != novos.size() || !unicos.equals(new HashSet<>(atuais))) {
            throw new BadRequestException("A lista precisa ter exatamente os " + oQue + " atuais, cada um uma vez.");
        }
    }

    /** Dá as posições 1..n na sequência recebida, em duas fases para nunca repetir uma posição no banco. */
    private void aplicarOrdemDosModulos(List<Modulo> naSequencia) {
        for (int i = 0; i < naSequencia.size(); i++) naSequencia.get(i).setOrdem(POSICAO_PROVISORIA + i);
        modulos.saveAllAndFlush(naSequencia);

        for (int i = 0; i < naSequencia.size(); i++) naSequencia.get(i).setOrdem(i + 1);
        modulos.saveAllAndFlush(naSequencia);
    }

    private void aplicarOrdemDasAulas(List<Aula> naSequencia) {
        for (int i = 0; i < naSequencia.size(); i++) naSequencia.get(i).setOrdem(POSICAO_PROVISORIA + i);
        aulas.saveAllAndFlush(naSequencia);

        for (int i = 0; i < naSequencia.size(); i++) naSequencia.get(i).setOrdem(i + 1);
        aulas.saveAllAndFlush(naSequencia);
    }
}
