import { Injectable, NgZone, OnDestroy, inject, signal } from '@angular/core';
import { Subject } from 'rxjs';

import { ApiService, ItemPedido } from './api.service';
import { errorApi, esErrorDeConexion } from './errores';
import { ErrorApi, Pedido } from './models';

const CLAVE_ALMACEN = 'comanda.pedidosPendientes';
const INTERVALO_REINTENTO_MS = 10_000;

/** Pedido confirmado en la tablet que todavía no llegó al servidor. */
export interface PedidoPendiente {
  clave: string;
  numeroMesa: number;
  items: ItemPedido[];
  resumen: string;
  creadoEn: number;
}

export interface ResultadoEnvio {
  pendiente: PedidoPendiente;
  pedido?: Pedido;
  error?: ErrorApi;
}

/**
 * F-08 (lado tablet) · Si la tablet pierde la red al confirmar, el pedido se guarda en el
 * dispositivo y se reenvía en orden cuando vuelve la conexión. Cada pedido lleva una clave de
 * idempotencia: si el servidor ya lo había recibido, el reintento no lo duplica.
 */
@Injectable({ providedIn: 'root' })
export class ColaPedidosService implements OnDestroy {
  private readonly api = inject(ApiService);

  readonly pendientes = signal<PedidoPendiente[]>(leer());
  private readonly resultadosSujeto = new Subject<ResultadoEnvio>();
  readonly resultados$ = this.resultadosSujeto.asObservable();

  private enviando = false;
  private readonly temporizador: ReturnType<typeof setInterval>;
  private readonly alVolverLaRed = () => this.reintentar();

  constructor() {
    const zona = inject(NgZone);
    this.temporizador = zona.runOutsideAngular(() =>
      setInterval(() => zona.run(() => this.reintentar()), INTERVALO_REINTENTO_MS),
    );
    window.addEventListener('online', this.alVolverLaRed);
  }

  static nuevaClave(): string {
    // crypto.randomUUID solo existe en contextos seguros (HTTPS/localhost); la tablet usa http://IP
    if (typeof crypto.randomUUID === 'function') return crypto.randomUUID();
    const b = crypto.getRandomValues(new Uint8Array(16));
    b[6] = (b[6] & 0x0f) | 0x40;
    b[8] = (b[8] & 0x3f) | 0x80;
    const h = Array.from(b, (x) => x.toString(16).padStart(2, '0')).join('');
    return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
  }

  tienePendiente(numeroMesa: number): boolean {
    return this.pendientes().some((p) => p.numeroMesa === numeroMesa);
  }

  encolar(pendiente: PedidoPendiente): void {
    this.pendientes.update((lista) => [...lista.filter((p) => p.clave !== pendiente.clave), pendiente]);
    guardar(this.pendientes());
  }

  /** Envía los pendientes uno a uno, en el orden en que se confirmaron. */
  reintentar(): void {
    const primero = this.pendientes()[0];
    if (this.enviando || !primero) return;
    this.enviando = true;
    this.api.crearPedido(primero.numeroMesa, primero.items, primero.clave).subscribe({
      next: (pedido) => this.terminar({ pendiente: primero, pedido }),
      error: (e: unknown) => {
        if (esErrorDeConexion(e)) {
          this.enviando = false; // sigue sin red: se reintenta más tarde
          return;
        }
        this.terminar({ pendiente: primero, error: errorApi(e) });
      },
    });
  }

  private terminar(resultado: ResultadoEnvio): void {
    this.pendientes.update((lista) => lista.filter((p) => p.clave !== resultado.pendiente.clave));
    guardar(this.pendientes());
    this.enviando = false;
    this.resultadosSujeto.next(resultado);
    this.reintentar();
  }

  ngOnDestroy(): void {
    clearInterval(this.temporizador);
    window.removeEventListener('online', this.alVolverLaRed);
  }
}

function leer(): PedidoPendiente[] {
  try {
    const crudo = localStorage.getItem(CLAVE_ALMACEN);
    return crudo ? (JSON.parse(crudo) as PedidoPendiente[]) : [];
  } catch {
    return [];
  }
}

function guardar(lista: PedidoPendiente[]): void {
  try {
    localStorage.setItem(CLAVE_ALMACEN, JSON.stringify(lista));
  } catch {
    /* almacenamiento no disponible: la cola vive solo en memoria */
  }
}
