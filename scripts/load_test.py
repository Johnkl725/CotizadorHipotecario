"""HTTP load test against a running local server, using 60 independent cookie jars.

Set LOAD_EXECUTIVE_PASSWORD and LOAD_APPROVER_PASSWORD in the process environment.
--persist exercises writes and leaves clearly labeled synthetic records in the database.
No credentials or personal data are included in the result artifact.
"""
import argparse
import concurrent.futures
import html.parser
import http.cookiejar
import json
import math
import os
import random
import statistics
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path


class CsrfParser(html.parser.HTMLParser):
    token = None
    def handle_starttag(self, tag, attrs):
        data = dict(attrs)
        if tag == 'input' and data.get('name') == '_csrf':
            self.token = data.get('value')


class Client:
    def __init__(self, base, username, password):
        self.base = base.rstrip('/')
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        page = self.opener.open(self.base + '/login', timeout=20).read().decode()
        parser = CsrfParser(); parser.feed(page)
        if not parser.token:
            raise RuntimeError('Login page has no CSRF token')
        form = urllib.parse.urlencode({'username': username, 'password': password, '_csrf': parser.token}).encode()
        response = self.opener.open(self.base + '/login', form, timeout=30)
        if '/login' in response.url:
            raise RuntimeError('Authentication failed')
        self.session = self.call('/api/session')

    def call(self, path, body=None):
        headers = {'Accept': 'application/json'}
        data = None
        if body is not None:
            data = json.dumps(body).encode()
            headers.update({'Content-Type': 'application/json', self.session['csrfHeader']: self.session['csrfToken']})
        request = urllib.request.Request(self.base + path, data, headers)
        with self.opener.open(request, timeout=20) as response:
            return json.load(response)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--base', default='http://localhost:8081')
    parser.add_argument('--users', type=int, default=60)
    parser.add_argument('--iterations', type=int, default=20)
    parser.add_argument('--persist', action='store_true')
    parser.add_argument('--output', default='artifacts/load-test.json')
    args = parser.parse_args()
    if not 1 <= args.users <= 100 or not 1 <= args.iterations <= 1000:
        parser.error('users 1..100 and iterations 1..1000')
    executive_password = os.environ['LOAD_EXECUTIVE_PASSWORD']
    approver_password = os.environ.get('LOAD_APPROVER_PASSWORD')
    if args.persist and not approver_password:
        parser.error('LOAD_APPROVER_PASSWORD required for --persist')
    # Warm authenticated sessions outside the measured workload; login cost is reported separately.
    login_start = time.perf_counter()
    def login(_):
        return (Client(args.base, 'ejecutivo', executive_password),
                Client(args.base, 'aprobador', approver_password) if args.persist else None)
    with concurrent.futures.ThreadPoolExecutor(max_workers=min(args.users, 6)) as pool:
        clients = list(pool.map(login, range(args.users)))
    login_seconds = time.perf_counter() - login_start
    barrier = threading.Barrier(args.users)
    unique_dnis = random.sample(range(90000000, 98999999), args.users)
    run_id = str(int(time.time()))

    def work(index):
        executive, approver = clients[index]
        measurements = []
        errors = []
        sample = {'valorInmueble': '300000.00', 'cuotaInicial': '60000.00', 'plazoMeses': 240,
                  'ingresosMensuales': '12000.00', 'deudasMensuales': '500.00', 'scoreCrediticio': 850}
        def measured(client, path, body=None):
            started = time.perf_counter()
            try:
                result = client.call(path, body)
                measurements.append((path.split('?')[0], (time.perf_counter()-started)*1000))
                return result
            except Exception as exc:
                errors.append({'path': path.split('?')[0], 'type': type(exc).__name__, 'status': getattr(exc, 'code', None)})
                return None
        barrier.wait(timeout=30)
        for iteration in range(args.iterations):
            result = measured(executive, '/api/simulaciones', sample)
            if result and abs(float(result['cuotaMensual']) - 2105.43) > 0.001:
                errors.append({'path': '/api/simulaciones', 'type': 'UnexpectedPayment'})
            if args.persist and iteration % 5 == 0:
                measured(executive, '/api/cotizaciones?page=0&size=12')
        if args.persist:
            q = measured(executive, '/api/cotizaciones', {**sample, 'dni': str(unique_dnis[index]),
                'nombres': 'Carga ' + run_id, 'apellidos': 'Usuario ' + str(index)})
            if q:
                q = measured(executive, f"/api/cotizaciones/{q['id']}/solicitud", {'teaPreferencial': '8.00', 'version': q['version']})
            if q:
                measured(approver, '/api/aprobaciones?page=0&size=12')
                q = measured(approver, f"/api/aprobaciones/{q['id']}/decision", {
                    'aprobar': True, 'comentario': 'Prueba de carga local ' + run_id, 'version': q['version']})
                if q and (q['estado'] != 'APROBADA' or float(q['cuotaMensualEstimada']) >= 2105.43):
                    errors.append({'path': '/decision', 'type': 'UnexpectedDecision'})
        return measurements, errors

    started = time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.users) as pool:
        results = list(pool.map(work, range(args.users)))
    elapsed = time.perf_counter() - started
    values = sorted(ms for result, _ in results for _, ms in result)
    failures = [error for _, errors in results for error in errors]
    def percentile(p):
        return round(values[max(0, math.ceil(len(values)*p)-1)], 2) if values else None
    report = {'scenario': 'mixed-persistent' if args.persist else 'simulation-only', 'concurrentUsers': args.users,
              'iterationsPerUser': args.iterations, 'successfulRequests': len(values), 'failedRequests': len(failures),
              'durationSeconds': round(elapsed, 3), 'loginSetupSeconds': round(login_seconds, 3),
              'successfulRequestsPerSecond': round(len(values)/elapsed, 2), 'p50Ms': percentile(.5),
              'p95Ms': percentile(.95), 'p99Ms': percentile(.99), 'maxMs': round(max(values),2) if values else None,
              'errors': failures[:20], 'timestampUtc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
              'notes': 'Local loopback; authenticated sessions warmed before measurement; no think time. Not a production SLA.'}
    output = Path(args.output); output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps(report, indent=2))
    return 1 if failures else 0


if __name__ == '__main__':
    raise SystemExit(main())
