import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import {
  Cierre,
  Cuenta,
  DetalleCuenta,
  EstadoCocina,
  Factura,
  Ingrediente,
  Mesa,
  NotificacionMesero,
  Pedido,
  ProductoMenu,
  ResumenDia,
  SolicitudPago,
} from './models';

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
  crearPedido(numeroMesa: number, items: ItemPedido[], claveIdempotencia?: string): Observable<Pedido> {
    return this.http.post<Pedido>('/api/pedidos', { numeroMesa, items, claveIdempotencia });
  }

  // F-08
  estadoCocina(): Observable<EstadoCocina> {
    return this.http.get<EstadoCocina>('/api/pedidos/cocina/estado');
  }

  confirmarRecepcion(pedidoIds: number[]): Observable<EstadoCocina> {
    return this.http.post<EstadoCocina>('/api/pedidos/cocina/ack', { pedidoIds });
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

  // F-14
  solicitarCuenta(pedidoId: number): Observable<Pedido> {
    return this.http.post<Pedido>(`/api/pedidos/${pedidoId}/solicitar-cuenta`, {});
  }

  // --- CU-02 · Caja ---------------------------------------------------------
  cuentas(): Observable<Cuenta[]> {
    return this.http.get<Cuenta[]>('/api/caja/cuentas');
  }

  cuenta(pedidoId: number): Observable<DetalleCuenta> {
    return this.http.get<DetalleCuenta>(`/api/caja/cuentas/${pedidoId}`);
  }

  cobrar(pedidoId: number, pagos: SolicitudPago[]): Observable<Factura> {
    return this.http.post<Factura>(`/api/caja/cuentas/${pedidoId}/cobro`, { pagos });
  }

  facturasDelDia(): Observable<Factura[]> {
    return this.http.get<Factura[]>('/api/caja/facturas');
  }

  resumenDia(efectivoContado?: number | null): Observable<ResumenDia> {
    const params: Record<string, string> = {};
    if (efectivoContado !== null && efectivoContado !== undefined) params['efectivoContado'] = String(efectivoContado);
    return this.http.get<ResumenDia>('/api/caja/resumen-dia', { params });
  }

  cerrarCaja(efectivoContado: number | null, observacion: string, confirmarInconsistencias: boolean): Observable<Cierre> {
    return this.http.post<Cierre>('/api/caja/cierre', { efectivoContado, observacion, confirmarInconsistencias });
  }

  cierres(): Observable<Cierre[]> {
    return this.http.get<Cierre[]>('/api/caja/cierres');
  }

  // --- Inventario (IU-04) -----------------------------------------------------
  inventario(): Observable<Ingrediente[]> {
    return this.http.get<Ingrediente[]>('/api/inventario');
  }

  registrarEntrada(ingredienteId: number, cantidad: number, observacion: string): Observable<Ingrediente> {
    return this.http.post<Ingrediente>('/api/inventario/entradas', { ingredienteId, cantidad, observacion });
  }
}
