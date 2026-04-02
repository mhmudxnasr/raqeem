package com.raqeem.app.domain.usecase

import com.raqeem.app.domain.model.Category
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.TransactionType
import com.raqeem.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    operator fun invoke(): Flow<List<Category>> {
        return repository.getAll()
    }
}

class GetCategoriesByTypeUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    operator fun invoke(type: TransactionType): Flow<List<Category>> {
        return repository.getByType(type)
    }
}

class UpdateCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(category: Category): Result<Unit> {
        return repository.update(category)
    }
}

class AddCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(category: Category): Result<Unit> {
        val normalizedName = category.name.trim()
        if (normalizedName.isBlank()) {
            return Result.Error("Category name can't be blank.")
        }
        return repository.add(category.copy(name = normalizedName))
    }
}

class DeleteCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        if (id.isBlank()) {
            return Result.Error("Choose a category to delete.")
        }
        return repository.delete(id)
    }
}
