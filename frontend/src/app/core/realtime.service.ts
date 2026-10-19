import { Injectable, OnDestroy, effect, inject, signal } from '@angular/core';
import { Client, StompSubscription } from '@stomp/stompjs';
import { Observable, Subject } from 'rxjs';

import { AuthService } from './auth.service';

interface Registro {
  destino: string;
  sujeto: Subject<unknown>;
  suscripcion?: StompSubscription;
}

/**
 * Canal en tiempo real (IS-04): STOMP sobre WebSocket, autenticado con el JWT de la sesión.
 *
 * - Se conecta solo cuando hay sesión y se desconecta al cerrarla.
 * - Se reconecta solo y vuelve a suscribir los tópicos. `reconectado$` avisa cada vez que
 *   la conexión se (re)establece, para que la vista recargue su estado desde la API y no
 *   se pierda ningún evento ocurrido durante la desconexión.
 */
@Injectable({ providedIn: 'root' })
export class RealtimeService implements OnDestroy {
  private readonly auth = inject(AuthService);

  readonly conectado = signal(false);

  private readonly conexiones = new Subject<void>();
  readonly reconectado$ = this.conexiones.asObservable();

  private readonly registros = new Set<Registro>();
  private readonly cliente: Client;

  constructor() {
    const protocolo = location.protocol === 'https:' ? 'wss' : 'ws';
    this.cliente = new Client({
      brokerURL: `${protocolo}://${location.host}/ws`,
      reconnectDelay: 2000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      // El token se lee en cada intento: así una reconexión usa siempre la sesión vigente
      beforeConnect: () => {
        this.cliente.connectHeaders = { Authorization: `Bearer ${this.auth.token() ?? ''}` };
      },
    });
    this.cliente.onConnect = () => {
      this.conectado.set(true);
      this.registros.forEach((r) => this.suscribir(r));
      this.conexiones.next();
    };
    this.cliente.onWebSocketClose = () => this.conectado.set(false);
    this.cliente.onStompError = () => this.conectado.set(false);

    effect(() => {
      const token = this.auth.token();
      if (token && !this.cliente.active) {
        this.cliente.activate();
      } else if (!token && this.cliente.active) {
        void this.cliente.deactivate();
        this.conectado.set(false);
      }
    });
  }

  /** Observa un tópico. La suscripción sobrevive a las reconexiones. */
  escuchar<T>(destino: string): Observable<T> {
    return new Observable<T>((observador) => {
      const registro: Registro = { destino, sujeto: new Subject<unknown>() };
      const sub = registro.sujeto.subscribe((v) => observador.next(v as T));
      this.registros.add(registro);
      if (this.cliente.connected) this.suscribir(registro);

      return () => {
        sub.unsubscribe();
        if (this.cliente.connected) registro.suscripcion?.unsubscribe();
        this.registros.delete(registro);
      };
    });
  }

  private suscribir(registro: Registro): void {
    registro.suscripcion = this.cliente.subscribe(registro.destino, (mensaje) => {
      try {
        registro.sujeto.next(JSON.parse(mensaje.body));
      } catch {
        registro.sujeto.next(mensaje.body);
      }
    });
  }

  ngOnDestroy(): void {
    void this.cliente.deactivate();
  }
}
