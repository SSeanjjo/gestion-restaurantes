# Evidencias de la refactorización

| Archivo | Qué es |
|---|---|
| `Caracterizacion.java` | Pruebas de caracterización del cobro (C1–C9) y de la anulación (A1–A3). Registran resultado, estado del pedido y de la mesa, llamadas a la pasarela y a la DIAN, y **el orden de los eventos**. Arma `CajaService` y `PedidoService` por reflexión con el constructor vigente, así que el mismo archivo corre sobre el código antes y después. |
| `antes.txt` | Salida sobre el commit base `578580c`. |
| `despues.txt` | Salida después de la intervención (se agrega en el commit de verificación). |
| `metricas.py`, `metricas_antes.txt` | Métricas de tamaño y acoplamiento de las clases afectadas (`python3 metricas.py`). |

**Cómo se ejecutó.** El entorno donde se hizo el análisis no podía descargar dependencias de
Maven Central, así que la caracterización se compiló y ejecutó contra *stubs* de las APIs de
Spring/JPA, sin base de datos. Es una evidencia de comportamiento de la lógica de servicio, no
reemplaza a la suite de JUnit: esa corre en GitHub Actions (`mvn verify`) en cada *push*.
