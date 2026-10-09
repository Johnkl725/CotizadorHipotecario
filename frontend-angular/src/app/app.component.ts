import { Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';
import { IconComponent } from './shared/icon.component';
import { errorMessage } from './services/solicitud.service';
import { finalize } from 'rxjs';
@Component({
  selector: 'app-root', standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconComponent],
  templateUrl: './app.component.html', styleUrl: './app.component.css'
})
export class AppComponent {
  busy=false;error='';
  constructor(public auth:AuthService,private router:Router) {}
  logout() {
    if(this.busy)return;
    this.busy=true;this.error='';
    this.auth.logout().pipe(finalize(()=>this.busy=false)).subscribe({
      next:()=>void this.router.navigate(['/login']),
      error:error=>this.error=errorMessage(error)
    });
  }
}
