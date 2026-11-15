package com.techmentor.api.service;

import com.techmentor.api.dto.AuditoriaDtos.RegistroResponse;
import com.techmentor.api.dto.CursoDtos.AulaResponse;
import com.techmentor.api.dto.CursoDtos.CursoRequest;
import com.techmentor.api.dto.CursoDtos.CursoResponse;
import com.techmentor.api.dto.CursoDtos.ModuloResponse;
import com.techmentor.api.dto.GestaoCursoDtos.AulaGestaoRequest;
import com.techmentor.api.dto.GestaoCursoDtos.MetricasCursoResponse;
import com.techmentor.api.dto.GestaoCursoDtos.ModuloGestaoRequest;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.ForbiddenException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.Curso;
import com.techmentor.api.model.Parceira;
import com.techmentor.api.model.SituacaoCurso;
import com.techmentor.api.model.SituacaoParceira;
import com.techmentor.api.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * A porta de entrada da parceira para a gestão de cursos. As regras de conteúdo são as do GestaoCursoService
 * (nada é copiado); aqui só se confere QUEM pode: a parceira precisa estar verificada e ativa, o curso precisa ser
 * dela (de outra, responde "não encontrado" para não revelar que existe) e não pode estar em revisão nem arquivado.
 */
@Service
@RequiredArgsConstructor
public class ParceiraCursoService {

    private final ParceiraService parceiras;
    private final GestaoCursoService gestao;

    // ------------------------------------------------------------------ leitura (qualquer situação da parceira)

    @Transactional(readOnly = true)
    public List<CursoResponse> listar(Usuario ator, SituacaoCurso filtro) {
        return gestao.listarDaParceira(parceiras.daConta(ator), filtro);
    }

    @Transactional(readOnly = true)
    public CursoResponse detalhar(Usuario ator, Long cursoId) {
        meu(parceiras.daConta(ator), gestao.cursoPorId(cursoId));
        return gestao.detalhar(cursoId);
    }

    @Transactional(readOnly = true)
    public MetricasCursoResponse metricas(Usuario ator, Long cursoId) {
        meu(parceiras.daConta(ator), gestao.cursoPorId(cursoId));
        return gestao.metricas(cursoId);
    }

    @Transactional(readOnly = true)
    public List<RegistroResponse> historico(Usuario ator, Long cursoId) {
        meu(parceiras.daConta(ator), gestao.cursoPorId(cursoId));
        return gestao.historico(cursoId);
    }

    // ------------------------------------------------------------------ escrita (parceira ativa, curso editável)

    @Transactional
    public CursoResponse criar(Usuario ator, CursoRequest dados) {
        return gestao.criar(dados, ativa(ator));
    }

    @Transactional
    public CursoResponse atualizar(Usuario ator, Long cursoId, CursoRequest dados) {
        Parceira parceira = ativa(ator);
        editavel(meu(parceira, gestao.cursoPorId(cursoId)));

        // O crédito é sempre o nome da parceira: o que vier no pedido é ignorado
        CursoRequest comCredito = new CursoRequest(dados.titulo(), dados.descricao(), dados.categoria(), dados.iconeUrl(),
                dados.ativo(), dados.nivel(), parceira.getNome(), dados.situacao());
        return gestao.atualizar(cursoId, comCredito);
    }

    @Transactional
    public CursoResponse enviarParaRevisao(Usuario ator, Long cursoId) {
        meu(ativa(ator), gestao.cursoPorId(cursoId));
        return gestao.enviarParaRevisao(cursoId);
    }

    @Transactional
    public ModuloResponse criarModulo(Usuario ator, Long cursoId, ModuloGestaoRequest dados) {
        editavel(meu(ativa(ator), gestao.cursoPorId(cursoId)));
        return gestao.criarModulo(cursoId, dados);
    }

    @Transactional
    public CursoResponse reordenarModulos(Usuario ator, Long cursoId, List<Long> ids) {
        editavel(meu(ativa(ator), gestao.cursoPorId(cursoId)));
        return gestao.reordenarModulos(cursoId, ids);
    }

    @Transactional
    public ModuloResponse atualizarModulo(Usuario ator, Long moduloId, ModuloGestaoRequest dados) {
        editavel(meu(ativa(ator), gestao.cursoDoModulo(moduloId)));
        return gestao.atualizarModulo(moduloId, dados);
    }

    @Transactional
    public void apagarModulo(Usuario ator, Long moduloId) {
        editavel(meu(ativa(ator), gestao.cursoDoModulo(moduloId)));
        gestao.apagarModulo(moduloId);
    }

    @Transactional
    public AulaResponse criarAula(Usuario ator, Long moduloId, AulaGestaoRequest dados) {
        editavel(meu(ativa(ator), gestao.cursoDoModulo(moduloId)));
        return gestao.criarAula(moduloId, dados);
    }

    @Transactional
    public ModuloResponse reordenarAulas(Usuario ator, Long moduloId, List<Long> ids) {
        editavel(meu(ativa(ator), gestao.cursoDoModulo(moduloId)));
        return gestao.reordenarAulas(moduloId, ids);
    }

    @Transactional
    public AulaResponse atualizarAula(Usuario ator, Long aulaId, AulaGestaoRequest dados) {
        editavel(meu(ativa(ator), gestao.cursoDaAula(aulaId)));
        return gestao.atualizarAula(aulaId, dados);
    }

    @Transactional
    public void apagarAula(Usuario ator, Long aulaId) {
        editavel(meu(ativa(ator), gestao.cursoDaAula(aulaId)));
        gestao.apagarAula(aulaId);
    }

    // ------------------------------------------------------------------ conferências

    /** A parceira da conta, e só se estiver verificada e sem suspensão. */
    private Parceira ativa(Usuario ator) {
        Parceira parceira = parceiras.daConta(ator);
        if (parceira.getSituacao() == SituacaoParceira.CONVIDADA) {
            throw new ForbiddenException("Sua conta ainda está em verificação pela equipe do TechMentor.");
        }
        if (parceira.getSituacao() == SituacaoParceira.SUSPENSA) {
            throw new ForbiddenException("Sua conta de parceira está suspensa.");
        }
        return parceira;
    }

    /** O curso é desta parceira? Se for de outra (ou do TechMentor), responde como se não existisse. */
    private static Curso meu(Parceira parceira, Curso curso) {
        if (curso.getParceira() == null || !curso.getParceira().getId().equals(parceira.getId())) {
            throw new NotFoundException("Curso não encontrado!");
        }
        return curso;
    }

    /** Em revisão a parceira espera a resposta; arquivado não se mexe mais. */
    private static void editavel(Curso curso) {
        if (curso.getSituacao() == SituacaoCurso.EM_REVISAO) {
            throw new ConflictException("Este curso está em revisão: aguarde a resposta da equipe do TechMentor.");
        }
        if (curso.getSituacao() == SituacaoCurso.ARQUIVADO) {
            throw new ConflictException("Este curso está arquivado e não pode mais ser editado.");
        }
    }
}
