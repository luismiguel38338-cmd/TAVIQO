# LOBO CLIPS AI

Red social multiplataforma de vídeos cortos verticales **9:16** con usuarios 100% reales, transmisiones en vivo, sistema completo de regalos virtuales con comisión 70/30, mensajería privada, panel administrativo protegido e inteligencia artificial auxiliar con **Gemini** (Firebase AI Logic).

## Identidad Visual
- **Azul Royal Oscuro**: `#0F1F4B`
- **Negro Profundo**: `#05070B`
- **Neón Cyan**: `#00F0FF`
- **Tipografía**: `Outfit` (títulos), `Plus Jakarta Sans` (cuerpo), `JetBrains Mono` (métricas/monedas)
- **Logotipo**: Icono adaptativo personalizado de Lobo Neón (`ic_launcher`) y banners generados en `app/src/main/res/drawable/`

## Principio de Datos Reales
- **Cero perfiles falsos, cero bots y cero métricas simuladas**: Todo perfil, clip, comentario, visualización, seguimiento, compra o regalo corresponde a usuarios autenticados reales.
- **Sin saldo inicial regalado**: Los usuarios nuevos inician con `0` monedas hasta completar compras verificadas o recibir regalos reales.

---

## Arquitectura Multiplataforma

```text
├── app/                     # App Android nativa (Kotlin + Jetpack Compose + Material 3 + Media3 ExoPlayer + CameraX)
├── web/                     # Cliente Web React + Vite conectado al mismo Cloud Firestore
├── ios/                     # Paquete SwiftUI iOS conectado al mismo proyecto y base Firestore
├── functions/               # Firebase Cloud Functions (verificación server-side de pagos y retiros)
├── firestore.rules          # Reglas de seguridad zero-trust + atomicidad de regalos
├── firestore.test.js        # Suite de pruebas automatizadas contra Firestore Emulator
├── firebase-blueprint.json  # Esquema formal de entidades y colecciones
└── .github/workflows/ci.yml # CI/CD automático para Android, Firestore Rules y Web
```

---

## Funcionalidades Implementadas en Android (`app/`)

1. **Autenticación Real (`AuthHelper.kt`, `AuthScreen.kt`)**
   - Inicio de sesión con Google mediante Jetpack `CredentialManager` (`GetSignInWithGoogleOption` y auto sign-in con `GetGoogleIdOption`).
   - App Check configurado al inicio de `MainActivity.onCreate` (`DebugAppCheckProviderFactory` en debug, `RecaptchaAppCheckProviderFactory` en release).
   - Cierre de sesión seguro limpiando credenciales y reconectando estado reactivamente.

2. **Feed Vertical 9:16 (`FeedScreen.kt`, `VideoPlayerBox.kt`)**
   - `VerticalPager` con reproducción automática vía **Media3 ExoPlayer**, toque para pausar/reanudar, control de silencio persistido en DataStore y modo pantalla completa limpio.
   - Filtros **Para ti** (ranking por actividad real + Gemini) y **Siguiendo**.
   - Contadores reales de visualizaciones, likes, comentarios, compartir (`Intent.ACTION_SEND`), favoritos y denuncias.

3. **Grabación y Publicación (`PublishClipScreen.kt`)**
   - Grabación nativa de vídeo vertical con **CameraX `VideoCapture`**, selector de galería sin permisos amplios (**Android Photo Picker**) y URL HTTPS.
   - Sugerencias de descripción y hashtags con **Gemini AI (`gemini-3.8-flash`)** mediante salida JSON estructurada (`Schema.obj`).
   - Pre-moderación automática de seguridad antes de publicar.

4. **Sistema Completo de Regalos Virtuales y Billetera (`GiftsAndWalletScreen.kt`, `GiftSheetAndOverlay.kt`)**
   - Catálogo: **Rosa (5)**, **Corazón (15)**, **Diamante (50)**, **León (150)**, **Cohete (300)** y **Lobo Alfa Exclusivo (1000)**.
   - Envío desde clips, transmisiones en vivo y perfiles de creadores, con animación de destello en tiempo real.
   - Reparto verificado **70% creador / 30% plataforma** validado atómicamente en Firestore (`existsAfter` / `getAfter` en `firestore.rules`) impidiendo saldos negativos, auto-regalos o transacciones duplicadas.
   - Órdenes de compra de monedas y solicitudes de retiro listas para verificación por Cloud Functions.

5. **Transmisiones en Vivo (`LiveStreamScreen.kt`)**
   - Creación de directos, reproducción en vivo, chat en tiempo real, contador real de espectadores, envío de regalos durante el directo, bloqueo y denuncias.

6. **Mensajería Privada y Notificaciones (`InboxAndChatScreen.kt`)**
   - Chats 1-a-1 en tiempo real, adjunto de imágenes vía Photo Picker, indicador de lectura (`readBy`), bloqueo de usuarios y centro de notificaciones.

7. **Panel Administrativo Protegido (`AdminPanelScreen.kt`)**
   - Acceso exclusivo para administradores autorizados en `/admins/{uid}` o correo verificado de administración en las reglas de Firestore.
   - Gestión de denuncias, moderación de clips, cierre de directos, verificación/suspensión de cuentas y auditoría de transacciones.

---

## Cómo Compilar Cada Versión

### Android
```bash
# Pruebas unitarias + Robolectric
gradle :app:testDebugUnitTest

# Compilar APK Debug
gradle :app:assembleDebug
```
*(En CI con Gradle Wrapper estándar: `./gradlew :app:testDebugUnitTest` y `./gradlew :app:assembleDebug`.)*

### Reglas de Seguridad Firestore
```bash
firebase emulators:exec --only firestore "node --test firestore.test.js"
```

### Web (`web/`)
```bash
cd web
npm install
# Configura VITE_FIREBASE_API_KEY y VITE_FIREBASE_APP_ID en web/.env
npm run build
```

### iOS (`ios/`)
1. Abre `ios/Package.swift` en **Xcode 16+** (macOS).
2. Añade tu archivo `GoogleService-Info.plist` descargado desde Firebase Console al target principal.
3. Compila para simulador o dispositivo iOS 17+.

---

## Conexiones y Credenciales Pendientes para Producción

1. **Firebase Storage**: Habilitar Firebase Storage en Firebase Console para alojar binarios de vídeo/imágenes subidos desde dispositivo (actualmente el feed reproduce URLs HTTPS y URIs locales de prueba en dispositivo).
2. **Cloud Functions y Pagos Reales (`functions/`)**: Activar plan Blaze, desplegar `firebase deploy --only functions` y configurar secretos de **Google Play Developer API** y **Stripe** en Secret Manager para acreditar compras verificadas.
3. **Streaming RTMP/HLS Dedicado**: Conectar un servidor de ingesta/transcodificación (por ejemplo Mux, Livepeer o Wowza/Cloudflare Stream) para emitir señal HLS en vivo desde cámara hacia los espectadores.
4. **Proveedor Correo/Contraseña (Opcional)**: Habilitar *Email/Password* en Firebase Console > Authentication si deseas añadir registro clásico adicional a Google Sign-In.
5. **Credenciales Web (`web/.env`)**: Añadir `VITE_FIREBASE_API_KEY` y `VITE_FIREBASE_APP_ID` desde Firebase Console.
