package com.megamind.yanguservice.utlis

data class BulkSendResult(
    val succeeded: List<String>,             // numéros envoyés avec succès
    val failed: Map<String, Throwable>       // numéro -> erreur
)

