package com.raqeem.app.data.repository

import com.raqeem.app.data.local.dao.CategoryDao
import com.raqeem.app.data.local.dao.SyncQueueDao
import com.raqeem.app.data.local.entity.SyncQueueEntity
import com.raqeem.app.data.mapper.toDomain
import com.raqeem.app.data.mapper.toEntity
import com.raqeem.app.domain.model.Category
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.TransactionType
import com.raqeem.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val syncQueueDao: SyncQueueDao,
) : CategoryRepository {

    override fun getAll(): Flow<List<Category>> {
        return categoryDao.getAll().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getByType(type: TransactionType): Flow<List<Category>> {
        return categoryDao.getByType(type.toApiString()).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getById(id: String): Flow<Category?> {
        return categoryDao.getById(id).map { it?.toDomain() }
    }

    override suspend fun add(category: Category): Result<Unit> {
        return try {
            categoryDao.insert(category.toEntity())
            syncQueueDao.insert(
                SyncQueueEntity(
                    id = UUID.randomUUID().toString(),
                    tableName = "categories",
                    recordId = category.id,
                    operation = "upsert",
                    payload = buildCategoryJson(category),
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to save category", e)
        }
    }

    override suspend fun update(category: Category): Result<Unit> {
        return try {
            categoryDao.update(category.toEntity())
            syncQueueDao.insert(
                SyncQueueEntity(
                    id = UUID.randomUUID().toString(),
                    tableName = "categories",
                    recordId = category.id,
                    operation = "upsert",
                    payload = buildCategoryJson(category),
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to update category", e)
        }
    }

    override suspend fun delete(id: String): Result<Unit> {
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            categoryDao.softDelete(id, now, now)
            syncQueueDao.insert(
                SyncQueueEntity(
                    id = UUID.randomUUID().toString(),
                    tableName = "categories",
                    recordId = id,
                    operation = "delete",
                    payload = buildJsonObject { put("id", id) }.toString(),
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to delete category", e)
        }
    }

    private fun buildCategoryJson(category: Category): String {
        return buildJsonObject {
            put("id", category.id)
            put("user_id", category.userId)
            put("name", category.name)
            put("type", category.type.toApiString())
            put("icon", category.icon)
            put("color", category.color)
            category.budgetCents?.let { put("budget_cents", it) }
            put("updated_at", Clock.System.now().toString())
        }.toString()
    }
}
