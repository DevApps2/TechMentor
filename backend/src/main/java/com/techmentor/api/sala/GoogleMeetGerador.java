package com.techmentor.api.sala;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.ConferenceSolutionKey;
import com.google.api.services.calendar.model.CreateConferenceRequest;
import com.google.api.services.calendar.model.EntryPoint;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class GoogleMeetGerador implements GeradorDeSala {

    private static final Logger log = LoggerFactory.getLogger(GoogleMeetGerador.class);

    @Value("${app.google.client-id:}")
    private String clientId;

    @Value("${app.google.client-secret:}")
    private String clientSecret;

    @Value("${app.google.refresh-token:}")
    private String refreshToken;

    @Value("${app.fuso:America/Sao_Paulo}")
    private String fuso;

    @Value("${app.google.convidar:true}")
    private boolean convidar;

    private volatile Calendar calendario;

    @Override
    public Optional<SalaCriada> criar(DadosDaSala dados) {
        if (!configurado()) {
            log.warn("Google Meet não configurado (faltam GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET ou GOOGLE_REFRESH_TOKEN): mentoria sem sala.");
            return Optional.empty();
        }
        try {

            Event evento = new Event()
                    .setSummary(dados.titulo())
                    .setDescription(dados.descricao())
                    // "Livre" na agenda da conta do projeto, para mentorias no mesmo horário não ficarem como bloqueio
                    .setTransparency("transparent")
                    .setStart(horario(dados.inicio()))
                    .setEnd(horario(dados.fim()))
                    .setAttendees(dados.emails().stream().map(e -> new EventAttendee().setEmail(e)).toList())
                    .setConferenceData(new ConferenceData().setCreateRequest(
                            new CreateConferenceRequest()
                                    .setRequestId(UUID.randomUUID().toString())
                                    .setConferenceSolutionKey(new ConferenceSolutionKey().setType("hangoutsMeet"))));

            Event criado = calendario().events().insert("primary", evento)
                    .setConferenceDataVersion(1)
                    .setSendUpdates(convidar ? "all" : "none")
                    .execute();

            String link = extrairLink(criado);
            if (link == null) {
                log.warn("O Google criou o evento {} mas não devolveu o link do Meet.", criado.getId());
                return Optional.empty();
            }
            return Optional.of(new SalaCriada(link, criado.getId()));

        } catch (Exception e) {
            log.error("Falha ao criar a sala do Google Meet: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void apagar(String idDoEvento) {
        if (idDoEvento == null || idDoEvento.isBlank() || !configurado()) return;
        try {
            calendario().events().delete("primary", idDoEvento)
                    .setSendUpdates(convidar ? "all" : "none")
                    .execute();
        } catch (Exception e) {
            log.error("Falha ao apagar o evento {} do Google: {}", idDoEvento, e.getMessage());
        }
    }

    @Override
    public boolean configurado() {
        return !clientId.isBlank() && !clientSecret.isBlank() && !refreshToken.isBlank();
    }

    private EventDateTime horario(LocalDateTime data) {
        long millis = data.atZone(ZoneId.of(fuso)).toInstant().toEpochMilli();
        return new EventDateTime().setDateTime(new DateTime(millis)).setTimeZone(fuso);
    }

    private String extrairLink(Event evento) {
        if (evento.getHangoutLink() != null) return evento.getHangoutLink();
        if (evento.getConferenceData() != null && evento.getConferenceData().getEntryPoints() != null) {
            for (EntryPoint ponto : evento.getConferenceData().getEntryPoints()) {
                if ("video".equals(ponto.getEntryPointType())) return ponto.getUri();
            }
        }
        return null;
    }

    /** Monta o cliente uma vez só. O token de acesso é renovado sozinho a partir do refresh token. */
    private Calendar calendario() throws IOException, GeneralSecurityException {
        if (calendario == null) {
            synchronized (this) {
                if (calendario == null) {
                    UserCredentials credenciais = UserCredentials.newBuilder()
                            .setClientId(clientId)
                            .setClientSecret(clientSecret)
                            .setRefreshToken(refreshToken)
                            .build();
                    HttpCredentialsAdapter autenticacao = new HttpCredentialsAdapter(credenciais);

                    // Limites de tempo: se o Google demorar, a confirmação da mentoria não fica pendurada
                    HttpRequestInitializer comLimites = requisicao -> {
                        autenticacao.initialize(requisicao);
                        requisicao.setConnectTimeout(5000);
                        requisicao.setReadTimeout(8000);
                    };

                    calendario = new Calendar.Builder(
                            GoogleNetHttpTransport.newTrustedTransport(),
                            GsonFactory.getDefaultInstance(),
                            comLimites)
                            .setApplicationName("TechMentor")
                            .build();
                }
            }
        }
        return calendario;
    }
}