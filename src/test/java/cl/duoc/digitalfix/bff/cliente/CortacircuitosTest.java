package cl.duoc.digitalfix.bff.cliente;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CortacircuitosTest {

    @Test
    void empieza_cerrado() {
        assertThat(nuevo(new AtomicLong(0)).estaAbierto()).isFalse();
    }

    @Test
    @DisplayName("se abre recien al tercer fallo seguido, no al primero")
    void seAbreAlTercerFallo() {
        Cortacircuitos corta = nuevo(new AtomicLong(0));

        corta.registrarFallo();
        corta.registrarFallo();
        assertThat(corta.estaAbierto()).isFalse();

        corta.registrarFallo();
        assertThat(corta.estaAbierto()).isTrue();
    }

    @Test
    @DisplayName("un exito borra la cuenta: los fallos tienen que ser seguidos")
    void unExitoReiniciaLaCuenta() {
        Cortacircuitos corta = nuevo(new AtomicLong(0));

        corta.registrarFallo();
        corta.registrarFallo();
        corta.registrarExito();
        corta.registrarFallo();

        assertThat(corta.fallosSeguidos()).isEqualTo(1);
        assertThat(corta.estaAbierto()).isFalse();
    }

    @Test
    void vuelve_a_cerrarse_cuando_pasa_el_tiempo() {
        AtomicLong ahora = new AtomicLong(0);
        Cortacircuitos corta = nuevo(ahora);

        corta.registrarFallo();
        corta.registrarFallo();
        corta.registrarFallo();
        assertThat(corta.estaAbierto()).isTrue();

        ahora.set(10_001);

        assertThat(corta.estaAbierto()).isFalse();
    }

    private Cortacircuitos nuevo(AtomicLong ahora) {
        return new Cortacircuitos(3, Duration.ofSeconds(10), ahora::get);
    }
}
