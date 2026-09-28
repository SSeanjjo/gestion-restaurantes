package co.edu.uniquindio.comanda.pedido;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.comanda.auth.UsuarioActual;
import co.edu.uniquindio.comanda.tiemporeal.Eventos;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    /** F-03 / F-05 / F-06 / F-07: registrar y confirmar el pedido de una mesa. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MESERO')")
    public PedidoDto crear(@Valid @RequestBody PedidoRequests.CrearPedido req, @AuthenticationPrincipal Jwt jwt) {
        return pedidoService.crearPedido(req, UsuarioActual.id(jwt));
    }

    /** F-09: comandas activas para la pantalla de cocina. */
    @GetMapping("/cocina")
    @PreAuthorize("hasAnyRole('COCINERO','ADMINISTRADOR')")
    public List<PedidoDto> cocina() {
        return pedidoService.comandasEnCocina();
    }

    /** F-10: iniciar preparación o marcar como listo. */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('COCINERO')")
    public PedidoDto cambiarEstado(@PathVariable Long id, @Valid @RequestBody PedidoRequests.CambiarEstado req) {
        return pedidoService.actualizarEstadoDesdeCocina(id, req.nuevoEstado());
    }

    /** F-10: marcar un plato de la comanda como preparado (punto verde). */
    @PatchMapping("/{id}/detalles/{detalleId}")
    @PreAuthorize("hasRole('COCINERO')")
    public PedidoDto marcarItem(@PathVariable Long id, @PathVariable Long detalleId,
            @RequestBody PedidoRequests.MarcarItem req) {
        return pedidoService.marcarItemPreparado(id, detalleId, req.preparado());
    }

    /** F-11: notificaciones de platos listos pendientes del mesero autenticado. */
    @GetMapping("/notificaciones")
    @PreAuthorize("hasRole('MESERO')")
    public List<Eventos.NotificacionMesero> notificaciones(@AuthenticationPrincipal Jwt jwt) {
        return pedidoService.notificacionesPendientes(UsuarioActual.id(jwt));
    }

    /** F-12: marcar el pedido como servido. */
    @PostMapping("/{id}/servido")
    @PreAuthorize("hasRole('MESERO')")
    public PedidoDto servido(@PathVariable Long id) {
        return pedidoService.marcarServido(id);
    }

    /** F-13 (provisional): anular un pedido que la cocina aún no empieza. */
    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('MESERO','ADMINISTRADOR')")
    public PedidoDto anular(@PathVariable Long id, @Valid @RequestBody PedidoRequests.Anular req,
            @AuthenticationPrincipal Jwt jwt) {
        return pedidoService.anular(id, req.motivo(), UsuarioActual.id(jwt), UsuarioActual.rol(jwt));
    }
}
