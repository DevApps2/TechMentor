package com.example.techmentor.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream

/** Reduz a foto para no máximo 800 px, corrige a rotação e converte para JPEG, para o envio ser rápido. */
fun reduzirFoto(context: Context, uri: Uri): ByteArray? {

    val original = runCatching {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }.getOrNull() ?: return null

    val maior = maxOf(original.width, original.height)
    val escala = if (maior > 800) 800f / maior else 1f

    // a matriz diz como transformar a imagem: reduzir e girar, de uma vez só
    val matriz = Matrix().apply {
        postScale(escala, escala)
        postRotate(lerRotacao(context, uri).toFloat())
    }

    val ajustada = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true)

    val saida = ByteArrayOutputStream()
    ajustada.compress(Bitmap.CompressFormat.JPEG, 85, saida)
    return saida.toByteArray()
}

/** Lê no arquivo quantos graus a foto precisa girar para ficar em pé. */
private fun lerRotacao(context: Context, uri: Uri): Int {

    val orientacao = runCatching {
        context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    return when (orientacao) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}