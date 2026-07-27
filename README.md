# Carga EV

Calculadora de coste y tiempo de recarga para coche eléctrico, pensada para usarse
en treinta segundos estando aparcado junto a un cargador público.

Aplicación Android nativa en **Kotlin + Jetpack Compose + Material 3**, sin conexión
a internet, sin registro y sin permisos.

---

## Estado: fase 1

Esta entrega contiene **solo la calculadora, los ajustes y las pruebas**, tal y como
se pidió. Las fases siguientes están descritas al final del documento.

| Fase | Contenido | Estado |
|---|---|---|
| 1 | Calculadora, perfil de vehículo editable, pruebas unitarias | **Entregado** |
| 2 | Comparador de hasta 5 cargadores | Pendiente |
| 3 | Historial local y varios perfiles de vehículo | Pendiente |
| 4 | Lectura del precio y la potencia con la cámara (OCR) | Propuesta |

---

## Requisitos

| Herramienta | Versión |
|---|---|
| Android Studio | Ladybug (2024.2.1) o posterior |
| JDK | 17 (el que trae Android Studio sirve) |
| Gradle | 8.9, incluido en el wrapper del repositorio |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.0.21 |
| `compileSdk` / `targetSdk` | 35 |
| `minSdk` | 28 (Android 9 Pie) |

---

## Abrir el proyecto en Android Studio

1. Descarga o clona el repositorio completo.
2. En Android Studio: **File → Open** y selecciona la **carpeta raíz** (la que
   contiene `settings.gradle.kts`). No uses *Import Project*.
3. Acepta la sincronización de Gradle. La primera vez descargará el AGP, Kotlin y
   las librerías de AndroidX; necesita conexión a internet.
4. Si te pide instalar el SDK 35 o las *build tools*, acepta.

No hace falta crear `local.properties`: Android Studio lo genera con la ruta de tu SDK.

### Qué archivos copiar

Si prefieres copiar los ficheros a mano en un proyecto nuevo, cópialos **todos**,
respetando las rutas. El proyecto es autocontenido:

```
settings.gradle.kts
build.gradle.kts
gradle.properties
gradlew                          (dale permisos de ejecución: chmod +x gradlew)
gradlew.bat
gradle/libs.versions.toml
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
app/build.gradle.kts
app/proguard-rules.pro
app/src/main/AndroidManifest.xml
app/src/main/java/...            (todo el árbol)
app/src/main/res/...             (todo el árbol)
app/src/test/java/...            (todo el árbol)
```

---

## Ejecutar la aplicación

1. Conecta un móvil con depuración USB o crea un emulador con API 28 o superior.
2. Selecciona la configuración **app** y pulsa **Run** (`Shift+F10`).

Desde la terminal:

```bash
./gradlew installDebug
```

---

## Ejecutar las pruebas

Las pruebas son de JVM: no necesitan emulador ni dispositivo.

```bash
./gradlew test
```

Para una sola clase:

```bash
./gradlew testDebugUnitTest --tests "com.pamoron.cargaev.domain.calc.Case1BasicDcSessionTest"
```

El informe HTML queda en `app/build/reports/tests/testDebugUnitTest/index.html`.

En Android Studio: clic derecho sobre `app/src/test/java` → **Run 'Tests in ...'**.

### Cobertura actual

**80 pruebas**, todas verdes, sobre el motor de cálculo y el formateo:

| Clase | Qué cubre |
|---|---|
| `Case1BasicDcSessionTest` | Caso 1: 20→80 %, DC 150 kW, 0,45 €/kWh, pérdidas 8 % |
| `Case2OversizedDcChargerTest` | Caso 2: 70→100 % en un poste de 300 kW |
| `Case3AcChargerTest` | Caso 3: poste AC de 22 kW frente a un coche de 11 kW |
| `Case4ExtraCostsTest` | Caso 4: precio efectivo con inicio de sesión y coste por minuto |
| `ValidationTest` | Entradas inválidas y divisiones entre cero |
| `EstimationModeTest` | Factores optimista / normal / conservadora |
| `RatingsTest` | Umbrales de precio y clasificación de cargadores |
| `MoneyTest` | Redondeo monetario con `BigDecimal` |
| `FormattersTest` | Formato español de importes, energía, potencia y tiempo |

---

## Generar el APK de prueba

### APK de depuración (el más rápido para instalar y probar)

```bash
./gradlew assembleDebug
```

Queda en:

```
app/build/outputs/apk/debug/app-debug.apk
```

Cópialo al móvil e instálalo activando *Instalar aplicaciones desconocidas*.

