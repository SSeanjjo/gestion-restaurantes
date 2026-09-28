import { HttpErrorResponse } from '@angular/common/http';

import { ErrorApi } from './models';

/** Extrae el cuerpo de error estándar del backend ({codigo, mensaje, detalles}). */
export function errorApi(err: unknown): ErrorApi {
  if (err instanceof HttpErrorResponse) {
    const cuerpo = err.error as Partial<ErrorApi> | null;
    if (cuerpo && typeof cuerpo === 'object' && cuerpo.codigo) {
      return { codigo: cuerpo.codigo, mensaje: cuerpo.mensaje ?? 'Error', detalles: cuerpo.detalles ?? null };
    }
    if (err.status === 0) {
      return { codigo: 'SIN_CONEXION', mensaje: 'No hay conexión con el servidor', detalles: null };
    }
  }
  return { codigo: 'ERROR', mensaje: 'Ocurrió un error inesperado', detalles: null };
}
