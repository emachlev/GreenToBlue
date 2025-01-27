package org.marvin.greentoblue

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.util.Log
import org.marvin.greentoblue.models.*
import java.io.*
import java.sql.Timestamp
import java.text.SimpleDateFormat
import android.database.sqlite.SQLiteOpenHelper as SQLiteOpenHelper

class ChatDatabaseAdapter(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    //region Init

    companion object {
        const val DATABASE_NAME = "chatdatabase.db"
        const val DATABASE_VERSION = 1

        const val TABLE_PARTICIPANT = "participants"
        const val TABLE_CHAT_METADATA = "chat_metadata"
        const val TABLE_CHAT_DATA = "chat_data"
        const val TABLE_MEDIA = "media"
        const val TABLE_CHUNK = "chunk"
        const val TABLE_CHUNK_MEDIA = "chunk_media"

        const val COL_CHUNK_BLOB = "chunk_data"
        const val COL_CHUNK_CHAT_COUNTER = "chat_counter"
        const val COL_CHUNK_COUNTER = "chunk_counter"

        const val COL_CHAT_KEY = "chat_key"
        const val COL_CHAT_NAME = "chat_name"
        const val COL_CHAT_COUNT = "chat_count"
        const val COL_CHAT_MEDIA_COUNT = "chat_media_count"
        const val COL_CHAT_MEDIA_FOUND = "chat_media_found"

        const val COL_PARTICIPANT_KEY = "participant_key"
        const val COL_PARTICIPANT_NAME = "participant_name"

        const val COL_CHAT_DATA = "chat"
        const val COL_CHAT_FROM_ME = "chat_from_me"
        const val COL_HAS_MEDIA = "has_media"
        const val COL_MEDIA_FOUND = "media_found"
        const val COL_MEDIA_NAME = "media_name"
        const val COL_MEDIA_CAPTION = "media_caption"
        const val COL_MEDIA_URI = "media_uri"
        const val COL_TIMESTAMP = "timestamp"

        const val COL_MEDIA_FILENAME = "media_filename"
        const val COL_MEDIA_LOCATION = "media_location"
        const val COL_MEDIA_HASH = "media_hash"

        const val COL_CHAT_SOURCE = "chat_source"


        private lateinit var mInstance: ChatDatabaseAdapter
        fun getInstance(context: Context): ChatDatabaseAdapter {
            if (!this::mInstance.isInitialized) {
                mInstance = ChatDatabaseAdapter(context.applicationContext)
            }
            return mInstance
        }
    }

    init {
        readableDatabase
    }

    override fun onCreate(db: SQLiteDatabase?) {
        initDatabase(db)
    }

    private fun initDatabase(db: SQLiteDatabase?) {
        val createParticipantsTable = "CREATE TABLE IF NOT EXISTS $TABLE_PARTICIPANT (" +
                "$COL_CHAT_KEY TEXT, " +
                "$COL_PARTICIPANT_KEY TEXT, " +
                "$COL_PARTICIPANT_NAME TEXT, " +
                "$COL_CHAT_SOURCE TEXT)"

        val createChatMetadataTable = "CREATE TABLE IF NOT EXISTS $TABLE_CHAT_METADATA (" +
                "$COL_CHAT_KEY TEXT, " +
                "$COL_CHAT_NAME TEXT, " +
                "$COL_CHAT_COUNT INT, " +
                "$COL_CHAT_MEDIA_COUNT INT, " +
                "$COL_CHAT_MEDIA_FOUND INT, " +
                "$COL_CHAT_SOURCE TEXT)"

        val createChatDataTable = "CREATE TABLE IF NOT EXISTS $TABLE_CHAT_DATA (" +
                "$COL_CHAT_KEY TEXT, " +
                "$COL_TIMESTAMP INT, " +
                "$COL_CHAT_FROM_ME INT, " +
                "$COL_PARTICIPANT_KEY TEXT, " +
                "$COL_CHAT_DATA TEXT, " +
                "$COL_HAS_MEDIA INT, " +
                "$COL_MEDIA_NAME TEXT, " +
                "$COL_MEDIA_CAPTION TEXT, " +
                "$COL_MEDIA_FOUND INT, " +
                "$COL_MEDIA_URI TEXT, " +
                "$COL_CHAT_SOURCE TEXT)"

        val createMediaTable = "CREATE TABLE IF NOT EXISTS $TABLE_MEDIA (" +
                "$COL_MEDIA_FILENAME TEXT, " +
                "$COL_MEDIA_LOCATION TEXT, " +
                "$COL_MEDIA_URI TEXT, " +
                "$COL_MEDIA_HASH TEXT)"

        val createChunkTable = "CREATE TABLE IF NOT EXISTS $TABLE_CHUNK (" +
                "$COL_CHAT_KEY TEXT, " +
                "$COL_CHAT_NAME TEXT, " +
                "$COL_CHUNK_COUNTER INT, " +
                "$COL_CHUNK_CHAT_COUNTER INT, " +
                "$COL_CHUNK_BLOB BLOB)"

        val createChunkMediaTable = "CREATE TABLE IF NOT EXISTS $TABLE_CHUNK_MEDIA (" +
                "$COL_CHAT_KEY TEXT, " +
                "$COL_CHUNK_COUNTER INT, " +
                "$COL_MEDIA_URI TEXT)"

        db?.execSQL(createParticipantsTable)
        db?.execSQL(createChatMetadataTable)
        db?.execSQL(createChatDataTable)
        db?.execSQL(createMediaTable)
        db?.execSQL(createChunkTable)
        db?.execSQL(createChunkMediaTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }

    //endregion

    //region Chat

    fun clearChatWhatsapp() {
        writableDatabase.use { db ->
            db.delete(TABLE_CHAT_METADATA, "$COL_CHAT_SOURCE=?", arrayOf<String>(ChatSources.SOURCE_WHATSAPP))
            db.delete(TABLE_CHAT_DATA, "$COL_CHAT_SOURCE=?", arrayOf<String>(ChatSources.SOURCE_WHATSAPP))
            db.delete(TABLE_PARTICIPANT, "$COL_CHAT_SOURCE=?", arrayOf<String>(ChatSources.SOURCE_WHATSAPP))
        }
    }

    fun clearChatFB() {
        writableDatabase.use { db ->
            db.delete(TABLE_CHAT_METADATA, "$COL_CHAT_SOURCE=?", arrayOf<String>(ChatSources.SOURCE_FB))
            db.delete(TABLE_CHAT_DATA, "$COL_CHAT_SOURCE=?", arrayOf<String>(ChatSources.SOURCE_FB))
            db.delete(TABLE_PARTICIPANT, "$COL_CHAT_SOURCE=?", arrayOf<String>(ChatSources.SOURCE_FB))
        }
    }

    fun clearChatMerged() {
        writableDatabase.use { db ->
            db.delete(TABLE_CHAT_METADATA, "$COL_CHAT_SOURCE=?", arrayOf<String>(ChatSources.SOURCE_MERGED))
            db.delete(TABLE_CHAT_DATA, "$COL_CHAT_SOURCE=?", arrayOf<String>(ChatSources.SOURCE_MERGED))
            db.delete(TABLE_PARTICIPANT, "$COL_CHAT_SOURCE=?", arrayOf<String>(ChatSources.SOURCE_MERGED))
        }
    }

    fun clearChatAll(){
        clearChatWhatsapp()
        clearChatFB()
        clearChatMerged()
    }

    fun addChatData(chatData: List<ChatDataModel>) {
        writableDatabase.use { db ->
            db.beginTransaction()

            try {
                chatData.forEach { chat ->
                    val cv = ContentValues()
                    cv.put(COL_CHAT_KEY, chat.chatID)
                    cv.put(COL_TIMESTAMP, chat.timestamp.time)
                    cv.put(COL_CHAT_FROM_ME, chat.chatFromMe)
                    cv.put(COL_PARTICIPANT_KEY, chat.participantID)
                    cv.put(COL_CHAT_DATA, chat.chatData)
                    cv.put(COL_HAS_MEDIA, chat.hasMedia)
                    cv.put(COL_MEDIA_NAME, chat.mediaName)
                    cv.put(COL_MEDIA_CAPTION, chat.mediaCaption)
                    cv.put(COL_MEDIA_FOUND, chat.mediaFound)
                    cv.put(COL_MEDIA_URI, chat.mediaURI.toString())
                    cv.put(COL_CHAT_SOURCE, chat.chatSource)

                    db.insert(TABLE_CHAT_DATA, null, cv)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    fun addChatMetadata(chatMetadataModel: ChatMetadataModel) {
        writableDatabase.use { db ->
            val chatCV = ContentValues()
            chatCV.put(COL_CHAT_KEY, chatMetadataModel.chatID)
            chatCV.put(COL_CHAT_NAME, chatMetadataModel.chatName)
            chatCV.put(COL_CHAT_COUNT, chatMetadataModel.chatCount)
            chatCV.put(COL_CHAT_MEDIA_COUNT, chatMetadataModel.mediaCount)
            chatCV.put(COL_CHAT_MEDIA_FOUND, chatMetadataModel.mediaFound)
            chatCV.put(COL_CHAT_SOURCE, chatMetadataModel.chatSource)

            db.insert(TABLE_CHAT_METADATA, null, chatCV)

            chatMetadataModel.chatParticipants.forEach { participant ->
                val participantCV = ContentValues()
                participantCV.put(COL_CHAT_KEY, chatMetadataModel.chatID)
                participantCV.put(COL_PARTICIPANT_KEY, participant.key)
                participantCV.put(COL_PARTICIPANT_NAME, participant.value)

                db.insert(TABLE_PARTICIPANT, null, participantCV)
            }
        }
    }

    fun updateParticipant(chatMetadataModel: ChatMetadataModel) {
        val chatID = chatMetadataModel.chatID
        writableDatabase.use { db ->
            val chatMetadataCV = ContentValues()
            chatMetadataCV.put(COL_CHAT_NAME, chatMetadataModel.chatName)
            db.update(TABLE_CHAT_METADATA, chatMetadataCV, "$COL_CHAT_KEY=?", arrayOf(chatID))

            chatMetadataModel.chatParticipants.forEach { participant ->
                val cv = ContentValues()
                cv.put(COL_PARTICIPANT_NAME, participant.value)

                db.update(
                    TABLE_PARTICIPANT,
                    cv,
                    "$COL_CHAT_KEY=? AND $COL_PARTICIPANT_KEY=?",
                    arrayOf(chatID, participant.key)
                )
            }
        }
    }

    fun mergeChats(selectedChats: List<ChatMetadataModel>) {
        val mainChat = selectedChats.maxByOrNull { it.chatCount }!!

        var totalChatCount = 0
        var totalMediaCount = 0
        var totalMediaFound = 0

        writableDatabase.use { db ->
            selectedChats.forEach { chat ->

                totalChatCount += chat.chatCount
                totalMediaCount += chat.mediaCount
                totalMediaFound += chat.mediaFound

                if (chat.isGroup()) {
                    val participantCV = ContentValues()
                    participantCV.put(COL_CHAT_KEY, mainChat.chatID)
                    participantCV.put(COL_CHAT_SOURCE, ChatSources.SOURCE_MERGED)

                    db.update(TABLE_PARTICIPANT, participantCV, "$COL_CHAT_KEY=?", arrayOf(chat.chatID))


                    val chatCV = ContentValues()
                    chatCV.put(COL_CHAT_KEY, mainChat.chatID)
                    chatCV.put(COL_CHAT_SOURCE, ChatSources.SOURCE_MERGED)
                    db.update(TABLE_CHAT_DATA, chatCV, "$COL_CHAT_KEY=?", arrayOf(chat.chatID))
                } else {
                    val participantCV = ContentValues()
                    participantCV.put(COL_CHAT_KEY, mainChat.chatID)
                    participantCV.put(COL_PARTICIPANT_KEY, chat.chatID)
                    participantCV.put(COL_PARTICIPANT_NAME, chat.chatName)
                    participantCV.put(COL_CHAT_SOURCE, ChatSources.SOURCE_MERGED)

                    db.insert(TABLE_PARTICIPANT, null, participantCV)

                    val chatCV = ContentValues()
                    chatCV.put(COL_CHAT_KEY, mainChat.chatID)
                    chatCV.put(COL_PARTICIPANT_KEY, chat.chatID)
                    chatCV.put(COL_CHAT_SOURCE, ChatSources.SOURCE_MERGED)
                    db.update(TABLE_CHAT_DATA, chatCV, "$COL_CHAT_KEY=?", arrayOf(chat.chatID))
                }

                if (chat.chatID != mainChat.chatID) {
                    db.delete(TABLE_CHAT_METADATA, "$COL_CHAT_KEY=?", arrayOf(chat.chatID))
                }
            }
            val metadataCV = ContentValues()
            metadataCV.put(COL_CHAT_COUNT, totalChatCount)
            metadataCV.put(COL_CHAT_MEDIA_COUNT, totalMediaCount)
            metadataCV.put(COL_CHAT_MEDIA_FOUND, totalMediaFound)
            metadataCV.put(COL_CHAT_SOURCE, ChatSources.SOURCE_MERGED)
            db.update(TABLE_CHAT_METADATA, metadataCV, "$COL_CHAT_KEY=?", arrayOf(mainChat.chatID))
        }
    }

    fun deleteChats(selectedChats: List<ChatMetadataModel>){
        writableDatabase.use { db ->
            db.beginTransaction()
            selectedChats.forEach{ chatMetadata ->
                db.delete(TABLE_CHAT_DATA, "$COL_CHAT_KEY=?", arrayOf(chatMetadata.chatID))
                db.delete(TABLE_PARTICIPANT, "$COL_CHAT_KEY=?", arrayOf(chatMetadata.chatID))
                db.delete(TABLE_CHAT_METADATA, "$COL_CHAT_KEY=?", arrayOf(chatMetadata.chatID))
                db.delete(TABLE_CHUNK, "$COL_CHAT_KEY=?", arrayOf(chatMetadata.chatID))
                db.delete(TABLE_CHUNK_MEDIA, "$COL_CHAT_KEY=?", arrayOf(chatMetadata.chatID))
            }
            db.setTransactionSuccessful()
            db.endTransaction()
        }
    }
    //endregion

    //region Media

    fun clearMedia() {
        writableDatabase.use { db ->
            db.delete(TABLE_MEDIA, "", arrayOf<String>())
        }
    }

    fun addMediaFiles(mediaFiles: List<MediaModel>) {
        writableDatabase.use { db ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaFiles.parallelStream().forEach { media ->
                    val cv = ContentValues()
                    cv.put(COL_MEDIA_FILENAME, media.fileName)
                    cv.put(COL_MEDIA_LOCATION, media.filePath)
                    cv.put(COL_MEDIA_URI, media.fileUri.toString())
                    cv.put(COL_MEDIA_HASH, media.fileHash)

                    db.insert(TABLE_MEDIA, null, cv)
                }
            } else {
                mediaFiles.forEach { media ->
                    val cv = ContentValues()
                    cv.put(COL_MEDIA_FILENAME, media.fileName)
                    cv.put(COL_MEDIA_LOCATION, media.filePath)
                    cv.put(COL_MEDIA_URI, media.fileUri.toString())
                    cv.put(COL_MEDIA_HASH, media.fileHash)

                    db.insert(TABLE_MEDIA, null, cv)
                }
            }
        }
    }

    fun getMediaFiles(mediaModels: MutableList<MediaModel>) {
        mediaModels.clear()
        readableDatabase.use { db ->
            val query = "SELECT * FROM $TABLE_MEDIA"
            db.rawQuery(query, null).use { curr ->
                if (curr.moveToFirst()) {
                    do {
                        val mediaModel = MediaModel(
                            curr.getString(curr.getColumnIndex(COL_MEDIA_FILENAME)),
                            curr.getString(curr.getColumnIndex(COL_MEDIA_LOCATION)),
                            Uri.parse(curr.getString(curr.getColumnIndex(COL_MEDIA_URI))),
                            curr.getString(curr.getColumnIndex(COL_MEDIA_HASH))
                        )
                        mediaModels.add(mediaModel)
                    } while (curr.moveToNext())
                }
            }
        }
    }

    fun getMediaFileByHash(hash: String) : Pair<Uri, String> {
        readableDatabase.use { db ->
            val query = "SELECT media_uri, media_filename FROM $TABLE_MEDIA WHERE media_hash = '$hash'"
            db.rawQuery(query, null).use { curr ->
                if (curr.moveToFirst()) {
                    return Pair(Uri.parse(curr.getString(curr.getColumnIndex(COL_MEDIA_URI))), curr.getString(curr.getColumnIndex(COL_MEDIA_FILENAME)))
                }
                return Pair(Uri.EMPTY, "");
            }
        }
    }

    fun updateMediaFoundCount(chatID: String, mediaFoundCount: Int) {
        writableDatabase.use { db ->
            val cv = ContentValues()
            cv.put(COL_CHAT_MEDIA_FOUND, mediaFoundCount)

            db.update(TABLE_CHAT_METADATA, cv, "$COL_CHAT_KEY=?", arrayOf(chatID))
        }
    }


    //endregion

    //region Chunks

    fun clearChunks(context: Context, chatID: String) {
        writableDatabase.use { db ->
            db.delete(TABLE_CHUNK, "$COL_CHAT_KEY=?", arrayOf(chatID))
            db.delete(TABLE_CHUNK_MEDIA, "$COL_CHAT_KEY=?", arrayOf(chatID))
        }
        val folder_chunks = File(context.filesDir, "chunks")
        folder_chunks.walk().filter { it.name.startsWith("chunk_$chatID") }.forEach {
            it.delete()
        }
        val folder_media = File(context.filesDir, ".")
        folder_media.walk().filter { it.name.startsWith("media_$chatID") }.forEach {
            it.delete()
        }
    }

    fun makeChunks(
        context: Context,
        chatMetadata: ChatMetadataModel,
        myName: String,
        chunkSize: Int,
        chunks: MutableList<ChunkDataModel>
    ) {
        val chatID = chatMetadata.chatID
        val participants = chatMetadata.chatParticipants

        Log.d("Par", participants.toString())
        var firstLine: String
        var endLine: String

        readableDatabase.use { db ->
            val query = "SELECT * FROM $TABLE_CHAT_DATA WHERE $COL_CHAT_KEY='$chatID' ORDER BY $COL_TIMESTAMP ASC"

            db.rawQuery(query, null).use { curr ->
                if (curr.moveToFirst()) {
                    var chunkCounter = 1
                    var chunk = ChunkDataModel(chatID, chatMetadata.chatName, chunkCounter)


                    var timestamp =
                        getFormattedTime(Timestamp(curr.getLong(curr.getColumnIndex(COL_TIMESTAMP))))

                    firstLine = "$timestamp - Green to Blue: BEGINNING OF CHUNK $chunkCounter\n"

                    var data = ByteArrayOutputStream(1024)
                    data.write(firstLine.toByteArray())

                    var mediaCounter = 0
                    do {
                        val chatData = ChatDataModel(
                            chatID,
                            Timestamp(curr.getLong(curr.getColumnIndex(COL_TIMESTAMP))),
                            curr.getString(curr.getColumnIndex(COL_CHAT_DATA)),
                            curr.getInt(curr.getColumnIndex(COL_CHAT_FROM_ME)) == 1,
                            curr.getString(curr.getColumnIndex(COL_PARTICIPANT_KEY)),
                            curr.getInt(curr.getColumnIndex(COL_HAS_MEDIA)) == 1,
                            curr.getString(curr.getColumnIndex(COL_MEDIA_NAME)),
                            curr.getString(curr.getColumnIndex(COL_MEDIA_CAPTION)),
                            curr.getInt(curr.getColumnIndex(COL_MEDIA_FOUND)) == 1,
                            Uri.parse(curr.getString(curr.getColumnIndex(COL_MEDIA_URI))),
                            curr.getString(curr.getColumnIndex(COL_CHAT_SOURCE))
                        )
                        val sender = when {
                            chatData.chatFromMe -> {
                                myName
                            }
                            chatMetadata.isGroup() -> {
                                participants[chatData.participantID] ?: chatMetadata.chatName
                            }
                            else -> {
                                chatMetadata.chatName
                            }
                        }

                        timestamp = getFormattedTime(chatData.timestamp)


                        val chat = if (!chatData.hasMedia) {
                            "$timestamp - $sender: ${chatData.chatData}"
                        } else {
                            if (chatData.mediaFound) {
                                mediaCounter += 1
                                chunk.mediaURI.add(chatData.mediaURI)
                                "$timestamp - $sender: ${chatData.mediaName} (file attached)" + if (chatData.mediaCaption.isNotEmpty()) "\n${chatData.mediaCaption}" else ""
                            } else {
                                "$timestamp - $sender: <media>" + if (chatData.mediaCaption.isNotEmpty()) "\n${chatData.mediaCaption}" else ""
                            }
                        } + "\n"

                        if (chat.trim() == "") continue

                        chunk.chatCount += 1

                        data.write(chat.toByteArray())

                        if (mediaCounter >= chunkSize) {
                            mediaCounter = 0

                            endLine = "$timestamp - Green to Blue: ENDING OF CHUNK $chunkCounter\n"

                            data.write(endLine.toByteArray())
                            data.close()

                            val chunkFile = File(context.filesDir ,"chunks/chunk_${chunk.chatID}_${chunk.chunkID}.txt")
                            chunkFile.parentFile?.mkdirs()
                            chunkFile.createNewFile()
                            chunkFile.writeBytes(data.toByteArray())

                            chunks.add(chunk)

                            chunkCounter += 1

                            chunk = ChunkDataModel(chatID, chatMetadata.chatName, chunkCounter)
                            data = ByteArrayOutputStream(1024)
                            firstLine =
                                "$timestamp - Green to Blue: BEGINNING OF CHUNK $chunkCounter\n"
                            data.write(firstLine.toByteArray())
                        }
                    } while (curr.moveToNext())
                    endLine = "$timestamp - Green to Blue: ENDING OF CHUNK $chunkCounter\n"
                    data.write(endLine.toByteArray())
                    data.close()
                    val chunkFile = File(context.filesDir ,"chunks/chunk_${chunk.chatID}_${chunk.chunkID}.txt")
                    chunkFile.parentFile?.mkdirs()
                    chunkFile.createNewFile()
                    chunkFile.writeBytes(data.toByteArray())
                    chunks.add(chunk)
                }
            }
        }
    }

    fun writeChunks(chatMetadata: ChatMetadataModel, chunks: List<ChunkDataModel>) {
        writableDatabase.use { db ->
            db.beginTransaction()
            try {
                chunks.forEach { chunk ->
                    val chunkCV = ContentValues()
                    chunkCV.put(COL_CHAT_KEY, chatMetadata.chatID)
                    chunkCV.put(COL_CHAT_NAME, chatMetadata.chatName)
                    chunkCV.put(COL_CHUNK_CHAT_COUNTER, chunk.chatCount)
                    chunkCV.put(COL_CHUNK_COUNTER, chunk.chunkID)
                    chunkCV.put(COL_CHUNK_BLOB, "")
                    db.insert(TABLE_CHUNK, null, chunkCV)

                    chunk.mediaURI.forEach { uri ->
                        val chunkUriCV = ContentValues()
                        chunkUriCV.put(COL_CHAT_KEY, chatMetadata.chatID)
                        chunkUriCV.put(COL_CHUNK_COUNTER, chunk.chunkID)
                        chunkUriCV.put(COL_MEDIA_URI, uri.toString())

                        db.insert(TABLE_CHUNK_MEDIA, null, chunkUriCV)
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun getFormattedTime(timestamp: Timestamp): String {
        return SimpleDateFormat("M/d/yy, h:mm a").format(timestamp)
    }

    fun getChatMetadata(chatMetadataList: MutableList<ChatMetadataModel>) {
        chatMetadataList.clear()
        readableDatabase.use { db ->
            val metadataQuery = "SELECT * FROM $TABLE_CHAT_METADATA ORDER BY $COL_CHAT_COUNT DESC"
            db.rawQuery(metadataQuery, null).use { metadataCurr ->
                if (metadataCurr.moveToFirst()) {
                    do {
                        val chatID =
                            metadataCurr.getString(metadataCurr.getColumnIndex(COL_CHAT_KEY))
                        val chatName =
                            metadataCurr.getString(metadataCurr.getColumnIndex(COL_CHAT_NAME))
                        val chatCount =
                            metadataCurr.getInt(metadataCurr.getColumnIndex(COL_CHAT_COUNT))
                        val chatMediaCount =
                            metadataCurr.getInt(metadataCurr.getColumnIndex(COL_CHAT_MEDIA_COUNT))
                        val chatMediaFound =
                            metadataCurr.getInt(metadataCurr.getColumnIndex(COL_CHAT_MEDIA_FOUND))
                        val chatSource =
                            metadataCurr.getString((metadataCurr.getColumnIndex(COL_CHAT_SOURCE)))
                        val chatMetadata = ChatMetadataModel(
                            chatID,
                            chatName,
                            chatCount,
                            chatMediaCount,
                            chatMediaFound,
                            chatSource
                        )
                        val participantQuery =
                            "SELECT * FROM $TABLE_PARTICIPANT WHERE $COL_CHAT_KEY='$chatID'"
                        db.rawQuery(participantQuery, null).use { participantCurr ->
                            if (participantCurr.moveToFirst()) {
                                do {
                                    val participantID = participantCurr.getString(participantCurr.getColumnIndex(COL_PARTICIPANT_KEY))
                                    val participantName = participantCurr.getString(participantCurr.getColumnIndex(COL_PARTICIPANT_NAME))
                                    chatMetadata.chatParticipants[participantID] = participantName
                                } while (participantCurr.moveToNext())
                            }
                        }

                        chatMetadataList.add(chatMetadata)
                    } while (metadataCurr.moveToNext())
                }
            }
        }
    }

    fun getChunks(chatID: String, chunks: MutableList<ChunkDataModel>) {
        readableDatabase.use { db ->
            val chunkMetadataQuery = "SELECT * FROM $TABLE_CHUNK WHERE $COL_CHAT_KEY = '$chatID'"
            db.rawQuery(chunkMetadataQuery, null).use { curr ->
                if (curr.moveToFirst()) {
                    do {
                        val chunk = ChunkDataModel(
                            chatID,
                            curr.getString(curr.getColumnIndex(COL_CHAT_NAME)),
                            curr.getInt(curr.getColumnIndex(COL_CHUNK_COUNTER)),
                            curr.getInt(curr.getColumnIndex(COL_CHUNK_CHAT_COUNTER)),
                            Uri.parse(curr.getString(curr.getColumnIndex(COL_CHUNK_BLOB)))
                        )
                        val chunkUriQuery =
                            "SELECT * FROM $TABLE_CHUNK_MEDIA WHERE $COL_CHAT_KEY = '$chatID' AND $COL_CHUNK_COUNTER = ${chunk.chunkID}"
                        db.rawQuery(chunkUriQuery, null).use { uriCurr ->
                            if (uriCurr.moveToFirst()) {
                                do {
                                    val uri = Uri.parse(
                                        uriCurr.getString(
                                            uriCurr.getColumnIndex(COL_MEDIA_URI)
                                        )
                                    )
                                    chunk.mediaURI.add(uri)
                                } while (uriCurr.moveToNext())
                            }
                        }
                        chunks.add(chunk)
                    } while (curr.moveToNext())
                }


            }
        }
    }

    //endregion

}