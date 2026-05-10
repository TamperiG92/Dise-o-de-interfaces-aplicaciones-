package com.example.rodapp.services

import kotlinx.coroutines.flow.MutableStateFlow

object RodandoEstado {
    val activa = MutableStateFlow(false)
    val distanciaMetros = MutableStateFlow(0f)
    val tiempoSegundos = MutableStateFlow(0L)
    val velocidadKmh = MutableStateFlow(0f)
    var kmInicio: Int = 0
}
