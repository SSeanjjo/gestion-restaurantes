package co.edu.uniquindio.comanda.menu;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.comanda.menu.MenuDtos.LineaRecetaDto;
import co.edu.uniquindio.comanda.menu.MenuDtos.ProductoMenuDto;
import co.edu.uniquindio.comanda.menu.MenuDtos.RecetaDto;

@Service
public class MenuService {

    private final ProductoMenuRepository productos;

    public MenuService(ProductoMenuRepository productos) {
        this.productos = productos;
    }

    /**
     * F-02: menú del mesero. Los platos suspendidos no se devuelven; los que no tienen
     * ingredientes suficientes se devuelven con {@code disponible = false}.
     */
    @Transactional(readOnly = true)
    public List<ProductoMenuDto> menuDisponible() {
        return productos.findConRecetaPorEstado(EstadoProducto.ACTIVO).stream()
                .map(p -> new ProductoMenuDto(p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(),
                        p.getCategoria(), CalculadoraReceta.disponible(p)))
                .toList();
    }

    /** Catálogo de recetas con el stock actual de cada ingrediente. */
    @Transactional(readOnly = true)
    public List<RecetaDto> catalogoRecetas() {
        return productos.findTodosConReceta().stream()
                .map(p -> new RecetaDto(p.getId(), p.getNombre(), p.getCategoria(), p.getEstado(), p.getPrecio(),
                        p.isActivo() && CalculadoraReceta.disponible(p),
                        p.getReceta().stream()
                                .sorted(Comparator.comparing(r -> r.getIngrediente().getNombre()))
                                .map(r -> new LineaRecetaDto(r.getIngrediente().getId(),
                                        r.getIngrediente().getNombre(), r.getIngrediente().getUnidad(),
                                        r.getCantidad(), r.getIngrediente().getStock()))
                                .toList()))
                .toList();
    }
}
