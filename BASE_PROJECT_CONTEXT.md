# Base de Conocimiento: Proyecto Android Nativas — Materia Desarrollo de Apps Nativas (Ucompensar)

> Este documento sirve como referencia completa para agentes de código (Claude Code, Gemini CLI, etc.) al iniciar un nuevo proyecto Android que reutiliza la arquitectura, autenticación, base de datos y patrones de UI/UX establecidos en el proyecto base **RodApp**.
>
> **Leer antes de tocar cualquier archivo.** Todo lo documentado aquí se aplica al nuevo proyecto salvo que se indique explícitamente lo contrario.

---

## 1. Contexto Académico

- **Ruta de proyecto base:** C:\nativasUcompensar\RodApp
- **Materia:** Desarrollo de Aplicaciones Nativas en Android
- **Institución:** Ucompensar
- **Propósito del proyecto base (RodApp):** Plataforma de práctica progresiva donde se fueron incorporando UX/UI, autenticación, Supabase y biometría clase a clase.
- **Propósito del proyecto nuevo:** Aplicación real que aplica todo lo aprendido. Reutiliza la misma infraestructura Supabase, las mismas credenciales y los mismos patrones de código.

---

## 2. Stack Tecnológico

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje | Kotlin | 2.0.21 |
| Plataforma | Android | Min SDK 24 / Target+Compile SDK 36 |
| Build system | Gradle (Kotlin DSL `.kts`) | 8.13.2 |
| Backend / DB | Supabase | SDK 3.1.4 |
| ORM / Queries | Supabase Postgrest-kt | (incluido en BOM) |
| Autenticación | Supabase Auth-kt | (incluido en BOM) |
| Cliente HTTP | Ktor Android | 3.1.3 |
| Serialización | kotlinx-serialization-json | 1.6.3 |
| UI | XML Layouts + ViewBinding | — |
| Design System | Material Design 3 (Material3) | 1.10.0 |
| Navegación | Single Activity + Fragments | Manual (sin Navigation Component) |
| Biometría | androidx.biometric | 1.1.0 |
| Almacenamiento seguro | androidx.security-crypto (EncryptedSharedPreferences) | 1.1.0 |
| JVM Target | Java 11 | — |

---

## 3. Credenciales Supabase (Reutilizar en el proyecto nuevo)

> Las mismas credenciales del proyecto base se usan en el proyecto nuevo. El proyecto Supabase ya tiene las tablas y la configuración de OAuth configuradas.

```
Supabase URL:  https://erttcudseqjrpyathmal.supabase.co
Supabase Anon Key: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVydHRjdWRzZXFqcnB5YXRobWFsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzU1MTUwMzUsImV4cCI6MjA5MTA5MTAzNX0.zmyADFrL2iPvGMhy9eU2SQ7VWZsF_h2E2D86RpMzgWA
Supabase Project ID: erttcudseqjrpyathmal
```

> **Nota para el agente:** Estas credenciales están hardcodeadas en el proyecto base (decisión académica). En el proyecto nuevo se mantiene el mismo enfoque por simplicidad de la materia. No proponer ni implementar gestión de secretos con `local.properties` / `BuildConfig` salvo que el usuario lo pida explícitamente.

---

## 4. Estructura de Carpetas (Template)

