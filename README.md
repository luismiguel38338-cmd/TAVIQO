# TAVIQO

> **El mundo es tu escenario.**

Red social internacional multiplataforma de vídeos cortos verticales **9:16**, transmisiones en vivo, comunidades, mensajería privada, regalos virtuales con comisión 70/30, panel administrativo protegido e inteligencia artificial auxiliar con **Gemini** (Firebase AI Logic).

## Identidad Visual
- **Negro Profundo**: `#05070B`
- **Azul Royal**: `#0F1F4B` / `#162B66`
- **Cyan Eléctrico**: `#00F0FF` / `#00C2FF`
- **Tipografía**: `Outfit` (títulos), `Plus Jakarta Sans` (cuerpo), `JetBrains Mono` (métricas/monedas)
- **Logotipo**: Marca geométrica original basada en una **T estilizada prismática** (`app_icon_taviqo.jpg` en `ic_launcher` y `TaviqoLogoMark` en Jetpack Compose / SVG Web).

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

2. **Feed Vertical 9:16 (`FeedScreen.kt`, `VideoPlayerBox.kt`)**
   - `VerticalPager` con reproducción automática vía **Media3 ExoPlayer**, toque para pausar/reanudar, control de silencio persistido en DataStore y modo pantalla completa limpio.
   - Filtros **Para ti** (ranking por actividad real + Gemini) y **Siguiendo**, más contadores reales de visualizaciones, likes, comentarios, compartir, favoritos y denuncias.

3. **Estudio y Publicación (`PublishClipScreen.kt`)**
   - Grabación nativa de vídeo vertical con **CameraX `VideoCapture`**, selector de galería sin permisos amplios (**Android Photo Picker**) y URL HTTPS.
   - Sugerencias de descripción y hashtags con **Gemini AI (`gemini-3.8-flash`)** mediante salida JSON estructurada (`Schema.obj`) y pre-moderación de seguridad.

4. **Sistema Completo de Regalos Virtuales y Billetera (`GiftsAndWalletScreen.kt`, `GiftSheetAndOverlay.kt`)**
   - Catálogo: **Rosa (5)**, **Corazón (15)**, **Diamante (50)**, **Corona Imperial (150)**, **Cohete (300)** y **Supernova Taviqo (1000)**.
   - Envío desde clips, transmisiones en vivo y perfiles de creadores, con animación en tiempo real y reparto **70% creador / 30% plataforma** validado atómicamente en `firestore.rules`.

5. **Directos, Mensajería Privada y Panel Admin (`LiveStreamScreen.kt`, `InboxAndChatScreen.kt`, `AdminPanelScreen.kt`)**
   - Salas en vivo con chat en tiempo real, espectadores reales y moderación.
   - Chats privados 1-a-1 con adjunto de imágenes (Photo Picker), estado de lectura (`readBy`), bloqueo y notificaciones.
   - Panel administrativo protegido en `/admins/{uid}` y correo verificado de administración.

---

## Cómo Compilar Cada Versión

### Android
```bash
# Pruebas unitarias + Robolectric
gradle :app:testDebugUnitTest

# Compilar APK Debug
gradle :app:assembleDebug
```

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
2. Añade `GoogleService-Info.plist` desde Firebase Console y compila para iOS 17+.

---

## Conexiones y Credenciales Pendientes para Producción
1. **Firebase Storage**: Habilitar Firebase Storage en Firebase Console para alojar binarios de vídeo/imágenes subidos desde dispositivo.
2. **Cloud Functions y Pagos Reales (`functions/`)**: Activar plan Blaze, desplegar `firebase deploy --only functions` y configurar secretos de **Google Play Developer API** y **Stripe** en Secret Manager.
3. **Streaming RTMP/HLS Dedicado**: Conectar un servidor de ingesta/transcodificación (Mux, Livepeer o Cloudflare Stream) para emitir señal HLS en vivo desde cámara.
4. **Proveedor Correo/Contraseña (Opcional)** y **Web/iOS**: Activar *Email/Password* en Firebase Console si se desea acceso clásico adicional, añadir `VITE_FIREBASE_*` en `web/.env` y `GoogleService-Info.plist` en Xcode.
