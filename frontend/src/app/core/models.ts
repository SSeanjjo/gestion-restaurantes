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
  cuentaSolicitadaEn: string | null;
  detalles: DetallePedido[];
}

export interface PedidoCambiado {
  tipo: 'CREADO' | 'ACTUALIZADO' | 'ANULADO' | 'REENVIADO';
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

// --- F-08 · Conexión con cocina -------------------------------------------------
export interface EstadoCocina {
  conectada: boolean;
  comandasEnCola: number;
}

// --- CU-02 · Caja ----------------------------------------------------------------
export type MetodoPago = 'EFECTIVO' | 'TARJETA';

export interface Cuenta {
  pedidoId: number;
  mesaNumero: number;
  meseroNombre: string;
  estado: EstadoPedido;
  cuentaSolicitada: boolean;
  cobrable: boolean;
  platos: number;
  total: number;
  fechaHora: string;
}

export interface Desglose {
  base: number;
  impuesto: number;
  tarifa: number;
  nombreImpuesto: string;
  total: number;
}

export interface DetalleCuenta {
  cuenta: Cuenta;
  items: DetallePedido[];
  desglose: Desglose;
}

export interface SolicitudPago {
  metodo: MetodoPago;
  monto: number;
  montoRecibido?: number | null;
  ultimosDigitos?: string | null;
}

export interface PagoFactura {
  metodo: MetodoPago;
  monto: number;
  montoRecibido: number | null;
  cambio: number | null;
  autorizacion: string | null;
  ultimosDigitos: string | null;
}

export interface Factura {
  id: number;
  numero: string;
  cufe: string;
  estadoDian: 'ACEPTADA' | 'RECHAZADA';
  mensajeDian: string;
  pedidoId: number;
  mesaNumero: number;
  fechaEmision: string;
  subtotal: number;
  impuesto: number;
  total: number;
  pagos: PagoFactura[];
}

export interface ResumenJornada {
  fecha: string;
  numeroFacturas: number;
  numeroTransacciones: number;
  totalVentas: number;
  totalEfectivo: number;
  totalTarjeta: number;
  totalImpuesto: number;
  efectivoContado: number | null;
  pedidosSinCobrar: number;
  inconsistencias: string[];
}

export interface ResumenDia {
  resumen: ResumenJornada;
  cajaCerrada: boolean;
}

export interface Cierre {
  id: number;
  fecha: string;
  ejecutadoPor: string;
  ejecutadoEn: string;
  numeroFacturas: number;
  numeroTransacciones: number;
  totalVentas: number;
  totalEfectivo: number;
  totalTarjeta: number;
  totalImpuesto: number;
  efectivoContado: number | null;
  inconsistencias: string | null;
  observacion: string | null;
  comprobanteDian: string;
}

export interface CajaActualizada {
  motivo: string;
  pedidoId: number | null;
}

// --- Inventario (IU-04) ------------------------------------------------------------
export interface Ingrediente {
  id: number;
  nombre: string;
  unidad: string;
  stock: number;
  stockMinimo: number;
  enNivelMinimo: boolean;
}

export interface AlertaInventario {
  ingredientes: Ingrediente[];
}
