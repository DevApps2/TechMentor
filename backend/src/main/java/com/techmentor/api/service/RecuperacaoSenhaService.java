package com.techmentor.api.service;

import com.techmentor.api.exceptions.ApiException;
import com.techmentor.api.model.RedefinicaoSenha;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.RedefinicaoSenhaRepository;
import com.techmentor.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecuperacaoSenhaService {

    private static final int VALIDADE_MINUTOS = 10;
    private static final int MAXIMO_TENTATIVAS = 5;
    private static final int INTERVALO_ENTRE_PEDIDOS_SEGUNDOS = 60;
    private static final String CODIGO_INVALIDO = "Código inválido ou expirado.";

    // SecureRandom é aleatório de verdade; o Random comum é previsível
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final RedefinicaoSenhaRepository redefinicaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Gera um código de 6 dígitos e envia por e-mail. Se o e-mail não existir, não faz nada
     * e não avisa: assim ninguém descobre quem tem conta no app.
     */
    @Transactional
    public void solicitarCodigo(String email) {

        Optional<Usuario> achado = usuarioRepository.findByEmail(email.trim());
        if (achado.isEmpty() || !achado.get().isAtivo()) {
            return;
        }
        Usuario usuario = achado.get();

        LocalDateTime agora = LocalDateTime.now();

        // Limite de pedidos: se já pediu há menos de 1 minuto, ignora (evita encher a caixa de e-mail)
        if (redefinicaoRepository.existsByUsuarioAndCriadoEmAfter(
                usuario, agora.minusSeconds(INTERVALO_ENTRE_PEDIDOS_SEGUNDOS))) {
            return;
        }

        // Só o código mais novo vale: os anteriores são invalidados
        redefinicaoRepository.findByUsuarioAndUsadoFalse(usuario)
                .forEach(antigo -> antigo.setUsado(true));

        String codigo = String.format("%06d", ALEATORIO.nextInt(1_000_000));

        RedefinicaoSenha pedido = new RedefinicaoSenha();
        pedido.setUsuario(usuario);
        pedido.setCodigoHash(passwordEncoder.encode(codigo));
        pedido.setExpiraEm(agora.plusMinutes(VALIDADE_MINUTOS));
        redefinicaoRepository.save(pedido);

        emailService.enviar(
                usuario.getEmail(),
                "Seu código de recuperação de senha - TechMentor",
                "Olá, " + usuario.getNome() + "!\n\n"
                        + "Seu código para redefinir a senha é: " + codigo + "\n\n"
                        + "Ele vale por " + VALIDADE_MINUTOS + " minutos. "
                        + "Se não foi você que pediu, ignore este e-mail."
        );
    }

    /**
     * Confere o código e, se estiver certo, troca a senha.
     * noRollbackFor: sem isso, o erro desfaria o "tentativas + 1" e o limite de tentativas não funcionaria.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public void redefinirSenha(String email, String codigo, String novaSenha) {

        Usuario usuario = usuarioRepository.findByEmail(email.trim())
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> ApiException.badRequest(CODIGO_INVALIDO));

        RedefinicaoSenha pedido = redefinicaoRepository
                .findFirstByUsuarioAndUsadoFalseOrderByCriadoEmDesc(usuario)
                .orElseThrow(() -> ApiException.badRequest(CODIGO_INVALIDO));

        if (pedido.getExpiraEm().isBefore(LocalDateTime.now())) {
            pedido.setUsado(true);
            throw ApiException.badRequest(CODIGO_INVALIDO);
        }

        if (pedido.getTentativas() >= MAXIMO_TENTATIVAS) {
            pedido.setUsado(true);
            throw ApiException.badRequest("Muitas tentativas. Peça um novo código.");
        }

        if (!passwordEncoder.matches(codigo.trim(), pedido.getCodigoHash())) {
            pedido.setTentativas(pedido.getTentativas() + 1);
            throw ApiException.badRequest(CODIGO_INVALIDO);
        }

        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        pedido.setUsado(true);
    }
}