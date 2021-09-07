package org.marvin.greentoblue.models

import android.net.Uri
import java.io.ByteArrayOutputStream

data class ChunkDataModel (
    val chatID: String,
    val chatName: String,
    val chunkID: Int,
    var chatCount: Int = 0,
    var data: Uri = Uri.EMPTY,
    val mediaURI: ArrayList<Uri> = arrayListOf()
)