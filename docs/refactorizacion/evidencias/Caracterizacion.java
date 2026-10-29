import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

import co.edu.uniquindio.comanda.auth.*;
import co.edu.uniquindio.comanda.caja.*;
import co.edu.uniquindio.comanda.caja.dian.ServicioFacturacionElectronica;
import co.edu.uniquindio.comanda.caja.pago.*;
import co.edu.uniquindio.comanda.common.*;
import co.edu.uniquindio.comanda.config.ComandaProperties;
import co.edu.uniquindio.comanda.mesa.*;
import co.edu.uniquindio.comanda.menu.*;
import co.edu.uniquindio.comanda.inventario.*;
import co.edu.uniquindio.comanda.pedido.*;
import co.edu.uniquindio.comanda.tiemporeal.Eventos;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Pruebas de caracterización del cobro (CU-02) y de la anulación (F-13).
 * Registran el comportamiento observable (resultado, estados, eventos y llamadas a sistemas
 * externos) para compararlo ANTES y DESPUÉS de la refactorización. Sin Spring ni Mockito: los
 * repositorios son proxies y los servicios se arman por reflexión con el constructor vigente,
 * de modo que el mismo archivo corre sobre las dos versiones del código.
 */
public class Caracterizacion {

    static final List<String> traza = new ArrayList<>();
    static final List<String> externos = new ArrayList<>();

    static void set(Object o, String f, Object v) throws Exception {
        Class<?> c = o.getClass();
        while (true) {
            try { Field fl = c.getDeclaredField(f); fl.setAccessible(true); fl.set(o, v); return; }
            catch (NoSuchFieldException e) { c = c.getSuperclass(); }
        }
    }

    static <T> T nuevo(Class<T> c) throws Exception {
        Constructor<T> k = c.getDeclaredConstructor(); k.setAccessible(true); return k.newInstance();
    }