### APK de *release* instalable

La variante *release* está configurada para **firmarse con la clave de depuración**,
de modo que puedas generar un APK optimizado y probarlo sin crear un almacén de claves:

```bash
./gradlew assembleRelease
```

```
app/build/outputs/apk/release/app-release.apk
```

Desde Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

> Para publicar en Google Play habría que crear un `keystore` propio, añadir un
> `signingConfig` de *release* y quitar `signingConfig = signingConfigs.getByName("debug")`
> de `app/build.gradle.kts`.

---

## Arquitectura

MVVM unidireccional con una única actividad y navegación en Compose.

```
UI (Compose)  ──eventos──▶  ViewModel  ──▶  ChargeCalculator (Kotlin puro)
     ▲                          │
     └────── StateFlow ─────────┘
                                │
                                ▼
                        SettingsRepository (DataStore)
```

- **`domain`** no importa nada de Android. Ahí viven los modelos y el motor de
  cálculo, lo que permite probarlo entero con JUnit en la JVM.
- **`data`** guarda el perfil, las preferencias y la última sesión en DataStore.
- **`ui`** solo transforma estado en píxeles. El único puente entre el dominio y los
  recursos de Android es `ui/common/RatingUi.kt`.

### Estructura de carpetas

```
app/src/main/java/com/pamoron/cargaev/
├─ CargaEvApplication.kt        Punto de entrada, crea el contenedor
├─ AppContainer.kt              Inyección de dependencias manual
├─ MainActivity.kt              Única actividad
├─ domain/
│  ├─ model/                    VehicleProfile, ChargeInput, ChargeResult, valoraciones…
│  └─ calc/
│     ├─ ChargeCalculator.kt    Motor de cálculo
│     ├─ ChargeCurve.kt         Curva de carga por tramos
│     └─ Money.kt               Redondeo monetario
├─ data/settings/               AppSettings, SettingsRepository (DataStore)
└─ ui/
   ├─ format/Formatters.kt      Formato español
   ├─ theme/                    Colores, tipografía, modo claro y oscuro
   ├─ navigation/               Grafo de navegación
   ├─ common/RatingUi.kt        Dominio → textos, iconos y colores
   ├─ calculator/               Pantalla principal, ViewModel, componentes
   └─ settings/                 Pantalla de ajustes y su ViewModel
```

---

## Fórmulas

```
energíaNecesaria   = capacidadÚtil × (socFinal − socInicial) / 100
energíaFacturada   = energíaNecesaria / (1 − pérdidas)
potenciaEfectiva   = min(potenciaCargador, potenciaMáxVehículo[AC|DC])
```

**Tiempo en corriente continua**, por tramos de estado de carga:

| Tramo | Factor sobre la potencia efectiva |
|---|---|
| 0–10 % | 0,60 |
| 10–30 % | 0,85 |
| 30–60 % | 0,90 |
| 60–80 % | 0,70 |
| 80–90 % | 0,40 |
| 90–100 % | 0,20 |

```
energíaTramo = capacidadÚtil × Δ%tramo / 100
horasTramo   = energíaTramo / (potenciaEfectiva × factorTramo)
```

**Tiempo en corriente alterna**, potencia estable:

```
potenciaMedia = potenciaEfectiva × 0,92
horas         = energíaNecesaria / potenciaMedia
```

**Factor final de estimación**: optimista × 0,90 · normal × 1,00 · conservadora × 1,15.

```
minutos          = Σ(horasTramo) × 60 × factorEstimación   (redondeado, mínimo 1)
costeEnergía     = energíaFacturada × precioKWh
costeTiempo      = minutos × precioPorMinuto
costeTotal       = costeEnergía + costeInicio + costeTiempo + costeEstacionamiento
precioEfectivo   = costeTotal / energíaFacturada
costePorMinuto   = costeTotal / minutos
autonomíaAñadida = energíaNecesaria / consumo100 × 100
costePor100Km    = costeTotal / autonomíaAñadida × 100
```

### Valoración del precio (editable en Ajustes)

| Precio efectivo | Valoración |
|---|---|
| hasta 0,25 €/kWh | Muy barato |
| hasta 0,39 €/kWh | Buen precio |
| hasta 0,55 €/kWh | Precio normal |
| hasta 0,70 €/kWh | Caro |
| más de 0,70 €/kWh | Muy caro |

La valoración principal usa **siempre el precio efectivo**, no el anunciado. Se
muestran los dos.

### Valoración de la potencia

