# Diagnóstico técnico y refactorización · CU-02 Cobro y facturación electrónica

Ingeniería de Software II · Universidad del Quindío · Taller de refactorización

> **Estado del documento:** diagnóstico (secciones 1 a 4). Las secciones 5 a 8 se completan en los
> commits de la intervención y de la verificación.
> Las líneas citadas corresponden al commit base **`578580c`** (antes de cualquier cambio).

## 1. Contexto

| Dato | Valor |
|---|---|
| Equipo | Sebastián José Lesmes Muñoz · Kevin David Puerta Hidalgo |
| Profesor | Juan Manuel Cardona |
| Repositorio | https://github.com/SSeanjjo/gestion-restaurantes |
| Rama de trabajo | `feature/parcial-2-arquitectura-cu02` |
| Funcionalidad elegida | **CU-02 · Cobrar la cuenta y emitir la factura electrónica** (`registrarFactura`, Contrato 3) |
| Tecnología | Java 21 · Spring Boot 3.4 · JPA/Hibernate · MariaDB · JUnit 5 + Mockito |
| Arquitectura | Monolito modular en capas (controlador → servicio → repositorio → entidad), eventos de dominio y puertos/adaptadores hacia el banco y la DIAN |
| Requisitos relacionados | RF-06 (total automático), RF-08 (método de pago), RD-05 (solo se cobra un pedido entregado), F-15 a F-19 (cuentas, cobro, factura), Contrato 3 |

**Por qué esta funcionalidad.** Es una transacción de negocio que toca **cinco entidades**
(`Pedido`, `Mesa`, `Factura`, `Pago`, `Usuario`) y **dos sistemas externos** (pasarela bancaria y DIAN).
Además tiene una regla de compensación (reversar la tarjeta si falla la factura), por lo que concentra
decisiones de diseño reales.

## 2. Arquitectura real de la funcionalidad

### 2.1 Componentes (rastreados en el código)

| Capa | Componente | Ruta (`backend/src/main/java/co/edu/uniquindio/comanda/`) | Responsabilidad declarada |
|---|---|---|---|
| Controlador | `CajaController.cobrar` | `caja/CajaController.java` (líneas 57–63) | Autoriza el rol (`CAJERO`/`ADMINISTRADOR`) y delega. **No** contiene reglas de negocio. |
| Servicio (Facade) | `CajaService.cobrar` | `caja/CajaService.java` (líneas 115–134) | Orquesta el cobro en una transacción |
| Estrategias | `ProcesadorPago` → `PagoEfectivo`, `PagoTarjeta` | `caja/pago/` | Validan y procesan cada parte del pago |
| Puerto/Adaptador | `PasarelaPago` → `PasarelaPagoSimulada` | `caja/pago/` | Autoriza y reversa tarjetas (IS-01) |
| Puerto/Adaptador | `ServicioFacturacionElectronica` → `DianSimulada` | `caja/dian/` | Valida la factura ante la DIAN (IS-02) |
| Dominio | `Desglose`, `Factura` (+ `Builder`), `Pago`, `Pedido.esCobrable/marcarFacturado`, `Mesa.liberar` | `caja/`, `pedido/`, `mesa/` | Invariantes: RD-05, base e impuesto, estado del pedido y de la mesa |
| Persistencia | `PedidoRepository.bloquearPorId`, `FacturaRepository`, `MesaRepository.findByIdParaActualizar`, `UsuarioRepository` | cada módulo | Bloqueo pesimista de pedido y mesa |
| Eventos | `Eventos.MesaCambiada`, `Eventos.CajaActualizada` → `PublicadorTiempoReal` | `tiemporeal/` | Notifican a las pantallas después del commit |

### 2.2 Flujo (estado actual)

```mermaid
sequenceDiagram
    autonumber
    participant UI as IU-03 Caja
    participant C as CajaController
    participant S as CajaService
    participant P as ProcesadorPago (estrategia)
    participant B as PasarelaPago (banco)
    participant D as DIAN
    participant BD as Repositorios / BD
    UI->>C: POST /api/caja/cuentas/{id}/cobro
    C->>S: cobrar(pedidoId, solicitud, cajeroId)
    S->>S: jornada.exigirCajaAbierta()
    S->>BD: usuarios.findById, pedidos.bloquearPorId (FOR UPDATE)
    S->>S: RD-05, YA_FACTURADO, suma de pagos = total
    loop cada parte del pago (tarjetas primero)
        S->>P: procesar(parte)
        P->>B: autorizar (solo tarjeta)
    end
    S->>BD: facturas.save(Factura.builder()…)
    S->>S: numero + CUFE (CalculadoraCufe)
    S->>D: validarFactura(documento)
    alt DIAN rechaza
        S->>P: revertir(pago) → B.reversar
        S-->>C: DIAN_RECHAZO (rollback)
    else DIAN acepta
        S->>BD: pedido.marcarFacturado(); mesas.findByIdParaActualizar → mesa.liberar()
        S-->>UI: eventos MesaCambiada + CajaActualizada (tras el commit)
    end
```

