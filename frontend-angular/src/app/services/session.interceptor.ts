import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';
export const sessionInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService), router = inject(Router);
  return next(req).pipe(catchError(error => {
    if (error.status === 401 && !['/api/login','/api/session'].includes(req.url)) {
      auth.user.set(null);
      void router.navigate(['/login']);
    }
    return throwError(() => error);
  }));
};
