import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth.service';
import { ConfigService } from '../../core/config.service';
import { errorApi } from '../../core/errores';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <main class="login">
      <form class="tarjeta" (ngSubmit)="entrar()" #f="ngForm">
        <h1>{{ config.nombreRestaurante() }}</h1>
        <p class="sub">Inicie sesión para continuar</p>

        <label>
          Usuario
          <input name="username" [(ngModel)]="username" required autocomplete="username" autocapitalize="off" />
        </label>
        <label>
          Contraseña
          <input name="password" type="password" [(ngModel)]="password" required autocomplete="current-password" />
        </label>

        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }

        <button type="submit" class="btn-primario" [disabled]="cargando() || f.invalid">
          {{ cargando() ? 'Ingresando…' : 'Ingresar' }}
        </button>
      </form>
    </main>
  `,
  styles: `
    .login { min-height: 100dvh; display: grid; place-items: center; padding: 16px; background: var(--fondo); }
    .tarjeta { width: min(380px, 100%); background: var(--superficie); border-radius: 16px; padding: 28px;
      box-shadow: var(--sombra); display: flex; flex-direction: column; gap: 14px; }
    h1 { margin: 0; font-size: 1.5rem; color: var(--verde-oscuro); }
    .sub { margin: 0 0 6px; color: var(--texto-suave); }
    label { display: flex; flex-direction: column; gap: 6px; font-weight: 600; font-size: .9rem; }
    input { min-height: 44px; padding: 0 12px; border: 1px solid var(--borde); border-radius: 10px; font: inherit; }
    input:focus { outline: 2px solid var(--verde); border-color: transparent; }
    .error { margin: 0; color: var(--rojo); font-weight: 600; }
  `,
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly config = inject(ConfigService);

  username = '';
  password = '';
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    const usuario = this.auth.usuario();
    if (usuario) this.router.navigateByUrl(this.auth.rutaInicio(usuario.rol));
  }

  entrar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.auth.login(this.username, this.password).subscribe({
      next: (r) => this.router.navigateByUrl(this.auth.rutaInicio(r.usuario.rol)),
      error: (e) => {
        this.error.set(errorApi(e).mensaje);
        this.cargando.set(false);
      },
    });
  }
}
