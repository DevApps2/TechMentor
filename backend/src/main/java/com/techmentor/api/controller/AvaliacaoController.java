package com.techmentor.api.controller;

import com.techmentor.api.dto.AvaliacaoDtos.AvaliacaoDaMentoriaResponse;
import com.techmentor.api.dto.AvaliacaoDtos.AvaliacaoPendenteResponse;
import com.techmentor.api.dto.AvaliacaoDtos.AvaliacaoRequest;
import com.techmentor.api.dto.AvaliacaoDtos.AvaliacaoResponse;
import com.techmentor.api.dto.AvaliacaoDtos.AvaliacoesDaMentoraResponse;
import com.techmentor.api.service.AvaliacaoService;
import com.techmentor.api.service.UsuarioAtualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.techmentor.api.dto.AvaliacaoDtos.OcultacaoRequest;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Avaliação", description = "Avaliação de mentorias concluídas, de 1 a 5 estrelas (a aluna avalia a mentora e a mentora avalia a aluna)")
public class AvaliacaoController {
    private final AvaliacaoService avaliacoes;
    private final UsuarioAtualService usuarios;

    public AvaliacaoController(AvaliacaoService avaliacoes, UsuarioAtualService usuarios) {
        this.avaliacoes = avaliacoes;
        this.usuarios = usuarios;
    }

    @Operation(summary = "Avaliar uma mentoria concluída",
            description = "Só quem participou, uma vez, dentro do prazo, e só depois de a mentoria ser concluída. "
                    + "Retorna 409 se não puder avaliar (o motivo vem na mensagem) e 403 se não participou.")
    @PostMapping("/agendamentos/{id}/avaliacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public AvaliacaoResponse avaliar(@Parameter(description = "ID do agendamento") @PathVariable Long id,
                                     @Valid @RequestBody AvaliacaoRequest request) {
        return avaliacoes.avaliar(usuarios.buscarAtual(), id, request);
    }

    @Operation(summary = "Situação da avaliação de uma mentoria",
            description = "Diz se a pessoa pode avaliar agora (e o motivo, se não), o prazo, o que ela já avaliou "
                    + "e o que a outra pessoa escreveu sobre ela.")
    @GetMapping("/agendamentos/{id}/avaliacoes")
    public AvaliacaoDaMentoriaResponse situacao(@Parameter(description = "ID do agendamento") @PathVariable Long id) {
        return avaliacoes.situacao(usuarios.buscarAtual(), id);
    }

    @Operation(summary = "Minhas avaliações pendentes",
            description = "Mentorias concluídas, dentro do prazo, que a pessoa ainda não avaliou.")
    @GetMapping("/avaliacoes/pendentes")
    public List<AvaliacaoPendenteResponse> pendentes() {
        return avaliacoes.pendentes(usuarios.buscarAtual());
    }

    @Operation(summary = "O que escreveram sobre mim",
            description = "As avaliações que a pessoa recebeu (sem as ocultadas pela moderação), da mais nova para a mais antiga.")
    @GetMapping("/avaliacoes/recebidas")
    public List<AvaliacaoResponse> recebidas() {
        return avaliacoes.recebidas(usuarios.buscarAtual());
    }

    @Operation(summary = "Avaliações públicas de uma mentora",
            description = "Nota média, distribuição por estrela e os comentários mais recentes, com o nome da aluna abreviado. "
                    + "Não precisa de cabeçalho.")
    @GetMapping("/mentoras/{id}/avaliacoes")
    public AvaliacoesDaMentoraResponse daMentora(@Parameter(description = "ID do perfil da mentora") @PathVariable Long id,
                                                 @Parameter(description = "Quantas avaliações trazer (padrão 20, máximo 50)")
                                                 @RequestParam(defaultValue = "20") int limite) {
        return avaliacoes.daMentora(id, limite);
    }

    @Operation(summary = "Esconder ou mostrar o comentário de uma avaliação",
            description = "Só para administradoras (moderação). O comentário deixa de aparecer para todo mundo, "
                    + "mas a nota continua valendo na média.")
    @PatchMapping("/admin/avaliacoes/{id}/ocultacao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ocultar(@Parameter(description = "ID da avaliação") @PathVariable Long id,
                        @Valid @RequestBody OcultacaoRequest request) {
        avaliacoes.ocultar(usuarios.buscarAtual(), id, request.oculta());
    }
}