# Estado del proyecto y prompt de continuación

Documento de traspaso para retomar el desarrollo de **Electro_Perico** en otra
sesión o con otro asistente. Actualizado tras cerrar las fases 1, 2 y 3.

---

## 1. Qué es esto

Aplicación Android nativa (**Kotlin, Jetpack Compose, Material 3, MVVM,
StateFlow, Navigation Compose, DataStore**) para calcular el coste y el tiempo
de una recarga de coche eléctrico junto a un cargador público. Interfaz en
español, sin internet, sin registro y sin permisos.

- Repositorio: `pamoron/IA_Claude` (público)
- Rama de trabajo: **`claude/ev-charging-calculator-b9z2uu`**
- Paquete: `com.pamoron.electroperico`
- `minSdk` 28 · `compileSdk`/`targetSdk` 35 · AGP 8.7.3 · Kotlin 2.0.21 · Gradle 8.9

## 2. Qué está hecho

| Fase | Contenido | Estado |
|---|---|---|
| 1 | Calculadora + ajustes (perfil editable, modos de estimación, umbrales de precio) | Terminada |
| 2 | Comparador de hasta 5 cargadores con puntuación de equilibrio 60/40 | Terminada |
| 3 | Historial local (editar, eliminar, duplicar, favoritos, 3 ordenaciones) | Terminada |
| 3b | **Varios perfiles de vehículo** | **Pendiente** |
| 4 | Lectura del precio y la potencia con la cámara (OCR) | Solo propuesta |

Cuatro pantallas conectadas por `ui/navigation/AppNavigation.kt`: calculadora
(inicio), comparador, historial y ajustes. Pie de página común en todas.

**141 pruebas unitarias, todas verdes.** Cubren el motor de cálculo, el
comparador, el historial, el redondeo monetario y el formato español.

## 3. Cómo verificar los cambios

**El módulo `app` no se puede compilar en el contenedor de Claude Code on the
web**: la política de red del entorno bloquea `dl.google.com`, así que Gradle no
puede resolver AGP ni AndroidX. Hay dos formas de comprobar el trabajo:

### a) Dominio puro en la JVM (rápido, local)

Existe un proyecto auxiliar que compila solo `domain/` y `ui/format/` más todas
las pruebas. Si no está, se recrea con un `build.gradle.kts` de Kotlin JVM cuyos
`sourceSets` apunten a `app/src/main/java` (incluyendo únicamente
`com/pamoron/electroperico/domain/**` y `com/pamoron/electroperico/ui/format/**`)
y a `app/src/test/java`:

```bash
cd <proyecto-auxiliar> && gradle test --console=plain -q
```

### b) Compilación real en GitHub Actions (imprescindible para la UI)

`.github/workflows/build-apk.yml` se dispara en cada `push`: ejecuta las
pruebas, compila el APK de depuración, lo sube como artefacto y publica la
versión `apk-ultima`. **Es la única forma de validar el código Compose.** El
flujo vuelca los errores de compilación (`e: ...`) al final del registro, para
poder diagnosticarlos sin descargar el log entero.

APK descargable: https://github.com/pamoron/IA_Claude/releases/tag/apk-ultima

## 4. Decisiones que conviene respetar

1. **`domain/` no importa nada de Android ni de kotlinx.serialization.** Es lo
   que permite probarlo entero en la JVM. Los objetos de transferencia con
   `@Serializable` viven en `data/`.
2. **El tiempo se calcula con la energía que entra en la batería**, no con la
   facturada: los factores de la curva DC y el 0,92 de AC ya describen la
   potencia del lado de la batería.
3. **Dinero en `BigDecimal` con `HALF_UP`**, y cada partida se redondea a dos
   decimales *antes* de sumarse, para que el total mostrado sea exactamente la
   suma de las partidas mostradas.
4. **Sin Hilt y sin Room.** Inyección manual en `AppContainer`; persistencia en
   DataStore con JSON. Evita procesadores de anotaciones y sus problemas de
   versión.
5. **Ninguna valoración depende solo del color**: siempre icono y texto.
6. Todo el texto visible vive en `app/src/main/res/values/strings.xml`. El
   puente entre el dominio y los recursos es `ui/common/RatingUi.kt`.
