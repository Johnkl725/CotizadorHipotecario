import os
import re

# We will read, modify, and write files.

base_dir = r"C:\Users\John\IdeaProjects\NewCotizador\src\main\java\com\example\newcotizador"
domain_model_dir = os.path.join(base_dir, "domain", "model")
domain_port_in_dir = os.path.join(base_dir, "domain", "port", "in")
domain_port_out_dir = os.path.join(base_dir, "domain", "port", "out")
persistence_dir = os.path.join(base_dir, "infrastructure", "adapter", "out", "persistence")
entity_dir = os.path.join(persistence_dir, "entity")
service_dir = os.path.join(base_dir, "application", "service")
web_dir = os.path.join(base_dir, "infrastructure", "adapter", "in", "web")

os.makedirs(domain_port_in_dir, exist_ok=True)
os.makedirs(domain_port_out_dir, exist_ok=True)
os.makedirs(entity_dir, exist_ok=True)

# Step 1: Create JPA Entities mirroring domain, and strip JPA from domain
for model_name in ["Cliente", "Cotizacion", "AuditoriaCotizacion", "Usuario"]:
    model_file = os.path.join(domain_model_dir, f"{model_name}.java")
    if not os.path.exists(model_file):
        continue
        
    with open(model_file, "r", encoding="utf-8") as f:
        content = f.read()
        
    # Generate Entity content
    entity_content = content.replace("package com.example.newcotizador.domain.model;", "package com.example.newcotizador.infrastructure.adapter.out.persistence.entity;\n\nimport com.example.newcotizador.domain.model.*;")
    entity_content = entity_content.replace(f"public class {model_name}", f"public class {model_name}JpaEntity")
    # if there is an enum, we need to map it carefully or keep it in domain
    
    with open(os.path.join(entity_dir, f"{model_name}JpaEntity.java"), "w", encoding="utf-8") as f:
        f.write(entity_content)
        
    # Strip JPA from domain
    domain_content = re.sub(r"@Entity\b.*?\n?", "", content, flags=re.MULTILINE|re.DOTALL)
    domain_content = re.sub(r"@Table\(.*?\)\n?", "", domain_content)
    domain_content = re.sub(r"@Id\b\n?", "", domain_content)
    domain_content = re.sub(r"@GeneratedValue\(.*?\)\n?", "", domain_content)
    domain_content = re.sub(r"@Column\(.*?\)\n?", "", domain_content)
    domain_content = re.sub(r"@Version\b\n?", "", domain_content)
    domain_content = re.sub(r"@ManyToOne\(.*?\)\n?", "", domain_content)
    domain_content = re.sub(r"@JoinColumn\(.*?\)\n?", "", domain_content)
    domain_content = re.sub(r"@Enumerated\(.*?\)\n?", "", domain_content)
    domain_content = re.sub(r"import jakarta\.persistence\..*?;\n", "", domain_content)
    
    # ensure it's clean
    domain_content = re.sub(r"\n\s*\n", "\n", domain_content)
    
    with open(model_file, "w", encoding="utf-8") as f:
        f.write(domain_content)

print("Step 1 and 3 and 4 done.")
