package cl.duoc.digitalfix.bff.cliente;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Cortacircuitos minimo para un microservicio.
 *
 * Cuando el catalogo esta caido, seguir llamandolo en cada peticion cuesta un
 * timeout completo por peticion: el frontend se arrastra y los hilos del BFF
 * se llenan esperando a alguien que no va a contestar. Tras varios fallos
 * seguidos se deja de intentar por un rato y se responde de inmediato.
 *
 * Es a proposito mas simple que resilience4j: son tres campos y se entiende de
 * una lectura, sin agregar una dependencia mas al proyecto.
 */
public class Cortacircuitos {

    private final int fallosParaAbrir;
    private final long msAbierto;
    private final LongSupplier reloj;

    private final AtomicInteger fallosSeguidos = new AtomicInteger();
    private final AtomicLong abiertoHasta = new AtomicLong();

    public Cortacircuitos(int fallosParaAbrir, Duration tiempoAbierto) {
        this(fallosParaAbrir, tiempoAbierto, System::currentTimeMillis);
    }

    /** Constructor para pruebas: permite inyectar el reloj. */
    public Cortacircuitos(int fallosParaAbrir, Duration tiempoAbierto, LongSupplier reloj) {
        this.fallosParaAbrir = fallosParaAbrir;
        this.msAbierto = tiempoAbierto.toMillis();
        this.reloj = reloj;
    }

    /** true cuando no vale la pena ni intentar la llamada. */
    public boolean estaAbierto() {
        return reloj.getAsLong() < abiertoHasta.get();
    }

    public void registrarExito() {
        fallosSeguidos.set(0);
        abiertoHasta.set(0);
    }

    public void registrarFallo() {
        if (fallosSeguidos.incrementAndGet() >= fallosParaAbrir) {
            abiertoHasta.set(reloj.getAsLong() + msAbierto);
        }
    }

    public int fallosSeguidos() {
        return fallosSeguidos.get();
    }
}
