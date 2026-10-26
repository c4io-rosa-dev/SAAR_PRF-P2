package br.edu.fatec.saarprf.service;

import br.edu.fatec.saarprf.AcidenteFixture;
import br.edu.fatec.saarprf.dto.ContagemRodovia;
import br.edu.fatec.saarprf.dto.FiltroAcidente;
import br.edu.fatec.saarprf.dto.ResumoAcidentes;
import br.edu.fatec.saarprf.dto.TotaisAcidentes;
import br.edu.fatec.saarprf.model.Acidente;
import br.edu.fatec.saarprf.repository.AcidenteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcidenteServiceTest {

    @Mock
    private AcidenteRepository repository;

    @InjectMocks
    private AcidenteService service;

    @Test
    void criarIgnoraIdsEnviadosENormalizaTexto() {
        Acidente novo = AcidenteFixture.valido();
        novo.setId(5L);
        novo.setIdPrf(9L);
        novo.setMunicipio("  sao   paulo ");
        when(repository.save(any(Acidente.class))).thenAnswer(inv -> inv.getArgument(0));

        Acidente salvo = service.criar(novo);

        assertThat(salvo.getId()).isNull();
        assertThat(salvo.getIdPrf()).isNull();
        assertThat(salvo.getMunicipio()).isEqualTo("SAO PAULO");
    }

    @Test
    void atualizarPreservaIdEIdPrf() {
        Acidente existente = AcidenteFixture.valido();
        existente.setId(1L);
        existente.setIdPrf(777L);
        Acidente dados = AcidenteFixture.valido();
        dados.setUf("RJ");
        dados.setIdPrf(null);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.save(any(Acidente.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Acidente> atualizado = service.atualizar(1L, dados);

        assertThat(atualizado).isPresent();
        assertThat(atualizado.get().getId()).isEqualTo(1L);
        assertThat(atualizado.get().getIdPrf()).isEqualTo(777L);
        assertThat(atualizado.get().getUf()).isEqualTo("RJ");
    }

    @Test
    void atualizarInexistenteRetornaVazio() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.atualizar(99L, AcidenteFixture.valido())).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void excluirInexistenteRetornaFalso() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThat(service.excluir(99L)).isFalse();
        verify(repository, never()).deleteById(anyLong());
    }

    @Test
    void excluirExistenteRemove() {
        when(repository.existsById(1L)).thenReturn(true);

        assertThat(service.excluir(1L)).isTrue();
        verify(repository).deleteById(1L);
    }

    @Test
    void resumoJuntaTotaisERodoviaMaisCritica() {
        FiltroAcidente filtro = new FiltroAcidente("SP", null, null, null);
        when(repository.totalizar(eq("SP"), isNull(), isNull(), isNull()))
                .thenReturn(new TotaisAcidentes(120L, 9L, 150L, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 8, 31)));
        when(repository.contarPorRodovia(eq("SP"), isNull(), isNull(), isNull(), any()))
                .thenReturn(List.of(new ContagemRodovia("BR-116", 40L)));

        ResumoAcidentes resumo = service.resumo(filtro);

        assertThat(resumo).isEqualTo(new ResumoAcidentes(120L, 9L, 150L,
                LocalDate.of(2025, 1, 1), LocalDate.of(2026, 8, 31), "BR-116", 40L));
    }

    @Test
    void resumoSemRegistrosNaoTemRodoviaCritica() {
        FiltroAcidente filtro = new FiltroAcidente(null, null, null, null);
        when(repository.totalizar(isNull(), isNull(), isNull(), isNull()))
                .thenReturn(new TotaisAcidentes(0L, 0L, 0L, null, null));
        when(repository.contarPorRodovia(isNull(), isNull(), isNull(), isNull(), any())).thenReturn(List.of());

        ResumoAcidentes resumo = service.resumo(filtro);

        assertThat(resumo.total()).isZero();
        assertThat(resumo.rodoviaMaisCritica()).isNull();
        assertThat(resumo.ocorrenciasRodovia()).isZero();
    }
}
