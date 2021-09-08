package org.marvin.greentoblue.listitemadapters

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import org.marvin.greentoblue.ChatExportActivity
import org.marvin.greentoblue.GenericFileProvider
import org.marvin.greentoblue.R
import org.marvin.greentoblue.models.ChunkDataModel
import java.io.File

class ChunkAdapter (private val activity: ChatExportActivity, private val chunkModelSource : List<ChunkDataModel>) : RecyclerView.Adapter<ChunkAdapter.ChunkViewHolder>() {

    class ChunkViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){
        val txtChunkName : TextView = itemView.findViewById(R.id.txtChunkName)
        val txtChunkDetails : TextView = itemView.findViewById(R.id.txtChunkDetails)
        val btnExport : ImageButton = itemView.findViewById(R.id.btnExport)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChunkViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.listviewitem_chunk,
            parent,
            false
        )
        return ChunkViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ChunkViewHolder, position: Int) {
        val chunk = chunkModelSource[position]
        holder.txtChunkName.text = "${chunk.chatName} Chunk ${chunk.chunkID}"
        holder.txtChunkDetails.text = "${chunk.chatCount} chats, with ${chunk.mediaURI.size} media files"
        holder.btnExport.setOnClickListener {
            val txtFile = File(activity.filesDir ,"WhatsApp Chat with ${chunk.chatName}.txt")
            val chunkFile = File(activity.filesDir ,"chunks/chunk_${chunk.chatID}_${chunk.chunkID}.txt")
            txtFile.writeBytes(chunkFile.readBytes())

            val txtFileUri = GenericFileProvider.getUriForFile(activity, activity.applicationContext.packageName, txtFile)

            val telegramIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
            telegramIntent.type = "*/*"

            val mediaUris = chunk.mediaURI
            val mediaFile = File(activity.filesDir ,"media_${chunk.chatID}_${chunk.chunkID}.g2bmedia")
            mediaFile.createNewFile()
            mediaFile.writeText(mediaUris.joinToString("\r\n"))

            val toSend: ArrayList<Uri> = arrayListOf(GenericFileProvider.getUriForFile(activity, activity.applicationContext.packageName, mediaFile))
            toSend.add(txtFileUri)

            telegramIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, toSend)
            activity.startActivity(telegramIntent)
        }
    }

    override fun getItemCount(): Int {
        return chunkModelSource.size
    }
}