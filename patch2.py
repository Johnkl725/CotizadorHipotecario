import sys

file_path = r'C:\Users\John\IdeaProjects\NewCotizador\src\main\java\com\example\newcotizador\application\service\CotizacionService.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('    private final CalculoService calculo;\n', '')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print('Done!')
