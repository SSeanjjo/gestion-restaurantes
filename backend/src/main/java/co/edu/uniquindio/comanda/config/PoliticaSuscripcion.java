package co.edu.uniquindio.comanda.config;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import co.edu.uniquindio.comanda.auth.Rol;
import co.edu.uniquindio.comanda.auth.UsuarioSesion;

/**
 * RNF-04 aplicado al canal en tiempo real: qué rol puede suscribirse a cada tópico.
 * Es una clase pura (sin Spring) para poder probarla de forma unitaria.
 */
public final class PoliticaSuscripcion {

    private static final String PREFIJO_MESERO = "/topic/meseros/";

    private static final Map<String, Set<Rol>> TOPICOS = Map.of(
            "/topic/mesas", EnumSet.of(Rol.MESERO, Rol.CAJERO, Rol.ADMINISTRADOR),
            "/topic/menu", EnumSet.of(Rol.MESERO, Rol.ADMINISTRADOR),
            "/topic/cocina", EnumSet.of(Rol.COCINERO, Rol.ADMINISTRADOR),
            "/topic/estado-cocina", EnumSet.of(Rol.MESERO, Rol.COCINERO, Rol.ADMINISTRADOR),
            "/topic/inventario/alertas", EnumSet.of(Rol.ADMINISTRADOR),
            "/topic/caja", EnumSet.of(Rol.CAJERO, Rol.ADMINISTRADOR));

    private PoliticaSuscripcion() {
    }

    public static boolean permitido(UsuarioSesion usuario, String destino) {
        if (usuario == null || destino == null) {
            return false;
        }
        // Cada mesero solo escucha sus propias notificaciones de plato listo (F-11)
        if (destino.startsWith(PREFIJO_MESERO)) {
            return usuario.rol() == Rol.MESERO
                    && destino.equals(PREFIJO_MESERO + usuario.id());
        }
        Set<Rol> roles = TOPICOS.get(destino);
        return roles != null && roles.contains(usuario.rol());
    }
}
