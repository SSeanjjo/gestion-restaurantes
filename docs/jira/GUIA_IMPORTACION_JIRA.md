# Guía: cargar el backlog en Jira

Sitio: `restaurante-software-ii.atlassian.net` · Proyecto: **EBS** (Scrum)

## Archivos

| Archivo | Cuándo usarlo |
|---|---|
| `jira_import_solo_nuevas.csv` | El proyecto **ya tiene** EBS-1 a EBS-13 (CU-01). Crea las épicas CU-02, CU-04 y Arquitectura, las historias F-14 a F-21 y 5 tareas técnicas (16 incidencias). |
| `jira_import_completo.csv` | El proyecto está vacío. Crea 4 épicas, las 21 historias (F-01 a F-21) y 5 tareas (30 incidencias). |

Cada historia trae: la historia de usuario, el SWR, el **criterio de aceptación** (columna *Verificación* del plan), la trazabilidad (caso de uso, componente, clase y prueba), la prioridad (MoSCoW del plan), los puntos (columna *Esfuerzo*) y el sprint.

## Antes de importar

1. **Crear los sprints** en *Backlog → Crear sprint* con estos nombres exactos (el importador los busca por nombre):
   - `EBS Sprint 1` (15–28 sep 2026) · F-01 a F-07, F-09
   - `EBS Sprint 2` (29 sep – 12 oct 2026) · F-10 a F-13
   - `EBS Sprint 3` (13–26 oct 2026) · F-08, F-14 a F-21 y tareas de arquitectura
2. Si el campo *Story Points* no aparece en el proyecto: *Configuración del proyecto → Tipos de incidencia → Historia → agregar campo "Story point estimate"*.

## Importar

1. Arriba a la derecha: **⚙ Configuración → Sistema → Importación externa → CSV** (o, en el proyecto, *… → Importar incidencias*).
2. Suba el archivo CSV, codificación **UTF-8**, separador **coma**.
3. Elija el proyecto **EBS**.
4. Asigne los campos:

| Columna del CSV | Campo de Jira |
|---|---|
| Issue Id | Issue ID |
| Parent Id | Parent ID (une cada historia a su épica) |
| Issue Type | Issue Type |
| Summary | Summary |
| Description | Description |
| Priority | Priority |
| Labels (las dos) | Labels |
| Story Points | Story point estimate |
| Sprint | Sprint |
| Status | Status (*Done* → Listo / Hecho) |

5. *Comenzar importación* y revisar el resumen.

## Después de importar

- Si el proyecto ya tenía EBS-1 a EBS-13, abra cada una y asígnele como **épica principal** "CU-01 Gestionar Pedido" (o selecciónelas todas en el backlog → *Cambiar épica principal*).
- Las claves que asigne Jira (por ejemplo EBS-22…) pueden no coincidir con las del plan. El resumen de cada historia empieza con su ID de funcionalidad (F-14…) y la descripción incluye la clave del plan, así que la trazabilidad se mantiene. Si quiere, actualice la columna *Clave Jira* del Plan de Requisitos con las claves reales.
- Flujo de trabajo del tablero: **Por hacer → En curso → En revisión → Hecho**. Definición de hecho: se cumple el criterio de aceptación, existe una prueba (automática o manual documentada) y el *pull request* pasó la integración continua.
- Para la sustentación: *Informes → Gráfico de evolución del sprint* (burndown) del Sprint 3 y la vista de épicas en la *Cronología*.

## Sprint 4 · Taller de refactorización

`jira_import_sprint4_refactorizacion.csv` crea la épica **E5 Refactorización y cierre de arquitectura** con
sus tareas (R-01 a R-07) y el defecto pendiente R-08. Antes de importarlo, cree los sprints
`EBS Sprint 4` (dos semanas) y `EBS Sprint 5`. Las tareas R-01 a R-06 quedan en *Done* porque el trabajo
está entregado en el repositorio; R-07 (informe y sustentación) y R-08 quedan por hacer.
Mapee las columnas igual que en la tabla de arriba (el tipo *Bug* debe existir en el proyecto; en la
plantilla Scrum viene por defecto).
