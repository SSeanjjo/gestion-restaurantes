import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';
import { Rol } from './models';

/** Protege una ruta: exige sesión y uno de los roles indicados en `data.roles`. */
export const rolGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const usuario = auth.usuario();
  if (!usuario) return router.parseUrl('/login');

  const roles = (route.data?.['roles'] as Rol[] | undefined) ?? [];
  if (roles.length === 0 || roles.includes(usuario.rol)) return true;
  return router.parseUrl(auth.rutaInicio(usuario.rol));
};
