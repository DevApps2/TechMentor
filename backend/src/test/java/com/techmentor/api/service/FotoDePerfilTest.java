package com.techmentor.api.service;

import com.techmentor.api.exceptions.ApiException;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A foto de perfil só vale se o CONTEÚDO do arquivo for uma imagem; o tipo que o app diz não basta. */
@SpringBootTest
@Transactional
class FotoDePerfilTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

    @Autowired
    private UsuarioService usuarios;

    @Autowired
    private UsuarioRepository repositorio;

    private Usuario criarUsuario() {
        Usuario u = new Usuario();
        u.setNome("Teste Foto");
        u.setEmail("f" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com");
        u.setSenhaHash("hash-de-teste");
        return repositorio.save(u);
    }

    @Test
    void reconheceOsTresFormatosPeloConteudo() {
        assertEquals("png", UsuarioService.extensaoPeloConteudo(PNG));
        assertEquals("jpg", UsuarioService.extensaoPeloConteudo(JPG));
        assertEquals("webp", UsuarioService.extensaoPeloConteudo(WEBP));
        assertNull(UsuarioService.extensaoPeloConteudo("<script>alert(1)</script>".getBytes()));
        assertNull(UsuarioService.extensaoPeloConteudo(new byte[0]));
    }

    @Test
    void aceitaUmaImagemDeVerdade() {
        Usuario u = criarUsuario();

        var resposta = usuarios.atualizarFoto(u.getId(), new MockMultipartFile("foto", "a.png", "image/png", PNG));

        assertTrue(resposta.fotoUrl().startsWith("/fotos/usuario-" + u.getId() + "-"), resposta.fotoUrl());
        assertTrue(resposta.fotoUrl().endsWith(".png"), resposta.fotoUrl());
    }

    @Test
    void usaAExtensaoDoConteudoEnaoADoQueOAppDisse() {
        Usuario u = criarUsuario();

        // o app diz "png", mas o conteúdo é JPEG
        var resposta = usuarios.atualizarFoto(u.getId(), new MockMultipartFile("foto", "a.png", "image/png", JPG));

        assertTrue(resposta.fotoUrl().endsWith(".jpg"), resposta.fotoUrl());
    }

    @Test
    void recusaArquivoQueDizSerImagemMasNaoE() {
        Usuario u = criarUsuario();

        ApiException erro = assertThrows(ApiException.class, () -> usuarios.atualizarFoto(u.getId(),
                new MockMultipartFile("foto", "a.png", "image/png", "<script>alert(1)</script>".getBytes())));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
    }

    @Test
    void recusaTipoQueNaoEImagem() {
        Usuario u = criarUsuario();

        ApiException erro = assertThrows(ApiException.class, () -> usuarios.atualizarFoto(u.getId(),
                new MockMultipartFile("foto", "a.txt", "text/plain", PNG)));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
    }
}