### 2.3 Pruebas que protegen hoy la funcionalidad

| Prueba | Qué cubre |
|---|---|
| `CajaServiceTest` (4 casos) | Cobro dividido feliz, pagos que no cuadran, rechazo de la DIAN con reverso de la tarjeta, pedido no entregado |
| `ProcesadoresPagoTest` | Efectivo insuficiente, cambio, tarjeta rechazada |
| `DesgloseTest`, `CalculadoraCufeTest`, `ResumenJornadaTest` | Funciones puras del dominio |
| *Sin pruebas* | Anulación de pedido (`PedidoService.anular`), publicación de eventos del cobro, falla del reverso |

## 3. Hallazgos

Cada afirmación se verificó contra el código y, cuando era posible, contra una ejecución
(`evidencias/Caracterizacion.java`, resultado en `evidencias/antes.txt`). Se separan **hechos**
(comprobados) de **hipótesis**.

### H-01 · `CajaService` mezcla la emisión de la factura electrónica con la orquestación del cobro

| Campo | Contenido |
|---|---|
| **ID / Tipo** | H-01 · **Deuda técnica** (cohesión y responsabilidad única) |
| **Ubicación** | `caja/CajaService.java`, método `emitirFactura`, líneas **176–194**; constructor líneas **58–71**; imports de `CalculadoraCufe`, `ServicioFacturacionElectronica` y `DocumentoFactura` en las líneas **24–26** |
| **Evidencia** | `String numero = config.prefijo() + "-" + factura.getId();` · `String cufe = CalculadoraCufe.calcular(numero, …, config.nitEmisor(), config.claveTecnica(), config.ambiente());` · `dian.validarFactura(new DocumentoFactura(numero, …, cufe))` · `throw new NegocioException(HttpStatus.BAD_GATEWAY, "DIAN_RECHAZO", …)` |
| **Observación (hechos)** | La clase tiene **9 dependencias en el constructor**, importa **8 paquetes ajenos** y tiene **9 referencias** a tipos de la DIAN/CUFE. Cambia por cinco razones distintas: (a) reglas del desglose, (b) medios de pago y compensación, (c) **numeración, CUFE y protocolo de la DIAN**, (d) ciclo de vida del pedido/mesa y (e) consultas para IU-03. La razón (c) evoluciona de forma independiente (resoluciones de la DIAN, versión del algoritmo del CUFE) y ya tiene otro cliente: `CierreCajaService` también habla con la DIAN. |
| **Criterio** | Principio de responsabilidad única (S de SOLID), cohesión, **testabilidad**: para probar el número y el CUFE hay que ejecutar todo `cobrar` con 6 *mocks* y fijar el `id` de la factura con reflexión (`CajaServiceTest`, líneas 67–71). |
| **Impacto y riesgo** | Cualquier cambio en la factura electrónica obliga a modificar y volver a probar la clase que también cobra y libera la mesa. El riesgo de regresión recae en el flujo con dinero. |
| **Acción propuesta** | *Extract Class*: crear `EmisorFactura` (construye, numera, calcula el CUFE y valida ante la DIAN) e inyectarlo en `CajaService`. |
| **Verificación** | Caracterización C1, C3, C8 idénticas antes y después; nuevas pruebas unitarias de `EmisorFactura`. |
| **Estado** | Pendiente → *seleccionado* |

### H-02 · El cobro libera la mesa saltándose el módulo `mesa` (y la secuencia está duplicada)

