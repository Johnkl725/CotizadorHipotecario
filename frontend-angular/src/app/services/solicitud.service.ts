import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { AuthService, SessionUser } from './auth.service';
export interface Cliente {
  clienteId: number; tipoDocumento: string; numeroDocumento: string; nombres: string; apellidos: string; ingresoMensualNeto: number;
}
export interface Producto { productoId: number; nombreProducto: string; tasaInteresReferencial: number; }
export interface Participante { tipoParticipacion: string; cliente: Cliente; }
export interface Inmueble { tipoInmueble: string; direccion: string; partidaRegistral: string; valorComercial: number; valorTasacion: number | null; }
export interface Solicitud {
  solicitudId: number; numeroExpediente: string; producto: Producto;
  montoSolicitado: number; moneda: string; plazoMeses: number; estadoActual: string;
  fechaCreacion: string; version: number; participantes: Participante[]; inmuebles: Inmueble[];
  ejecutivo: SessionUser; gestorRiesgo: SessionUser | null;
  cuotaMensual: number; ltv: number; dsti: number;
  historial: { historialId: number; estadoNuevo: string; fechaCambio: string; comentario: string; empleadoId: number }[];
}
export interface RegistroSolicitud {
  productoId: number; montoSolicitado: number; plazoMeses: number;
  participantes: {clienteId: number; tipoParticipacion: string}[]; inmuebles: Inmueble[];
}
export interface Pagina<T> { content: T[]; totalElements: number; totalPages: number; number: number; }
@Injectable({ providedIn: 'root' })
export class SolicitudService {
  constructor(private http: HttpClient, private auth: AuthService) {}
  productos() { return this.http.get<Producto[]>('/api/productos'); }
  clientes(q = '') { return this.http.get<Cliente[]>('/api/clientes', {params: {q}}); }
  listar(page = 0, estado = '', q = '') { return this.http.get<Pagina<Solicitud>>('/api/solicitudes', {params:{page,size:12,estado,q}}); }
  detalle(id: number) { return this.http.get<Solicitud>('/api/solicitudes/' + id); }
  registrar(solicitud: RegistroSolicitud) { return this.auth.mutation<Solicitud>('/api/solicitudes/registrar', solicitud); }
  evaluar(solicitud: Solicitud) { return this.auth.mutation<Solicitud>('/api/solicitudes/' + solicitud.solicitudId + '/evaluar', {version:solicitud.version}); }
  decidir(solicitud: Solicitud, aprobar: boolean, comentario: string) {
    return this.auth.mutation<Solicitud>('/api/solicitudes/' + solicitud.solicitudId + (aprobar ? '/aprobar' : '/rechazar'), {version:solicitud.version,comentario});
  }
}
export function errorMessage(error: HttpErrorResponse): string {
  if (error.status === 0) return 'No fue posible conectar con el servidor. Comprueba la conexión antes de volver a intentar la operación.';
  if (error.status === 401) return 'La sesión no es válida. Revisa tus credenciales e inicia sesión nuevamente.';
  if (error.status === 403) return 'La operación no está autorizada o tu sesión necesita renovarse. Vuelve a iniciar sesión.';
  if (error.status === 409) return typeof error.error?.message === 'string' ? error.error.message : 'El expediente cambió. Actualiza su detalle antes de continuar.';
  if (error.status >= 500) return 'El servidor no pudo completar la operación. Verifica si se registró antes de volver a enviarla.';
  return typeof error.error?.message === 'string' ? error.error.message : 'No se pudo completar la operación. Revisa los datos e inténtalo nuevamente.';
}
