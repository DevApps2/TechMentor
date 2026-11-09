package com.techmentor.api;

import com.techmentor.api.dto.SegurancaDtos.BloqueioResponse;
import com.techmentor.api.exceptions.ApiException;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.UsuarioRepository;
import com.techmentor.api.service.BloqueioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bloquear e desbloquear: cada pessoa vê só os próprios bloqueios, e as regras de erro são claras. */
@SpringBootTest
@Transactional
class BloqueioDeUsuarioTest {

    @Autowired
    private BloqueioService bloqueios;

    @Autowired
    private UsuarioRepository usuarios;

    private Usuario ana;
    private Usuario maria;
    private Usuario carla;

    private Usuario criarUsuario(String nome) {
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail("b" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com");
        u.setSenhaHash("hash-de-teste");
        return usuarios.save(u);
    }

    private static HttpStatus statusDe(Executable acao) {
        return assertThrows(ApiException.class, acao).getStatus();
    }

    @BeforeEach
    void preparar() {
        ana = criarUsuario("Ana");
        maria = criarUsuario("Maria");
        carla = criarUsuario("Carla");
    }

    @Test
    void bloquearFazAPessoaAparecerNaLista() {
        BloqueioResponse resposta = bloqueios.bloquear(ana, maria.getId());

        assertEquals(maria.getId(), resposta.usuarioId());
        assertEquals("Maria", resposta.nome());

        List<BloqueioResponse> lista = bloqueios.listar(ana);
        assertEquals(1, lista.size());
        assertEquals(maria.getId(), lista.get(0).usuarioId());
    }

    @Test
    void cadaPessoaVeSoOsProprioBloqueios() {
        bloqueios.bloquear(ana, maria.getId());

        assertTrue(bloqueios.listar(maria).isEmpty());
        assertTrue(bloqueios.listar(carla).isEmpty());
    }

    @Test
    void naoBloqueiaASiMesmaNemDuasVezesNemQuemNaoExiste() {
        assertEquals(HttpStatus.BAD_REQUEST, statusDe(() -> bloqueios.bloquear(ana, ana.getId())));
        assertEquals(HttpStatus.BAD_REQUEST, statusDe(() -> bloqueios.bloquear(ana, null)));
        assertEquals(HttpStatus.NOT_FOUND, statusDe(() -> bloqueios.bloquear(ana, 999_999L)));

        bloqueios.bloquear(ana, maria.getId());
        assertEquals(HttpStatus.CONFLICT, statusDe(() -> bloqueios.bloquear(ana, maria.getId())));
    }

    @Test
    void bloquearNaoBloqueiaNaDirecaoContraria() {
        bloqueios.bloquear(ana, maria.getId());

        // a Maria pode bloquear a Ana por conta própria: são dois bloqueios independentes
        bloqueios.bloquear(maria, ana.getId());

        assertEquals(1, bloqueios.listar(ana).size());
        assertEquals(1, bloqueios.listar(maria).size());
    }

    @Test
    void desbloquearTiraDaListaESoSeExistia() {
        bloqueios.bloquear(ana, maria.getId());

        bloqueios.desbloquear(ana, maria.getId());

        assertTrue(bloqueios.listar(ana).isEmpty());
        assertEquals(HttpStatus.NOT_FOUND, statusDe(() -> bloqueios.desbloquear(ana, maria.getId())));
        // quem nunca bloqueou ninguém também recebe 404
        assertEquals(HttpStatus.NOT_FOUND, statusDe(() -> bloqueios.desbloquear(carla, maria.getId())));
    }
}
