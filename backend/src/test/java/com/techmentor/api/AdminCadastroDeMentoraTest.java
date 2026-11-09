package com.techmentor.api;

import com.techmentor.api.dto.AdminDtos.MentoraCadastroRequest;
import com.techmentor.api.dto.MentoriaDtos.MentoraResponse;
import com.techmentor.api.exceptions.ApiException;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.UsuarioRepository;
import com.techmentor.api.service.AdminService;
import com.techmentor.api.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A administradora cadastra a mentora (conta + perfil de uma vez); só ela pode, e o e-mail não repete. */
@SpringBootTest
@Transactional
class AdminCadastroDeMentoraTest {

    private static final String SENHA_PROVISORIA = "provisoria123";

    @Autowired
    private AdminService admin;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarios;

    private Usuario administradora;

    private Usuario criarUsuario(Usuario.Tipo tipo) {
        Usuario u = new Usuario();
        u.setNome("Teste " + tipo);
        u.setEmail("t" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com");
        u.setSenhaHash("hash-de-teste");
        u.setTipo(tipo);
        return usuarios.save(u);
    }

    private static MentoraCadastroRequest pedido(String email) {
        return new MentoraCadastroRequest("  Nova Mentora  ", email, SENHA_PROVISORIA,
                "Bio de teste com mais de dez letras.", "Engenheira", "Empresa X", 5, Set.of());
    }

    @BeforeEach
    void preparar() {
        administradora = criarUsuario(Usuario.Tipo.ADMIN);
    }

    @Test
    void criaAContaDeMentoraEOPerfilDeUmaVez() {
        String email = "m" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";

        MentoraResponse perfil = admin.cadastrarMentora(administradora, pedido(email));

        assertEquals("Engenheira", perfil.cargo());

        Usuario conta = usuarios.findByEmail(email).orElseThrow();
        assertEquals(Usuario.Tipo.MENTORA, conta.getTipo());
        assertEquals("Nova Mentora", conta.getNome());   // o nome vem sem os espaços das pontas
        assertTrue(conta.isAtivo());
    }

    @Test
    void aMentoraEntraComASenhaProvisoria() {
        String email = "m" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";
        admin.cadastrarMentora(administradora, pedido(email));

        Usuario entrou = usuarioService.loginUsuario(email, SENHA_PROVISORIA);

        assertEquals(Usuario.Tipo.MENTORA, entrou.getTipo());
    }

    @Test
    void oEmailEhNormalizadoEnaoRepete() {
        String base = "m" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";

        admin.cadastrarMentora(administradora, pedido("  " + base.toUpperCase() + " "));

        // o e-mail ficou em minúsculas, sem espaços
        assertTrue(usuarios.findByEmail(base).isPresent());

        ApiException repetido = assertThrows(ApiException.class,
                () -> admin.cadastrarMentora(administradora, pedido(base)));
        assertEquals(HttpStatus.CONFLICT, repetido.getStatus());
    }

    @Test
    void soAAdministradoraCadastra() {
        Usuario aluna = criarUsuario(Usuario.Tipo.ALUNA);
        Usuario outraMentora = criarUsuario(Usuario.Tipo.MENTORA);

        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApiException.class,
                () -> admin.cadastrarMentora(aluna, pedido("a@teste.com"))).getStatus());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApiException.class,
                () -> admin.cadastrarMentora(outraMentora, pedido("b@teste.com"))).getStatus());
    }
}
