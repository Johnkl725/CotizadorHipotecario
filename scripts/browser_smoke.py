"""Browser acceptance against local SQL Server; creates one synthetic quotation.
Requires Python playwright and installed Chromium. Credentials come from environment.
"""
import json
import os
import secrets
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

BASE = os.environ.get('TEST_BASE_URL', 'http://localhost:8081')
output = Path('artifacts'); output.mkdir(exist_ok=True)
errors = []
with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    executive = browser.new_context(viewport={'width': 1440, 'height': 1080})
    page = executive.new_page()
    page.on('pageerror', lambda err: errors.append(str(err)))
    page.on('console', lambda msg: errors.append(msg.text) if msg.type == 'error' else None)
    def login(tab, username, password):
        tab.goto(BASE + '/login')
        tab.locator('[name=username]').fill(username)
        tab.locator('[name=password]').fill(password)
        tab.get_by_role('button', name='Ingresar al espacio').click()
        tab.wait_for_url('**/' + ('simulador' if username == 'ejecutivo' else 'aprobaciones'))
        expect(tab.locator('#username')).to_have_text(username)
    page.goto(BASE + '/login')
    page.screenshot(path=str(output/'login-desktop.png'), full_page=True)
    login(page, 'ejecutivo', os.environ['LOAD_EXECUTIVE_PASSWORD'])
    for field, value in {'valorInmueble':'300000','cuotaInicial':'60000','ingresosMensuales':'12000',
                         'deudasMensuales':'500','scoreCrediticio':'850'}.items():
        page.locator(f'[name={field}]').fill(value)
    page.locator('#simulate-button').click()
    expect(page.locator('#save-button')).to_be_enabled()
    expect(page.locator('#result-cuota')).to_contain_text('2,105.43')
    page.screenshot(path=str(output/'simulador-desktop.png'), full_page=True)
    dni = str(90000000 + secrets.randbelow(9000000))
    page.locator('[name=dni]').fill(dni)
    page.locator('[name=nombres]').fill('Prueba Navegador')
    page.locator('[name=apellidos]').fill('Umbral')
    page.locator('#save-button').click()
    card = page.locator('.quote-item').filter(has_text=dni)
    expect(card).to_be_visible()
    card.locator('input[type=number]').fill('8.00')
    card.get_by_role('button', name='Solicitar tasa preferencial').click()
    expect(card.locator('.badge')).to_have_text('Pendiente de aprobación')
    approval_context = browser.new_context(viewport={'width':1440, 'height':1080})
    approval = approval_context.new_page()
    approval.on('pageerror', lambda err: errors.append(str(err)))
    approval.on('console', lambda msg: errors.append(msg.text) if msg.type == 'error' else None)
    login(approval, 'aprobador', os.environ['LOAD_APPROVER_PASSWORD'])
    pending = approval.locator('.quote-item').filter(has_text=dni)
    expect(pending).to_be_visible()
    approval.screenshot(path=str(output/'aprobaciones-desktop.png'), full_page=True)
    pending.locator('textarea').fill('Verificación de flujo en navegador: perfil revisado.')
    pending.get_by_role('button', name='Aprobar tasa').click()
    expect(approval.locator('#global-message')).to_contain_text('aprobada correctamente')
    approval.locator('#status-filter').select_option('APROBADA')
    approved = approval.locator('.quote-item').filter(has_text=dni)
    expect(approved.locator('.badge')).to_have_text('Aprobada')
    page.locator('#refresh-button').click()
    # Other sessions may have created newer quotes; follow real pagination.
    for _ in range(100):
        page.wait_for_function("!document.querySelector('#quote-list').textContent.includes('Cargando propuestas')")
        if card.count():
            break
        if page.locator('#next-page').is_disabled():
            break
        page.locator('#next-page').click()
    expect(card.locator('.badge')).to_have_text('Aprobada')
    page.set_viewport_size({'width':390, 'height':844})
    page.screenshot(path=str(output/'simulador-mobile.png'), full_page=True)
    assert page.evaluate('document.documentElement.scrollWidth <= innerWidth'), 'Mobile horizontal overflow'
    # Editing input must invalidate previous results and prevent saving stale calculations.
    page.locator('[name=valorInmueble]').fill('400000')
    expect(page.locator('#save-button')).to_be_disabled()
    page.get_by_role('button', name='Cerrar sesión').click()
    page.wait_for_url('**/login?logout')
    assert not errors, errors
    (output/'browser-smoke.json').write_text(json.dumps({'passed':True, 'checks':['login','simulation','save',
        'request-rate','approval','executive-status','mobile-no-overflow','invalidate-stale','logout'],
        'consoleErrors':errors}, indent=2), encoding='utf-8')
    browser.close()
print('Browser smoke passed: executive + approver workflow, mobile layout, no console errors.')
