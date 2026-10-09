import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting,HttpTestingController } from '@angular/common/http/testing';
import { SolicitudService,Solicitud } from './solicitud.service';
describe('Versioned decisions',()=>{
  let service:SolicitudService;let http:HttpTestingController;
  beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});service=TestBed.inject(SolicitudService);http=TestBed.inject(HttpTestingController);});
  afterEach(()=>http.verify());
  it('uses CSRF and a body with version, without client-supplied actor IDs',()=>{
    service.decidir({solicitudId:12,version:3} as Solicitud,true,'Ingreso & respaldo + verificado').subscribe();
    http.expectOne('/api/csrf').flush({token:'csrf',headerName:'X-CSRF-TOKEN'});
    const request=http.expectOne('/api/solicitudes/12/aprobar');
    expect(request.request.headers.get('X-CSRF-TOKEN')).toBe('csrf');
    expect(request.request.headers.has('Authorization')).toBeFalse();
    expect(request.request.params.keys()).toEqual([]);
    expect(request.request.body).toEqual({version:3,comentario:'Ingreso & respaldo + verificado'});
    request.flush({solicitudId:12,version:4,estadoActual:'APROBADO'});
  });
  it('loads a paginated and filtered server portfolio',()=>{
    service.listar(2,'EN_EVALUACION','Perez').subscribe();
    const request=http.expectOne(r=>r.url==='/api/solicitudes');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('estado')).toBe('EN_EVALUACION');
    request.flush({content:[],totalElements:0,totalPages:0,number:2});
  });
});
