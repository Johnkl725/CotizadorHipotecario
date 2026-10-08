import os
import re

entity_dir = r"C:\Users\John\IdeaProjects\NewCotizador\src\main\java\com\example\newcotizador\infrastructure\adapter\out\persistence\entity"

def fix_entity(file_name):
    path = os.path.join(entity_dir, file_name)
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    
    content = re.sub(r"\bCliente\b(?!\.class)", "ClienteJpaEntity", content)
    content = re.sub(r"\bUsuario\b(?!\.class)", "UsuarioJpaEntity", content)
    content = re.sub(r"\bCotizacion\b(?!\.class)", "CotizacionJpaEntity", content)
    # Undo renaming the enum, because it doesn't have an entity equivalent
    content = content.replace("EstadoCotizacionJpaEntity", "EstadoCotizacion")
    # Undo renaming class definitions wrongly if they got caught
    content = content.replace("class ClienteJpaEntityJpaEntity", "class ClienteJpaEntity")
    content = content.replace("class UsuarioJpaEntityJpaEntity", "class UsuarioJpaEntity")
    content = content.replace("class CotizacionJpaEntityJpaEntity", "class CotizacionJpaEntity")
    
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)

for model_name in ["ClienteJpaEntity.java", "CotizacionJpaEntity.java", "AuditoriaCotizacionJpaEntity.java", "UsuarioJpaEntity.java"]:
    fix_entity(model_name)

print("Fixed entities")
