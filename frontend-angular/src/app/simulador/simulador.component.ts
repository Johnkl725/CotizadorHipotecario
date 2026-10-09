import { Component, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { finalize, Subscription, Subject, debounceTime, distinctUntilChanged, switchMap, catchError, of } from 'rxjs';
import { SolicitudService, Solicitud, RegistroSolicitud, Cliente, Producto, errorMessage } from '../services/solicitud.service';
import { IconComponent } from '../shared/icon.component';
import { monthlyEstimate } from '../shared/estimate';

@Component({
  selector: 'app-simulador', standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, IconComponent],
  templateUrl: './simulador.component.html', styleUrl: './simulador.component.css'
})
export class SimuladorComponent implements OnDestroy {
  simuladorForm: FormGroup;
  busy = false;
  submitted = false;
  error = '';
  saved: Solicitud | null = null;
  productos: Producto[] = [];
  clientes: Cliente[] = [];
  catalogLoading = false;
  private search = new Subject<string>();
  private subscriptions = new Subscription();
  private changes: Subscription;
  constructor(private fb: FormBuilder, public api: SolicitudService) {
    this.simuladorForm = this.fb.group({
      valorInmueble: [350000, [Validators.required, Validators.min(1), Validators.max(999999999)]],
      cuotaInicial: [70000, [Validators.required, Validators.min(0)]],
      plazoMeses: [240, [Validators.required, Validators.min(1), Validators.max(360), Validators.pattern(/^\d+$/)]],
      tasaInteres: [null, [Validators.required, Validators.min(0), Validators.max(100)]],
      productoId: [null, [Validators.required, Validators.min(1), Validators.max(32767), Validators.pattern(/^\d+$/)]],
      participantes: this.fb.array([]),
      inmuebles: this.fb.array([])
    });
    this.addParticipante();
    this.addInmueble();
    this.changes = this.simuladorForm.valueChanges.subscribe(() => { this.saved = null; this.error = ''; });
    this.subscriptions.add(this.search.pipe(debounceTime(300),distinctUntilChanged(),
      switchMap(q => this.api.clientes(q).pipe(catchError(error => { this.error=errorMessage(error);return of([]); }))))
      .subscribe(clients => this.mergeClients(clients)));
    this.loadCatalogs();
  }
  ngOnDestroy() { this.changes.unsubscribe(); this.subscriptions.unsubscribe(); }
  loadCatalogs() {
    this.catalogLoading = true;
    this.subscriptions.add(this.api.productos().pipe(finalize(()=>this.catalogLoading=false)).subscribe({
      next: products => {
        this.productos=products;
        if(products.length && !this.values.productoId) {
          this.simuladorForm.patchValue({productoId:products[0].productoId,tasaInteres:products[0].tasaInteresReferencial});
        }
      }, error:error=>this.error=errorMessage(error)
    }));
    this.subscriptions.add(this.api.clientes().subscribe({next:clients=>this.mergeClients(clients),error:error=>this.error=errorMessage(error)}));
  }
  private mergeClients(clients: Cliente[]) {
    this.clientes = Array.from(new Map([...this.clientes,...clients].map(c=>[c.clienteId,c])).values());
  }
  searchClients(query: string) { this.search.next(query); }
  selectProduct() {
    const product=this.productos.find(p=>p.productoId===Number(this.values.productoId));
    this.simuladorForm.patchValue({tasaInteres:product?.tasaInteresReferencial ?? null});
  }
  selectClient(index: number) {
    const group=this.participantes.at(index);
    const client=this.clientes.find(c=>c.clienteId===Number(group.value.clienteId));
    group.patchValue({nombres:client?.nombres ?? '',apellidos:client?.apellidos ?? '',
      numeroDocumento:client?.numeroDocumento ?? '',ingresoMensualNeto:client?.ingresoMensualNeto ?? null});
  }
  get participantes() { return this.simuladorForm.get('participantes') as FormArray; }
  get inmuebles() { return this.simuladorForm.get('inmuebles') as FormArray; }
  get values() { return this.simuladorForm.getRawValue(); }
  get principal() { return Number(this.values.valorInmueble) - Number(this.values.cuotaInicial); }
  get validFinance() {
    return ['valorInmueble', 'cuotaInicial', 'plazoMeses', 'tasaInteres'].every(key => this.simuladorForm.get(key)?.valid)
      && this.principal > 0 && this.principal <= Number(this.values.valorInmueble);
  }
  get payment() { return this.validFinance ? monthlyEstimate(this.principal, Number(this.values.tasaInteres), Number(this.values.plazoMeses)) : null; }
  get ltv() { return this.validFinance ? this.principal / Number(this.values.valorInmueble) * 100 : null; }
  get income() { return this.participantes.controls.reduce((sum, control) => sum + Number(control.get('ingresoMensualNeto')?.value || 0), 0); }
  get dsti() { return this.income > 0 && this.payment !== null ? this.payment / this.income * 100 : null; }
  get interest() { return this.payment !== null ? Math.max(0, this.payment * Number(this.values.plazoMeses) - this.principal) : null; }
  get initialPercent() { return this.validFinance ? Number(this.values.cuotaInicial) / Number(this.values.valorInmueble) * 100 : 0; }
  get guaranteeTotal() {
    return this.inmuebles.controls.reduce((sum, c) => sum + Math.min(Number(c.value.valorComercial || 0), c.value.valorTasacion == null ? Infinity : Number(c.value.valorTasacion)), 0);
  }
  get duplicateClients() {
    const ids = this.participantes.controls.map(c => c.value.clienteId).filter(Boolean);
    return new Set(ids).size !== ids.length;
  }
  addParticipante() {
    if(this.participantes.length>=5)return;
    this.participantes.push(this.fb.group({
      clienteId: [null, [Validators.required, Validators.min(1), Validators.pattern(/^\d+$/)]],
      nombres: ['', [Validators.required, Validators.maxLength(50), Validators.pattern(/\S/)]],
      apellidos: ['', [Validators.required, Validators.maxLength(50), Validators.pattern(/\S/)]],
      numeroDocumento: ['', [Validators.required, Validators.maxLength(15)]],
      ingresoMensualNeto: [null, [Validators.required, Validators.min(0.01), Validators.max(999999999)]]
    }));
  }
  removeParticipante(i: number) { if (this.participantes.length > 1) this.participantes.removeAt(i); }
  addInmueble() {
    if(this.inmuebles.length>=5)return;
    this.inmuebles.push(this.fb.group({
      tipoInmueble: ['DEPARTAMENTO', Validators.required],
      direccion: ['', [Validators.required, Validators.maxLength(150), Validators.pattern(/\S/)]],
      partidaRegistral: ['', Validators.maxLength(30)],
      valorComercial: [this.inmuebles.length ? null : 350000, [Validators.required, Validators.min(1), Validators.max(999999999)]],
      valorTasacion: [null, [Validators.min(1), Validators.max(999999999)]]
    }));
  }
  removeInmueble(i: number) { if (this.inmuebles.length > 1) this.inmuebles.removeAt(i); }
  invalid(group: FormGroup | import('@angular/forms').AbstractControl, name: string) {
    const c = group.get(name); return !!c && c.invalid && (c.touched || this.submitted);
  }
  onSubmit() {
    if (this.busy || this.saved) return;
    this.submitted = true;
    this.simuladorForm.markAllAsTouched();
    if (this.simuladorForm.invalid || !this.validFinance || this.duplicateClients) {
      this.error = 'Revisa los campos señalados. Completa los participantes y las garantías antes de registrar.';
      return;
    }
    const value = this.values;
    const solicitud: RegistroSolicitud = {
      productoId: Number(value.productoId),
      montoSolicitado: Number(this.principal.toFixed(2)), plazoMeses: Number(value.plazoMeses),
      participantes: value.participantes.map((p: {clienteId:number}, i:number)=>({
        clienteId:Number(p.clienteId),tipoParticipacion:i===0?'TITULAR':'CODEUDOR'
      })),
      inmuebles: value.inmuebles
    };
    this.busy = true; this.error = '';
    this.simuladorForm.disable({ emitEvent: false });
    this.api.registrar(solicitud).pipe(finalize(() => {
      this.busy = false; this.simuladorForm.enable({ emitEvent: false });
    })).subscribe({
      next: response => { this.saved = response; },
      error: error => { this.error = errorMessage(error); }
    });
  }
}
