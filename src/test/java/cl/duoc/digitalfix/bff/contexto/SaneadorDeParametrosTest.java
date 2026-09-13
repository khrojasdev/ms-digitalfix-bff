package cl.duoc.digitalfix.bff.contexto;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

class SaneadorDeParametrosTest {

    @Test
    @DisplayName("el companyId que manda el cliente se descarta, escriba como escriba")
    void descartaLaEmpresaDelCliente() {
        Map<String, String[]> entrada = new LinkedHashMap<>();
        entrada.put("companyId", new String[] {"3"});
        entrada.put("company_id", new String[] {"4"});
        entrada.put("COMPANYID", new String[] {"5"});
        entrada.put("empresaId", new String[] {"6"});

        MultiValueMap<String, String> salida = SaneadorDeParametros.sanear(entrada);

        assertThat(salida).isEmpty();
    }

    @Test
    void conserva_los_parametros_legitimos() {
        Map<String, String[]> entrada = new LinkedHashMap<>();
        entrada.put("page", new String[] {"2"});
        entrada.put("size", new String[] {"50"});
        entrada.put("soloActivos", new String[] {"false"});
        entrada.put("companyId", new String[] {"3"});

        MultiValueMap<String, String> salida = SaneadorDeParametros.sanear(entrada);

        assertThat(salida.keySet()).containsExactlyInAnyOrder("page", "size", "soloActivos");
        assertThat(salida.getFirst("size")).isEqualTo("50");
    }

    @Test
    void conserva_los_valores_repetidos_de_un_mismo_parametro() {
        Map<String, String[]> entrada = Map.of("sort", new String[] {"nombre,asc", "codigo,desc"});

        MultiValueMap<String, String> salida = SaneadorDeParametros.sanear(entrada);

        assertThat(salida.get("sort")).containsExactly("nombre,asc", "codigo,desc");
    }

    @Test
    void tolera_que_no_venga_ningun_parametro() {
        assertThat(SaneadorDeParametros.sanear(null)).isEmpty();
    }
}
