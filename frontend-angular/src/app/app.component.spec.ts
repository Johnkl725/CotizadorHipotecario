import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { AppComponent } from './app.component';
import { AuthService, SessionUser } from './services/auth.service';
describe('Navigation by authenticated role',()=>{
  beforeEach(async()=>{await TestBed.configureTestingModule({imports:[AppComponent],providers:[provideRouter([]),provideHttpClient()]}).compileComponents();});
  it('shows simulator and personal portfolio for an executive',()=>{
    TestBed.inject(AuthService).user.set({nombres:'Carlos',rolPrincipal:'EJECUTIVO_COMERCIAL'} as SessionUser);
    const fixture=TestBed.createComponent(AppComponent);fixture.detectChanges();
    const element:HTMLElement=fixture.nativeElement;
    expect(element.querySelector('nav a[href="/simulador"]')).toBeTruthy();
    expect(element.querySelector('nav')?.textContent).toContain('Mis solicitudes');
  });
  it('shows risk management without origination for a manager',()=>{
    TestBed.inject(AuthService).user.set({nombres:'Laura',rolPrincipal:'GESTOR_RIESGOS'} as SessionUser);
    const fixture=TestBed.createComponent(AppComponent);fixture.detectChanges();
    const element:HTMLElement=fixture.nativeElement;
    expect(element.querySelector('nav a[href="/simulador"]')).toBeNull();
    expect(element.querySelector('nav')?.textContent).toContain('Bandeja de riesgos');
  });
  it('does not show private navigation without a session',()=>{
    const fixture=TestBed.createComponent(AppComponent);fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('nav')).toBeNull();
  });
});