```
app/src/main/java/com/example/<appname>/
├── SupabaseClient.kt            ← Singleton de inicialización Supabase
├── SecurityManager.kt           ← Biometría y EncryptedSharedPreferences
├── activities/
│   ├── SplashActivity.kt        ← Launcher. Verifica sesión. 3s delay.
│   ├── OnboardingActivity.kt    ← Bienvenida. Botón → LoginActivity
│   ├── LoginActivity.kt         ← Auth hub: Email, Google, Biometría
│   └── RegisterActivity.kt      ← Registro + inserción en tabla 'users'
└── main/
    ├── MainActivity.kt          ← Single Activity. DrawerLayout + BottomNav
    ├── admin/
    │   ├── AdminFragment.kt
    │   └── UsuarioFragment.kt
    ├── perfil/
    │   └── PerfilFragment.kt    ← Editor de perfil + toggle biometría
    └── <dominio>/               ← Reemplazar con los fragments de negocio
        ├── HomeFragment.kt
        ├── <Entidad>.kt          ← Data class serializable
        └── <Entidad>Adapter.kt  ← RecyclerView.Adapter

app/src/main/res/
├── layout/
│   ├── activity_splash.xml
│   ├── activity_onboarding.xml
│   ├── activity_login.xml
│   ├── activity_register.xml
│   ├── activity_main.xml        ← DrawerLayout root
│   ├── fragment_home.xml
│   ├── fragment_perfil.xml
│   └── item_<entidad>.xml       ← Item card para RecyclerView
├── menu/
│   ├── bottom_nav_menu.xml      ← Items del BottomNavigationView
│   └── nav_drawer_menu.xml      ← Items del NavigationView lateral
├── values/
│   ├── colors.xml
│   ├── strings.xml
│   └── themes.xml
├── drawable/                    ← Iconos, logos, backgrounds
└── xml/
    ├── backup_rules.xml
    └── data_extraction_rules.xml
```

---

## 5. Configuración Gradle

### `gradle/libs.versions.toml` (Version Catalog)

```toml
[versions]
agp = "8.13.2"
kotlin = "2.0.21"
coreKtx = "1.15.0"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
appcompat = "1.6.1"
material = "1.10.0"
activity = "1.10.0"
constraintlayout = "2.1.4"
lifecycleRuntimeKtx = "2.8.7"
supabase = "3.1.4"
ktor = "3.1.3"
serialization = "1.6.3"
biometric = "1.1.0"
securityCrypto = "1.1.0"

[libraries]
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
androidx-activity = { group = "androidx.activity", name = "activity", version.ref = "activity" }
androidx-constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "constraintlayout" }
supabase-bom = { module = "io.github.jan-tennert.supabase:bom", version.ref = "supabase" }
supabase-postgrest = { module = "io.github.jan-tennert.supabase:postgrest-kt" }
supabase-auth = { module = "io.github.jan-tennert.supabase:auth-kt" }
ktor-bom = { module = "io.ktor:ktor-bom", version.ref = "ktor" }
ktor-client-android = { module = "io.ktor:ktor-client-android" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
androidx-biometric = { group = "androidx.biometric", name = "biometric", version.ref = "biometric" }
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

### `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.<appname>"   // ← cambiar por el paquete del nuevo proyecto
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.<appname>"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true   // OBLIGATORIO — siempre habilitado
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Supabase (BOM controla versiones)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)

    // Ktor (BOM controla versiones)
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.client.android)

    // Serialización
    implementation(libs.kotlinx.serialization.json)

    // Biometría y almacenamiento seguro
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)
}
```

---

## 6. AndroidManifest.xml — Configuración Obligatoria

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Permisos siempre necesarios -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.<AppName>">

        <!-- LAUNCHER: SplashActivity -->
        <activity
            android:name=".activities.SplashActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".activities.OnboardingActivity"
            android:exported="false" />

        <!-- CRÍTICO: LoginActivity debe tener el intent-filter del deep link OAuth -->
        <activity
            android:name=".activities.LoginActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <!-- El scheme y host deben coincidir con los configurados en SupabaseClient -->
                <data android:scheme="rodapp" android:host="login" />
            </intent-filter>
        </activity>

        <activity
            android:name=".activities.RegisterActivity"
            android:exported="false"
            android:windowSoftInputMode="adjustResize" />

        <activity
            android:name=".main.MainActivity"
            android:exported="false" />

    </application>
</manifest>
```

**Puntos críticos del Manifest:**
- `SplashActivity` siempre es el `LAUNCHER`.
- `LoginActivity` tiene `launchMode="singleTop"` para evitar instancias duplicadas al retornar del OAuth.
- El `intent-filter` con `scheme="rodapp"` y `host="login"` es lo que permite que el redirect de Google SSO vuelva a la app. **Si cambia el scheme en `SupabaseClient`, debe cambiar aquí también.**

---

## 7. SupabaseClient.kt — Inicialización del Cliente

Archivo en: `com.example.<appname>/SupabaseClient.kt`

