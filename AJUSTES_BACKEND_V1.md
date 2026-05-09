# Ajustes Backend V1 — RodApp

**Fecha:** 2026-05-04  
**Rama:** `santiago`  
**Proyecto Supabase:** `erttcudseqjrpyathmal` (us-east-1)

---

## Qué se implementó

### 1. Base de datos (Supabase / PostgreSQL)

Se creó el schema completo de la aplicación mediante una migración aplicada directamente al proyecto de Supabase.

**Tablas creadas (8):**

| Tabla | Propósito |
|---|---|
| `users` | Ampliada con `updated_at`. Extiende Supabase Auth usando `role = 'client' \| 'admin'` |
| `user_preferences` | Preferencias por usuario: notificaciones, tema, unidades, biometría |
| `motos` | Motocicletas del usuario (múltiples por cuenta). Placa con restricción `UNIQUE` |
| `soat` | Historial de pólizas SOAT por moto (cada renovación es un nuevo registro) |
| `rtm` | Historial de revisiones técnico-mecánicas por moto |
| `registros_combustible` | Tanqueos con tipo, costo, kilometraje y coordenadas GPS opcionales |
| `registros_mantenimiento` | Mantenimientos con tipo, fecha, km, intervalo de repetición y notas |
| `documentos` | Documentos adicionales: `LICENCIA`, `SEGURO_TODO_RIESGO` o `PERSONALIZADO` |

**Vista:**
- `v_historial` — UNION de combustible + mantenimiento + documentos, ordenado por fecha descendente, para alimentar `HistorialFragment`.

**Seguridad:**
- RLS habilitado en las 8 tablas.
- Cada usuario solo accede a sus propios datos via políticas `USING (auth.uid() = user_id)` o `USING (moto_es_propia(moto_id))`.
- Función helper `is_admin()` permite que usuarios con `role = 'admin'` lean todos los registros (panel admin).
- Función helper `moto_es_propia(uuid)` centraliza la verificación de propiedad para tablas hijas.

**Trigger:**
- `trg_new_user_preferences` — crea automáticamente una fila en `user_preferences` cada vez que se inserta un usuario en `users`.

**Índices:**
- `idx_motos_user_id` — para cargar motos del usuario rápidamente.
- `idx_soat_moto_vencimiento`, `idx_rtm_moto_vencimiento`, `idx_documentos_moto_vencimiento` — para alertas de vencimiento.
- `idx_combustible_moto_created`, `idx_mantenimiento_moto_fecha` — para historial ordenado.

---

### 2. Supabase Storage

Se crearon 3 buckets con sus políticas de acceso:

| Bucket | Acceso | Límite | Tipos |
|---|---|---|---|
| `avatars` | Público (lectura) | 5 MB | jpg, png, webp |
| `rtm-docs` | Privado | 5 MB | jpg, png |
| `documentos-adjuntos` | Privado | 10 MB | jpg, png, pdf |

Convención de paths: `{user_id}/{moto_id}/{record_id}.{ext}` — el primer segmento es el `user_id`, lo que permite que las políticas RLS de Storage validen la propiedad sin consultar la base de datos.

---

### 3. Capa de modelos Kotlin (`models/Models.kt`)

Se crearon data classes `@Serializable` para cada tabla:

- `Moto` — con `@EncodeDefault(NEVER)` en el campo `id` para que el campo no se incluya en el JSON de insert (evita conflicto con el `DEFAULT gen_random_uuid()` de Postgres).
- `SoatInsert`, `RtmInsert`, `CombustibleInsert`, `MantenimientoInsert`, `DocumentoInsert` — clases de solo-insert sin campo `id`.
- `UsuarioInfo` — para leer nombre y correo del usuario autenticado.

---

### 4. SharedViewModel (`SharedViewModel.kt`)

ViewModel con scope de `Activity` (accedido con `activityViewModels()`) que persiste el contexto de la moto activa entre fragments:

```
motoId: String?     — UUID de la moto registrada/seleccionada
motoNombre: String? — "Marca Modelo" para mostrar en UI
```

**Flujo del dato:**  
`RegistroMotoFragment` (inserta moto) → guarda en `SharedViewModel` → `RegistroSOATFragment`, `RegistroRTMFragment`, `CombustibleFragment`, `MantenimientoFragment`, `NuevoDocumentoFragment` (leen `motoId` para asociar registros).

---

### 5. Recursos Android

