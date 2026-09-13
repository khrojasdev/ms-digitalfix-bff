package cl.duoc.digitalfix.bff.cliente;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReintentosTest {

    @Test
    @DisplayName("un GET que falla se repite una vez")
    void reintentaLoIdempotente() {
        AtomicInteger intentos = new AtomicInteger();

        String resultado = Reintentos.conUnReintento(HttpMethod.GET, () -> {
            if (intentos.incrementAndGet() == 1) {
                throw new IllegalStateException("conexion rechazada");
            }
            return "ok";
        });

        assertThat(resultado).isEqualTo("ok");
        assertThat(intentos.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("un POST que falla NO se repite: podria crear dos veces el recurso")
    void noReintentaLoNoIdempotente() {
        AtomicInteger intentos = new AtomicInteger();

        assertThatThrownBy(() -> Reintentos.conUnReintento(HttpMethod.POST, () -> {
            intentos.incrementAndGet();
            throw new IllegalStateException("conexion rechazada");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(intentos.get()).isEqualTo(1);
    }

    @Test
    void seRindeTrasElSegundoIntento() {
        AtomicInteger intentos = new AtomicInteger();

        assertThatThrownBy(() -> Reintentos.conUnReintento(HttpMethod.GET, () -> {
            intentos.incrementAndGet();
            throw new IllegalStateException("sigue caido");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(intentos.get()).isEqualTo(2);
    }
}
