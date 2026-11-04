package com.techmentor.api.service;

import com.techmentor.api.dto.AdminDtos.MentoraCadastroRequest;
import com.techmentor.api.dto.MentoriaDtos.MentoraRequest;
import com.techmentor.api.dto.MentoriaDtos.MentoraResponse;
import com.techmentor.api.dto.SegurancaDtos.DenunciaResponse;
import com.techmentor.api.exceptions.ApiException;
import com.techmentor.api.model.Denuncia;
import com.techmentor.api.model.Usuario;
import com.techmentor.api.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ações que só administradoras fazem e que não cabiam nos serviços existentes.
 * Reaproveita o MentoraService e o DenunciaService, então as regras de cada um continuam valendo.
 */
@Service
@Transactional
public class AdminService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final MentoraService mentoras;
    private final DenunciaService denunciaService;

    public AdminService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                        MentoraService mentoras, DenunciaService denunciaService) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.mentoras = mentoras;
        this.denunciaService = denunciaService;
    }

    /**
     * Cria a conta (tipo MENTORA) e o perfil de mentora na mesma transação:
     * se o perfil for recusado (ex.: área que não existe), a conta também não é criada.
     */
    public MentoraResponse cadastrarMentora(Usuario actor, MentoraCadastroRequest request) {
        RegraAcesso.exigirAdmin(actor);
        if (request == null) throw ApiException.badRequest("Dados da mentora são obrigatórios.");

        String email = Emails.normalizar(request.email());
        if (usuarios.existsByEmail(email)) throw ApiException.conflict("E-mail já cadastrado.");

        Usuario conta = new Usuario();
        conta.setNome(request.nome().trim());
        conta.setEmail(email);
        conta.setSenhaHash(passwordEncoder.encode(request.senha()));
        conta.setTipo(Usuario.Tipo.MENTORA);
        conta = usuarios.save(conta);

        return mentoras.criarMentora(conta, new MentoraRequest(request.bio(), request.cargo(),
                request.empresa(), request.anosExperiencia(), request.areasIds()));
    }

    /**
     * Muda a situação de uma denúncia (ABERTA, EM_ANALISE ou RESOLVIDA). Passa pelo DenunciaService,
     * que também avisa a denunciante por notificação; gravar direto no repositório pularia esse aviso.
     */
    public DenunciaResponse alterarStatusDenuncia(Usuario actor, Long id, Denuncia.Status status) {
        RegraAcesso.exigirAdmin(actor);
        return denunciaService.atualizarStatus(actor, id, status);
    }
}