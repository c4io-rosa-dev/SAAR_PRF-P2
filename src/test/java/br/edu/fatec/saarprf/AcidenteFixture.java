package br.edu.fatec.saarprf;

import br.edu.fatec.saarprf.model.Acidente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public final class AcidenteFixture {

    private AcidenteFixture() {
    }

    public static Acidente valido() {
        Acidente a = new Acidente();
        a.setIdPrf(652493L);
        a.setDataOcorrencia(LocalDate.of(2025, 1, 1));
        a.setHorario(LocalTime.of(6, 20));
        a.setUf("SP");
        a.setMunicipio("GUARULHOS");
        a.setRodovia("BR-116");
        a.setQuilometro(new BigDecimal("225"));
        a.setCausaAcidente("Reação tardia ou ineficiente do condutor");
        a.setTipoAcidente("Tombamento");
        a.setClassificacao("Com Vítimas Feridas");
        a.setMortos(0);
        a.setFeridos(1);
        a.setLatitude(new BigDecimal("-23.48586772"));
        a.setLongitude(new BigDecimal("-46.54075317"));
        return a;
    }
}
