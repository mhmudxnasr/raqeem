package com.raqeem.app.domain.usecase

import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.Transfer
import com.raqeem.app.domain.repository.TransferRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllTransfersUseCase @Inject constructor(
    private val repository: TransferRepository,
) {
    operator fun invoke(): Flow<List<Transfer>> {
        return repository.getAll()
    }
}

class GetTransfersByAccountUseCase @Inject constructor(
    private val repository: TransferRepository,
) {
    operator fun invoke(accountId: String): Flow<List<Transfer>> {
        return repository.getByAccount(accountId)
    }
}

class GetTransferUseCase @Inject constructor(
    private val repository: TransferRepository,
) {
    operator fun invoke(id: String): Flow<Transfer?> {
        return repository.getById(id)
    }
}

class UpdateTransferUseCase @Inject constructor(
    private val repository: TransferRepository,
) {
    suspend operator fun invoke(transfer: Transfer): Result<Unit> {
        if (transfer.fromAmountCents <= 0) {
            return Result.Error("Amount must be greater than 0")
        }
        if (transfer.fromAccountId == transfer.toAccountId) {
            return Result.Error("Cannot transfer to the same account")
        }
        return repository.update(transfer)
    }
}

class DeleteTransferUseCase @Inject constructor(
    private val repository: TransferRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return repository.delete(id)
    }
}

