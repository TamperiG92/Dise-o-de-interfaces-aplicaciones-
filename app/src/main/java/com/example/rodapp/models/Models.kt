package com.example.rodapp.models

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Moto(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val id: String? = null,
    val user_id: String,
    val marca: String,
    val modelo: String,
    val cilindrada: Int? = null,
    val placa: String,
    val odometro_inicial: Int = 0,
    val foto_url: String? = null,
    val activa: Boolean = true
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SoatInsert(
    val moto_id: String,
    val numero_poliza: String,
    val aseguradora: String,
    val fecha_inicio: String,
    val fecha_vencimiento: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val foto_url: String? = null
)

@Serializable
data class RtmInsert(
    val moto_id: String,
    val numero_certificado: String,
    val nombre_cda: String,
    val fecha_expedicion: String,
    val fecha_vencimiento: String,
    val foto_url: String? = null
)

@Serializable
data class CombustibleInsert(
    val moto_id: String,
    val tipo_gasolina: String,
    val costo: Double,
    val kilometraje: Int,
    val litros: Double? = null,
    val latitud: Double? = null,
    val longitud: Double? = null
)

@Serializable
data class MantenimientoInsert(
    val moto_id: String,
    val tipo: String,
    val fecha: String,
    val kilometraje: Int,
    val repetir_cada_km: Int? = null,
    val notas: String? = null
)

@Serializable
data class DocumentoInsert(
    val moto_id: String,
    val tipo: String,
    val nombre: String,
    val entidad_emisora: String? = null,
    val fecha_vencimiento: String? = null,
    val recordatorio_activo: Boolean = true,
    val archivo_url: String? = null
)

@Serializable
data class HistorialItem(
    val id: String,
    val moto_id: String,
    val tipo: String,
    val created_at: String,
    val subtipo: String? = null,
    val valor: String? = null,
    val kilometraje: Int? = null,
    val litros: Double? = null
)

@Serializable data class SoatRecord(val fecha_vencimiento: String)
@Serializable data class RtmRecord(val fecha_vencimiento: String)
@Serializable data class MotoOdo(val odometro_inicial: Int)
@Serializable data class KmRecord(val kilometraje: Int)

@Serializable
data class UsuarioInfo(
    val id: String,
    val name: String,
    val lastname: String? = null,
    val correo: String? = null,
    val url_photo: String? = null
)

@Serializable
data class UserPreferences(
    val notificaciones_push: Boolean = true,
    val alertas_mantenimiento: Boolean = true
)

@Serializable
data class DocumentoAlerta(
    val tipo: String = "",
    val nombre: String,
    val fecha_vencimiento: String? = null
)