```kotlin
package com.example.<appname>

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://erttcudseqjrpyathmal.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVydHRjdWRzZXFqcnB5YXRobWFsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzU1MTUwMzUsImV4cCI6MjA5MTA5MTAzNX0.zmyADFrL2iPvGMhy9eU2SQ7VWZsF_h2E2D86RpMzgWA"
    ) {
        install(Auth) {
            scheme = "rodapp"   // Debe coincidir con el intent-filter del Manifest
            host = "login"      // Debe coincidir con el intent-filter del Manifest
        }
        install(Postgrest)
    }
}
```

**Uso desde cualquier Activity/Fragment:**
```kotlin
// Leer sesión actual
val user = SupabaseClient.client.auth.currentUserOrNull()

// Query a tabla
val result = SupabaseClient.client.postgrest.from("tabla").select { filter { eq("id", user.id) } }

// Insert
SupabaseClient.client.postgrest.from("tabla").insert(dataObject)

// Upsert
SupabaseClient.client.postgrest.from("tabla").upsert(dataObject)
```

---

## 8. SecurityManager.kt — Biometría y Almacenamiento Cifrado

Archivo en: `com.example.<appname>/SecurityManager.kt`

```kotlin
package com.example.<appname>

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurityManager {

    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_PASSWORD = "user_password"

    private fun getEncryptedPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isBiometricEnabled(context: Context): Boolean =
        getEncryptedPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getEncryptedPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun saveCredentials(context: Context, email: String, password: String?) {
        val editor = getEncryptedPrefs(context).edit().putString(KEY_USER_EMAIL, email)
        if (password != null) editor.putString(KEY_USER_PASSWORD, password)
        else editor.remove(KEY_USER_PASSWORD)
        editor.apply()
    }

    fun getCredentials(context: Context): Pair<String?, String?> {
        val prefs = getEncryptedPrefs(context)
        return Pair(prefs.getString(KEY_USER_EMAIL, null), prefs.getString(KEY_USER_PASSWORD, null))
    }

    fun clearCredentials(context: Context) {
        getEncryptedPrefs(context).edit()
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PASSWORD)
            .remove(KEY_BIOMETRIC_ENABLED)
            .apply()
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Autenticación biométrica",
        subtitle: String = "Ingresa con tu huella digital",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
                override fun onAuthenticationFailed() { /* silent, usuario puede reintentar */ }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
```

---

## 9. Flujo de Autenticación Completo

### 9.1 Diagrama de flujo

```
App abre
    └─ SplashActivity (3 segundos)
         ├─ currentUserOrNull() != null  ──→  MainActivity
         └─ null                         ──→  OnboardingActivity
                                                  └─ botón "Comienza"  ──→  LoginActivity

LoginActivity
    ├─ [Huella habilitada?]
    │   └─ SÍ: muestra BiometricPrompt automáticamente al abrir
    │          ├─ Éxito + tiene password guardada  → performLogin() con credenciales cifradas
    │          └─ Éxito + sin password (Google SSO) → navigateToMain() directo
    │
    ├─ Email + Password
    │   └─ signInWith(Email) → SessionStatus.Authenticated (isNew=true)
    │        ├─ Biometría NO habilitada + disponible → showEnableBiometricDialog()
    │        └─ Ya habilitada → navigateToMain()
    │
    ├─ Google Sign-In
    │   └─ signInWith(Google, redirectUrl="rodapp://login")
    │        → Abre navegador / selector de cuentas de Google
    │        → Redirect a rodapp://login#access_token=...
    │        → onNewIntent() / onCreate() captura intent
    │        → handleDeeplinks(intent)
    │        → SessionStatus.Authenticated (isNew=true)
    │        → showEnableBiometricDialog() si no tiene huella
    │
    └─ "No tienes cuenta?" → RegisterActivity
              └─ signUpWith(Email) + insertar en tabla 'users'
                    └─ → LoginActivity

navigateToMain()
    → Intent(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK)
    → MainActivity
    → finish() [limpia el back stack]
```

### 9.2 Observer de SessionStatus en LoginActivity

Este patrón es **crítico para el Google SSO**. La sesión se detecta de forma reactiva:

