package com.megamind.yanguservice.data.local

import com.megamind.yanguservice.data.local.dto.ContactDto
import com.megamind.yanguservice.data.local.entity.ContactEntity
import com.megamind.yanguservice.domain.model.Contact

fun ContactDto.toDomain(): Contact = Contact(
    id = id,
    name = name,
    phoneNumber = phoneNumber,
)

fun Contact.toEntity(): ContactEntity = ContactEntity(
    id = id,
    name = name,
    phoneNumber = phoneNumber,
)
