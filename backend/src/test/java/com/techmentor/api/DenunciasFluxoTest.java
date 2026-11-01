package com.techmentor.api;

import com.techmentor.api.dto.SegurancaDtos.DenunciaRequest;
import com.techmentor.api.dto.SegurancaDtos.DenunciaResponse;
import com.techmentor.api.exceptions.ApiException;
import com.techmentor.api.model.Denuncia;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.UsuarioRepository;
import com.techmentor.api.service.DenunciaService;
import com.techmentor.api.service.NotificacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Regras do fluxo de denúncia: limites contra abuso, quem pode mudar o status e o aviso a quem denunciou. */
@SpringBootTest
@Transactional
class DenunciasFluxoTest {

    @Autowired
    private DenunciaService denuncias;

    @Autowired
    private NotificacaoService notificacoes;

    @Autowired
    private UsuarioRepository usuarios;

    private Usuario criarUsuario(String nome, Usuario.Tipo tipo) {
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail("t" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com");
        u.setSenhaHash("hash-de-teste");
        u.setTipo(tipo);
        return usuarios.save(u);
    }

    private DenunciaRequest relato(Long denunciadoId) {
        return new DenunciaRequest(denunciadoId, Denuncia.Motivo.ASSEDIO, "Mensagens ofensivas no chat.");
    }

    @Test
    void naoDeixaDenunciarAMesmaPessoaDuasVezesEm24Horas() {
        Usuario quemDenuncia = criarUsuario("Denunciante", Usuario.Tipo.ALUNA);
        Usuario alvo = criarUsuario("Alvo", Usuario.Tipo.ALUNA);

        denuncias.criar(quemDenuncia, relato(alvo.getId()));

        ApiException erro = assertThrows(ApiException.class, () -> denuncias.criar(quemDenuncia, relato(alvo.getId())));

        assertEquals(HttpStatus.CONFLICT, erro.getStatus());
    }

    @Test
    void limitaQuantasDenunciasPorHora() {
        Usuario quemDenuncia = criarUsuario("Denunciante", Usuario.Tipo.ALUNA);

        // denúncias gerais (sem alvo) não caem na regra de "mesma pessoa", só no limite por hora
        for (int i = 0; i < 5; i++) {
            denuncias.criar(quemDenuncia, relato(null));
        }

        ApiException erro = assertThrows(ApiException.class, () -> denuncias.criar(quemDenuncia, relato(null)));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, erro.getStatus());
    }

    @Test
    void soAdministradorMudaOStatus() {
        Usuario aluna = criarUsuario("Denunciante", Usuario.Tipo.ALUNA);
        DenunciaResponse criada = denuncias.criar(aluna, relato(null));

        ApiException erro = assertThrows(ApiException.class,
                () -> denuncias.atualizarStatus(aluna, criada.id(), Denuncia.Status.RESOLVIDA));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
    }

    @Test
    void mudarOStatusAvisaQuemDenunciouUmaVezSo() {
        Usuario admin = criarUsuario("Equipe", Usuario.Tipo.ADMIN);
        Usuario aluna = criarUsuario("Denunciante", Usuario.Tipo.ALUNA);
        Usuario alvo = criarUsuario("Pessoa Denunciada", Usuario.Tipo.ALUNA);

        DenunciaResponse criada = denuncias.criar(aluna, relato(alvo.getId()));
        assertEquals(0, notificacoes.listarDTO(aluna.getId(), null).size());

        DenunciaResponse emAnalise = denuncias.atualizarStatus(admin, criada.id(), Denuncia.Status.EM_ANALISE);

        assertEquals(Denuncia.Status.EM_ANALISE, emAnalise.status());
        assertEquals("Pessoa Denunciada", emAnalise.denunciadoNome());
        assertEquals(1, notificacoes.listarDTO(aluna.getId(), null).size());

        // repetir o mesmo status não gera outra notificação
        denuncias.atualizarStatus(admin, criada.id(), Denuncia.Status.EM_ANALISE);
        assertEquals(1, notificacoes.listarDTO(aluna.getId(), null).size());

        denuncias.atualizarStatus(admin, criada.id(), Denuncia.Status.RESOLVIDA);
        assertEquals(2, notificacoes.listarDTO(aluna.getId(), null).size());
    }
}