| Continua | Valoración |
|---|---|
| menos de 50 kW | Lento para viajes |
| 50 – 99 kW | Adecuado |
| 100 – 179 kW | Muy adecuado |
| 180 kW o más | Sobredimensionado para el coche |

| Alterna | Valoración |
|---|---|
| hasta 3,7 kW | Muy lento |
| hasta 7,4 kW | Normal |
| hasta el máximo del coche | Óptimo |
| por encima del máximo del coche | Limitado por el vehículo |

Un cargador **no** es mejor solo por tener más potencia: la app compara potencia,
precio y tiempo real estimado, y avisa cuando los kW sobrantes no aportan nada.

---

## Decisiones y supuestos técnicos

1. **El tiempo se calcula con la energía que entra en la batería, no con la
   facturada.** Los factores de la curva DC describen la potencia media que acepta
   la batería, y el `× 0,92` de AC es el rendimiento del cargador de a bordo:
   aplicar además las pérdidas las contaría dos veces. El modo conservador cubre la
   desviación.
2. **Dinero en `BigDecimal` con `HALF_UP`.** Cada partida se redondea a dos decimales
   *antes* de sumarse, de modo que el total mostrado es exactamente la suma de las
   partidas mostradas. Los precios unitarios se guardan con cuatro decimales y se
   muestran con dos.
3. **El coste por minuto se factura sobre los minutos redondeados** que ve el
   usuario, para que «minutos × €/min» cuadre con la partida del desglose.
4. **Aviso de sobredimensionado** cuando la potencia del cargador supera en más de un
   5 % la del vehículo. Un poste de 160 kW frente a un coche de 155 kW no merece un
   aviso; uno de 300 kW sí. La *clasificación* DC usa los umbrales fijos de la tabla.
5. **La clasificación en alterna es relativa al perfil**: el límite superior lo marca
   la potencia máxima AC del coche, así que sigue siendo correcta si editas el perfil.
6. **Formato español determinista**: `DecimalFormat` con símbolos fijos (coma decimal,
   punto de millares) en lugar de depender del ICU del dispositivo, y espacio duro
   antes del símbolo del euro.
7. **Sin Room en la fase 1.** DataStore basta para un perfil y unas preferencias.
   Room entrará con el historial y el comparador, que sí son colecciones.
8. **Sin Hilt.** `AppContainer` manual y `viewModelFactory`: el proyecto compila sin
   procesadores de anotaciones y sin sus incompatibilidades de versión.
9. **Los resultados se recalculan en vivo**, pero solo aparecen tras el primer
   «Calcular»; después se actualizan solos al mover un deslizador.
10. **Ninguna cifra se presenta como exacta.** Todas las pantallas lo indican.

### Accesibilidad

- Textos grandes en las tarjetas de resultado; botones de 48 dp o más.
- Modo claro y oscuro, con color dinámico en Android 12 o superior.
- Las valoraciones **nunca** dependen solo del color: siempre llevan icono y texto.
- Las filas «etiqueta / valor» se anuncian como una sola unidad al lector de pantalla.

---

## Limitaciones conocidas

- La curva de carga es un modelo por tramos, no la curva real medida del ATTO 2.
  El tiempo es orientativo y puede variar con la temperatura, el acondicionamiento
  de la batería y el reparto de potencia de la estación.
- La app no sabe si el operador cobra por minuto o por sesión: hay que introducirlo.
- Un único perfil de vehículo en esta fase.

---

## Fase 2 propuesta: lectura con la cámara (OCR)

**No implementada todavía.** Idea de trabajo para cuando la calculadora esté rodada:

- Botón de cámara junto a los campos de precio y potencia.
- Reconocimiento de texto en el dispositivo con **ML Kit Text Recognition v2**
  (`com.google.mlkit:text-recognition`), que funciona sin conexión.
- Captura con **CameraX** y análisis del fotograma en directo, sin guardar imágenes.
- Extracción por expresiones regulares de los patrones habituales de los postes:
  `0,45 €/kWh`, `0.45 EUR/kWh`, `150 kW`, `50kW`, `€/min`.
- Los valores detectados se proponen en un diálogo de confirmación; **nunca** se
  rellenan solos, porque un OCR mal leído daría un presupuesto equivocado.
- Requeriría el permiso `CAMERA`, solicitado solo al pulsar el botón, y una nota
  explicando que la imagen no sale del dispositivo.
- Riesgos a evaluar: pantallas retroiluminadas de noche, reflejos, tarifas por tramos
  horarios y postes que muestran el precio solo en la app del operador.

Junto con el OCR encajarían el comparador (fase 2) y el historial (fase 3), que ya
tienen sitio reservado en el grafo de navegación.
