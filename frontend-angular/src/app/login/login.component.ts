import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { errorMessage } from '../services/solicitud.service';
import { IconComponent } from '../shared/icon.component';
@Component({
  selector:'app-login', standalone:true, imports:[FormsModule,IconComponent],
  templateUrl:'./login.component.html',styleUrl:'./login.component.css'
})
export class LoginComponent {
  username=''; password=''; busy=false; error='';
  constructor(private auth: AuthService, private router: Router) {}
  login() {
    if (this.busy || !this.username.trim() || !this.password) return;
    this.busy=true;this.error='';
    this.auth.login(this.username,this.password).pipe(finalize(()=>this.busy=false)).subscribe({
      next:user=>{this.password='';void this.router.navigate([user.rolPrincipal==='EJECUTIVO_COMERCIAL'?'/simulador':'/aprobaciones']);},
      error:error=>{this.password='';this.error=error.status===401?'Matrícula o contraseña incorrectas.':errorMessage(error);}
    });
  }
}
