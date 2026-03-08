package com.glazev.celebrationai.service

import com.glazev.celebrationai.data.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class SyncManager(
    private val dao: CelebrationDao,
    private val authManager: AuthManager
) {
    private val db = FirebaseFirestore.getInstance()

    suspend fun syncToCloud() {
        val userId = authManager.getUserId() ?: return
        val celebrations = dao.getAllCelebrationsSync()
        
        val batch = db.batch()
        celebrations.forEach { celebration ->
            val docRef = db.collection("users").document(userId)
                .collection("celebrations").document(celebration.id.toString())
            
            val data = hashMapOf(
                "id" to celebration.id,
                "name" to celebration.name,
                "type" to celebration.type.name,
                "customType" to celebration.customType,
                "date" to celebration.date,
                "hobby" to celebration.hobby,
                "profession" to celebration.profession,
                "tone" to celebration.tone.name,
                "group" to celebration.group.name,
                "reminderHour" to celebration.reminderHour,
                "reminderMinute" to celebration.reminderMinute,
                "savedGreeting" to celebration.savedGreeting,
                "giftIdeas" to celebration.giftIdeas,
                "greetingHistory" to celebration.greetingHistory
            )
            batch.set(docRef, data, SetOptions.merge())
        }
        batch.commit().await()
    }

    // НОВЫЙ МЕТОД: Удаление конкретного документа из облака
    suspend fun deleteFromCloud(celebrationId: Int) {
        val userId = authManager.getUserId() ?: return
        try {
            db.collection("users").document(userId)
                .collection("celebrations").document(celebrationId.toString())
                .delete()
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncFromCloud() {
        val userId = authManager.getUserId() ?: return
        val snapshot = db.collection("users").document(userId)
            .collection("celebrations").get().await()
            
        snapshot.documents.forEach { doc ->
            try {
                val c = Celebration(
                    id = doc.getLong("id")?.toInt() ?: 0,
                    name = doc.getString("name") ?: "",
                    type = try { CelebrationType.valueOf(doc.getString("type") ?: "BIRTHDAY") } catch(e: Exception) { CelebrationType.BIRTHDAY },
                    customType = doc.getString("customType") ?: "",
                    date = doc.getLong("date") ?: 0L,
                    hobby = doc.getString("hobby") ?: "",
                    profession = doc.getString("profession") ?: "",
                    tone = try { CelebrationTone.valueOf(doc.getString("tone") ?: "SOLEMN") } catch(e: Exception) { CelebrationTone.SOLEMN },
                    group = try { CelebrationGroup.valueOf(doc.getString("group") ?: "NONE") } catch(e: Exception) { CelebrationGroup.NONE },
                    reminderHour = doc.getLong("reminderHour")?.toInt() ?: 9,
                    reminderMinute = doc.getLong("reminderMinute")?.toInt() ?: 0,
                    savedGreeting = doc.getString("savedGreeting"),
                    giftIdeas = doc.getString("giftIdeas"),
                    greetingHistory = doc.getString("greetingHistory") ?: ""
                )
                dao.insertCelebration(c)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
