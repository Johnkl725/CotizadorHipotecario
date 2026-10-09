import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, Subject, Subscription, debounceTime, distinctUntilChanged } from 'rxjs';
import { SolicitudService, Solicitud, errorMessage } from '../services/solicitud.service';
import { AuthService } from '../services/auth.service';
import { IconComponent } from '../shared/icon.component';
@Component({
  selector:'app-aprobaciones',standalone:true,imports:[CommonModule,FormsModule,RouterLink,IconComponent],
  templateUrl:'./aprobaciones.component.html',styleUrl:'./aprobaciones.component.css'
})
export class AprobacionesComponent implements OnInit, OnDestroy {
  query='';filter='';page=0;total=0;totalPages=0;
  solicitudes:Solicitud[]=[];
  selected:Solicitud|null=null;
  comentario='';busy=false;loading=false;error='';success='';
  private search=new Subject<string>();
  private subscriptions=new Subscription();
  private listRequest?:Subscription;
  constructor(public api:SolicitudService,public auth:AuthService){}
  ngOnInit(){
    this.subscriptions.add(this.search.pipe(debounceTime(300),distinctUntilChanged()).subscribe(()=>this.load(0)));
    this.load();
  }
  ngOnDestroy(){this.subscriptions.unsubscribe();this.listRequest?.unsubscribe();}
  searchChanged(){this.search.next(this.query);}
  get pending(){return this.solicitudes.filter(s=>s.estadoActual==='REGISTRADO').length;}
  get evaluating(){return this.solicitudes.filter(s=>s.estadoActual==='EN_EVALUACION').length;}
  get approved(){return this.solicitudes.filter(s=>s.estadoActual==='APROBADO').length;}
  load(page=this.page){
    this.listRequest?.unsubscribe();
    this.loading=true;this.error='';
    this.listRequest=this.api.listar(page,this.filter,this.query).pipe(finalize(()=>this.loading=false)).subscribe({
      next:result=>{this.solicitudes=result.content;this.total=result.totalElements;this.totalPages=result.totalPages;this.page=result.number;},
      error:error=>this.error=errorMessage(error)
    });
  }
  clearFilters(){this.query='';this.filter='';this.load(0);}
  clientName(s:Solicitud){
    const client=s.participantes.find(p=>p.tipoParticipacion==='TITULAR')?.cliente;
    return client?client.nombres+' '+client.apellidos:'Sin titular';
  }
  stateLabel(state:string){
    return ({REGISTRADO:'Registrada',EN_EVALUACION:'En evaluación',APROBADO:'Aprobada',RECHAZADO:'Rechazada'} as Record<string,string>)[state]||state;
  }
  select(s:Solicitud){
    if(this.busy)return;
    this.busy=true;this.error='';this.success='';
    this.subscriptions.add(this.api.detalle(s.solicitudId).pipe(finalize(()=>this.busy=false)).subscribe({
      next:detail=>{this.selected=detail;this.comentario='';},
      error:error=>this.error=errorMessage(error)
    }));
  }
  canDecide(s:Solicitud){
    return this.auth.isManager() && s.estadoActual==='EN_EVALUACION' && s.gestorRiesgo?.empleadoId===this.auth.user()?.empleadoId;
  }
  canApprove(s:Solicitud){return this.canDecide(s) && s.ltv<=90 && s.dsti<=40;}
  evaluate(s:Solicitud){
    if(this.busy||!this.auth.isManager())return;
    this.busy=true;this.error='';this.success='';
    this.subscriptions.add(this.api.evaluar(s).pipe(finalize(()=>this.busy=false)).subscribe({
      next:response=>{this.selected=response;this.success='Evaluación iniciada. El expediente está asignado a tu usuario.';this.load();},
      error:error=>this.error=errorMessage(error)
    }));
  }
  decide(approve:boolean){
    const s=this.selected;
    if(this.busy||!s||!this.canDecide(s)||!this.comentario.trim()||(approve&&!this.canApprove(s)))return;
    this.busy=true;this.error='';this.success='';
    this.subscriptions.add(this.api.decidir(s,approve,this.comentario.trim()).pipe(finalize(()=>this.busy=false)).subscribe({
      next:response=>{this.selected=response;this.comentario='';this.success=approve?'Solicitud aprobada. La decisión quedó registrada.':'Solicitud rechazada. El fundamento quedó registrado.';this.load();},
      error:error=>this.error=errorMessage(error)
    }));
  }
}
