import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ConfigService } from '../../core/config.service';
import { errorApi } from '../../core/errores';
import {
  ETIQUETA_ESTADO_PEDIDO,
  EstadoPedido,
  Mesa,
  NotificacionMesero,
  Pedido,
  PlatoNoDisponible,
  ProductoMenu,
} from '../../core/models';
import { RealtimeService } from '../../core/realtime.service';

interface LineaBorrador {
  producto: ProductoMenu;
  cantidad: number;
}

const MOTIVOS_ANULACION = ['El cliente canceló', 'El cliente se retiró', 'Error de digitación'];

/**
 * IU-01 · Tablet del mesero.
 * F-01 mesas · F-02 menú · F-03/F-04 pedido en construcción · F-05 alerta de stock
 * F-11 notificaciones · F-12 servido · F-13 anular
 */
@Component({
  selector: 'app-mesero',
  imports: [CurrencyPipe, FormsModule],
  templateUrl: './mesero.component.html',
  styleUrl: './mesero.component.css',
})
export class MeseroComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly realtime = inject(RealtimeService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly auth = inject(AuthService);
  protected readonly config = inject(ConfigService);

  protected readonly etiquetaEstado = ETIQUETA_ESTADO_PEDIDO;
  protected readonly motivos = MOTIVOS_ANULACION;
  protected readonly conectado = this.realtime.conectado;

  // --- Estado -----------------------------------------------------------------
  protected readonly mesas = signal<Mesa[]>([]);
  protected readonly menu = signal<ProductoMenu[]>([]);
  protected readonly categoria = signal<string>('Todos');
  protected readonly mesaSeleccionada = signal<number | null>(null);
  protected readonly pedidoActivo = signal<Pedido | null>(null);
  protected readonly cargandoPedido = signal(false);
  protected readonly borrador = signal<LineaBorrador[]>([]);
  protected readonly noDisponibles = signal<PlatoNoDisponible[]>([]);
  protected readonly notificaciones = signal<NotificacionMesero[]>([]);
  protected readonly enviando = signal(false);
  protected readonly mensaje = signal<{ tipo: 'ok' | 'error'; texto: string } | null>(null);

  // Anulación
  protected readonly anulando = signal(false);
  protected motivoAnulacion = MOTIVOS_ANULACION[0];

  // --- Derivados --------------------------------------------------------------
  protected readonly categorias = computed(() => [
    'Todos',
    ...Array.from(new Set(this.menu().map((p) => p.categoria))),
  ]);

  protected readonly menuFiltrado = computed(() => {
    const c = this.categoria();
    return c === 'Todos' ? this.menu() : this.menu().filter((p) => p.categoria === c);
  });

  protected readonly totalBorrador = computed(() =>
    this.borrador().reduce((suma, l) => suma + l.producto.precio * l.cantidad, 0),
  );

  protected readonly unidadesBorrador = computed(() => this.borrador().reduce((s, l) => s + l.cantidad, 0));

  private temporizadorMensaje?: ReturnType<typeof setTimeout>;

  ngOnInit(): void {
    this.cargarMesas();
    this.cargarMenu();
    this.cargarNotificaciones();

    const usuarioId = this.auth.usuario()?.id;

    // F-01: cambios de estado de mesas en tiempo real
    this.realtime
      .escuchar<Mesa>('/topic/mesas')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((mesa) => {
        this.mesas.update((lista) => lista.map((m) => (m.numero === mesa.numero ? mesa : m)));
        if (mesa.numero === this.mesaSeleccionada()) this.cargarPedidoActivo(mesa.numero);
      });

    // F-02: el stock cambió → recalcular disponibilidad
    this.realtime
      .escuchar('/topic/menu')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.cargarMenu());

    // F-11: notificaciones de plato listo solo para este mesero
    this.realtime
      .escuchar<NotificacionMesero>(`/topic/meseros/${usuarioId}`)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((n) => this.recibirNotificacion(n));

    // Tras una reconexión se recarga todo por si se perdieron eventos
    this.realtime.reconectado$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.cargarMesas();
      this.cargarMenu();
      this.cargarNotificaciones();
      const n = this.mesaSeleccionada();
      if (n !== null) this.cargarPedidoActivo(n);
    });
  }

  // --- F-01 -------------------------------------------------------------------
  seleccionarMesa(mesa: Mesa): void {
    if (this.mesaSeleccionada() !== mesa.numero) {
      this.borrador.set([]);
      this.noDisponibles.set([]);
      this.anulando.set(false);
    }
    this.mesaSeleccionada.set(mesa.numero);
    this.cargarPedidoActivo(mesa.numero);
  }

  // --- F-02 / F-04 ------------------------------------------------------------
  agregar(producto: ProductoMenu): void {
    if (!producto.disponible || this.mesaSeleccionada() === null || this.pedidoActivo()) return;
    this.borrador.update((lineas) => {
      const existente = lineas.find((l) => l.producto.id === producto.id);
      return existente
        ? lineas.map((l) => (l === existente ? { ...l, cantidad: l.cantidad + 1 } : l))
        : [...lineas, { producto, cantidad: 1 }];
    });
  }

  cambiarCantidad(productoId: number, delta: number): void {
    this.borrador.update((lineas) =>
      lineas
        .map((l) => (l.producto.id === productoId ? { ...l, cantidad: l.cantidad + delta } : l))
        .filter((l) => l.cantidad > 0),
    );
  }

  quitar(productoId: number): void {
    this.borrador.update((lineas) => lineas.filter((l) => l.producto.id !== productoId));
    this.noDisponibles.update((lista) => lista.filter((p) => p.productoId !== productoId));
  }

  esNoDisponible(productoId: number): boolean {
    return this.noDisponibles().some((p) => p.productoId === productoId);
  }

  // --- F-03 · F-05 · F-06 · F-07 ---------------------------------------------
  confirmar(): void {
    const mesa = this.mesaSeleccionada();
    if (mesa === null || this.borrador().length === 0 || this.enviando()) return;

    this.enviando.set(true);
    this.noDisponibles.set([]);
    const items = this.borrador().map((l) => ({ productoId: l.producto.id, cantidad: l.cantidad }));

    this.api.crearPedido(mesa, items).subscribe({
      next: (pedido) => {
        this.enviando.set(false);
        this.borrador.set([]);
        this.pedidoActivo.set(pedido);
        this.avisar('ok', `Pedido de la mesa ${mesa} enviado a cocina`);
      },
      error: (e) => {
        this.enviando.set(false);
        const err = errorApi(e);
        // F-05: se conservan los demás platos y se marcan los que no se pueden preparar
        if (err.codigo === 'INGREDIENTES_INSUFICIENTES' || err.codigo === 'PRODUCTO_INACTIVO') {
          this.noDisponibles.set((err.detalles as PlatoNoDisponible[]) ?? []);
          this.cargarMenu();
        }
        if (err.codigo === 'MESA_OCUPADA') this.cargarPedidoActivo(mesa);
        this.avisar('error', err.mensaje);
      },
    });
  }

  // --- F-11 · F-12 ------------------------------------------------------------
  private recibirNotificacion(n: NotificacionMesero): void {
    if (n.tipo === 'ATENDIDA') {
      this.notificaciones.update((lista) => lista.filter((x) => x.pedidoId !== n.pedidoId));
      return;
    }
    this.notificaciones.update((lista) => [n, ...lista.filter((x) => x.pedidoId !== n.pedidoId)]);
    this.sonar();
  }

  marcarServido(pedidoId: number): void {
    this.api.marcarServido(pedidoId).subscribe({
      next: (pedido) => {
        this.notificaciones.update((lista) => lista.filter((x) => x.pedidoId !== pedidoId));
        if (this.pedidoActivo()?.id === pedido.id) this.pedidoActivo.set(pedido);
      },
      error: (e) => this.avisar('error', errorApi(e).mensaje),
    });
  }

  // --- F-13 -------------------------------------------------------------------
  anular(pedido: Pedido): void {
    this.api.anular(pedido.id, this.motivoAnulacion).subscribe({
      next: () => {
        this.anulando.set(false);
        this.pedidoActivo.set(null);
        this.avisar('ok', `Pedido de la mesa ${pedido.mesaNumero} anulado`);
      },
      error: (e) => this.avisar('error', errorApi(e).mensaje),
    });
  }

  // --- Utilidades de vista ----------------------------------------------------
  claseMesa(m: Mesa): string {
    if (m.numero === this.mesaSeleccionada()) return 'seleccionada';
    return m.estado.toLowerCase();
  }

  claseEstado(estado: EstadoPedido): string {
    return 'estado-' + estado.toLowerCase().replace(/_/g, '-');
  }

  salir(): void {
    this.auth.logout();
  }

  // --- Carga de datos ---------------------------------------------------------
  private cargarMesas(): void {
    this.api.mesas().subscribe({ next: (m) => this.mesas.set(m) });
  }

  private cargarMenu(): void {
    this.api.menu().subscribe({ next: (m) => this.menu.set(m) });
  }

  private cargarNotificaciones(): void {
    this.api.notificaciones().subscribe({ next: (n) => this.notificaciones.set(n) });
  }

  private cargarPedidoActivo(numero: number): void {
    this.cargandoPedido.set(true);
    this.api.pedidoActivo(numero).subscribe({
      next: (p) => {
        if (this.mesaSeleccionada() === numero) this.pedidoActivo.set(p);
        this.cargandoPedido.set(false);
      },
      error: () => this.cargandoPedido.set(false),
    });
  }

  private avisar(tipo: 'ok' | 'error', texto: string): void {
    clearTimeout(this.temporizadorMensaje);
    this.mensaje.set({ tipo, texto });
    this.temporizadorMensaje = setTimeout(() => this.mensaje.set(null), tipo === 'ok' ? 3000 : 6000);
  }

  private sonar(): void {
    try {
      const ctx = new AudioContext();
      const osc = ctx.createOscillator();
      osc.frequency.value = 880;
      osc.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 0.15);
    } catch {
      /* sin audio disponible */
    }
  }
}
