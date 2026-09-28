import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ConfigService } from '../../core/config.service';
import { errorApi } from '../../core/errores';
import { DetallePedido, Pedido, PedidoCambiado } from '../../core/models';
import { RealtimeService } from '../../core/realtime.service';

const EN_COCINA = new Set(['REGISTRADO', 'EN_PREPARACION', 'LISTO_PARA_SERVIR']);

/**
 * IU-02 · Pantalla de cocina.
 * F-07 recepción en tiempo real · F-09 comandas y tiempo en cola · F-10 preparación y "listo"
 */
@Component({
  selector: 'app-cocina',
  imports: [DatePipe],
  templateUrl: './cocina.component.html',
  styleUrl: './cocina.component.css',
})
export class CocinaComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly realtime = inject(RealtimeService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly auth = inject(AuthService);
  protected readonly config = inject(ConfigService);
  protected readonly conectado = this.realtime.conectado;

  protected readonly comandas = signal<Pedido[]>([]);
  protected readonly ahora = signal(Date.now());
  /** Pedidos con una acción en curso, para bloquear la doble pulsación (SWR-10). */
  protected readonly ocupados = signal<Set<number>>(new Set());
  protected readonly error = signal<string | null>(null);

  protected readonly ordenadas = computed(() =>
    [...this.comandas()].sort((a, b) => a.fechaHora.localeCompare(b.fechaHora)),
  );

  ngOnInit(): void {
    const reloj = setInterval(() => this.ahora.set(Date.now()), 1000);
    this.destroyRef.onDestroy(() => clearInterval(reloj));

    this.cargar();

    this.realtime
      .escuchar<PedidoCambiado>('/topic/cocina')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((evento) => this.aplicar(evento));

    // Al reconectar se recargan las comandas: ninguna se pierde aunque se caiga la red
    this.realtime.reconectado$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.cargar());
  }

  // --- F-09: temporizador ------------------------------------------------------
  minutos(p: Pedido): number {
    return Math.max(0, Math.floor((this.ahora() - new Date(p.fechaHora.substring(0, 19)).getTime()) / 60000));
  }

  claseTiempo(p: Pedido): string {
    const m = this.minutos(p);
    if (m < 10) return 'verde';
    if (m <= 15) return 'ambar';
    return 'rojo';
  }

  // --- F-10 ---------------------------------------------------------------------
  iniciar(p: Pedido): void {
    this.accion(p.id, () => this.api.cambiarEstado(p.id, 'EN_PREPARACION'));
  }

  marcarListo(p: Pedido): void {
    this.accion(p.id, () => this.api.cambiarEstado(p.id, 'LISTO_PARA_SERVIR'));
  }

  alternarPlato(p: Pedido, d: DetallePedido): void {
    if (p.estado !== 'EN_PREPARACION') return;
    this.accion(p.id, () => this.api.marcarDetalle(p.id, d.id, !d.preparado));
  }

  estaOcupado(id: number): boolean {
    return this.ocupados().has(id);
  }

  salir(): void {
    this.auth.logout();
  }

  // --- Internos -------------------------------------------------------------------
  private accion(id: number, llamada: () => ReturnType<ApiService['cambiarEstado']>): void {
    if (this.estaOcupado(id)) return;
    this.ocupados.update((s) => new Set(s).add(id));
    llamada().subscribe({
      next: (pedido) => {
        this.reemplazar(pedido);
        this.liberar(id);
      },
      error: (e) => {
        this.error.set(errorApi(e).mensaje);
        setTimeout(() => this.error.set(null), 5000);
        this.liberar(id);
        this.cargar();
      },
    });
  }

  private liberar(id: number): void {
    this.ocupados.update((s) => {
      const copia = new Set(s);
      copia.delete(id);
      return copia;
    });
  }

  private cargar(): void {
    this.api.comandasCocina().subscribe({ next: (c) => this.comandas.set(c) });
  }

  private aplicar(evento: PedidoCambiado): void {
    if (evento.tipo === 'CREADO') this.sonar();
    this.reemplazar(evento.pedido);
  }

  private reemplazar(pedido: Pedido): void {
    this.comandas.update((lista) => {
      const sinEste = lista.filter((p) => p.id !== pedido.id);
      return EN_COCINA.has(pedido.estado) ? [...sinEste, pedido] : sinEste;
    });
  }

  private sonar(): void {
    try {
      const ctx = new AudioContext();
      const osc = ctx.createOscillator();
      osc.frequency.value = 660;
      osc.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 0.2);
    } catch {
      /* sin audio disponible */
    }
  }
}
