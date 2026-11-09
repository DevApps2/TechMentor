package com.techmentor.api.controller;

import com.techmentor.api.dto.SenhaDtos;
import com.techmentor.api.dto.UsuarioDtos.CreateRequest;
import com.techmentor.api.dto.UsuarioDtos.LoginRequest;
import com.techmentor.api.dto.UsuarioDtos.Response;
import com.techmentor.api.dto.UsuarioDtos.Resumo;
import com.techmentor.api.dto.UsuarioDtos.UpdateRequest;
import com.techmentor.api.service.LimiteDeCadastro;
import com.techmentor.api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import com.techmentor.api.security.JwtService;
import com.techmentor.api.dto.UsuarioDtos.LoginResponse;
import com.techmentor.api.service.UsuarioAtualService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/usuarios")
@Tag(name = "Usuário", description = "Cadastrar, criar, listar e deleter usuários")
public class UsuarioController {

    private final UsuarioService service;
    private final JwtService jwtService;
    private final UsuarioAtualService usuarios;
    private final LimiteDeCadastro limiteDeCadastro;

    // true = a API está atrás de um proxy (ngrok, nginx) e o IP real vem no cabeçalho X-Forwarded-For
    @Value("${app.proxy.confiavel:false}")
    private boolean proxyConfiavel;

    @Operation(summary = "Listar usuários")
    @GetMapping
    public ResponseEntity<List<Response>> listarUsuarios() {
        return ResponseEntity.ok(service.listarUsuarios());
    }

    @Operation(summary = "Buscar usuários pelo nome",
            description = "Usado para escolher quem denunciar ou bloquear. Retorna até 20 usuários ativos (sem e-mail); mínimo de 2 letras.")
    @GetMapping("/busca")
    public ResponseEntity<List<Resumo>> buscarPorNome(
            @Parameter(description = "Parte do nome") @RequestParam String nome) {
        return ResponseEntity.ok(service.buscarPorNome(nome, usuarios.buscarAtual().getId()));
    }

    @Operation(summary = "Cadastrar usuário", description = "Cria a conta de uma nova usuária. Retorna 409 se o e-mail já estiver cadastrado.")
    @PostMapping
    public ResponseEntity<Response> cadastrar(@Valid @RequestBody CreateRequest dto, HttpServletRequest request) {
        limiteDeCadastro.registrar(ipDoCliente(request));
        return ResponseEntity.status(201).body(service.criarUsuario(dto));
    }

    @Operation(
            summary = "Entrar na plataforma",
            description = "Valida e-mail e senha e devolve os dados do usuário e o token JWT."
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest dto
    ) {
        var usuario = service.loginUsuario(dto.email(), dto.senha());

        String token = jwtService.gerarToken(
                usuario.getId().toString(),
                usuario.versaoDoToken()
        );

        LoginResponse resposta = new LoginResponse(
                Response.from(usuario),
                token,
                "Bearer",
                jwtService.getExpirationSeconds()
        );

        return ResponseEntity.ok(resposta);
    }

    @Operation(summary = "Obter usuário por ID", description = "Dados do perfil, XP, nível e sequência. Retorna 404 se não existir ou estiver desativado.")
    @GetMapping("/{id}")
    public ResponseEntity<Response> obter(@Parameter(description = "ID do usuário") @PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @Operation(summary = "Atualizar usuário", description = "Atualiza nome, e-mail e foto; bio e áreas de interesse só mudam se forem enviadas. Devolve o usuário atualizado.")
    @PutMapping("/{id}")
    public ResponseEntity<Response> atualizar(
            @Parameter(description = "ID do usuário") @PathVariable Long id,
            @Valid @RequestBody UpdateRequest dto) {

        return ResponseEntity.ok(service.atualizarUsuario(id, dto));
    }

    @Operation(summary = "Enviar foto de perfil",
            description = "Recebe uma imagem (JPG, PNG ou WEBP) e atualiza a foto do usuário. Devolve o usuário atualizado.")
    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response> enviarFoto(
            @Parameter(description = "ID do usuário") @PathVariable Long id,
            @RequestParam("foto") MultipartFile foto) {

        return ResponseEntity.ok(service.atualizarFoto(id, foto));
    }

    @Operation(summary = "Deletar usuário")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@Parameter(description = "ID do usuário") @PathVariable Long id) {
        service.desativarUsuario(id);
        return ResponseEntity.status(200).build();
    }

    @Operation(summary = "Trocar a senha",
            description = "Para quem já está logado: exige a senha atual. Retorna 400 se a senha atual estiver errada "
                    + "e 429 se houver muitas tentativas erradas seguidas.")
    @PatchMapping("/{id}/senha")
    public ResponseEntity<Void> trocarSenha(@Parameter(description = "ID do usuário") @PathVariable Long id,
                                            @Valid @RequestBody SenhaDtos.TrocarSenhaRequest dto) {
        service.trocarSenha(id, dto.senhaAtual(), dto.novaSenha());
        return ResponseEntity.noContent().build();
    }

    /** O endereço de quem chamou. Só confia no X-Forwarded-For se a API estiver mesmo atrás de um proxy. */
    private String ipDoCliente(HttpServletRequest request) {
        if (proxyConfiavel) {
            String encaminhado = request.getHeader("X-Forwarded-For");
            if (encaminhado != null && !encaminhado.isBlank()) {
                return encaminhado.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
