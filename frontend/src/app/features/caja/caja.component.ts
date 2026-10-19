import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ConfigService } from '../../core/config.service';
import { errorApi } from '../../core/errores';
import { ETIQUETA_ESTADO_PEDIDO, Cuenta, DetalleCuenta, Factura, SolicitudPago } from '../../core/models';
import { RealtimeService } from '../../core/realtime.service';

type Modo = 'EFECTIVO' | 'TARJETA' | 'DIVIDIDO';

/**
 * IU-03 · Módulo de caja (CU-02).
 * F-15 cuentas y desglose · F-16 efectivo con cambio · F-17 tarjeta por pasarela
 * F-18 pago dividido · F-19 factura electrónica DIAN y liberación de la mesa
 */
@Component({
  selector: 'app-caja',
  imports: [CurrencyPipe, DatePipe, FormsModule],
  templateUrl: './caja.component.html',
  styleUrl: './caja.component.css',
})
export class CajaComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly realtime = inject(RealtimeService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly auth = inject(AuthService);
  protected readonly config = inject(ConfigService);
  protected readonly conectado = this.realtime.conectado;
  protected readonly etiquetaEstado = ETIQUETA_ESTADO_PEDIDO;

  protected readonly cuentas = signal<Cuenta[]>([]);
  protected readonly seleccionada = signal<number | null>(null);
  protected readonly detalle = signal<DetalleCuenta | null>(null);
  protected readonly facturas = signal<Factura[]>([]);
  protected readonly ultima = computed(() => this.facturas()[0] ?? null);
  protected readonly procesando = signal(false);
  protected readonly mensaje = signal<{ tipo: 'ok' | 'error'; texto: string } | null>(null);

  // Formulario de pago
  protected readonly modo = signal<Modo>('EFECTIVO');
  protected recibido: number | null = null;
  protected digitos = '';
  protected montoEfectivo: number | null = null;

  protected readonly total = computed(() => this.detalle()?.desglose.total ?? 0);

  ngOnInit(): void {
    this.cargarCuentas();
    this.cargarFacturas();

    // Cambios en cuentas (pedido entregado, cuenta solicitada, factura emitida, caja cerrada)
    this.realtime
      .escuchar('/topic/caja')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.cargarCuentas();
        const id = this.seleccionada();
        if (id !== null) this.cargarDetalle(id);
      });
    this.realtime.reconectado$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.cargarCuentas());
  }

  seleccionar(c: Cuenta): void {
    this.seleccionada.set(c.pedidoId);
    this.modo.set('EFECTIVO');
    this.recibido = null;
    this.digitos = '';
    this.montoEfectivo = null;
    this.cargarDetalle(c.pedidoId);
  }

  cambiarModo(m: Modo): void {
    this.modo.set(m);
  }

  /** Cambio a devolver en efectivo, calculado en tiempo real. */
  cambio(): number | null {
    const efectivo = this.porcionEfectivo();
    if (this.recibido === null || efectivo === null) return null;
    return this.recibido - efectivo;
  }

  porcionEfectivo(): number | null {
    switch (this.modo()) {
      case 'EFECTIVO':
        return this.total();
      case 'TARJETA':
        return null;
      case 'DIVIDIDO':
        return this.montoEfectivo;
    }
  }

  porcionTarjeta(): number {
    return this.total() - (this.porcionEfectivo() ?? 0);
  }

  puedeCobrar(): boolean {
    const d = this.detalle();
    if (!d || !d.cuenta.cobrable || this.procesando()) return false;
    const efectivo = this.porcionEfectivo();
    const necesitaTarjeta = this.modo() !== 'EFECTIVO';
    const necesitaEfectivo = this.modo() !== 'TARJETA';
    if (necesitaTarjeta && !/^\d{4}$/.test(this.digitos)) return false;
    if (this.modo() === 'DIVIDIDO' && (efectivo === null || efectivo <= 0 || efectivo >= this.total())) return false;
    if (necesitaEfectivo && (this.recibido === null || efectivo === null || this.recibido < efectivo)) return false;
    return true;
  }

  cobrar(): void {
    const d = this.detalle();
    if (!d || !this.puedeCobrar()) return;
    this.procesando.set(true);
    this.api.cobrar(d.cuenta.pedidoId, this.pagos()).subscribe({
      next: (factura) => {
        this.procesando.set(false);
        this.facturas.update((lista) => [factura, ...lista]);
        this.avisar('ok', `Factura ${factura.numero} validada por la DIAN · mesa ${factura.mesaNumero} liberada`);
        this.seleccionada.set(null);
        this.detalle.set(null);
        this.cargarCuentas();
      },
      error: (e) => {
        this.procesando.set(false);
        this.avisar('error', errorApi(e).mensaje);
      },
    });
  }

  claseCuenta(c: Cuenta): string {
    if (c.cuentaSolicitada) return 'solicitada';
    return c.cobrable ? 'entregada' : 'servicio';
  }

  textoCuenta(c: Cuenta): string {
    if (c.cuentaSolicitada) return 'Cuenta solicitada';
    return c.cobrable ? 'Lista para cobrar' : 'En servicio';
  }

  metodos(f: Factura): string {
    return f.pagos.map((p) => (p.metodo === 'EFECTIVO' ? 'Efectivo' : `Tarjeta ****${p.ultimosDigitos}`)).join(' + ');
  }

  salir(): void {
    this.auth.logout();
  }

  private pagos(): SolicitudPago[] {
    const tarjeta: SolicitudPago = { metodo: 'TARJETA', monto: this.porcionTarjeta(), ultimosDigitos: this.digitos };
    switch (this.modo()) {
      case 'EFECTIVO':
        return [{ metodo: 'EFECTIVO', monto: this.total(), montoRecibido: this.recibido }];
      case 'TARJETA':
        return [tarjeta];
      case 'DIVIDIDO':
        return [{ metodo: 'EFECTIVO', monto: this.montoEfectivo ?? 0, montoRecibido: this.recibido }, tarjeta];
    }
  }

  private cargarCuentas(): void {
    this.api.cuentas().subscribe({ next: (c) => this.cuentas.set(c) });
  }

  private cargarDetalle(pedidoId: number): void {
    this.api.cuenta(pedidoId).subscribe({
      next: (d) => {
        if (this.seleccionada() === pedidoId) this.detalle.set(d);
      },
      error: () => {
        this.seleccionada.set(null);
        this.detalle.set(null);
      },
    });
  }

  private cargarFacturas(): void {
    this.api.facturasDelDia().subscribe({ next: (f) => this.facturas.set(f) });
  }

  private temporizador?: ReturnType<typeof setTimeout>;

  private avisar(tipo: 'ok' | 'error', texto: string): void {
    clearTimeout(this.temporizador);
    this.mensaje.set({ tipo, texto });
    this.temporizador = setTimeout(() => this.mensaje.set(null), tipo === 'ok' ? 4000 : 7000);
  }
}
