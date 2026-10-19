package co.edu.uniquindio.comanda.cocina;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.comanda.pedido.PedidoRequests;
import co.edu.uniquindio.comanda.tiemporeal.Eventos;
import jakarta.validation.Valid;

/** F-08 · Confirmación de recepción de comandas y estado de la conexión con cocina. */
@RestController
@RequestMapping("/api/pedidos/cocina")
public class EntregaComandaController {

    private final ColaComandas cola;

    public EntregaComandaController(ColaComandas cola) {
        this.cola = cola;
    }

    /** La pantalla de cocina confirma (ACK) las comandas que ya está mostrando. */
    @PostMapping("/ack")
    @PreAuthorize("hasRole('COCINERO')")
    public Eventos.EstadoCocina confirmar(@Valid @RequestBody PedidoRequests.ConfirmarRecepcion req) {
        return cola.confirmar(req.pedidoIds());
    }

    /** Indicador de la tablet: ¿hay pantalla de cocina conectada? ¿cuántas comandas esperan ACK? */
    @GetMapping("/estado")
    @PreAuthorize("hasAnyRole('MESERO','COCINERO','ADMINISTRADOR')")
    public Eventos.EstadoCocina estado() {
        return cola.estado();
    }
}
