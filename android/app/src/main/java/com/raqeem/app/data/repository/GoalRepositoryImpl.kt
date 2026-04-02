package com.raqeem.app.data.repository

import com.raqeem.app.data.local.dao.GoalDao
import com.raqeem.app.data.local.dao.SyncQueueDao
import com.raqeem.app.data.local.entity.SyncQueueEntity
import com.raqeem.app.data.mapper.toDomain
import com.raqeem.app.data.mapper.toEntity
import com.raqeem.app.domain.model.Goal
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao,
    private val syncQueueDao: SyncQueueDao,
) : GoalRepository {

    override fun getAll(): Flow<List<Goal>> {
        return goalDao.getAll().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getById(id: String): Flow<Goal?> {
        return goalDao.getById(id).map { it?.toDomain() }
    }

    override suspend fun add(goal: Goal): Result<Unit> {
        return try {
            goalDao.insert(goal.toEntity())
            enqueueSync("upsert", goal.id, buildGoalJson(goal))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to save goal", e)
        }
    }

    override suspend fun update(goal: Goal): Result<Unit> {
        return try {
            goalDao.update(goal.toEntity())
            enqueueSync("upsert", goal.id, buildGoalJson(goal))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to update goal", e)
        }
    }

    override suspend fun delete(id: String): Result<Unit> {
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            goalDao.softDelete(id, now, now)
            enqueueSync("delete", id, buildJsonObject { put("id", id) }.toString())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to delete goal", e)
        }
    }

    override suspend fun markComplete(id: String): Result<Unit> {
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            goalDao.markComplete(id, now)
            enqueueSync("upsert", id, buildJsonObject {
                put("id", id)
                put("is_completed", true)
                put("updated_at", Clock.System.now().toString())
            }.toString())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to complete goal", e)
        }
    }

    private suspend fun enqueueSync(operation: String, recordId: String, payload: String) {
        syncQueueDao.insert(
            SyncQueueEntity(
                id = UUID.randomUUID().toString(),
                tableName = "goals",
                recordId = recordId,
                operation = operation,
                payload = payload,
            )
        )
    }

    private fun buildGoalJson(goal: Goal): String {
        return buildJsonObject {
            put("id", goal.id)
            put("user_id", goal.userId)
            put("name", goal.name)
            put("target_cents", goal.targetCents)
            put("current_cents", goal.currentCents)
            put("currency", goal.currency.name)
            goal.deadline?.let { put("deadline", it.toString()) }
            put("is_completed", goal.isCompleted)
            put("icon", goal.icon)
            goal.note?.let { put("note", it) }
            put("updated_at", Clock.System.now().toString())
        }.toString()
    }
}
