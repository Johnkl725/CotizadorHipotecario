import os

replacements = {
    'package com.example.newcotizador.controller': 'package com.example.newcotizador.infrastructure.adapter.in.web',
    'package com.example.newcotizador.service': 'package com.example.newcotizador.application.service',
    'package com.example.newcotizador.repository': 'package com.example.newcotizador.infrastructure.adapter.out.persistence',
    'package com.example.newcotizador.entity': 'package com.example.newcotizador.domain.model',
    'import com.example.newcotizador.entity': 'import com.example.newcotizador.domain.model',
    'import com.example.newcotizador.repository': 'import com.example.newcotizador.infrastructure.adapter.out.persistence',
    'import com.example.newcotizador.service': 'import com.example.newcotizador.application.service'
}

for root, _, files in os.walk('src/main/java'):
    for file in files:
        if file.endswith('.java'):
            path = os.path.join(root, file)
            with open(path, 'r', encoding='utf-8') as f:
                content = f.read()
            for old, new in replacements.items():
                content = content.replace(old, new)
            with open(path, 'w', encoding='utf-8') as f:
                f.write(content)

for root, _, files in os.walk('src/test/java'):
    for file in files:
        if file.endswith('.java'):
            path = os.path.join(root, file)
            with open(path, 'r', encoding='utf-8') as f:
                content = f.read()
            for old, new in replacements.items():
                content = content.replace(old, new)
            with open(path, 'w', encoding='utf-8') as f:
                f.write(content)

print('Done fixing imports.')