| Campo | Contenido |
|---|---|
| **ID / Tipo** | H-02 · **Riesgo arquitectónico** (límites del módulo) y duplicación |
| **Ubicación** | `caja/CajaService.java`, método `cerrarPedido`, líneas **197–202**; `pedido/PedidoService.java`, método `anular`, líneas **202–210** |
| **Evidencia** | `Mesa mesa = mesas.findByIdParaActualizar(pedido.getMesa().getId()).orElseThrow(); mesa.liberar(); eventos.publishEvent(new Eventos.MesaCambiada(PedidoMapper.mesa(mesa, null)));` (en caja) y `mesa.liberar();` … `eventos.publishEvent(new Eventos.MesaCambiada(PedidoMapper.mesa(mesa, null)));` (en pedido) |
| **Observación (hechos)** | Dos servicios de módulos distintos repiten la regla «liberar la mesa y avisar a las tablets». El paquete `caja` usa directamente `MesaRepository` (3 referencias) y arma un `MesaDto` con un mapper del módulo `pedido`. Existe `MesaService`, pero solo expone lecturas. |
| **Criterio** | Encapsulamiento del módulo (el dueño del ciclo de vida de `Mesa` es `mesa`), DRY, capas: un servicio de dominio no debe llegar a la persistencia de otro módulo. |
| **Impacto y riesgo** | Si cambia la regla (p. ej. un estado intermedio «En limpieza»), hay que editar dos lugares. Olvidar el evento deja la cuadrícula de mesas de las tablets desactualizada. |
| **Acción propuesta** | *Move Method*: añadir `MesaService.liberar(…)` (cambia el estado y publica el evento) y que `CajaService` y `PedidoService` lo usen. |
| **Verificación** | Caracterización C1, C8, A1: mismos estados y **mismo orden de eventos**. |
| **Estado** | Pendiente → *seleccionado* |

### H-03 · La compensación del cobro no tolera que el reverso falle

| Campo | Contenido |
|---|---|
| **ID / Tipo** | H-03 · **Defecto latente** (no se manifiesta con el adaptador simulado) |
| **Ubicación** | `caja/CajaService.java`, método `revertir`, líneas **204–206**; se invoca desde las líneas **131** y **171** |
| **Evidencia** | `pagos.forEach(p -> procesador(p.getMetodo()).revertir(p));` dentro de un `catch (RuntimeException e)` que luego hace `throw e;` |
| **Observación** | **Hecho** (caracterización C9, `evidencias/antes.txt`): con un banco cuyo `reversar` lanza una excepción, (1) el error que llega al cajero es la excepción del reverso y **se pierde `DIAN_RECHAZO`**, y (2) el bucle se corta: la segunda tarjeta **no se reversa** (`Pasarela.reversar(AUT-10000)` aparece y `AUT-18000` nunca). **Hipótesis:** un banco real puede fallar por tiempo de espera; con `PasarelaPagoSimulada` no ocurre. |
| **Criterio** | Fiabilidad / tolerancia a fallos (ISO 25010), integridad financiera. |
| **Impacto y riesgo** | Un cliente puede quedar cobrado sin factura y sin que el sistema lo registre. |
| **Acción propuesta** | Intentar el reverso de **cada** pago, conservar el error original (`addSuppressed`) y registrar los reversos fallidos para conciliarlos (tabla de reversos pendientes). |
| **Verificación** | Prueba con una pasarela que falla: deben intentarse los dos reversos y propagarse `DIAN_RECHAZO`. |
| **Estado** | Pendiente (**no se interviene**, ver §4) |

## 4. Priorización

Puntaje = (impacto × riesgo) ÷ esfuerzo. Cada factor va de 1 a 3.

| ID | Impacto | Riesgo | Esfuerzo | Puntaje | ¿Conserva el comportamiento? | Decisión |
|---|:-:|:-:|:-:|:-:|---|---|
| H-01 | 3 | 3 | 2 | **4,5** | Sí | **1.ª refactorización** |
| H-02 | 2 | 2 | 1 | **4,0** | Sí | **2.ª refactorización** |
| H-03 | 3 | 3 | 3 | 3,0 | **No** (cambia lo que ocurre ante un fallo) | Pendiente, se declara como corrección de defecto |

**Razón de selección.** H-01 tiene el mayor impacto sobre la mantenibilidad y está en el flujo con
dinero, pero se puede extraer sin cambiar ningún resultado observable. H-02 es pequeña y
complementa a H-01: deja a `CajaService` hablando solo con sus colaboradores directos. H-03 es el
hallazgo más grave por su efecto financiero, pero **no es un refactor**: corregirlo cambia el
comportamiento (qué error recibe el cajero y qué se persiste), exige una decisión de negocio (cómo
conciliar un reverso fallido) y un esfuerzo mayor (nueva tabla y proceso de reintento). Se deja
registrado como defecto y como primer ítem del siguiente sprint.

## 5. Intervención

*(se completa en el commit de la intervención)*

## 6. Verificación

*(se completa en el commit de verificación)*

## 7. Cierre

*(se completa en el commit de verificación)*

## 8. Git

| Commit | Contenido |
|---|---|
| `578580c` | Estado base (antes de la intervención) |
