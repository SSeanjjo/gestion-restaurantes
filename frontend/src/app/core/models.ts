export type Rol = 'MESERO' | 'COCINERO' | 'CAJERO' | 'ADMINISTRADOR';

export type EstadoMesa = 'DISPONIBLE' | 'OCUPADA' | 'RESERVADA';

export type EstadoPedido =
  | 'REGISTRADO'
  | 'EN_PREPARACION'
  | 'LISTO_PARA_SERVIR'
  | 'ENTREGADO'
  | 'FACTURADO'
  | 'CANCELADO';

export interface Usuario {
  id: number;
  username: string;
  nombre: string;
  rol: Rol;
}

export interface LoginResponse {
  token: string;
  usuario: Usuario;
}

export interface Mesa {
  id: number;
  numero: number;
  capacidad: number;
  estado: EstadoMesa;
  pedidoActivoId: number | null;
  estadoPedido: EstadoPedido | null;
}

export interface ProductoMenu {
  id: number;
  nombre: string;
  descripcion: string | null;
  precio: number;
  categoria: string;
  disponible: boolean;
}

export interface DetallePedido {
  id: number;
  productoId: number;
  producto: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  preparado: boolean;
}

export interface Pedido {
  id: number;
  mesaNumero: number;
  meseroId: number;
  meseroNombre: string;
  fechaHora: string;
  estado: EstadoPedido;
  total: number;
  detalles: DetallePedido[];
}

export interface PedidoCambiado {
  tipo: 'CREADO' | 'ACTUALIZADO' | 'ANULADO';
  pedido: Pedido;
}

export interface NotificacionMesero {
  meseroId: number;
  tipo: 'LISTO' | 'RECORDATORIO' | 'ATENDIDA';
  pedidoId: number;
  mesaNumero: number;
  platos: string[];
  fecha: string | null;
}

export interface PlatoNoDisponible {
  productoId: number;
  nombre: string;
}

export interface ErrorApi {
  codigo: string;
  mensaje: string;
  detalles: unknown;
}

export const ETIQUETA_ESTADO_PEDIDO: Record<EstadoPedido, string> = {
  REGISTRADO: 'Registrado',
  EN_PREPARACION: 'En preparación',
  LISTO_PARA_SERVIR: 'Listo para servir',
  ENTREGADO: 'Entregado',
  FACTURADO: 'Facturado',
  CANCELADO: 'Anulado',
};
