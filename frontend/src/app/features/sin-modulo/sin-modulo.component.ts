import { Component, inject } from '@angular/core';

import { AuthService } from '../../core/auth.service';

/** Roles cuyo módulo aún no existe (cajero y administrador llegan con CU-02 a CU-06). */
@Component({
  selector: 'app-sin-modulo',
  template: `
    <main class="vacio">
      <h1>Hola, {{ auth.usuario()?.nombre }}</h1>
      <p>El módulo para el rol <strong>{{ auth.usuario()?.rol }}</strong> todavía no está disponible en esta versión.</p>
      <button class="btn-secundario" (click)="auth.logout()">Cerrar sesión</button>
    </main>
  `,
  styles: `
    .vacio { min-height: 100dvh; display: grid; place-content: center; gap: 12px; text-align: center; padding: 16px; }
    h1 { margin: 0; }
  `,
})
export class SinModuloComponent {
  protected readonly auth = inject(AuthService);
}
