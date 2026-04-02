package com.raqeem.app.domain.usecase

import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.Transfer
import com.raqeem.app.domain.repository.TransferRepository
import javax.inject.Inject

class AddTransferUseCase @Inject constructor(
    private val repository: TransferRepository,
) {
    suspend operator fun invoke(transfer: Transfer): Result<Unit> {
        if (transfer.fromAmountCents <= 0) {
            return Result.Error("Amount must be greater than 0")
        }
        if (transfer.fromAccountId == transfer.toAccountId) {
            return Result.Error("Cannot transfer to the same account")
        }
        return repository.add(transfer)
    }
}
