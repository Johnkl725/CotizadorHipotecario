import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, of } from 'rxjs';
import { AuthService } from './auth.service';
export const authGuard: CanActivateFn = route => {
  const auth = inject(AuthService), router = inject(Router);
  return (auth.user() ? of(auth.user()) : auth.restore()).pipe(map(user => {
    if (!user) return router.createUrlTree(['/login']);
    if (route.data['role'] && route.data['role'] !== user.rolPrincipal) return router.createUrlTree(['/aprobaciones']);
    return true;
  }));
};
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService), router = inject(Router);
  return auth.restore().pipe(map(user => user
    ? router.createUrlTree([user.rolPrincipal === 'EJECUTIVO_COMERCIAL' ? '/simulador' : '/aprobaciones'])
    : true));
};
