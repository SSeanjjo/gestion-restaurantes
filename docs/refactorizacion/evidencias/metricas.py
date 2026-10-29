import re, sys, pathlib
B = pathlib.Path(__file__).resolve().parents[3] / 'backend/src/main/java/co/edu/uniquindio/comanda'
def ctor_params(src, clase):
    m = re.search(r'public\s+' + clase + r'\s*\((.*?)\)\s*\{', src, re.S)
    return 0 if not m or not m.group(1).strip() else len(re.findall(r'[^,]+', re.sub(r'<[^>]*>', '', m.group(1))))
def analizar(ruta, clase, paquete):
    src = (B / ruta).read_text(encoding='utf-8')
    lineas = [l for l in src.splitlines()]
    codigo = [l for l in lineas if l.strip() and not l.strip().startswith(('//', '*', '/*', 'import ', 'package '))]
    imports = re.findall(r'^import (co\.edu\.uniquindio\.comanda\.[\w.]+);', src, re.M)
    pk = lambda i: '.'.join(p for p in i.split('.') if p[0].islower() or p == 'co')
    otros = sorted({pk(i) for i in imports} - {'co.edu.uniquindio.comanda.' + paquete})
    return dict(clase=clase, lineas=len(lineas), lineas_codigo=len(codigo), params_ctor=ctor_params(src, clase),
                imports_internos=len(imports), paquetes_ajenos=len(otros), metodos=len(re.findall(r'^\s{4}(?:public|private|protected)?\s*(?:static\s+)?[\w<>\[\], ?]+\s+\w+\([^)]*\)\s*(?:throws \w+\s*)?\{', src, re.M)) - 1)
filas = [analizar('caja/CajaService.java', 'CajaService', 'caja')]
for ruta, clase, pq in [('mesa/MesaService.java', 'MesaService', 'mesa'), ('caja/EmisorFactura.java', 'EmisorFactura', 'caja'), ('pedido/PedidoService.java', 'PedidoService', 'pedido')]:
    if (B / ruta).exists(): filas.append(analizar(ruta, clase, pq))
todo = ''.join(p.read_text(encoding='utf-8') for p in B.rglob('*.java'))
print('| Clase | Líneas | Líneas de código | Parámetros del constructor | Imports internos | Paquetes ajenos que importa | Métodos |')
print('|---|---|---|---|---|---|---|')
for f in filas:
    print(f"| `{f['clase']}` | {f['lineas']} | {f['lineas_codigo']} | {f['params_ctor']} | {f['imports_internos']} | {f['paquetes_ajenos']} | {f['metodos']} |")
print()
print('Secuencia `mesa.liberar()` + evento `MesaCambiada` repetida fuera de `Mesa`/`MesaService`:', len(re.findall(r'\bmesa\.liberar\(\)', todo)) - len(re.findall(r'^\s+(?:mesa|m)\.liberar\(\);\s*$', (B/'mesa/MesaService.java').read_text(encoding='utf-8') if (B/'mesa/MesaService.java').exists() else '', re.M)) , 'sitio(s)')
print('Usos de `MesaRepository` dentro del paquete caja:', len(re.findall(r'MesaRepository', ''.join(p.read_text(encoding="utf-8") for p in (B/'caja').rglob('*.java')))))
print('Referencias a `CalculadoraCufe`/`ServicioFacturacionElectronica` en CajaService:', len(re.findall(r'CalculadoraCufe|ServicioFacturacionElectronica|DocumentoFactura', (B/'caja/CajaService.java').read_text(encoding='utf-8'))))
