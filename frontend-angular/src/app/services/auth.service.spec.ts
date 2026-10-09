import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting,HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
describe('Server-authenticated session',()=>{
  let auth:AuthService;let http:HttpTestingController;
  beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});auth=TestBed.inject(AuthService);http=TestBed.inject(HttpTestingController);});
  afterEach(()=>http.verify());
  it('derives role from the server after login and clears it only after logout succeeds',()=>{
    auth.login('ejecutivo','test-password').subscribe();
    http.expectOne('/api/csrf').flush({token:'login-csrf',headerName:'X-CSRF-TOKEN'});
    const login=http.expectOne('/api/login');
    expect(login.request.body).toContain('username=ejecutivo');
    expect(login.request.headers.get('X-CSRF-TOKEN')).toBe('login-csrf');
    login.flush({authenticated:true});
    http.expectOne('/api/session').flush({empleadoId:5,codigoMatricula:'ejecutivo',rolPrincipal:'EJECUTIVO_COMERCIAL'});
    expect(auth.isExecutive()).toBeTrue();
    auth.logout().subscribe();
    http.expectOne('/api/csrf').flush({token:'rotated-csrf',headerName:'X-CSRF-TOKEN'});
    http.expectOne('/api/logout').flush(null,{status:204,statusText:'No Content'});
    expect(auth.user()).toBeNull();
  });
  it('does not authenticate a failed login',()=>{
    auth.login('aprobador','wrong').subscribe({error:()=>{}});
    http.expectOne('/api/csrf').flush({token:'csrf',headerName:'X-CSRF-TOKEN'});
    http.expectOne('/api/login').flush({message:'invalid'},{status:401,statusText:'Unauthorized'});
    expect(auth.user()).toBeNull();
  });
});
