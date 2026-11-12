package com.jarvis.personalassistant.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "USER", "JARVIS", "TOOL"
    val content: String,
    val modelId: String,
    val languageCode: String,
    val tokensUsed: Int,
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "memory_facts")
data class MemoryFactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // "PREFERENCE", "PROJECT", "CONTACT", "ROUTINE"
    val title: String,
    val factContent: String,
    val isPinned: Boolean = false,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "whatsapp_notifications")
data class WhatsAppNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val senderName: String,
    val messagePreview: String,
    val category: String, // "URGENT", "WORK", "PERSONAL", "OTP", "GROUP"
    val priorityScore: Int, // 1..100
    val suggestedReplyDraft: String,
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "automation_rules")
data class AutomationRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleName: String,
    val triggerType: String, // "TIME_DAILY", "BATTERY_LOW", "WHATSAPP_URGENT"
    val triggerCondition: String,
    val actionCommand: String,
    val isEnabled: Boolean = true
)

@Entity(tableName = "token_usage_ledger")
data class TokenUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modelId: String,
    val providerName: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val latencyMs: Long,
    val timestampMs: Long = System.currentTimeMillis()
)

@Dao
interface JarvisDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestampMs ASC")
    fun observeMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearMessages()

    @Query("SELECT * FROM memory_facts ORDER BY isPinned DESC, updatedAtMs DESC")
    fun observeMemories(): Flow<List<MemoryFactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemory(memory: MemoryFactEntity)

    @Query("DELETE FROM memory_facts WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM memory_facts")
    suspend fun clearAllMemories()

    @Query("SELECT * FROM whatsapp_notifications ORDER BY timestampMs DESC LIMIT 100")
    fun observeWhatsAppNotifications(): Flow<List<WhatsAppNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWhatsAppNotification(item: WhatsAppNotificationEntity)

    @Query("DELETE FROM whatsapp_notifications")
    suspend fun clearWhatsAppNotifications()

    @Query("SELECT * FROM automation_rules ORDER BY id ASC")
    fun observeAutomationRules(): Flow<List<AutomationRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAutomationRule(rule: AutomationRuleEntity)

    @Query("SELECT COALESCE(SUM(promptTokens + completionTokens), 0) FROM token_usage_ledger WHERE timestampMs >= :sinceMs")
    suspend fun getTotalTokensSince(sinceMs: Long): Int

    @Insert
    suspend fun recordTokenUsage(entry: TokenUsageEntity)
}

@Database(
    entities = [
        ChatMessageEntity::class,
        MemoryFactEntity::class,
        WhatsAppNotificationEntity::class,
        AutomationRuleEntity::class,
        TokenUsageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun jarvisDao(): JarvisDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getInstance(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_personal_ai.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
