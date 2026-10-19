import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ConfigService } from '../../core/config.service';
import { errorApi } from '../../core/errores';
import { AlertaInventario, Cierre, Ingrediente, ResumenDia } from '../../core/models';
import { RealtimeService } from '../../core/realtime.service';

type Vista = 'resumen' | 'inventario' | 'cierre';

/**
 * IU-04 · Panel de administración.
 * Resumen del día · F-21 inventario con alertas y entradas · F-20 conciliación y cierre de caja
 */
@Component({
  selector: 'app-admin',
  imports: [CurrencyPipe, DatePipe, DecimalPipe, FormsModule, RouterLink],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css',
})
export class AdminComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly realtime = inject(RealtimeService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly auth = inject(AuthService);
  protected readonly config = inject(ConfigService);
  protected readonly hoy = new Date();

  protected readonly vista = signal<Vista>('resumen');
  protected readonly resumen = signal<ResumenDia | null>(null);
  protected readonly ingredientes = signal<Ingrediente[]>([]);
  protected readonly cierres = signal<Cierre[]>([]);
  protected readonly ultimoCierre = signal<Cierre | null>(null);
  protected readonly mensaje = signal<{ tipo: 'ok' | 'error'; texto: string } | null>(null);
  protected readonly ocupado = signal(false);

  protected readonly enMinimo = computed(() => this.ingredientes().filter((i) => i.enNivelMinimo));

  // Formularios
  protected entradas: Record<number, number | null> = {};
  protected efectivoContado: number | null = null;
  protected observacion = '';
  protected confirmarInconsistencias = false;

  ngOnInit(): void {
    this.cargarResumen();
    this.cargarInventario();
    this.cargarCierres();

    this.realtime
      .escuchar('/topic/caja')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.cargarResumen());

    // SWR-06: alerta en vivo cuando un ingrediente llega a su nivel mínimo
    this.realtime
      .escuchar<AlertaInventario>('/topic/inventario/alertas')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((a) => {
        this.cargarInventario();
        this.avisar('error', `Stock mínimo: ${a.ingredientes.map((i) => i.nombre).join(', ')}`);
      });
    this.realtime.reconectado$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.cargarResumen();
      this.cargarInventario();
    });
  }

  ir(v: Vista): void {
    this.vista.set(v);
    if (v === 'cierre') this.conciliar();
  }

  // --- Inventario ---------------------------------------------------------------
  registrarEntrada(i: Ingrediente): void {
    const cantidad = this.entradas[i.id];
    if (!cantidad || cantidad <= 0) return;
    this.api.registrarEntrada(i.id, cantidad, 'Entrada desde el panel de administración').subscribe({
      next: (actualizado) => {
        this.ingredientes.update((lista) => lista.map((x) => (x.id === actualizado.id ? actualizado : x)));
        this.entradas[i.id] = null;
        this.avisar('ok', `Entrada registrada: ${actualizado.nombre} ahora tiene ${actualizado.stock} ${actualizado.unidad}`);
      },
      error: (e) => this.avisar('error', errorApi(e).mensaje),
    });
  }

  // --- Cierre de caja -------------------------------------------------------------
  conciliar(): void {
    this.cargarResumen(this.efectivoContado);
  }

  cerrarCaja(): void {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.api.cerrarCaja(this.efectivoContado, this.observacion, this.confirmarInconsistencias).subscribe({
      next: (c) => {
        this.ocupado.set(false);
        this.ultimoCierre.set(c);
        this.avisar('ok', `Caja cerrada · reporte enviado a la DIAN (${c.comprobanteDian})`);
        this.cargarResumen();
        this.cargarCierres();
      },
      error: (e) => {
        this.ocupado.set(false);
        const err = errorApi(e);
        this.avisar('error', err.mensaje);
        this.conciliar();
      },
    });
  }

  salir(): void {
    this.auth.logout();
  }

  // --- Carga ------------------------------------------------------------------------
  private cargarResumen(efectivoContado: number | null = null): void {
    this.api.resumenDia(efectivoContado).subscribe({ next: (r) => this.resumen.set(r) });
  }

  private cargarInventario(): void {
    this.api.inventario().subscribe({ next: (i) => this.ingredientes.set(i) });
  }

  private cargarCierres(): void {
    this.api.cierres().subscribe({ next: (c) => this.cierres.set(c) });
  }

  private temporizador?: ReturnType<typeof setTimeout>;

  private avisar(tipo: 'ok' | 'error', texto: string): void {
    clearTimeout(this.temporizador);
    this.mensaje.set({ tipo, texto });
    this.temporizador = setTimeout(() => this.mensaje.set(null), 6000);
  }
}
