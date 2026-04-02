package com.raqeem.app.domain.usecase

import com.raqeem.app.domain.model.Transaction
import com.raqeem.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    operator fun invoke(limit: Int = 20): Flow<List<Transaction>> {
        return repository.getRecent(limit)
    }
}

class GetAllTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    operator fun invoke(): Flow<List<Transaction>> {
        return repository.getAll()
    }
}

class GetTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    operator fun invoke(id: String): Flow<Transaction?> {
        return repository.getById(id)
    }
}
