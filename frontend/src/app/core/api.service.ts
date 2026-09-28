import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { Mesa, NotificacionMesero, Pedido, ProductoMenu } from './models';

export interface ItemPedido {
  productoId: number;
  cantidad: number;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  config(): Observable<{ nombreRestaurante: string }> {
    return this.http.get<{ nombreRestaurante: string }>('/api/config');
  }

  // F-01
  mesas(): Observable<Mesa[]> {
    return this.http.get<Mesa[]>('/api/mesas');
  }

  pedidoActivo(numeroMesa: number): Observable<Pedido | null> {
    return this.http
      .get<Pedido>(`/api/mesas/${numeroMesa}/pedido-activo`, { observe: 'response' })
      .pipe(map((r) => (r.status === 204 ? null : r.body)));
  }

  // F-02
  menu(): Observable<ProductoMenu[]> {
    return this.http.get<ProductoMenu[]>('/api/menu');
  }

  // F-03 · F-05 · F-06 · F-07
  crearPedido(numeroMesa: number, items: ItemPedido[]): Observable<Pedido> {
    return this.http.post<Pedido>('/api/pedidos', { numeroMesa, items });
  }

  // F-09
  comandasCocina(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>('/api/pedidos/cocina');
  }

  // F-10
  cambiarEstado(pedidoId: number, nuevoEstado: 'EN_PREPARACION' | 'LISTO_PARA_SERVIR'): Observable<Pedido> {
    return this.http.patch<Pedido>(`/api/pedidos/${pedidoId}/estado`, { nuevoEstado });
  }

  marcarDetalle(pedidoId: number, detalleId: number, preparado: boolean): Observable<Pedido> {
    return this.http.patch<Pedido>(`/api/pedidos/${pedidoId}/detalles/${detalleId}`, { preparado });
  }

  // F-11
  notificaciones(): Observable<NotificacionMesero[]> {
    return this.http.get<NotificacionMesero[]>('/api/pedidos/notificaciones');
  }

  // F-12
  marcarServido(pedidoId: number): Observable<Pedido> {
    return this.http.post<Pedido>(`/api/pedidos/${pedidoId}/servido`, {});
  }

  // F-13
  anular(pedidoId: number, motivo: string): Observable<Pedido> {
    return this.http.post<Pedido>(`/api/pedidos/${pedidoId}/anular`, { motivo });
  }
}
