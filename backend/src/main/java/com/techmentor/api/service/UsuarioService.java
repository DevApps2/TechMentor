package com.techmentor.api.service;

import com.techmentor.api.dto.UsuarioDtos.CreateRequest;
import com.techmentor.api.dto.UsuarioDtos.Response;
import com.techmentor.api.dto.UsuarioDtos.Resumo;
import com.techmentor.api.dto.UsuarioDtos.UpdateRequest;
import com.techmentor.api.exceptions.ApiException;
import com.techmentor.api.exceptions.ConflictException;
import com.techmentor.api.exceptions.NotAuthorizedException;
import com.techmentor.api.exceptions.NotFoundException;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;







@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final LimiteDeTentativasDeLogin limiteDeLogin;

    @Value("${app.pasta-fotos:fotos}")
    private String pastaFotos;

    public List<Response> listarUsuarios() {
        return repository.findByAtivoTrue()
                .stream()
                .map(Response::from)
                .toList();
    }

    /** Busca usuários ativos pelo nome (para denunciar ou bloquear); no máximo 20, sem o próprio solicitante. */
    public List<Resumo> buscarPorNome(String nome, Long solicitanteId) {
        String termo = nome == null ? "" : nome.trim();
        if (termo.length() < 2) {
            throw ApiException.badRequest("Digite pelo menos 2 letras para buscar.");
        }
        return repository.findTop20ByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(termo).stream()
                .filter(u -> !u.getId().equals(solicitanteId))
                .map(Resumo::from)
                .toList();
    }

    public Response criarUsuario(CreateRequest dto) {

        String email = Emails.normalizar(dto.email());

        if (repository.existsByEmail(email)) {
            throw new ConflictException("Usuário já cadastrado!");
        }

        Usuario usuario = new Usuario();

        usuario.setId(null);
        usuario.setNome(dto.nome());
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(dto.senha()));

        Usuario usuarioSalvo = repository.save(usuario);

        return Response.from(usuarioSalvo);
    }

    public Response buscarPorId(Long id) {
        return Response.from(repository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!")));
    }

    @Transactional
    public Response atualizarUsuario(Long id, UpdateRequest dto) {

        Usuario existente = repository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Usuário não encontrado!"));

        String email = Emails.normalizar(dto.email());

        // equalsIgnoreCase: conta antiga com maiúsculas no e-mail não conta como "mudou" se só a caixa é diferente
        boolean mudouEmail =
                !existente.getEmail().equalsIgnoreCase(email);

        if (mudouEmail && repository.existsByEmail(email)) {
            throw new ConflictException("E-mail já cadastrado!");
        }

        existente.setNome(dto.nome());
        existente.setEmail(email);
        existente.setFotoUrl(dto.fotoUrl());
        if (dto.bio() != null) {
            existente.setBio(dto.bio());
        }
        if (dto.areasInteresse() != null) {
            existente.getAreasInteresse().clear();
            dto.areasInteresse().stream()
                    .map(String::trim)
                    .filter(area -> !area.isEmpty())
                    .forEach(existente.getAreasInteresse()::add);
        }

        return Response.from(existente);
    }

    public Usuario loginUsuario(String email, String senha) {

        String emailNormalizado = Emails.normalizar(email);

        // Muitas senhas erradas seguidas: responde 429 antes de conferir qualquer coisa (vale até com a senha certa)
        limiteDeLogin.exigirLivre(emailNormalizado);

        Optional<Usuario> achado = repository.findByEmail(emailNormalizado);

        // A mesma resposta para e-mail que não existe, senha errada e conta desativada: ninguém descobre quem tem conta
        boolean entrou = achado.isPresent()
                && passwordEncoder.matches(senha, achado.get().getSenhaHash())
                && achado.get().isAtivo();

        if (!entrou) {
            limiteDeLogin.registrarFalha(emailNormalizado);
            throw new NotAuthorizedException("Credenciais inválidas!");
        }

        limiteDeLogin.limpar(emailNormalizado);
        return achado.get();
    }

    public void desativarUsuario(Long id) {

        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

        if(!usuario.isAtivo()){
            throw new ConflictException("Usuário já desativado!");
        }

        usuario.setAtivo(false);
        repository.save(usuario);
    }



    @Transactional
    public Response atualizarFoto(Long id, MultipartFile arquivo) {

        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

        if (arquivo == null || arquivo.isEmpty()) {
            throw ApiException.badRequest("Envie uma imagem.");
        }

        String extensao = switch (arquivo.getContentType() == null ? "" : arquivo.getContentType()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> null;
        };
        if (extensao == null) {
            throw ApiException.badRequest("Use uma imagem JPG, PNG ou WEBP.");
        }

        // O tipo que o app informa pode ser falso: confere os primeiros bytes do arquivo
        byte[] inicioDoArquivo;
        try (InputStream entrada = arquivo.getInputStream()) {
            inicioDoArquivo = entrada.readNBytes(12);
        } catch (IOException e) {
            throw ApiException.badRequest("Não foi possível ler a imagem.");
        }
        extensao = extensaoPeloConteudo(inicioDoArquivo);
        if (extensao == null) {
            throw ApiException.badRequest("O arquivo não é uma imagem JPG, PNG ou WEBP válida.");
        }

        try {
            Path pasta = Path.of(pastaFotos).toAbsolutePath();
            Files.createDirectories(pasta);

            String nome = "usuario-" + id + "-" + UUID.randomUUID() + "." + extensao;
            arquivo.transferTo(pasta.resolve(nome));

            usuario.setFotoUrl("/fotos/" + nome);
        } catch (IOException e) {
            throw ApiException.badRequest("Não foi possível salvar a imagem.");
        }

        return Response.from(usuario);
    }

    /** "jpg", "png" ou "webp" se os primeiros bytes são mesmo de uma dessas imagens; senão, null. */
    static String extensaoPeloConteudo(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "webp";
        }
        return null;
    }

    /**
     * Troca a senha de quem está logado. Exige a senha atual: como o projeto não usa token, é ela que prova que quem
     * pede é a dona da conta. Errar a senha atual conta no mesmo limite de tentativas do login.
     */
    @Transactional
    public void trocarSenha(Long id, String senhaAtual, String novaSenha) {

        Usuario usuario = repository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

        String chave = Emails.normalizar(usuario.getEmail());
        limiteDeLogin.exigirLivre(chave);

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
            limiteDeLogin.registrarFalha(chave);
            throw ApiException.badRequest("A senha atual está incorreta.");
        }

        if (passwordEncoder.matches(novaSenha, usuario.getSenhaHash())) {
            throw ApiException.badRequest("A nova senha deve ser diferente da atual.");
        }

        limiteDeLogin.limpar(chave);
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuario.invalidarTokens();
    }
}
