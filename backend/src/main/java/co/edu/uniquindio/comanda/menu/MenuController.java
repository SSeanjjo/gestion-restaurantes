package co.edu.uniquindio.comanda.menu;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.comanda.menu.MenuDtos.ProductoMenuDto;
import co.edu.uniquindio.comanda.menu.MenuDtos.RecetaDto;

@RestController
@RequestMapping("/api")
public class MenuController {

    private final MenuService menu;

    public MenuController(MenuService menu) {
        this.menu = menu;
    }

    /** F-02: menú activo con disponibilidad calculada según recetas e inventario. */
    @GetMapping("/menu")
    @PreAuthorize("hasAnyRole('MESERO','ADMINISTRADOR')")
    public List<ProductoMenuDto> menu() {
        return menu.menuDisponible();
    }

    /** Catálogo de recetas (ingredientes y cantidades por plato). */
    @GetMapping("/recetas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<RecetaDto> recetas() {
        return menu.catalogoRecetas();
    }
}
