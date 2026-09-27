'use strict';
(() => {
  const $ = id => document.getElementById(id);
  const isExecutive = document.body.dataset.page === 'simulador';
  const money = value => value == null ? '—' : new Intl.NumberFormat('es-PE', {style:'currency', currency:'PEN', maximumFractionDigits:2}).format(value);
  const percent = value => value == null ? '—' : new Intl.NumberFormat('es-PE', {maximumFractionDigits:4}).format(value) + ' %';
  let session, policy, snapshot, page = 0, totalPages = 0, listRequest = 0, simulationRequest = 0;
  const stateNames = {BORRADOR:'Borrador', PENDIENTE_APROBACION:'Pendiente de aprobación', APROBADA:'Aprobada', RECHAZADA:'Rechazada'};
  function element(tag, className, text) { const node = document.createElement(tag); if (className) node.className = className; if (text != null) node.textContent = text; return node; }
  function message(text, error = false) { const area = $('global-message'); area.replaceChildren(); if(text) area.append(element('p', 'notice' + (error ? ' error' : ''), text)); }
  async function api(path, data) {
    const headers = {Accept:'application/json'};
    if(data !== undefined) { headers['Content-Type'] = 'application/json'; headers[session.csrfHeader] = session.csrfToken; }
    const response = await fetch(path, {method:data === undefined ? 'GET' : 'POST', headers, credentials:'same-origin', ...(data === undefined ? {} : {body:JSON.stringify(data)})});
    if (response.status === 401 || response.redirected && new URL(response.url).pathname === '/login') { window.location.assign('/login'); throw new Error('Tu sesión terminó. Inicia sesión nuevamente.'); }
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) { const fields = payload.fieldErrors ? Object.values(payload.fieldErrors).join(' · ') : ''; throw new Error([payload.message || 'No se pudo completar la operación. Inténtalo nuevamente.', fields].filter(Boolean).join(' ')); }
    return payload;
  }
  function formatDate(value) { if(!value) return ''; const normalized = /(?:Z|[+-]\d{2}:?\d{2})$/i.test(value) ? value : value + 'Z'; const date = new Date(normalized); return Number.isNaN(date.getTime()) ? '' : new Intl.DateTimeFormat('es-PE', {day:'2-digit',month:'short',year:'numeric',timeZone:'America/Lima'}).format(date); }
  async function busy(button, task) { const previous = button.textContent; button.disabled = true; button.textContent = 'Procesando…'; try { await task(); } catch(error) { message(error.message || 'No se pudo conectar con el servidor.', true); } finally { button.disabled = false; button.textContent = previous; } }
  function metric(label, value, risky = false) { const node = element('div', 'metric' + (risky ? ' risky' : '')); node.append(element('small', '', label), element('strong', '', value)); return node; }
  function renderQuote(q) {
    const card = element('article', 'quote-item');
    const header = element('div', 'quote-top');
    const identity = element('div'); identity.append(element('h3', '', `${q.nombres} ${q.apellidos}`), element('p', 'fine', `DNI ${q.dni} · Propuesta #${q.id} · ${formatDate(q.fechaCreacion)}`));
    const badge = element('span', 'badge ' + ({PENDIENTE_APROBACION:'pending', APROBADA:'approved', RECHAZADA:'rejected'}[q.estado] || ''), stateNames[q.estado] || q.estado);
    header.append(identity, badge); card.append(header);
    const metrics = element('div', 'metrics');
    const effectiveTea = q.estado === 'APROBADA' ? q.teaPreferencialSolicitada : q.teaCalculada;
    metrics.append(metric('Financiamiento', money(q.montoPrestamo)), metric('Cuota mensual', money(q.cuotaMensualEstimada)), metric('TEA vigente', percent(effectiveTea)), metric(q.estado === 'APROBADA' ? 'TEA original' : 'TEA solicitada', percent(q.estado === 'APROBADA' ? q.teaCalculada : q.teaPreferencialSolicitada)), metric('LTV', percent(q.ltvPorcentaje), Number(q.ltvPorcentaje) > 90), metric('DSTI', percent(q.dstiPorcentaje), policy && Number(q.dstiPorcentaje) > Number(policy.dstiMaximo)), metric('Score crediticio', q.scoreCrediticio == null ? 'No informado' : q.scoreCrediticio, policy && q.scoreCrediticio != null && Number(q.scoreCrediticio) < Number(policy.scoreMinimo)), metric('Plazo', `${q.plazoMeses} meses`));
    card.append(metrics);
    if (!isExecutive) { const details = element('div', 'metrics'); details.append(metric('Ingreso mensual', money(q.ingresosMensuales)), metric('Otras cuotas', money(q.deudasMensuales)), metric('Valor del inmueble', money(q.valorInmueble)), metric('Cuota inicial', money(q.cuotaInicial))); card.append(details); card.append(element('p', 'quote-note', `Ejecutivo: ${q.ejecutivo || '—'}`)); }
    if(q.motivos?.length) card.append(element('p', 'quote-note', q.motivos.join(' · ')));
    if(q.comentarioDecision) card.append(element('p', 'quote-note', `Decisión de ${q.aprobador || 'aprobador'}: ${q.comentarioDecision}`));
    if(isExecutive && (q.estado === 'BORRADOR' || q.estado === 'RECHAZADA')) {
      const cloneBtn = element('button', 'button', 'Clonar para ajustar ↗');
      cloneBtn.style.marginTop = '0.5rem'; cloneBtn.style.marginBottom = '0.5rem';
      cloneBtn.type = 'button';
      cloneBtn.addEventListener('click', () => {
        busy(cloneBtn, async () => {
          const clone = await api(`/api/cotizaciones/${q.id}/clonar`, {});
          message(`Propuesta clonada exitosamente (Borrador #${clone.id}).`);
          page = 0;
          await loadQuotes();
        });
      });
      card.append(cloneBtn);
    }
    if(isExecutive && q.estado === 'BORRADOR') {
      if(q.elegiblePreferencial) {
        const form = element('form', 'quote-actions'); const label = element('label', '', 'TEA preferencial solicitada (%)'); const input = element('input'); input.type='number'; input.min='0'; input.max=String(q.teaCalculada); input.step='0.01'; input.required=true; input.placeholder='Ingresa una tasa menor a la vigente'; label.append(input); const button = element('button', 'button secondary', 'Solicitar tasa preferencial →'); button.type='submit'; form.append(label,button);
        form.addEventListener('submit', event => {event.preventDefault(); if(!form.reportValidity()) return; busy(button, async () => { await api(`/api/cotizaciones/${q.id}/solicitud`, {teaPreferencial:input.value, version:q.version}); message(`La propuesta #${q.id} se envió a aprobación.`); await loadQuotes(); }); }); card.append(form);
      } else card.append(element('p','quote-note','Este perfil no cumple la política para solicitar una tasa preferencial.'));
    }
    if(!isExecutive && q.estado === 'PENDIENTE_APROBACION') {
      const form = element('form','decision-form'); const label = element('label','','Fundamento de la decisión'); const comment = element('textarea'); comment.required=true; comment.maxLength=500; comment.placeholder='Describe el sustento de tu evaluación'; label.append(comment); const actions = element('div','decision-buttons');
      const approve = element('button','button primary','Aprobar tasa ↗'); const reject = element('button','button danger','Rechazar solicitud'); approve.type='submit'; approve.value='true'; reject.type='submit'; reject.value='false'; actions.append(approve,reject); form.append(label,actions);
      form.addEventListener('submit', event => { event.preventDefault(); if(!event.submitter || !form.reportValidity()) return; const approved = event.submitter.value === 'true'; const clicked = event.submitter; approve.disabled=true; reject.disabled=true; busy(clicked, async () => { await api(`/api/aprobaciones/${q.id}/decision`, {aprobar:approved, comentario:comment.value.trim(), version:q.version}); message(`Propuesta #${q.id} ${approved ? 'aprobada' : 'rechazada'} correctamente.`); await loadQuotes(); }).finally(() => {approve.disabled=false; reject.disabled=false;}); }); card.append(form);
    }
    return card;
  }
  async function loadQuotes() {
    const request = ++listRequest; $('previous-page').disabled=true; $('next-page').disabled=true; $('quote-list').replaceChildren(element('div','empty-state','Cargando propuestas…'));
    const endpoint = isExecutive ? '/api/cotizaciones' : '/api/aprobaciones'; const filter = !isExecutive ? '&estado=' + encodeURIComponent($('status-filter').value) : '';
    try {
      const result = await api(`${endpoint}?page=${page}&size=12${filter}`); if(request !== listRequest) return;
      totalPages = result.totalPages; if(page > 0 && page >= totalPages) {page=Math.max(0,totalPages-1); return loadQuotes();}
      const list = $('quote-list'); list.replaceChildren();
      if(result.content.length) result.content.forEach(q => list.append(renderQuote(q)));
      else {const empty = element('div','empty-state'); empty.append(element('strong','',isExecutive ? 'Tu próxima propuesta empieza aquí.' : 'Todo al día en esta bandeja.'),element('p','',isExecutive ? 'Calcula y guarda una cotización para darle seguimiento.' : 'No hay solicitudes en el estado seleccionado.')); list.append(empty);}
      $('page-label').textContent=totalPages ? `${page+1} de ${totalPages} · ${result.totalElements} propuestas` : 'Sin propuestas';
      if($('total-quotes')) $('total-quotes').textContent=result.totalElements;
      $('previous-page').disabled=page===0; $('next-page').disabled=page+1>=totalPages;
    } catch(error) { if(request !== listRequest) return; $('quote-list').replaceChildren(element('div','empty-state','No se pudo cargar la bandeja. Usa Actualizar para volver a intentar.')); $('page-label').textContent=''; message(error.message, true); }
  }
  function readSimulation() { const values = Object.fromEntries(new FormData($('simulation-form')).entries()); values.plazoMeses = Number(values.plazoMeses); values.scoreCrediticio = values.scoreCrediticio === '' ? null : Number(values.scoreCrediticio); return values; }
  function invalidateSimulation() {simulationRequest++; snapshot=null; $('save-button').disabled=true; $('eligibility').textContent='Los datos cambiaron. Vuelve a calcular para actualizar la propuesta.'; ['cuota','prestamo','tea','tem','intereses','ltv','dsti'].forEach(key=>$('result-'+key).textContent='—'); $('risk-reasons').replaceChildren(); ['ltv','dsti'].forEach(key=>$(key+'-bar').style.width='0%');}
  function bindSimulation() {
    const form = $('simulation-form'); form.addEventListener('input', invalidateSimulation); form.addEventListener('change', invalidateSimulation);
    form.addEventListener('submit', event => {event.preventDefault(); if(!form.reportValidity()) return; const submitted = readSimulation(); const request=++simulationRequest; snapshot=null; $('save-button').disabled=true; busy($('simulate-button'), async () => {const result=await api('/api/simulaciones',submitted); if(request!==simulationRequest) return; snapshot=submitted; $('save-button').disabled=false; message('Propuesta calculada. Puedes guardarla con los datos del cliente.'); $('result-cuota').textContent=money(result.cuotaMensual); $('result-prestamo').textContent=money(result.montoPrestamo); $('result-tea').textContent=percent(result.tea); $('result-tem').textContent=percent(result.tem); $('result-intereses').textContent=money(result.totalIntereses); $('result-ltv').textContent=percent(result.ltvPorcentaje); $('result-dsti').textContent=percent(result.dstiPorcentaje); [['ltv',result.ltvPorcentaje,90],['dsti',result.dstiPorcentaje,Number(policy.dstiMaximo)]].forEach(([key,value,limit])=>{ $(key+'-bar').style.width=Math.max(0,Math.min(100,Number(value)))+'%'; $(key+'-bar').classList.toggle('risky',Number(value)>limit); }); $('eligibility').textContent=result.elegiblePreferencial ? 'Este perfil es elegible para solicitar una tasa preferencial. Sujeto a evaluación del aprobador.' : 'Este perfil no cumple la política para solicitar una tasa preferencial.'; $('risk-reasons').replaceChildren(...(result.motivos || []).map(reason=>element('li','',reason))); }); });
    $('save-form').addEventListener('submit', event => {event.preventDefault(); if(!snapshot || !$('save-form').reportValidity()) return; const payload={...snapshot,...Object.fromEntries(new FormData($('save-form')).entries())}; busy($('save-button'),async()=>{ const quote=await api('/api/cotizaciones',payload); snapshot=null; message(`Propuesta #${quote.id} guardada correctamente.`); page=0; await loadQuotes(); }).finally(()=>{$('save-button').disabled=!snapshot;}); });
  }
  async function init() {
    $('refresh-button').addEventListener('click',loadQuotes); $('previous-page').addEventListener('click',()=>{if(page>0){page--;loadQuotes();}}); $('next-page').addEventListener('click',()=>{if(page+1<totalPages){page++;loadQuotes();}}); $('status-filter')?.addEventListener('change',()=>{page=0;loadQuotes();});
    if(isExecutive) {bindSimulation(); $('simulate-button').disabled=true;}
    try { [session,policy] = await Promise.all([api('/api/session'),api('/api/politica')]); $('username').textContent=session.username; $('policy-note').textContent=`Política configurable de demostración · TEA base ${percent(policy.teaBase)} · Inicial mínima ${percent(policy.cuotaInicialMinimaPorcentaje)} · Score ≥ ${policy.scoreMinimo} · DSTI ≤ ${percent(policy.dstiMaximo)}`; if(isExecutive) { const select=$('simulation-form').elements.plazoMeses; [...select.options].filter(option=>Number(option.value)>Number(policy.plazoMaximoMeses)).forEach(option=>option.remove()); if(!select.value && select.options.length) select.selectedIndex=select.options.length-1; $('simulate-button').disabled=false; } await loadQuotes(); }
    catch(error) {message(error.message || 'No se pudo inicializar el espacio de trabajo. Recarga la página.',true); $('policy-note').textContent='No se pudo cargar la política. Recarga la página para continuar.';}
  }
  init();
})();

