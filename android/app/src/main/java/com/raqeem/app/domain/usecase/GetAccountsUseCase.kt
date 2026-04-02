package com.raqeem.app.domain.usecase

import com.raqeem.app.domain.model.Account
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAccountsUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    operator fun invoke(): Flow<List<Account>> {
        return repository.getAll()
    }
}

class GetAccountUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    operator fun invoke(id: String): Flow<Account?> {
        return repository.getById(id)
    }
}

class AddAccountUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(account: Account): Result<Unit> {
        val normalizedName = account.name.trim()
        if (normalizedName.isBlank()) {
            return Result.Error("Account name can't be blank.")
        }
        if (account.initialAmountCents < 0 || account.balanceCents < 0) {
            return Result.Error("Account balances can't be negative.")
        }
        return repository.add(account.copy(name = normalizedName))
    }
}

class UpdateAccountUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(account: Account): Result<Unit> {
        val normalizedName = account.name.trim()
        if (normalizedName.isBlank()) {
            return Result.Error("Account name can't be blank.")
        }
        if (account.initialAmountCents < 0 || account.balanceCents < 0) {
            return Result.Error("Account balances can't be negative.")
        }
        return repository.update(account.copy(name = normalizedName))
    }
}

class DeleteAccountUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        if (id.isBlank()) {
            return Result.Error("Choose an account to delete.")
        }
        return repository.delete(id)
    }
}
