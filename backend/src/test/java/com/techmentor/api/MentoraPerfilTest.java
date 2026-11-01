package com.techmentor.api;

import com.techmentor.api.dto.MentoriaDtos.MentoraRequest;
import com.techmentor.api.dto.MentoriaDtos.MentoraResponse;
import com.techmentor.api.exceptions.ApiException;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.UsuarioRepository;
import com.techmentor.api.service.MentoraService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Quem pode ter perfil de mentora e o que acontece no primeiro acesso. */
@SpringBootTest
@Transactional
class MentoraPerfilTest {

    @Autowired
    private MentoraService mentoras;

    @Autowired
    private UsuarioRepository usuarios;

    private Usuario criarUsuario(Usuario.Tipo tipo) {
        Usuario u = new Usuario();
        u.setNome("Teste " + tipo);
        u.setEmail("t" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com");
        u.setSenhaHash("hash-de-teste");
        u.setTipo(tipo);
        return usuarios.save(u);
    }

    private MentoraRequest perfil() {
        return new MentoraRequest("Bio de teste com mais de dez letras.", "Engenheira", "Empresa X", 5, Set.of());
    }

    @Test
    void alunaNaoPodeCriarPerfilDeMentora() {
        Usuario aluna = criarUsuario(Usuario.Tipo.ALUNA);

        ApiException erro = assertThrows(ApiException.class, () -> mentoras.criarMentora(aluna, perfil()));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
    }

    @Test
    void mentoraSemPerfilRecebe404() {
        Usuario mentora = criarUsuario(Usuario.Tipo.MENTORA);

        ApiException erro = assertThrows(ApiException.class, () -> mentoras.minhaMentora(mentora));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatus());
    }

    @Test
    void depoisDeCriarOPerfilOMinhaDevolveOMesmo() {
        Usuario mentora = criarUsuario(Usuario.Tipo.MENTORA);

        MentoraResponse criada = mentoras.criarMentora(mentora, perfil());
        MentoraResponse minha = mentoras.minhaMentora(mentora);

        assertEquals(criada.id(), minha.id());
        assertEquals("Engenheira", minha.cargo());
    }

    @Test
    void naoDeixaCriarDoisPerfisParaAMesmaMentora() {
        Usuario mentora = criarUsuario(Usuario.Tipo.MENTORA);
        mentoras.criarMentora(mentora, perfil());

        ApiException erro = assertThrows(ApiException.class, () -> mentoras.criarMentora(mentora, perfil()));

        assertEquals(HttpStatus.CONFLICT, erro.getStatus());
    }
}