package fr.piotr.dismoitoutsms.messages

import android.graphics.Bitmap

interface MessagePart

data class TextPart(val text: String): MessagePart

data class ImagePart(val image: Bitmap): MessagePart