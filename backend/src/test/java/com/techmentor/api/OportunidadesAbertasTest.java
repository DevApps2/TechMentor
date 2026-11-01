package com.techmentor.api;

import com.techmentor.api.dto.OportunidadeDtos.OportunidadeResponse;
import com.techmentor.api.model.Oportunidade;
import com.techmentor.api.repository.OportunidadeRepository;
import com.techmentor.api.service.OportunidadeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A lista de oportunidades não pode mostrar as que já passaram do prazo de inscrição. */
@SpringBootTest
@Transactional
class OportunidadesAbertasTest {

    @Autowired
    private OportunidadeRepository repositorio;

    @Autowired
    private OportunidadeService servico;

    private Oportunidade salvar(String titulo, LocalDate prazo) {
        Oportunidade o = new Oportunidade();
        o.setTipo(Oportunidade.Tipo.ESTAGIO);
        o.setTitulo(titulo);
        o.setPrazoInscricao(prazo);
        return repositorio.save(o);
    }

    private List<String> titulos() {
        return servico.listar(null, null).stream().map(OportunidadeResponse::titulo).toList();
    }

    @Test
    void naoListaVencidasMasListaPrazoDeHojeEmDiante() {
        LocalDate hoje = LocalDate.now();

        salvar("T-vencida", hoje.minusDays(1));
        salvar("T-hoje", hoje);
        salvar("T-futura", hoje.plusDays(10));
        salvar("T-sem-prazo", null);

        List<String> titulos = titulos();

        assertFalse(titulos.contains("T-vencida"), "oportunidade vencida não deve aparecer: " + titulos);
        assertTrue(titulos.contains("T-hoje"), "o prazo de hoje ainda vale: " + titulos);
        assertTrue(titulos.contains("T-futura"), titulos.toString());
        assertTrue(titulos.contains("T-sem-prazo"), titulos.toString());
    }

    @Test
    void ordenaPeloPrazoMaisProximoESemPrazoPorUltimo() {
        LocalDate hoje = LocalDate.now();

        salvar("T-longe", hoje.plusDays(20));
        salvar("T-sem-prazo", null);
        salvar("T-perto", hoje.plusDays(2));

        List<String> meus = titulos().stream().filter(t -> t.startsWith("T-")).toList();

        assertEquals(List.of("T-perto", "T-longe", "T-sem-prazo"), meus);
    }

    @Test
    void naoListaInativas() {
        Oportunidade inativa = salvar("T-inativa", LocalDate.now().plusDays(5));
        inativa.setAtiva(false);

        assertFalse(titulos().contains("T-inativa"));
    }
}
