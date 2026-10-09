"""Real SQL-backed role workflow. Creates two clearly labelled browser-test requests."""
import json
import os
import re
from pathlib import Path
from datetime import datetime
from playwright.sync_api import sync_playwright, expect

BASE = os.environ.get('COTIZADOR_TEST_URL','http://127.0.0.1:8081').rstrip('/')
OUT = Path(__file__).resolve().parents[1] / 'artifacts' / 'roles-review'
OUT.mkdir(parents=True,exist_ok=True)
PASSWORDS = {name:os.environ['COTIZADOR_TEST_'+name.upper()+'_PASSWORD'] for name in ['ejecutivo','aprobador']}
run = datetime.now().strftime('%Y%m%d-%H%M%S')
with sync_playwright() as p:
    browser=p.chromium.launch(channel='chrome',headless=True)
    executive_context=browser.new_context(viewport={'width':1440,'height':1000})
    manager_context=browser.new_context(viewport={'width':1440,'height':1000})
    executive=executive_context.new_page()
    manager=manager_context.new_page()
    errors=[]
    for page in [executive,manager]:
        page.on('pageerror',lambda e:errors.append(str(e)))
    def login(page,name):
        page.goto(BASE+'/login',wait_until='networkidle')
        expect(page.locator('#username')).to_be_visible()
        page.locator('#username').fill(name)
        page.locator('#password').fill(PASSWORDS[name])
        page.get_by_role('button',name='Ingresar a mi espacio').click()
        expect(page.get_by_role('button',name='Cerrar sesión')).to_be_visible()
    executive.goto(BASE+'/login',wait_until='networkidle')
    assert executive.locator('.login-box .primary').evaluate("e=>getComputedStyle(e).backgroundColor") == 'rgb(33, 90, 77)', 'Global stylesheet was not applied'
    executive.screenshot(path=str(OUT/'login-desktop.png'),full_page=True)
    login(executive,'ejecutivo')
    expect(executive.locator('nav')).to_contain_text('Mis solicitudes')
    expect(executive.locator('nav')).not_to_contain_text('Bandeja de riesgos')
    login(manager,'aprobador')
    expect(manager.locator('nav')).to_contain_text('Bandeja de riesgos')
    expect(manager.locator('nav a[href="/simulador"]')).to_have_count(0)
    created=[]
    for decision in ['aprobar','rechazar']:
        executive.goto(BASE+'/simulador',wait_until='networkidle')
        expect(executive.locator('#annual-rate')).not_to_have_value('')
        executive.locator('#property-value').fill('150000')
        executive.locator('#down-payment').fill('50000')
        # The synthetic customer is explicitly seeded by the local bootstrap.
        option=executive.locator('#client-0 option').filter(has_text='70000001')
        expect(option).to_have_count(1)
        executive.locator('#client-0').select_option(value=option.get_attribute('value'))
        expect(executive.locator('#income-0')).not_to_have_value('')
        executive.locator('#commercial-0').fill('150000')
        executive.locator('#address-0').fill('Prueba UI roles '+run+' '+decision)
        if decision=='aprobar':
            executive.evaluate('scrollTo(0,0)')
            executive.screenshot(path=str(OUT/'ejecutivo-desktop.png'),full_page=True)
        with executive.expect_response(lambda r:'/api/solicitudes/registrar' in r.url and r.request.method=='POST') as response:
            executive.get_by_role('button',name='Registrar solicitud',exact=True).click()
        assert response.value.status==201, (response.value.status,response.value.text())
        request=response.value.json()
        created.append({'id':request['solicitudId'],'expediente':request['numeroExpediente'],'decision':decision})
        expect(executive.get_by_role('status')).to_contain_text('registrada correctamente')
        # Refresh proves the portfolio is read from SQL, not in-memory fixtures.
        executive.goto(BASE+'/aprobaciones',wait_until='networkidle')
        executive.reload(wait_until='networkidle')
        executive.get_by_role('searchbox').fill(request['numeroExpediente'])
        expect(executive.locator('tbody tr')).to_have_count(1)
        executive.get_by_role('button',name='Ver detalle').click()
        expect(executive.locator('.detail-panel')).to_be_visible()
        expect(executive.get_by_role('button',name='Iniciar evaluación',exact=True)).to_have_count(0)
        expect(executive.get_by_role('button',name='Confirmar aprobación',exact=True)).to_have_count(0)
        manager.goto(BASE+'/aprobaciones',wait_until='networkidle')
        manager.get_by_role('searchbox').fill(request['numeroExpediente'])
        expect(manager.locator('tbody tr')).to_have_count(1)
        manager.get_by_role('button',name='Ver detalle').click()
        manager.get_by_role('button',name='Iniciar evaluación',exact=True).click()
        expect(manager.locator('#decision-comment')).to_be_visible()
        manager.locator('#decision-comment').fill('Prueba UI '+run+': '+('Ingresos & respaldo verificados' if decision=='aprobar' else 'Documentación insuficiente'))
        manager.get_by_role('button',name='Confirmar aprobación' if decision=='aprobar' else 'Confirmar rechazo',exact=True).click()
        expect(manager.get_by_role('status')).to_contain_text('Solicitud aprobada' if decision=='aprobar' else 'Solicitud rechazada')
        expect(manager.locator('.audit-history li')).to_have_count(3)
        if decision=='aprobar':
            manager.evaluate('scrollTo(0,0)')
            manager.screenshot(path=str(OUT/'gestor-desktop.png'),full_page=True)
        executive.reload(wait_until='networkidle')
        executive.get_by_role('searchbox').fill(request['numeroExpediente'])
        expect(executive.locator('tbody tr')).to_have_count(1)
        expect(executive.locator('tbody')).to_contain_text('Aprobada' if decision=='aprobar' else 'Rechazada')
    # HTTP authorization independently of hidden navigation.
    csrf=executive_context.request.get(BASE+'/api/csrf').json()
    blocked=executive_context.request.post(BASE+'/api/solicitudes/'+str(created[0]['id'])+'/aprobar',
        headers={csrf['headerName']:csrf['token']},data={'version':2,'comentario':'Forbidden role test'})
    assert blocked.status==403
    assert manager_context.request.get(BASE+'/api/clientes').status==403
    manager.goto(BASE+'/simulador',wait_until='networkidle')
    expect(manager).to_have_url(re.compile('/aprobaciones$'))
    for width in [1440,1024,768,390,320]:
        for page,path,filename in [(executive,'/simulador','ejecutivo'),(manager,'/aprobaciones','gestor')]:
            page.set_viewport_size({'width':width,'height':844})
            page.goto(BASE+path,wait_until='networkidle')
            assert not page.evaluate('document.documentElement.scrollWidth>innerWidth'),(width,path)
            if width==390:page.screenshot(path=str(OUT/(filename+'-mobile.png')),full_page=True)
    executive.get_by_role('button',name='Cerrar sesión').click()
    expect(executive.locator('#username')).to_be_visible()
    assert executive_context.request.get(BASE+'/api/session').status==401
    executive.reload(wait_until='networkidle')
    expect(executive.locator('#username')).to_be_visible()
    # The manager's independent session stays active.
    assert manager_context.request.get(BASE+'/api/session').status==200
    assert not errors,errors
    report={'status':'passed','created_requests':created,'roles':['EJECUTIVO_COMERCIAL','GESTOR_RIESGOS'],
        'widths':[1440,1024,768,390,320],'browser_errors':errors,'database':'SQL Server','logout_isolation':True}
    (OUT/'report.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print(json.dumps(report))
    browser.close()
