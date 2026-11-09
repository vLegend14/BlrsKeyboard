# BlrsKeyboard

Teclado Android (IME) nativo para uso personal. Reemplazo de Gboard optimizado para francés/español (AZERTY) y programación en Termux.

## Características

### Diseño
- **Layout AZERTY** optimizado para francés y español
- **Capas múltiples**: abc (letras), sym (símbolos), sym2 (símbolos adicionales)
- **Barra de herramientas** configurable (2 filas con accesos rápidos)
- **Tema oscuro** con paleta BlrsDesign (5 variantes de color)
- **Sin permiso INTERNET**, sin telemetría

### Gestos
- **Pulsación larga** (300ms): inserta carácter alternativo
- **Swipe up** (>22px): popup con variantes
- **Repetición**: ⌫ Supr ↑↓←→ (inicio 400ms, intervalo 60ms)
- **Modificadores con lock**:
  - Shift: 1 toque mayúscula, 2 toques <400ms lock
  - Ctrl/Alt: 1 uso, 2 toques <400ms lock
- **Space arrastra cursor**: 1 carácter cada 14px
- **Pares automáticos**: ( [ { " ` (apóstrofe NO se autocierra)

### Funciones especiales
- **Teclas de control**: Ctrl+C funciona en Termux (KeyEvent reales con metaState)
- **Panel de emojis**: 5 categorías (Smileys, People, Animals, Food, Travel)
- **Macros**: accesos rápidos a emojis y ajustes
- **Configuración**: Activity con vista previa del teclado

## Arquitectura

```
app/src/main/java/dev/blrs/keyboard/
├── model/
│   ├── Key.kt              # Modelo de tecla
│   └── BlrsKeyboardPreset.kt  # Generador de layout mk4
├── theme/
│   └── BlrsDesign.kt       # Sistema de temas
├── input/
│   ├── KeySink.kt          # Interfaz para input
│   └── InputConnectionSink.kt  # Implementación Android
├── settings/
│   └── SettingsRepository.kt   # Configuración (única fuente de verdad)
├── ui/
│   ├── KeyboardView.kt     # Vista de teclado (Canvas)
│   └── EmojiPanel.kt       # Panel de emojis
├── BlrsKeyboardService.kt  # Servicio IME
└── SettingsActivity.kt     # Activity de configuración
```

### Stack técnico
- **Lenguaje**: Kotlin
- **SDK**: minSdk 26 (Android 8.0), targetSdk 34 (Android 14)
- **UI**: View con Canvas (sin Compose en IME)
- **Arquitectura**: KeyboardView desacoplada via KeySink

## Compilación

### Requisitos
- Nix (shell.nix configurado)
- O Android SDK + Gradle

### Con Nix
```bash
nix-shell
gradle assembleDebug
```

### Sin Nix
```bash
export ANDROID_HOME=/path/to/android-sdk
gradle assembleDebug
```

### Firmar APK
```bash
keytool -genkey -v -keystore blrs.keystore -alias blrs -keyalg RSA -keysize 2048 -validity 10000
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore blrs.keystore app/build/outputs/apk/debug/app-debug.apk blrs
```

## Instalación

1. Transferir APK al dispositivo
2. Instalar con `adb install app-debug.apk`
3. Ir a **Configuración → Sistema → Idiomas → Teclado en pantalla**
4. Activar **BlrsKeyboard**
5. Seleccionar como teclado predeterminado

## Configuración

Abrir la app BlrsKeyboard para acceder a:
- **Layout**: AZERTY, QWERTY, etc.
- **Mostrar números**: en fila superior
- **Mostrar barra**: accesos rápidos
- **Tema**: 5 variantes (Default, Ocean, Forest, Sunset, Midnight)
- **Ajuste de acento**: A, B, C
- **Altura de teclas**: ajustable

## Uso en Termux

El teclado está optimizado para uso con Termux:
- Ctrl+C funciona correctamente (envía KeyEvent con metaState)
- Flechas para navegación
- Símbolos de programación accesibles

## Desarrollo

### Tests
```bash
gradle test
```

Tests incluidos:
- `BlrsKeyboardPresetTest`: generación de layout
- `ModifierLockTest`: lógica de modificadores
- `PairLogicTest`: pares automáticos
- `SettingsRepositoryTest`: configuración

### Añadir nuevos emojis
Editar `EmojiPanel.kt` y modificar el map `EMOJIS`.

### Añadir nuevos temas
Editar `BlrsDesign.kt` y añadir a `VARIANTS`.

## Licencia

Uso personal. No distribuir.

## Autor

Fr4casad0 - Blrs
