package co.edu.uniquindio.comanda.menu;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoMenuRepository extends JpaRepository<ProductoMenu, Long> {

    /** Productos (con su receta e ingredientes) en un estado dado, para calcular disponibilidad. */
    @Query("""
            select distinct p from ProductoMenu p
              left join fetch p.receta r
              left join fetch r.ingrediente
             where p.estado = :estado
             order by p.categoria, p.nombre
            """)
    List<ProductoMenu> findConRecetaPorEstado(@Param("estado") EstadoProducto estado);

    @Query("""
            select distinct p from ProductoMenu p
              left join fetch p.receta r
              left join fetch r.ingrediente
             order by p.categoria, p.nombre
            """)
    List<ProductoMenu> findTodosConReceta();

    @Query("""
            select distinct p from ProductoMenu p
              left join fetch p.receta r
              left join fetch r.ingrediente
             where p.id in :ids
            """)
    List<ProductoMenu> findConRecetaPorIds(@Param("ids") Collection<Long> ids);

    /** Solo los ids de ingredientes que usan los productos (sin cargar las entidades). */
    @Query("select distinct r.ingrediente.id from RecetaIngrediente r where r.producto.id in :ids")
    List<Long> idsIngredientesDeProductos(@Param("ids") Collection<Long> ids);
}
