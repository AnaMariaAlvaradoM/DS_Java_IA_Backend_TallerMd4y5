package co.sillon;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ChatbotTest {
    @ParameterizedTest @ValueSource(strings={"Su cupón es X","CUPON de bienvenida","Un 15 % menos","Servicio gratuito","Es gratis",
            "Tienes un descuento","Una promoción especial"})
    void bloqueaOfertasInventadas(String respuesta) { assertThat(Asistente.ofrecePromocion(respuesta)).isTrue(); }
    @ParameterizedTest @ValueSource(strings={"El corte dura 30 minutos.","El precio es 35000 COP.","Te atiende el equipo del salón."})
    void conservaInformacionDelCatalogo(String respuesta) { assertThat(Asistente.ofrecePromocion(respuesta)).isFalse(); }
}
