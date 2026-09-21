package com.megamind.yanguservice.data.local.dto

/** Projection des colonnes lues par le DAO, indépendante de l'entité Room. */
data class ContactDto(
    val id: String,
    val name: String,
    val phoneNumber: String,
)
