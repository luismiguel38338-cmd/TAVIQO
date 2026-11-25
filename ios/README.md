# TAVIQO — Cliente iOS (SwiftUI)

> **El mundo es tu escenario.**

Este módulo contiene el paquete Swift (`Package.swift`) y la aplicación base SwiftUI (`Sources/LoboClipsApp.swift`) conectada al mismo proyecto Firebase (`gen-lang-client-0174699991`) y base de datos Cloud Firestore (`ai-studio-android-loboclip-f031fd05-ba55-4b71-8396-2e4f1629a0fa`).

## Requisitos para compilar en macOS / Xcode
1. Xcode 16+ e iOS 17+ SDK.
2. Descarga `GoogleService-Info.plist` para el Bundle ID de iOS desde **Firebase Console**.
3. Activa **Sign in with Google** (`GoogleSignIn-iOS`) y **Firebase App Check** (`AppAttestProviderFactory` / `DeviceCheckProviderFactory`).
4. Abre `ios/Package.swift` en Xcode o intégralo como paquete local en tu proyecto `.xcodeproj` y ejecuta en simulador o iPhone físico.