```kotlin
// En LoginActivity.onCreate(), ANTES de setContentView()
private fun observeSessionStatus() {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            SupabaseClient.client.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        if (!isNavigating) {
                            if (status.isNew) {
                                isNavigating = true
                                // Sesión nueva: ofrecer huella o ir directo
                                if (!SecurityManager.isBiometricEnabled(this@LoginActivity)) {
                                    showEnableBiometricDialog(user?.email ?: "", null)
                                } else {
                                    navigateToMain()
                                }
                            } else {
                                // Sesión restaurada (app reiniciada): solo entrar si huella NO está habilitada
                                if (!SecurityManager.isBiometricEnabled(this@LoginActivity)) {
                                    isNavigating = true
                                    navigateToMain()
                                }
                                // Si huella está habilitada, esperar a que el usuario la use
                            }
                        }
                    }
                    is SessionStatus.RefreshFailure -> { /* mostrar error de red */ }
                    else -> {}
                }
            }
        }
    }
}
```

### 9.3 Deep Link handling (Google SSO redirect)

```kotlin
// LoginActivity debe tener launchMode="singleTop" en el Manifest

override fun onCreate(savedInstanceState: Bundle?) {
    // Llamar ANTES de setContentView
    handleAuthRedirect(intent)
    // ...
}

override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleAuthRedirect(intent)
}

private fun handleAuthRedirect(intent: Intent?) {
    val data = intent?.dataString
    if (data != null && (data.contains("#access_token=") || data.contains("error_description="))) {
        try {
            SupabaseClient.client.handleDeeplinks(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error de retorno: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
```

### 9.4 Google Sign-In (lanzar)

```kotlin
private fun performGoogleLogin() {
    lifecycleScope.launch {
        try {
            // queryParams["prompt"] = "select_account" fuerza el selector de cuentas
            SupabaseClient.client.auth.signInWith(Google, redirectUrl = "rodapp://login") {
                queryParams["prompt"] = "select_account"
            }
        } catch (e: Exception) {
            Toast.makeText(this@LoginActivity, "Error al abrir Google: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
```

### 9.5 Logout

```kotlin
private fun performLogout() {
    lifecycleScope.launch {
        try {
            SupabaseClient.client.auth.signOut()
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        } catch (e: Exception) {
            Toast.makeText(this@MainActivity, "Error al cerrar sesión", Toast.LENGTH_SHORT).show()
        }
    }
}
```

---

## 10. Base de Datos Supabase — Tablas

### Tabla `users`
Almacena el perfil público del usuario. Vinculada a `auth.users` de Supabase por el campo `id`.

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | `uuid` | PK, mismo UUID que `auth.users.id` |
| `name` | `text` | Primer nombre |
| `lastname` | `text` | Apellido |

### Data Class correspondiente

```kotlin
// En activities/RegisterActivity.kt (o en un archivo de modelos separado)
@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val lastname: String
)
```

### Operaciones CRUD sobre `users`

```kotlin
// INSERT (durante registro)
val userProfile = UserProfile(id = userId, name = firstName, lastname = lastName)
SupabaseClient.client.postgrest.from("users").insert(userProfile)

// SELECT (cargar perfil)
val profile = SupabaseClient.client.postgrest.from("users")
    .select { filter { eq("id", user.id) } }
    .decodeSingle<UserProfile>()

// UPSERT (guardar cambios de perfil)
SupabaseClient.client.postgrest.from("tabla").upsert(updatedProfile)
```

### Agregar nuevas tablas al proyecto nuevo

Seguir el mismo patrón:
1. Crear la tabla en el dashboard de Supabase.
2. Crear la `@Serializable data class` con los mismos nombres de columna (snake_case si aplica, o configurar `@SerialName`).
3. Operar con `SupabaseClient.client.postgrest.from("nombre_tabla")`.

---

## 11. Modelos de Datos

### Reglas para data classes

- Siempre anotar con `@Serializable` (de `kotlinx.serialization`).
- Los nombres de los campos deben coincidir exactamente con los nombres de las columnas en Supabase (o usar `@SerialName("nombre_columna")`).
- Ubicar los modelos en el paquete donde se usan o en un subpaquete `model/` si el proyecto crece.

```kotlin
@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val lastname: String
)
```

---

## 12. Navegación — Single Activity Pattern

