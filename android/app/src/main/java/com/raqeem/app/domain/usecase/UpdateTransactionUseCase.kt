package com.raqeem.app.domain.usecase

import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.Transaction
import com.raqeem.app.domain.repository.TransactionRepository
import javax.inject.Inject

class UpdateTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(transaction: Transaction): Result<Unit> {
        if (transaction.amountCents <= 0) {
            return Result.Error("Amount must be greater than 0")
        }
        return repository.update(transaction)
    }
}
