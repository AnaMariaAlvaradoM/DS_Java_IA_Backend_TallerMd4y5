package co.sillon;

import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.assertj.core.api.Assertions.assertThat;

class AgendaTest {
    final Salon salon=new Salon();
    final LocalDate lunes=LocalDate.of(2030,1,7);
    final LocalDateTime ayer=lunes.minusDays(1).atStartOfDay();
    final List<ReglasAgenda.Tramo> ocupado=List.of(new ReglasAgenda.Tramo(lunes.atTime(10,0),lunes.atTime(10,45)));

    @ParameterizedTest @CsvSource({"07:30,false","08:00,true","09:00,true","09:30,false","10:00,false","10:30,false",
            "11:00,true","12:15,false","17:00,true","17:30,false","18:00,false"})
    void evaluaFranjaConUnaCita(LocalTime hora, boolean esperado) {
        assertThat(ReglasAgenda.disponible(salon,lunes.atTime(hora),45,ayer,ocupado)).isEqualTo(esperado);
    }
    @Test void aceptaCitaQueTerminaJustoAlInicioDeOtra() {
        assertThat(ReglasAgenda.disponible(salon,lunes.atTime(9,30),30,ayer,ocupado)).isTrue();
    }
    @Test void aceptaElCierreExacto() {
        assertThat(ReglasAgenda.disponible(salon,lunes.atTime(17,30),30,ayer,ocupado)).isTrue();
    }
    @Test void rechazaElPasado() {
        assertThat(ReglasAgenda.disponible(salon,lunes.atTime(9,0),30,lunes.atTime(11,0),List.of())).isFalse();
    }
    @Test void respetaDomingosSegunLaConfiguracion() {
        var domingo=lunes.minusDays(1).atTime(9,0);
        assertThat(ReglasAgenda.disponible(salon,domingo,30,ayer,List.of())).isFalse();
        salon.abreDomingos=true;
        assertThat(ReglasAgenda.disponible(salon,domingo,30,ayer,List.of())).isTrue();
    }
    @Test void calculaIntervaloDesdeApertura() {
        salon.horaApertura=LocalTime.of(8,10);
        assertThat(ReglasAgenda.disponible(salon,lunes.atTime(8,40),30,ayer,List.of())).isTrue();
        assertThat(ReglasAgenda.disponible(salon,lunes.atTime(8,30),30,ayer,List.of())).isFalse();
    }
    @ParameterizedTest @CsvSource({"PENDIENTE,CONFIRMADA,true","PENDIENTE,CANCELADA,true","PENDIENTE,COMPLETADA,false",
            "CONFIRMADA,CANCELADA,true","CONFIRMADA,COMPLETADA,true","CONFIRMADA,NO_ASISTIO,true","CONFIRMADA,PENDIENTE,false",
            "COMPLETADA,CONFIRMADA,false","CANCELADA,CONFIRMADA,false","NO_ASISTIO,CONFIRMADA,false"})
    void transiciones(String desde, String hasta, boolean permitido) {
        assertThat(ReglasAgenda.transicion(desde,hasta)).isEqualTo(permitido);
    }
}