    @SuppressWarnings("unchecked")
    static <T> T repo(Class<T> tipo, Map<String, Function<Object[], Object>> metodos) {
        return (T) Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[] { tipo }, (p, m, a) -> {
            Function<Object[], Object> f = metodos.get(m.getName());
            if (f != null) return f.apply(a == null ? new Object[0] : a);
            if (m.getName().equals("toString")) return tipo.getSimpleName();
            if (m.getName().equals("hashCode")) return 1;
            if (m.getName().equals("equals")) return p == a[0];
            Class<?> r = m.getReturnType();
            if (r == Optional.class) return Optional.empty();
            if (r == boolean.class) return false;
            if (List.class.isAssignableFrom(r)) return List.of();
            return null;
        });
    }

    /** Arma una instancia con el único constructor público, resolviendo dependencias del registro. */
    static Object construir(Class<?> c, Map<Class<?>, Object> reg) throws Exception {
        Constructor<?> k = c.getConstructors()[0];
        Object[] args = new Object[k.getParameterCount()];
        for (int i = 0; i < args.length; i++) {
            Class<?> t = k.getParameterTypes()[i];
            Object v = reg.get(t);
            if (v == null) {
                if (t.isInterface() && !List.class.isAssignableFrom(t)) throw new IllegalStateException("falta " + t);
                v = construir(t, reg);
                reg.put(t, v);
            }
            args[i] = v;
        }
        return k.newInstance(args);
    }

    static String evento(Object e) {
        if (e instanceof Eventos.MesaCambiada m) return "MesaCambiada(mesa=" + m.mesa().numero() + ",estado=" + m.mesa().estado() + ",pedidoActivo=" + m.mesa().pedidoActivoId() + ")";
        if (e instanceof Eventos.CajaActualizada c) return "CajaActualizada(" + c.motivo() + "," + c.pedidoId() + ")";
        if (e instanceof Eventos.PedidoCambiado p) return "PedidoCambiado(" + p.tipo() + ",pedido=" + p.pedido().id() + "," + p.pedido().estado() + ")";
        return e.getClass().getSimpleName();
    }

    // ----- escenario base ------------------------------------------------------------------
    static class Mundo {
        Mesa mesa; Pedido pedido; Usuario cajero; Usuario mesero;
        List<Object> eventos = new ArrayList<>();
        int facturasGuardadas = 0; int dianLlamadas = 0; boolean dianAcepta = true; boolean cajaCerrada = false;
        boolean yaFacturado = false; boolean reversoFalla = false;
        Map<Class<?>, Object> reg = new HashMap<>();
        CajaService caja; PedidoService pedidoService;

        Mundo(EstadoPedido estado, String total) throws Exception {
            mesa = nuevo(Mesa.class); set(mesa, "id", 5L); set(mesa, "numero", 5); set(mesa, "capacidad", 4); mesa.ocupar();
            mesero = nuevo(Usuario.class); set(mesero, "id", 2L); set(mesero, "nombre", "Mesero 2");
            cajero = nuevo(Usuario.class); set(cajero, "id", 4L); set(cajero, "nombre", "Cajero 4");
            pedido = nuevo(Pedido.class); set(pedido, "id", 20L); set(pedido, "estado", estado);
            set(pedido, "total", new BigDecimal(total)); set(pedido, "mesa", mesa); set(pedido, "mesero", mesero);

            reg.put(PedidoRepository.class, repo(PedidoRepository.class, Map.of(
                    "bloquearPorId", a -> Optional.of(pedido), "buscarCompleto", a -> Optional.of(pedido))));
            reg.put(MesaRepository.class, repo(MesaRepository.class, Map.of(
                    "findByIdParaActualizar", a -> Optional.of(mesa))));
            reg.put(UsuarioRepository.class, repo(UsuarioRepository.class, Map.of(
                    "findById", a -> ((Long) a[0]) == 4L ? Optional.of(cajero) : Optional.of(mesero))));
            reg.put(FacturaRepository.class, repo(FacturaRepository.class, Map.of(
                    "existsByPedidoId", a -> yaFacturado,
                    "save", a -> { facturasGuardadas++; try { set(a[0], "id", 15L); } catch (Exception e) { throw new RuntimeException(e); } return a[0]; })));
            reg.put(ServicioFacturacionElectronica.class, new ServicioFacturacionElectronica() {
                public Respuesta validarFactura(DocumentoFactura d) { dianLlamadas++; externos.add("DIAN.validarFactura(" + d.numero() + ")"); return new Respuesta(dianAcepta, dianAcepta ? "ok" : "CUFE inválido"); }
                public String enviarReporteDiario(ReporteDiario r) { return "x"; }
            });
            reg.put(JornadaOperativa.class, new JornadaOperativa() {
                public boolean cajaCerrada(java.time.LocalDate f) { return cajaCerrada; }
            });
            reg.put(ApplicationEventPublisher.class, (ApplicationEventPublisher) eventos::add);
            reg.put(ComandaProperties.class, new ComandaProperties(null, null, null, null, null,
                    new ComandaProperties.Facturacion("900123456", "FEBS", new BigDecimal("0.08"), "INC", "clave", 2)));
            reg.put(PasarelaPago.class, new PasarelaPago() {
                public Autorizacion autorizar(BigDecimal m, String d, String r) {
                    externos.add("Pasarela.autorizar(" + m + "," + d + ")");
                    return "0000".equals(d) ? new Autorizacion(false, null, "Rechazada") : new Autorizacion(true, "AUT-" + m.intValue(), "Aprobada");
                }
                public void reversar(String c) { externos.add("Pasarela.reversar(" + c + ")"); if (reversoFalla) throw new RuntimeException("tiempo de espera agotado"); }
            });
            reg.put(ProductoMenuRepository.class, repo(ProductoMenuRepository.class, Map.of()));
            reg.put(IngredienteRepository.class, repo(IngredienteRepository.class, Map.of()));
            reg.put(MovimientoInventarioRepository.class, repo(MovimientoInventarioRepository.class, Map.of()));
            reg.put(ReglaDisponibilidad.class, new DisponibilidadPorReceta());
            reg.put(List.class, List.of(new PagoEfectivo(), new PagoTarjeta((PasarelaPago) reg.get(PasarelaPago.class))));
        }

        CajaService caja() throws Exception { return (CajaService) construir(CajaService.class, reg); }
        PedidoService pedidos() throws Exception { return (PedidoService) construir(PedidoService.class, reg); }
    }

    // ----- escenarios ----------------------------------------------------------------------
    interface Accion { Object ejecutar(Mundo m) throws Exception; }

    static void escenario(String nombre, EstadoPedido estado, String total, java.util.function.Consumer<Mundo> preparar, Accion accion) throws Exception {
        externos.clear();
        Mundo m = new Mundo(estado, total);
        if (preparar != null) preparar.accept(m);
        String resultado;
        try {
            Object r = accion.ejecutar(m);
            resultado = r == null ? "ok" : "ok " + r;
        } catch (NegocioException e) {
            resultado = "NegocioException " + e.getCodigo();
        } catch (RuntimeException e) {
            resultado = "RuntimeException " + e.getMessage();
        }
        traza.add("== " + nombre);
        traza.add("resultado: " + resultado);
        traza.add("pedido.estado: " + m.pedido.getEstado());
        traza.add("mesa.estado: " + m.mesa.getEstado());
        traza.add("facturas guardadas: " + m.facturasGuardadas + " | llamadas DIAN: " + m.dianLlamadas);
        traza.add("externos: " + externos);
        traza.add("eventos: " + m.eventos.stream().map(Caracterizacion::evento).toList());
    }

    static CajaDtos.SolicitudCobro cobro(SolicitudPago... p) { return new CajaDtos.SolicitudCobro(List.of(p)); }
    static SolicitudPago efectivo(String monto, String recibido) { return new SolicitudPago(MetodoPago.EFECTIVO, new BigDecimal(monto), recibido == null ? null : new BigDecimal(recibido), null); }
    static SolicitudPago tarjeta(String monto, String digitos) { return new SolicitudPago(MetodoPago.TARJETA, new BigDecimal(monto), null, digitos); }

    static String resumen(CajaDtos.FacturaDto f) {
        return "factura " + f.numero() + " cufe(" + f.cufe().length() + ") dian=" + f.estadoDian() + " pagos=" + f.pagos().size() + " total=" + f.total();
    }

    public static void main(String[] args) throws Exception {
        escenario("C1 cobro dividido efectivo+tarjeta", EstadoPedido.ENTREGADO, "28000", null,
                m -> resumen(m.caja().cobrar(20L, cobro(efectivo("10000", "20000"), tarjeta("18000", "4242")), 4L)));
        escenario("C2 pagos que no suman el total", EstadoPedido.ENTREGADO, "28000", null,
                m -> m.caja().cobrar(20L, cobro(tarjeta("20000", "4242")), 4L));
        escenario("C3 la DIAN rechaza: se reversa la tarjeta", EstadoPedido.ENTREGADO, "28000", m -> m.dianAcepta = false,
                m -> m.caja().cobrar(20L, cobro(tarjeta("28000", "4242")), 4L));
        escenario("C4 pedido no entregado", EstadoPedido.EN_PREPARACION, "28000", null,
                m -> m.caja().cobrar(20L, cobro(efectivo("28000", null)), 4L));
        escenario("C5 pedido ya facturado", EstadoPedido.ENTREGADO, "28000", m -> m.yaFacturado = true,
                m -> m.caja().cobrar(20L, cobro(efectivo("28000", null)), 4L));
        escenario("C6 tarjeta rechazada por el banco (efectivo + 0000)", EstadoPedido.ENTREGADO, "28000", null,
                m -> m.caja().cobrar(20L, cobro(efectivo("10000", "10000"), tarjeta("18000", "0000")), 4L));
        escenario("C7 caja cerrada", EstadoPedido.ENTREGADO, "28000", m -> m.cajaCerrada = true,
                m -> { m.caja(); return m.caja().cobrar(20L, cobro(efectivo("28000", null)), 4L); });
        escenario("C8 efectivo con cambio", EstadoPedido.ENTREGADO, "28000", null,
                m -> resumen(m.caja().cobrar(20L, cobro(efectivo("28000", "50000")), 4L)));
        escenario("C9 (H-03) el reverso de la 1.ª tarjeta falla mientras se compensa el rechazo de la DIAN", EstadoPedido.ENTREGADO, "28000",
                m -> { m.dianAcepta = false; m.reversoFalla = true; },
                m -> m.caja().cobrar(20L, cobro(tarjeta("10000", "4242"), tarjeta("18000", "4343")), 4L));
        escenario("A1 anular pedido registrado (mesero dueño)", EstadoPedido.REGISTRADO, "28000", null,
                m -> m.pedidos().anular(20L, "Cliente se fue", 2L, Rol.MESERO).estado());
        escenario("A2 anular pedido en preparación", EstadoPedido.EN_PREPARACION, "28000", null,
                m -> m.pedidos().anular(20L, "x", 2L, Rol.MESERO));
        escenario("A3 anular pedido ajeno", EstadoPedido.REGISTRADO, "28000", null,
                m -> m.pedidos().anular(20L, "x", 9L, Rol.MESERO));
        traza.forEach(System.out::println);
    }
}
