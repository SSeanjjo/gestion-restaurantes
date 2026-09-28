package co.edu.uniquindio.comanda.mesa;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.comanda.pedido.PedidoDto;
import co.edu.uniquindio.comanda.pedido.PedidoService;

@RestController
@RequestMapping("/api/mesas")
public class MesaController {

    private final PedidoService pedidoService;

    public MesaController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    /** F-01: estado de todas las mesas. */
    @GetMapping
    @PreAuthorize("hasAnyRole('MESERO','CAJERO','ADMINISTRADOR')")
    public List<MesaDto> listar() {
        return pedidoService.listarMesas();
    }

    /** F-01: pedido activo de la mesa seleccionada (204 si no tiene). */
    @GetMapping("/{numero}/pedido-activo")
    @PreAuthorize("hasAnyRole('MESERO','CAJERO','ADMINISTRADOR')")
    public ResponseEntity<PedidoDto> pedidoActivo(@PathVariable Integer numero) {
        return pedidoService.pedidoActivoDeMesa(numero)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
