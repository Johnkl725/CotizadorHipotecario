import { Routes } from '@angular/router';
import { SimuladorComponent } from './simulador/simulador.component';
import { AprobacionesComponent } from './aprobaciones/aprobaciones.component';
import { LoginComponent } from './login/login.component';
import { authGuard, guestGuard } from './services/auth.guard';
export const routes: Routes = [
  {path:'login',component:LoginComponent,canActivate:[guestGuard],title:'Acceso · Umbral'},
  {path:'simulador',component:SimuladorComponent,canActivate:[authGuard],data:{role:'EJECUTIVO_COMERCIAL'},title:'Simulador · Umbral'},
  {path:'aprobaciones',component:AprobacionesComponent,canActivate:[authGuard],title:'Solicitudes · Umbral'},
  {path:'',redirectTo:'/login',pathMatch:'full'},
  {path:'**',redirectTo:'/login'}
];