### Estructura en `MainActivity`

- Root layout: `DrawerLayout`
- Dentro del drawer: `CoordinatorLayout` o `LinearLayout` con `Toolbar` + `FragmentContainerView` + `BottomNavigationView`
- Drawer lateral: `NavigationView` (anclado a `GravityCompat.START`)

### Reemplazar fragments

```kotlin
private fun replaceFragment(fragment: Fragment) {
    supportFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, fragment)
        .commit()
}
```

### Sincronización BottomNav ↔ Drawer

```kotlin
// BottomNav selecciona → actualiza Drawer
binding.bottomNav.setOnItemSelectedListener { item ->
    val fragment = when (item.itemId) {
        R.id.nav_home -> HomeFragment()
        R.id.nav_catalogo -> CatalogoFragment()
        // ... más items
        else -> null
    }
    fragment?.let {
        replaceFragment(it)
        binding.navView.setCheckedItem(item.itemId)  // sincronizar drawer
        true
    } ?: false
}

// Drawer selecciona → delega al BottomNav (para items compartidos)
binding.navView.setNavigationItemSelectedListener { item ->
    when (item.itemId) {
        R.id.nav_logout -> performLogout()
        R.id.nav_perfil -> { replaceFragment(PerfilFragment()); uncheckBottomNav() }
        else -> binding.bottomNav.selectedItemId = item.itemId  // delegar
    }
    binding.drawerLayout.closeDrawer(GravityCompat.START)
    true
}
```

### Quitar selección del BottomNav (para fragments que no están en él)

```kotlin
private fun uncheckBottomNav() {
    binding.bottomNav.menu.setGroupCheckable(0, true, false)
    for (i in 0 until binding.bottomNav.menu.size()) {
        binding.bottomNav.menu.getItem(i).isChecked = false
    }
    binding.bottomNav.menu.setGroupCheckable(0, true, true)
}
```

### Hamburger menu — ícono visible en toolbar

```kotlin
val toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.inicio, R.string.inicio)
toggle.drawerArrowDrawable.color = resources.getColor(R.color.white, theme)  // ícono blanco
binding.drawerLayout.addDrawerListener(toggle)
toggle.syncState()
```

### Back button cierra el drawer si está abierto

```kotlin
override fun onBackPressed() {
    if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    } else {
        super.onBackPressed()
    }
}
```

### Edge-to-Edge en MainActivity

```kotlin
ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { _, insets ->
    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    val lp = binding.toolbar.layoutParams as ViewGroup.MarginLayoutParams
    lp.topMargin = systemBars.top          // toolbar respeta status bar
    binding.toolbar.layoutParams = lp
    binding.bottomNav.setPadding(0, 0, 0, systemBars.bottom)  // nav bar
    insets
}
```

---

## 13. ViewBinding — Convención de Uso

ViewBinding está **siempre habilitado**. Nunca usar `findViewById` en Activities principales.

```kotlin
// En Activity
private lateinit var binding: ActivityMainBinding

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    // Acceso: binding.toolbar, binding.bottomNav, etc.
}

// En Fragment
private var _binding: FragmentHomeBinding? = null
private val binding get() = _binding!!

override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    _binding = FragmentHomeBinding.inflate(inflater, container, false)
    return binding.root
}

override fun onDestroyView() {
    super.onDestroyView()
    _binding = null  // evitar memory leaks
}
```

> En los fragments del proyecto base se usa `inflater.inflate(R.layout.fragment_x, container, false)` con `findViewById` directo (estilo antiguo). En el proyecto nuevo, **usar siempre ViewBinding** en fragments también.

---

## 14. Coroutines — Patrón de Uso

Todas las operaciones de red (Supabase) son suspend functions. Siempre ejecutar dentro de un scope:

```kotlin
// En Activity
lifecycleScope.launch {
    try {
        // operación suspend
    } catch (e: Exception) {
        // manejo de error
    }
}

// En Fragment
viewLifecycleOwner.lifecycleScope.launch {  // preferir viewLifecycleOwner en fragments
    try {
        // operación suspend
    } catch (e: Exception) {
        // manejo de error
    }
}
```

