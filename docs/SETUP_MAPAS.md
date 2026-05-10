# Configuración del Servicio de Mapas

## Por qué el mapa no funciona al clonar el repo

El `MapaFragment` usa **Google Maps SDK** y **Google Places API**, que requieren una API key de Google Cloud. Esta key se guarda en `local.properties`, un archivo que **no está en git** (está en `.gitignore`) porque contiene rutas del sistema y credenciales específicas de cada equipo.

Cuando clonas el repo, ese archivo no existe → el mapa compila pero falla en runtime sin mensaje claro.

---

## Pasos para configurar la API key

### 1. Abre (o crea) el archivo `local.properties` en la raíz del proyecto

El archivo debe quedar así (ajusta la ruta del SDK a tu máquina):

```properties
sdk.dir=C\:\\Users\\TuUsuario\\AppData\\Local\\Android\\Sdk
MAPS_API_KEY=LA_API_KEY_DEL_PROYECTO
```

> Pídele la API key a Santiago o al responsable del proyecto de Google Cloud.

### 2. Sincroniza el proyecto en Android Studio

`File → Sync Project with Gradle Files`

---

## Problema de restricciones en Google Cloud Console

Las API keys de Google Maps pueden estar **restringidas por aplicación Android**: se vinculan al nombre del paquete (`com.example.rodapp`) más el **SHA-1 del keystore de debug**.

El keystore de debug es diferente en cada equipo, por lo que aunque tengas la key correcta, Google puede rechazar las peticiones desde tu máquina con el error:

```
com.google.android.gms.common.api.ApiException: 9011
```

o el mapa aparece vacío y el logcat muestra `API key not authorized`.

### Opciones para solucionarlo

**Opción A — Quitar la restricción Android (más fácil para desarrollo)**

1. Entra a [console.cloud.google.com](https://console.cloud.google.com)
2. Ve a **APIs & Services → Credentials**
3. Selecciona la API key del proyecto
4. En "Application restrictions" selecciona **None** (o "HTTP referrers")
5. Guarda

Esto permite que cualquier dispositivo use la key. Adecuado solo para desarrollo; antes de producción debe restringirse.

**Opción B — Agregar el SHA-1 de tu keystore de debug**

1. Ejecuta en tu terminal:
   ```bash
   # macOS / Linux
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

   # Windows
   keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
   ```
2. Copia el valor **SHA1**
3. En Google Cloud Console → Credentials → la API key → "Android apps" → agrega:
   - Package name: `com.example.rodapp`
   - SHA-1: el valor que copiaste

Repite esto para cada compañero que necesite usar el mapa.

**Opción C — Usar un debug keystore compartido**

Copiar el archivo `debug.keystore` del equipo donde ya funciona a `~/.android/debug.keystore` en cada equipo. Así todos usan el mismo certificado y la restricción de SHA-1 existente sigue siendo válida. No requiere cambios en Google Cloud.

---

## Dependencias de mapas en el proyecto

```toml
# gradle/libs.versions.toml
play-services-maps     = "19.0.0"   # Google Maps SDK
places                 = "4.0.0"    # Google Places API (búsqueda de lugares)
play-services-location = "21.3.0"   # FusedLocationProvider (ubicación actual)
```

---

## Checklist de verificación

- [ ] `local.properties` contiene `MAPS_API_KEY=...`
- [ ] Gradle sincronizado después de agregar la key
- [ ] La app tiene permisos de ubicación concedidos en el dispositivo
- [ ] La API key no tiene restricciones de SHA-1 que bloqueen tu dispositivo
- [ ] Las APIs **Maps SDK for Android** y **Places API** están habilitadas en el proyecto de Google Cloud
