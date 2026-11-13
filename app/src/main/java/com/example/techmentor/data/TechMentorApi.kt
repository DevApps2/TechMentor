package com.example.techmentor.data

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface TechMentorApi {

    @POST("usuarios/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("usuarios")
    suspend fun cadastrar(@Body body: CadastroRequest): UsuarioResponse

    @GET("usuarios/{id}")
    suspend fun obterUsuario(@Path("id") id: Long): UsuarioResponse

    @GET("matriculas/usuarios/{id}")
    suspend fun listarMatriculas(@Path("id") id: Long): List<MatriculaResponse>

    @GET("api/agendamentos")
    suspend fun listarAgendamentos(): List<AgendamentoResponse>

    // ---- Mentoria ----

    @GET("api/areas-atuacao")
    suspend fun listarAreas(): List<AreaResponse>

    // A administradora e a própria mentora podem criar uma área nova
    @POST("api/areas-atuacao")
    suspend fun criarArea(@Body corpo: AdminAreaRequest): AreaResponse

    // busca e area são opcionais: quando nulos, o Retrofit nem manda o parâmetro
    @GET("api/mentoras")
    suspend fun listarMentoras(
        @Query("busca") busca: String?,
        @Query("area") area: String?
    ): List<MentoraResponse>

    @GET("api/mentoras/{id}")
    suspend fun obterMentora(@Path("id") id: Long): MentoraResponse

    // a API devolve só os horários ainda não reservados
    @GET("api/mentoras/{id}/disponibilidades")
    suspend fun listarHorarios(@Path("id") mentoraId: Long): List<HorarioResponse>

    @POST("api/agendamentos")
    suspend fun criarAgendamento(
        @Body body: AgendamentoRequest
    ): AgendamentoResponse

    // 404 = a mentora ainda não preencheu o perfil
    @GET("api/mentoras/minha")
    suspend fun minhaMentora(): MentoraResponse

    @POST("api/mentoras")
    suspend fun criarPerfilDeMentora(
        @Body body: MentoraRequest
    ): MentoraResponse

    @PUT("api/mentoras/{id}")
    suspend fun atualizarPerfilDeMentora(
        @Path("id") mentoraId: Long,
        @Body body: MentoraRequest
    ): MentoraResponse

    @POST("api/mentoras/{id}/disponibilidades")
    suspend fun criarHorario(
        @Path("id") mentoraId: Long,
        @Body body: HorarioRequest
    ): HorarioResponse

    @DELETE("api/disponibilidades/{id}")
    suspend fun excluirHorario(
        @Path("id") horarioId: Long
    )

    // A mentora confirma: AGENDADA vira CONFIRMADA e a aluna é avisada
    @PATCH("api/agendamentos/{id}/confirmacao")
    suspend fun confirmarAgendamento(
        @Path("id") agendamentoId: Long
    ): AgendamentoResponse

    // Aluna ou mentora cancela: o horário volta a ficar livre e a outra pessoa é avisada
    @PATCH("api/agendamentos/{id}/cancelamento")
    suspend fun cancelarAgendamento(
        @Path("id") agendamentoId: Long
    ): AgendamentoResponse

    // Troca por outro horário livre da mesma mentora (será usado na próxima etapa)
    @PATCH("api/agendamentos/{id}/reagendamento")
    suspend fun reagendarAgendamento(
        @Path("id") agendamentoId: Long,
        @Body body: ReagendamentoRequest
    ): AgendamentoResponse

    // ---- Oportunidades ----

    // tipo (ESTAGIO, EMPREGO, BOLSA ou EVENTO) e area são opcionais: quando nulos, o Retrofit nem manda o parâmetro
    @GET("api/oportunidades")
    suspend fun listarOportunidades(
        @Query("tipo") tipo: String?,
        @Query("area") area: String?,
        // texto para buscar no título, na organização, na área e na descrição
        @Query("busca") busca: String? = null
    ): List<OportunidadeResponse>

    // ---- Segurança ----

    // devolve orientações e canais de ajuda juntos; a tela separa pelo tipo
    @GET("api/seguranca/conteudos")
    suspend fun listarConteudosSeguranca(): List<ConteudoSegurancaResponse>

    // mínimo de 2 letras; o backend tira da lista quem está buscando
    @GET("usuarios/busca")
    suspend fun buscarUsuarios(
        @Query("nome") nome: String
    ): List<UsuarioResumoResponse>

    @POST("api/denuncias")
    suspend fun denunciar(
        @Body body: DenunciaRequest
    ): DenunciaResponse

    // as denúncias que a própria pessoa fez, da mais nova para a mais antiga; status: ABERTA, EM_ANALISE ou RESOLVIDA
    @GET("api/denuncias/minhas")
    suspend fun minhasDenuncias(): List<DenunciaResponse>

    @GET("api/bloqueios")
    suspend fun listarBloqueios(): List<BloqueioResponse>

    @POST("api/bloqueios")
    suspend fun bloquear(
        @Body body: BloqueioRequest
    ): BloqueioResponse

    @DELETE("api/bloqueios/{usuarioId}")
    suspend fun desbloquear(
        @Path("usuarioId") bloqueadoId: Long
    )

    // vagas e bolsas: candidatura; eventos: inscrição (o backend recusa o tipo errado com 400)
    @POST("api/oportunidades/{id}/candidaturas")
    suspend fun candidatar(
        @Path("id") oportunidadeId: Long
    ): InscricaoResponse

    @POST("api/oportunidades/{id}/inscricoes")
    suspend fun inscreverEmEvento(
        @Path("id") oportunidadeId: Long
    ): InscricaoResponse

    @GET("api/candidaturas/minhas")
    suspend fun minhasCandidaturas(): List<InscricaoResponse>

    @GET("api/inscricoes/minhas")
    suspend fun minhasInscricoes(): List<InscricaoResponse>

    // retira a candidatura/inscrição enquanto a equipe ainda não respondeu (409 se já foi aprovada ou recusada)
    @DELETE("api/candidaturas/{id}")
    suspend fun desistirDaCandidatura(
        @Path("id") inscricaoId: Long
    )

    @GET("conquistas/usuarios/{id}")
    suspend fun listarConquistasDoUsuario(@Path("id") id: Long): List<UsuarioConquistaResponse>

    @GET("conquistas")
    suspend fun listarConquistas(): List<ConquistaResponse>

    @GET("desafios")
    suspend fun listarDesafiosAtivos(): List<DesafioResponse>

    @GET("desafios/usuarios/{id}")
    suspend fun listarDesafiosDoUsuario(@Path("id") id: Long): List<UsuarioDesafioResponse>

    @POST("desafios/{titulo}/participacao/usuarios/{id}")
    suspend fun participarDoDesafio(
        @Path("titulo") titulo: String,
        @Path("id") usuarioId: Long
    ): UsuarioDesafioResponse

    @PUT("usuarios/{id}")
    suspend fun atualizarUsuario(
        @Path("id") id: Long,
        @Body body: AtualizarUsuarioRequest
    ): UsuarioResponse

    @Multipart
    @POST("usuarios/{id}/foto")
    suspend fun enviarFoto(
        @Path("id") id: Long,
        @Part foto: MultipartBody.Part
    ): UsuarioResponse

    @POST("usuarios/senha/esqueci")
    suspend fun esqueciSenha(@Body body: EsqueciSenhaRequest)

    @POST("usuarios/senha/redefinir")
    suspend fun redefinirSenha(@Body body: RedefinirSenhaRequest)

    // ---- Avaliações ----

    @GET("api/agendamentos/{id}")
    suspend fun obterAgendamento(
        @Path("id") agendamentoId: Long
    ): AgendamentoResponse

    // mentorias concluídas, dentro do prazo, que a pessoa ainda não avaliou
    @GET("api/avaliacoes/pendentes")
    suspend fun avaliacoesPendentes(): List<AvaliacaoPendenteResponse>

    @GET("api/agendamentos/{id}/avaliacoes")
    suspend fun situacaoDaAvaliacao(
        @Path("id") agendamentoId: Long
    ): AvaliacaoDaMentoriaResponse

    // 409 = não dá para avaliar agora (não concluída, prazo vencido ou já avaliou); o motivo vem na mensagem
    @POST("api/agendamentos/{id}/avaliacoes")
    suspend fun avaliar(
        @Path("id") agendamentoId: Long,
        @Body body: AvaliacaoRequest
    ): AvaliacaoResponse

    // mentoraId é o id do PERFIL da mentora
    @GET("api/mentoras/{id}/avaliacoes")
    suspend fun avaliacoesDaMentora(
        @Path("id") mentoraId: Long,
        @Query("limite") limite: Int
    ): AvaliacoesDaMentoraResponse
    // ---- Notificações ----

    // lida = false traz só as não lidas; lida = null traz todas
    @GET("notificacoes/usuarios/{id}")
    suspend fun listarNotificacoes(
        @Path("id") usuarioId: Long,
        @Query("lida") lida: Boolean?
    ): List<NotificacaoResponse>

    // notificacaoId = null marca todas como lidas; a resposta é vazia (204)
    @PATCH("notificacoes/leitura/usuarios/{id}")
    suspend fun marcarNotificacoesComoLidas(
        @Path("id") usuarioId: Long,
        @Query("id") notificacaoId: Long?
    )

    // 400 = senha atual errada (a mensagem vem da API); 429 = muitas tentativas
    @PATCH("usuarios/{id}/senha")
    suspend fun trocarSenha(@Path("id") usuarioId: Long, @Body body: TrocarSenhaRequest)

    // lista os cursos ativos; categoria é opcional
    @GET("cursos")
    suspend fun listarCursos(@Query("categoria") categoria: String? = null): List<CursoResponse>

    // cada aula do curso com a situação dela para esta usuária
    @GET("progresso/{titulo}/usuarios/{usuarioId}/aulas")
    suspend fun listarAulasComProgresso(
        @Path("titulo") titulo: String,
        @Path("usuarioId") usuarioId: Long
    ): List<AulaProgressoResponse>

    // conclui a aula (a anterior precisa estar concluída; matricula no curso se ainda não estiver) e dá o XP
    @POST("progresso/{titulo}/{ordemModulo}/{ordemAula}/usuarios/{usuarioId}")
    suspend fun concluirAula(
        @Path("titulo") titulo: String,
        @Path("ordemModulo") ordemModulo: Int,
        @Path("ordemAula") ordemAula: Int,
        @Path("usuarioId") usuarioId: Long
    ): ProgressoAulaResponse
}