7. La validación del motor exige que los números sean **finitos**: un NaN o un
   infinito devuelve `CalculationOutcome.Failure`, nunca una excepción.

## 5. Lo que falta

### Pendiente inmediato: la ilustración de Perico

El icono del lanzador y el avatar del pie llevan un **marcador de posición**
generado (un avatar genérico azul), porque la ilustración se aportó como imagen
en la conversación y no llegó al disco. Para ponerla:

- Pie de página: `app/src/main/res/drawable-nodpi/avatar_great.png` (192×192).
- Icono: `app/src/main/res/mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_foreground.png`
  (108, 162, 216, 324 y 432 px), o directamente con **Image Asset** de Android
  Studio, que los genera los cinco.

Al hacer `push`, el APK se recompila con la ilustración nueva.

### Fase 3b: varios perfiles de vehículo

`AppSettings.vehicle` es hoy un único perfil. Habría que pasar a
`profiles: List<VehicleProfile>` más `activeProfileId`, añadir un selector en
ajustes y permitir crear, duplicar y borrar perfiles. `VehicleProfile` ya tiene
`id`, y todos los cálculos reciben el perfil como parámetro, así que el cambio
está acotado a `SettingsRepository`, `SettingsViewModel` y `SettingsScreen`.

### Fase 4: OCR con la cámara

Sin implementar. Plan en el README: ML Kit Text Recognition v2 + CameraX, todo
en el dispositivo, con confirmación manual obligatoria de lo leído.

### Deuda técnica conocida

- `ComparatorScreen.kt` mantiene su propio diálogo de formulario privado; el
  historial ya usa el compartido `ui/common/ChargerFormDialog.kt`. Conviene que
  el comparador use también el compartido y borrar el duplicado.
- Entre 162,75 kW y 180 kW un cargador DC dispara el aviso de «sobredimensionado»
  y a la vez se clasifica como «muy adecuado». Ambas afirmaciones son ciertas,
  pero la combinación puede confundir; se mantiene porque los umbrales fijos son
  los que se pidieron.
- No hay pruebas instrumentadas de UI (Compose UI Test).

---

## 6. Prompt para retomar el proyecto

> Continúas el desarrollo de **Electro_Perico**, una app Android en Kotlin +
> Jetpack Compose + Material 3 que calcula el coste y el tiempo de recarga de un
> coche eléctrico. El repositorio es `pamoron/IA_Claude` y la rama de trabajo es
> `claude/ev-charging-calculator-b9z2uu`.
>
> **Lee primero `ESTADO.md` y luego `README.md`**: describen la arquitectura, las
> fórmulas, las decisiones tomadas y lo que falta.
>
> Contexto que necesitas saber antes de empezar:
> - Las fases 1 (calculadora), 2 (comparador) y 3 (historial) están terminadas,
>   con 141 pruebas unitarias en verde.
> - **Es muy probable que no puedas compilar el módulo `app` localmente**, porque
>   la política de red del entorno suele bloquear `dl.google.com` (Google Maven).
>   Comprueba con `curl -sS "$HTTPS_PROXY/__agentproxy/status"`. Si está
>   bloqueado, no intentes rodearlo: valida el dominio puro con el proyecto JVM
>   auxiliar y **usa GitHub Actions como compilador real** — cada `push` dispara
>   `.github/workflows/build-apk.yml`, que compila y deja los errores `e:` al
>   final del registro.
> - Mantén el estilo del proyecto: todo el código comentado en español, el texto
>   visible en `strings.xml`, `domain/` sin dependencias de Android, y una prueba
>   unitaria por cada fórmula o regla nueva.
>
> Tareas por orden de prioridad:
> 1. Sustituir el marcador de posición del icono y del avatar del pie por la
>    ilustración real (ver el apartado 5 de `ESTADO.md`).
> 2. Fase 3b: varios perfiles de vehículo.
> 3. Quitar el diálogo de formulario duplicado de `ComparatorScreen.kt` y usar el
>    compartido `ui/common/ChargerFormDialog.kt`.
> 4. Fase 4: lectura del precio y la potencia con la cámara (ML Kit + CameraX),
>    con confirmación manual obligatoria.
>
> Confirma que las pruebas siguen en verde y que CI compila antes de dar por
> terminada cada tarea.