**Nunca** lanzar coroutines sin manejo de excepciones cuando se interactúa con Supabase.

---

## 15. RecyclerView + Adapter — Patrón Base

### Data class del item

```kotlin
data class <Entidad>(
    val nombre: String,
    val precio: Double,
    val descripcion: String,
    val imageRes: Int   // Si la imagen es un drawable local
    // Para imágenes de URL, usar String y cargar con Glide/Coil
)
```

### Adapter

```kotlin
class <Entidad>Adapter(private val items: List<Entidad>) :
    RecyclerView.Adapter<<Entidad>Adapter.<Entidad>ViewHolder>() {

    inner class <Entidad>ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imagen: ImageView = itemView.findViewById(R.id.image_item)
        val nombre: TextView = itemView.findViewById(R.id.tv_nombre)
        val precio: TextView = itemView.findViewById(R.id.tv_precio)
        val botonAccion: Button = itemView.findViewById(R.id.btn_accion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): <Entidad>ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_<entidad>, parent, false)
        return <Entidad>ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: <Entidad>ViewHolder, position: Int) {
        val item = items[position]
        holder.nombre.text = item.nombre
        holder.precio.text = "$${item.precio}"
        holder.imagen.setImageResource(item.imageRes)
        holder.botonAccion.setOnClickListener { /* lógica */ }
    }
}
```

### Usar en Fragment

```kotlin
val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_items)
recyclerView.adapter = <Entidad>Adapter(listaItems)
recyclerView.layoutManager = LinearLayoutManager(requireContext())
```

---

## 16. Perfil de Usuario — PerfilFragment

Permite editar nombre, apellido, email, contraseña y el toggle de huella.

**Operaciones:**
1. `loadUserProfile()`: lee `auth.currentUserOrNull()` para el email y hace `SELECT` en tabla `users` para nombre/apellido.
2. `saveChanges()`: hace `upsert` en tabla `users`, llama `auth.updateUser { email }` si cambió el email, llama `auth.updateUser { password }` si se ingresó nueva contraseña.
3. Si huella está activa y cambia email/password, actualiza `SecurityManager.saveCredentials()` en consecuencia.

---

## 17. Sistema de Temas y Colores

### Tema base (`themes.xml`)

```xml
<style name="Base.Theme.<AppName>" parent="Theme.Material3.DayNight.NoActionBar">
    <!-- Material3 Day/Night, sin ActionBar nativa (se usa Toolbar manual) -->
</style>

<style name="Theme.<AppName>" parent="Base.Theme.<AppName>" />
```

**Siempre usar `Theme.Material3.DayNight.NoActionBar`** como padre. La app usa `Toolbar` manual configurada como `ActionBar` con `setSupportActionBar()`.

### Paleta de colores del proyecto base

```xml
<!-- Fondo principal de la app -->
<color name="splash_background_color">#7289cd</color>

<!-- Fondo de pantallas de auth -->
<color name="login_background_color">#7289cd</color>
<color name="onboarding_background_color">#7289cd</color>

<!-- Texto sobre fondos claros -->
<color name="login_text_color">#0d0d0d</color>
<color name="onboarding_text_color">#0d0d0d</color>

<!-- Fondo de inputs y botones principales -->
<color name="login_input_background_color">#FBF8F8</color>
<color name="login_button_background_color">#FBF8F8</color>
<color name="google_button_background_color">#FBF8F8</color>

<!-- Texto de botones -->
<color name="login_button_text_color">#000000</color>
<color name="google_button_text_color">#000000</color>

<!-- Links y acentos -->
<color name="recover_password_text_color">#00FFFF</color>
<color name="register_link_text_color">#00FFFF</color>
<color name="register_text_color">#00FFFF</color>

<!-- Texto sobre fondo azul -->
<color name="no_account_text_color">#FBF8F8</color>
<color name="color_bienvenidaText">#FBF8F8</color>
```

> En el proyecto nuevo se pueden cambiar estos colores pero mantener la estructura de nombres.

---

## 18. Strings — Convenciones

- **Todos los textos visibles al usuario van en `strings.xml`**, sin hardcodear strings en código Kotlin/XML de layouts.
- **Idioma base: Español (es)** — el proyecto base está completamente en español.
- Nombrar strings en formato `snake_case` descriptivo.
- Usar prefijos por sección: `login_`, `register_`, `perfil_`, etc.

