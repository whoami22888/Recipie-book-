package com.beyondhuman.kitchen.vision

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class IngredientAnalyzer {
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(0.55f).build())

    fun analyze(
        context: Context,
        uri: Uri,
        onSuccess: (List<IngredientCandidate>) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        runCatching { InputImage.fromFilePath(context, uri) }
            .onFailure(onFailure)
            .onSuccess { image ->
                labeler.process(image)
                    .addOnSuccessListener { labels ->
                        onSuccess(
                            labels
                                .map { IngredientCandidate(it.text, it.confidence) }
                                .distinctBy { it.label.lowercase() }
                                .sortedByDescending { it.confidence }
                        )
                    }
                    .addOnFailureListener(onFailure)
            }
    }
}
