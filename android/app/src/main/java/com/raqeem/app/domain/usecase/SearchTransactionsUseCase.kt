package com.raqeem.app.domain.usecase

import com.raqeem.app.domain.model.Transaction
import com.raqeem.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    operator fun invoke(query: String): Flow<List<Transaction>> {
        return repository.search(query)
    }
}