---

## 19. SplashActivity — Patrón

```kotlin
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {
            delay(3000)
            val currentUser = SupabaseClient.client.auth.currentUserOrNull()
            if (currentUser != null) {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                startActivity(Intent(this@SplashActivity, OnboardingActivity::class.java))
            }
            finish()
        }
    }
}
```

---

## 20. Manejo de Errores — Convenciones

Mapear los mensajes de Supabase a mensajes en español amigables:

```kotlin
} catch (e: Exception) {
    val errorMsg = when {
        e.message?.contains("Invalid login credentials", ignoreCase = true) == true ->
            "Correo o contraseña incorrectos"
        e.message?.contains("User already registered", ignoreCase = true) == true ->
            "Este correo ya está registrado"
        e.message?.contains("network", ignoreCase = true) == true ->
            "Error de conexión. Revisa tu internet"
        else -> "Error inesperado. Intenta de nuevo."
    }
    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
}
```

---

## 21. Validaciones de Formulario — Convenciones

```kotlin
// Email
if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
    Toast.makeText(this, "Email no válido", Toast.LENGTH_SHORT).show()
    return
}

// Password mínimo
if (password.length < 6) {
    Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
    return
}

// Campos vacíos
if (name.isEmpty() || email.isEmpty()) {
    Toast.makeText(this, "Por favor llena los campos obligatorios", Toast.LENGTH_SHORT).show()
    return
}
```

---

## 22. Edge-to-Edge — Patrón Estándar

Todas las activities llaman `enableEdgeToEdge()` en `onCreate()` y gestionan los insets:

```kotlin
enableEdgeToEdge()
setContentView(R.layout.activity_x)

ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
    insets
}
```

---

## 23. Convenciones de Nomenclatura

| Elemento | Convención | Ejemplo |
|---|---|---|
| Activities | `PascalCase` + sufijo `Activity` | `LoginActivity` |
| Fragments | `PascalCase` + sufijo `Fragment` | `HomeFragment` |
| Data classes | `PascalCase` | `UserProfile`, `Product` |
| Adapters | `PascalCase` + sufijo `Adapter` | `ProductoAdapter` |
| Layouts de Activity | `activity_<nombre>.xml` | `activity_login.xml` |
| Layouts de Fragment | `fragment_<nombre>.xml` | `fragment_home.xml` |
| Layouts de items | `item_<nombre>.xml` | `item_producto.xml` |
| IDs en XML (views) | `camelCase` con prefijo de tipo | `editTextEmail`, `buttonLogin`, `recyclerProductos` |
| Variables privadas en clase | `camelCase` | `editTextEmail`, `binding` |
| Constantes en `object` | `SCREAMING_SNAKE_CASE` | `PREFS_NAME`, `KEY_USER_EMAIL` |
| Paquetes | `lowercase` | `activities`, `perfil`, `productos` |

---

## 24. Estado Actual del Proyecto Base (Features Implementadas vs Pendientes)

### Implementado ✓
- Splash con verificación de sesión Supabase
- Onboarding
- Login con Email/Password
- Login con Google SSO (OAuth via Supabase)
- Registro con Email + inserción en tabla `users`
- Biometría (huella) para login rápido
- Almacenamiento cifrado de credenciales (EncryptedSharedPreferences AES256)
- Sincronización de credenciales cifradas cuando cambia email/password
- MainActivity con DrawerLayout + BottomNavigationView + 6 Fragments
- PerfilFragment con edición de datos + toggle de huella
- RecyclerView con adapter y data class para productos (datos mock)
- EdgeToEdge en todas las pantallas
- ViewBinding en MainActivity

### No implementado (esqueleto vacío) ✗
- Lógica de carrito de compras
- Lógica de favoritos
- Panel de administración
- Carga de imágenes desde URL (actualmente usa `R.drawable`)
- Paginación de listas
- Búsqueda / filtros en catálogo

---


*Documento generado el 2026-05-03 a partir del análisis completo del proyecto base RodApp (commit `e5ede89`). Refleja el estado de la rama `main` en ese punto.*
