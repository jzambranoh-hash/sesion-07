# HelpDesk — Taller de Open API (proyecto base)

Proyecto base del **Taller 4: Open API**. Completa los `// TODO` del contrato y escribe la
implementación siguiendo el documento del taller.

| Dato | Valor |
|---|---|
| Puerto | `8080` |
| Contrato | `src/main/resources/api.yaml` (aquí están los TODO 1 a 10) |
| Implementación | Debes crear `TicketApiDelegateImpl.java` (TODO 11 a 20) |
| Validador | `src/main/java/com/nttdata/ms_tickets/validacion/Validador.java` |
| Swagger UI | http://localhost:8080/swagger-ui.html |

## Requisitos
- JDK 17 · IntelliJ IDEA · Postman
- Conexión a internet la primera vez (Maven descarga dependencias y el plugin generador)

## Abrir el proyecto
1. Descomprime el archivo.
2. **File > Open…** y selecciona la carpeta `ms-tickets` (la que contiene el `pom.xml`). Elige **Trust Project**.
3. Verifica el SDK 17 en **File > Project Structure > Project**.

## Ciclo de trabajo del taller
```
1. Editas api.yaml            (el contrato manda)
2. mvn clean install          (el plugin genera interfaces y modelos)
3. Reload All Maven Projects  (IntelliJ indexa lo generado)
4. Escribes la lógica         (TicketApiDelegateImpl)
5. Ejecutas el Validador      (mide tu avance sobre 20)
```

> **Importante:** después de cada `mvn clean install`, pulsa **↻ Reload All Maven Projects**
> en el panel Maven. Sin ese refresco, IntelliJ no reconoce las clases generadas.

## Cómo validar tu avance
Ejecuta `validacion/Validador.java` con el botón ▶ (no necesita que la aplicación esté encendida).
Muestra 20 verificaciones agrupadas por criterio y tu **puntaje automático** sobre 20.

| Criterio | Puntos |
|---|---|
| Contrato: endpoints | 4 |
| Contrato: modelos | 3 |
| Contrato: validaciones y errores | 3 |
| Implementación: reglas de negocio | 6 |
| Implementación: errores | 4 |
| **Total** | **20** |

Al inicio el puntaje es 0: sube a medida que completas los TODO.
Si muchas verificaciones dicen *"aún no generado"*, falta ejecutar `mvn clean install` y recargar Maven.

## Probar en Postman
Cuando el Validador llegue a 20/20, ejecuta `MsTicketsApplication` y prueba los 8 escenarios de
`ms-tickets.postman_collection.json` (**Import** en Postman).
