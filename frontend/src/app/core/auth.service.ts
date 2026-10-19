import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { LoginResponse, Rol, Usuario } from './models';

const CLAVE_SESION = 'comanda.sesion';

interface Sesion {
  token: string;
  usuario: Usuario;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly sesion = signal<Sesion | null>(leerSesion());

  readonly usuario = computed(() => this.sesion()?.usuario ?? null);
  readonly token = computed(() => this.sesion()?.token ?? null);

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', { username, password }).pipe(
      tap((r) => {
        const sesion: Sesion = { token: r.token, usuario: r.usuario };
        guardarSesion(sesion);
        this.sesion.set(sesion);
      }),
    );
  }

  logout(): void {
    guardarSesion(null);
    this.sesion.set(null);
    this.router.navigate(['/login']);
  }

  /** Ruta de inicio según el rol. */
  rutaInicio(rol: Rol | undefined): string {
    switch (rol) {
      case 'MESERO':
        return '/mesero';
      case 'COCINERO':
        return '/cocina';
      case 'CAJERO':
        return '/caja';
      case 'ADMINISTRADOR':
        return '/admin';
      default:
        return '/sin-modulo';
    }
  }
}

function leerSesion(): Sesion | null {
  try {
    const crudo = localStorage.getItem(CLAVE_SESION);
    if (!crudo) return null;
    const sesion = JSON.parse(crudo) as Sesion;
    const payload = JSON.parse(atob(sesion.token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    if (typeof payload.exp === 'number' && payload.exp * 1000 < Date.now()) return null;
    return sesion;
  } catch {
    return null;
  }
}

function guardarSesion(sesion: Sesion | null): void {
  try {
    if (sesion) localStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
    else localStorage.removeItem(CLAVE_SESION);
  } catch {
    /* almacenamiento no disponible: la sesión vive solo en memoria */
  }
}
