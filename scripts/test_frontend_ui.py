"""Read-only public frontend smoke test; never creates requests or submits credentials."""
import os
from pathlib import Path
from playwright.sync_api import sync_playwright, expect
BASE=os.environ.get('COTIZADOR_TEST_URL','http://127.0.0.1:8081').rstrip('/')
OUT=Path(__file__).resolve().parents[1]/'artifacts'/'roles-review'
OUT.mkdir(parents=True,exist_ok=True)
with sync_playwright() as p:
    browser=p.chromium.launch(channel='chrome',headless=True)
    page=browser.new_page()
    errors=[]
    page.on('pageerror',lambda e:errors.append(str(e)))
    for width in [1440,1024,768,390,320]:
        page.set_viewport_size({'width':width,'height':844})
        page.goto(BASE+'/login',wait_until='networkidle')
        expect(page.get_by_role('heading',name='Bienvenido a tu espacio')).to_be_visible()
        assert page.locator('.login-box .primary').evaluate('e=>getComputedStyle(e).backgroundColor')=='rgb(33, 90, 77)'
        assert not page.evaluate('document.documentElement.scrollWidth>innerWidth')
        page.get_by_role('button',name='Ejecutivo comercial Simula y gestiona tu cartera').click()
        expect(page.locator('#username')).to_have_value('ejecutivo')
        page.get_by_role('button',name='Gestor de riesgos Evalúa y decide solicitudes').click()
        expect(page.locator('#username')).to_have_value('aprobador')
        expect(page.locator('nav')).to_have_count(0)
        expect(page.get_by_role('button',name='Ingresar a mi espacio')).to_be_disabled()
        if width==390:page.screenshot(path=str(OUT/'login-mobile.png'),full_page=True)
    page.goto(BASE+'/simulador',wait_until='networkidle')
    expect(page).to_have_url(BASE+'/login')
    assert not errors,errors
    print('Public UI passed at 1440, 1024, 768, 390 and 320 px; no writes.')
    browser.close()
