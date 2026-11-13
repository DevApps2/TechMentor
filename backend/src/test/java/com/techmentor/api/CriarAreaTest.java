package com.techmentor.api;

import com.techmentor.api.dto.MentoriaDtos.AreaResponse;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.ForbiddenException;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.UsuarioRepository;
import com.techmentor.api.service.MentoraService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** A administradora e a própria mentora podem criar uma área de atuação; a aluna não, e o nome não repete. */
@SpringBootTest
@Transactional
class CriarAreaTest {

    @Autowired
    private MentoraService mentoras;

    @Autowired
    private UsuarioRepository usuarios;

    private Usuario conta(Usuario.Tipo tipo) {
        Usuario u = new Usuario();
        u.setNome("Teste " + tipo);
        u.setEmail("t" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com");
        u.setSenhaHash("hash-de-teste");
        u.setTipo(tipo);
        return usuarios.save(u);
    }

    private static String nomeUnico() {
        return "Area " + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void aMentoraCriaUmaArea() {
        String nome = nomeUnico();

        AreaResponse criada = mentoras.criarArea(conta(Usuario.Tipo.MENTORA), "  " + nome + "  ");

        assertEquals(nome, criada.nome());   // o nome vem sem os espaços das pontas
    }

    @Test
    void aAdminTambemCria() {
        String nome = nomeUnico();

        assertEquals(nome, mentoras.criarArea(conta(Usuario.Tipo.ADMIN), nome).nome());
    }

    @Test
    void aAlunaNaoCria() {
        Usuario aluna = conta(Usuario.Tipo.ALUNA);

        assertThrows(ForbiddenException.class, () -> mentoras.criarArea(aluna, nomeUnico()));
    }

    @Test
    void oNomeNaoRepete() {
        Usuario mentora = conta(Usuario.Tipo.MENTORA);
        String nome = nomeUnico();
        mentoras.criarArea(mentora, nome);

        assertThrows(ConflictException.class, () -> mentoras.criarArea(mentora, nome.toUpperCase()));
    }
}