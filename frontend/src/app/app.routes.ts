import { Routes } from '@angular/router';

import { rolGuard } from './core/rol.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent),
  },
  {
    // IU-01 · Tablet del mesero
    path: 'mesero',
    canActivate: [rolGuard],
    data: { roles: ['MESERO'] },
    loadComponent: () => import('./features/mesero/mesero.component').then((m) => m.MeseroComponent),
  },
  {
    // IU-02 · Pantalla de cocina
    path: 'cocina',
    canActivate: [rolGuard],
    data: { roles: ['COCINERO'] },
    loadComponent: () => import('./features/cocina/cocina.component').then((m) => m.CocinaComponent),
  },
  {
    path: 'sin-modulo',
    canActivate: [rolGuard],
    loadComponent: () => import('./features/sin-modulo/sin-modulo.component').then((m) => m.SinModuloComponent),
  },
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: '**', redirectTo: 'login' },
];
