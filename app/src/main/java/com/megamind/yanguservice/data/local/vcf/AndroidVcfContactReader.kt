package com.megamind.yanguservice.data.local.vcf

import android.content.Context
import android.net.Uri
import com.megamind.yanguservice.domain.model.VcfContacts
import com.megamind.yanguservice.domain.utils.VcfContactReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException

class AndroidVcfContactReader(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher,
    private val parser: VcfParser,
) : VcfContactReader {
    override suspend fun read(uri: String): VcfContacts = withContext(dispatcher) {
        val input = try {
            context.contentResolver.openInputStream(Uri.parse(uri))
        } catch (error: SecurityException) {
            throw IllegalStateException("L'accès au fichier VCF a été refusé", error)
        } ?: throw IllegalStateException("Impossible d'ouvrir le fichier VCF")

        try {
            input.use(parser::parse)
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            throw IllegalStateException("Impossible de lire le fichier VCF", error)
        }
    }
}
