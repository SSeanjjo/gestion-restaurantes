import { Injectable, inject, signal } from '@angular/core';

import { ApiService } from './api.service';

/** Datos públicos del restaurante (el nombre sale de RESTAURANTE_NOMBRE en el backend). */
@Injectable({ providedIn: 'root' })
export class ConfigService {
  private readonly api = inject(ApiService);
  readonly nombreRestaurante = signal('Comanda');

  constructor() {
    this.api.config().subscribe({
      next: (c) => this.nombreRestaurante.set(c.nombreRestaurante),
      error: () => {
        /* se mantiene el nombre por defecto */
      },
    });
  }
}