**`res/values/arrays.xml`** (archivo nuevo):  
Arrays para los 4 spinners de la app:
- `marcas_moto` — 15 marcas más "Seleccionar marca" como posición 0 (usada para validar)
- `aseguradoras_soat` — 11 aseguradoras más placeholder en posición 0
- `tipos_gasolina` — Regular, Premium, Diésel, Extra (mapeados a `REGULAR | PREMIUM | DIESEL | EXTRA` en DB)
- `tipos_mantenimiento` — 11 tipos de servicio

**`res/values/strings.xml`** (11 strings nuevos):  
Mensajes de éxito/error y labels para los nuevos flujos: `moto_registrada`, `soat_guardado`, `rtm_guardada`, `combustible_registrado`, `mantenimiento_guardado`, `documento_guardado`, `error_placa_duplicada`, `error_fecha_requerida`, `btn_ver_documentos`, `label_placa_formato`, `label_seleccionar_fecha`.

---

### 6. Fragments conectados al backend

Todos los fragments pasaron de ser stubs de navegación a tener lógica real de persistencia:

| Fragment | Implementación |
|---|---|
| `register_user` | Agrega `correo` al insert en `users` (campo faltante) |
| `GarajeFragment` | Al cargar, consulta `motos` en Supabase. Si existe una moto, actualiza la UI con nombre/placa y cambia el botón a "Ver Documentos" |
| `RegistroMotoFragment` | Configura spinner de marcas, valida campos, inserta en `motos`, obtiene el `id` generado por query de placa (UNIQUE), guarda en SharedViewModel y navega a `GarajeDocumentosFragment` |
| `RegistroSOATFragment` | Spinner de aseguradoras, date pickers con `MaterialDatePicker`, convierte `MM/dd/yyyy` → `yyyy-MM-dd` para DB, inserta en `soat` |
| `RegistroRTMFragment` | Date pickers (expedición y vencimiento), inserta en `rtm` |
| `CombustibleFragment` | Spinner de tipos de gasolina con mapeo a constantes DB (`REGULAR`, etc.), inserta en `registros_combustible` |
| `MantenimientoFragment` | Spinner de tipos, date picker, inserta en `registros_mantenimiento` con `repetir_cada_km` y `notas` opcionales |
| `NuevoDocumentoFragment` | Date picker opcional para vencimiento, inserta en `documentos` con `tipo = PERSONALIZADO` |
| `PerfilFragment` | Carga `name` del usuario desde la tabla `users` y lo muestra en `txt_nombre_perfil` |

---

## Cómo funciona (arquitectura del flujo de datos)

```
Supabase Auth
    └── Usuario autenticado (auth.uid())
            │
            ▼
    register_user.kt  ──insert──▶  users (name, lastname, correo)
                                         │ trigger
                                         ▼
                                   user_preferences (auto-creado)
            │
            ▼
    RegistroMotoFragment ──insert──▶ motos (user_id, marca, modelo, placa, ...)
            │                              │
            │ guarda moto.id               │
            ▼                              │
    SharedViewModel.motoId ◀──────────────┘
            │
            ├──▶ RegistroSOATFragment ──insert──▶ soat (moto_id, poliza, fechas)
            ├──▶ RegistroRTMFragment  ──insert──▶ rtm  (moto_id, certificado, fechas)
            ├──▶ CombustibleFragment  ──insert──▶ registros_combustible
            ├──▶ MantenimientoFragment──insert──▶ registros_mantenimiento
            └──▶ NuevoDocumentoFragment──insert──▶ documentos (tipo=PERSONALIZADO)

GarajeFragment ──select──▶ motos WHERE user_id = auth.uid()
    └── Si existe moto: actualiza UI + guarda en SharedViewModel

PerfilFragment ──select──▶ users WHERE id = auth.uid()
    └── Muestra nombre en txt_nombre_perfil
```

---

## Pendiente (fuera de este ajuste)

- `DocumentosAdicionalesFragment` — Licencia y Seguro Todo Riesgo (mismo patrón: insert en `documentos` con tipo `LICENCIA` o `SEGURO_TODO_RIESGO`)
- `HistorialFragment` — Consumir la vista `v_historial` para mostrar el feed de actividad
- Upload real de archivos a Storage (foto RTM, documentos adjuntos, avatar)
- `GarajeFragment` — Lista de motos cuando el usuario tiene más de una
- `user_preferences` — Guardar/cargar las preferencias del usuario (tema, unidades, notificaciones)
- **Bifurcación por rol en login** — Después de autenticar, `login.kt` debe consultar `users.role` y navegar a `AdminActivity` si `role = 'admin'` o a `MainActivity` si `role = 'client'`. Actualmente todos los usuarios van a `MainActivity` sin importar su rol. Además, `AdminUsersFragment` usa datos mock hardcodeados y debe conectarse a Supabase (la RLS ya permite que el admin lea todos los registros).
