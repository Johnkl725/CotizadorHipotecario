import { Injectable, computed, signal } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, of, switchMap, tap } from 'rxjs';

export interface SessionUser {
  empleadoId: number; codigoMatricula: string; nombres: string; apellidos: string;
  rolPrincipal: 'EJECUTIVO_COMERCIAL' | 'GESTOR_RIESGOS';
}
@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly user = signal<SessionUser | null>(null);
  readonly isExecutive = computed(() => this.user()?.rolPrincipal === 'EJECUTIVO_COMERCIAL');
  readonly isManager = computed(() => this.user()?.rolPrincipal === 'GESTOR_RIESGOS');
  readonly roleLabel = computed(() => this.isExecutive() ? 'Ejecutivo comercial' : 'Gestor de riesgos');
  constructor(private http: HttpClient) {}
  restore(): Observable<SessionUser | null> {
    return this.http.get<SessionUser>('/api/session').pipe(tap(user => this.user.set(user)), catchError(() => {
      this.user.set(null); return of(null);
    }));
  }
  mutation<T>(url: string, body: unknown, headers = new HttpHeaders()): Observable<T> {
    return this.http.get<{token: string; headerName: string}>('/api/csrf').pipe(
      switchMap(csrf => this.http.post<T>(url, body, { headers: headers.set(csrf.headerName, csrf.token) }))
    );
  }
  login(username: string, password: string) {
    const body = new HttpParams().set('username', username.trim()).set('password', password);
    return this.mutation('/api/login', body.toString(), new HttpHeaders({'Content-Type':'application/x-www-form-urlencoded'}))
      .pipe(switchMap(() => this.http.get<SessionUser>('/api/session')), tap(user => this.user.set(user)));
  }
  logout() {
    return this.mutation<void>('/api/logout', {}).pipe(tap(() => this.user.set(null)));
  }
}
