package com.raqeem.app.domain.usecase

import com.raqeem.app.domain.model.Goal
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGoalsUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    operator fun invoke(): Flow<List<Goal>> {
        return repository.getAll()
    }
}

class AddGoalUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    suspend operator fun invoke(goal: Goal): Result<Unit> {
        if (goal.targetCents <= 0) {
            return Result.Error("Target amount must be greater than 0")
        }
        if (goal.name.isBlank()) {
            return Result.Error("Goal name is required")
        }
        return repository.add(goal)
    }
}

class UpdateGoalUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    suspend operator fun invoke(goal: Goal): Result<Unit> {
        if (goal.targetCents <= 0) {
            return Result.Error("Target amount must be greater than 0")
        }
        if (goal.name.isBlank()) {
            return Result.Error("Goal name is required")
        }
        return repository.update(goal)
    }
}

class DeleteGoalUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        if (id.isBlank()) {
            return Result.Error("Goal id is required")
        }
        return repository.delete(id)
    }
}

class CompleteGoalUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return repository.markComplete(id)
    }
}
