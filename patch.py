import sys

file_path = r'C:\Users\John\IdeaProjects\NewCotizador\src\main\java\com\example\newcotizador\application\service\CotizacionService.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace in crear
old_crear = '''    @Transactional(timeout = 10)
    public CotizacionResponse crear(CrearCotizacionRequest request, String username) {
        SimulacionResponse resultado = calculo.simular(request.simulacion());
        Cliente cliente = clientes.findByDni(request.dni()).orElseGet(() -> {'''

new_crear = '''    @Transactional(timeout = 10)
    public CotizacionResponse crear(CrearCotizacionRequest request, String username) {
        Cliente cliente = clientes.findByDni(request.dni()).orElseGet(() -> {'''
content = content.replace(old_crear, new_crear)

old_crear2 = '''        Cotizacion c = new Cotizacion();
        c.setCliente(cliente); c.setEjecutivo(usuario(username));
        c.setValorInmueble(request.valorInmueble()); c.setCuotaInicial(request.cuotaInicial()); c.setPlazoMeses(request.plazoMeses());
        c.setMontoPrestamo(resultado.montoPrestamo()); c.setLtvPorcentaje(resultado.ltvPorcentaje());
        c.setTeaCalculada(resultado.tea()); c.setCuotaMensualEstimada(resultado.cuotaMensual());
        c.setIngresosMensuales(request.ingresosMensuales()); c.setDeudasMensuales(request.deudasMensuales());
        c.setScoreCrediticio(request.scoreCrediticio()); c.setDstiPorcentaje(resultado.dstiPorcentaje());
        c.setEstado(EstadoCotizacion.BORRADOR); c.setFechaCreacion(LocalDateTime.now(ZoneOffset.UTC));'''

new_crear2 = '''        Cotizacion c = new Cotizacion();
        c.setCliente(cliente); c.setEjecutivo(usuario(username));
        c.setValorInmueble(request.valorInmueble()); c.setCuotaInicial(request.cuotaInicial()); c.setPlazoMeses(request.plazoMeses());
        c.setIngresosMensuales(request.ingresosMensuales()); c.setDeudasMensuales(request.deudasMensuales());
        c.setScoreCrediticio(request.scoreCrediticio()); 

        PoliticaRiesgo pr = new PoliticaRiesgo(politica.teaBase(), politica.cuotaInicialMinimaPorcentaje(), politica.scoreMinimo(), politica.dstiMaximo(), politica.plazoMaximoMeses());
        c.simular(pr);

        c.setEstado(EstadoCotizacion.BORRADOR); c.setFechaCreacion(LocalDateTime.now(ZoneOffset.UTC));'''
content = content.replace(old_crear2, new_crear2)

old_decidir = '''        if (request.aprobar()) {
            SimulacionResponse resultado = calculo.simularConTea(new SimulacionRequest(c.getValorInmueble(), c.getCuotaInicial(),
                c.getPlazoMeses(), c.getIngresosMensuales(), c.getDeudasMensuales(), c.getScoreCrediticio()), c.getTeaPreferencialSolicitada());
            c.setCuotaMensualEstimada(resultado.cuotaMensual()); c.setDstiPorcentaje(resultado.dstiPorcentaje());
            c.setEstado(EstadoCotizacion.APROBADA);
            guardarAuditoria(c, "TASA_PREFERENCIAL_APROBADA", c.getTeaPreferencialSolicitada(), username); // HU 3
        }'''

new_decidir = '''        if (request.aprobar()) {
            PoliticaRiesgo pr = new PoliticaRiesgo(politica.teaBase(), politica.cuotaInicialMinimaPorcentaje(), politica.scoreMinimo(), politica.dstiMaximo(), politica.plazoMaximoMeses());
            c.simularConTea(pr, c.getTeaPreferencialSolicitada());
            c.setEstado(EstadoCotizacion.APROBADA);
            guardarAuditoria(c, "TASA_PREFERENCIAL_APROBADA", c.getTeaPreferencialSolicitada(), username); // HU 3
        }'''
content = content.replace(old_decidir, new_decidir)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print('Done!')
