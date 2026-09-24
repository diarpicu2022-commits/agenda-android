# Agenda — reglas para trabajar en este repo

Este archivo lleva al repo las reglas que Diego tiene en su configuración local. Una sesión en la
nube no ve esa configuración: **lo que dice aquí manda**. El estado del proyecto y lo siguiente
que hay que hacer están en [`CONTINUAR.md`](CONTINUAR.md).

## Producto

Agenda personal de Diego (estudiante universitario que también trabaja, Colombia, español).
App Android nativa: Kotlin + Jetpack Compose, módulos `:app` y `:core:domain`.
Arquitectura: [`docs/arquitectura.md`](docs/arquitectura.md).
Contrato de diseño: [`docs/ux/anexos/2026-09-23-agenda-direcciones.md`](docs/ux/anexos/2026-09-23-agenda-direcciones.md), dirección A «Tablero de salidas».

## Diseño: el contrato manda

- **Nada** de color, tipografía, espaciado, forma, icono o movimiento fuera del contrato del anexo.
  Si algo no funciona, se para, se explica con una alternativa concreta y **se espera permiso**.
  Cada cambio aprobado se anota con fecha en el «Registro de enmiendas» del anexo.
- **Implementación paso a paso**: tokens → componente clave → esqueleto → resto de componentes → pantalla completa → estados.
  Al terminar **cada** paso: mostrar lo hecho, citar la cláusula que lo respalda, decir cuál es el siguiente y **esperar visto bueno**.
- Animar solo `transform` y `opacity`, respetando la reducción de movimiento (corte instantáneo).
- Prohibido el diseño de fábrica: degradados violeta, tarjetas redondeadas con sombra repetida, encabezado centrado con tres columnas, emoji como icono, copy genérico.
- Accesibilidad mínima: contraste AA de piso y AAA en cifras que se leen para decidir (hora de salida, salón, horas de la lista); toque ≥ 48 dp; foco visible y orden lógico; una vista para cada estado (cargando, vacío, error, sin permiso, dato viejo).

## Verificación: medida, nunca a ojo

Lo comprobable se comprueba **ejecutando algo**:
- `./gradlew assembleDebug lintDebug testDebugUnitTest :core:domain:test assembleDebugAndroidTest`
- Capturas en dispositivo o emulador (claro/oscuro, 360 y 411 dp, escala de fuente 2,0) con los scripts de `tools/verificacion/`,
  que miden el contraste **sobre el render**, el tamaño de los toques y los textos.
- Se informa de lo que salió, **incluidos los fallos propios**. Lo que no se pudo medir se declara pendiente.
- Cuidado con los falsos positivos: nodos recortados por barras o bordes; capturas tomadas mientras la UI aún se dibuja.
  Solo vale una captura si dos volcados de uiautomator seguidos coinciden.

## Código

- Lógica de tiempo y conflictos **solo** en `core/domain` (Kotlin puro, con pruebas). La UI no hace cuentas.
- Dinero, horas e instantes con `java.time`; reloj inyectable (`Clock`) en los ViewModels.
- Seguridad: base de datos cifrada (Room + SQLCipher, clave en Android Keystore), sin copia de seguridad en la nube,
  solo HTTPS. La clave de Google **nunca** en git: va en `local.properties` y restringida en Google Cloud.

## Git y PR

- **Nunca firmar**: nada de `Co-Authored-By: Claude`, `Claude-Session:`, «Generated with Claude Code» ni enlaces a claude.ai,
  ni en commits, ni en PR, ni en código o documentación.
- Todo PR lleva esta plantilla, con las cuatro secciones en este orden:

```markdown
## <Titulo del trabajo>
📑 **Feature:** <que se hizo, en una linea>

## Desarrollador
👷 **Dev:** Diego Armando Pinta Cuasquen

## Cambios (clases,archivos,etc)
<archivos tocados y descripcion de los cambios>

## Pantallazos funcionalidades
<capturas del antes/despues o de la funcionalidad>
```

Los pantallazos no son opcionales. Si de verdad no aplica ninguna captura, se escribe por qué.
