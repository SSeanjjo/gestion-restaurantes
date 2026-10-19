package co.edu.uniquindio.comanda.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.comanda.auth.Rol;
import co.edu.uniquindio.comanda.auth.UsuarioSesion;

/** RNF-04 en el canal WebSocket: cada rol solo escucha los tópicos que le corresponden. */
class PoliticaSuscripcionTest {

    private final UsuarioSesion mesero7 = new UsuarioSesion(7L, "mesero1", Rol.MESERO);
    private final UsuarioSesion cocinero = new UsuarioSesion(3L, "cocina", Rol.COCINERO);
    private final UsuarioSesion cajero = new UsuarioSesion(4L, "caja", Rol.CAJERO);

    @Test
    void elMeseroSoloEscuchaSusPropiasNotificaciones() {
        assertThat(PoliticaSuscripcion.permitido(mesero7, "/topic/meseros/7")).isTrue();
        assertThat(PoliticaSuscripcion.permitido(mesero7, "/topic/meseros/8")).isFalse();
        assertThat(PoliticaSuscripcion.permitido(cocinero, "/topic/meseros/3")).isFalse();
    }

    @Test
    void laCocinaYLaCajaTienenSusTopicos() {
        assertThat(PoliticaSuscripcion.permitido(cocinero, "/topic/cocina")).isTrue();
        assertThat(PoliticaSuscripcion.permitido(mesero7, "/topic/cocina")).isFalse();
        assertThat(PoliticaSuscripcion.permitido(cajero, "/topic/caja")).isTrue();
        assertThat(PoliticaSuscripcion.permitido(cocinero, "/topic/caja")).isFalse();
    }

    @Test
    void rechazaTopicosDesconocidosOSinUsuario() {
        assertThat(PoliticaSuscripcion.permitido(cajero, "/topic/otro")).isFalse();
        assertThat(PoliticaSuscripcion.permitido(null, "/topic/mesas")).isFalse();
    }
}
