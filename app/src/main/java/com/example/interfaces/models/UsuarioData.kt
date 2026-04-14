package com.example.interfaces.models

import kotlinx.serialization.Serializable

@Serializable
data class UsuarioData(
    val id: String,
    val name: String,
    val lastname: String
)
