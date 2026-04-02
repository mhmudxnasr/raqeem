package com.raqeem.app.domain.usecase

import com.raqeem.app.domain.model.LedgerEntry
import com.raqeem.app.domain.model.Transfer
import com.raqeem.app.domain.repository.AccountRepository
import com.raqeem.app.domain.repository.CategoryRepository
import com.raqeem.app.domain.repository.GoalRepository
import com.raqeem.app.domain.repository.TransactionRepository
import com.raqeem.app.domain.repository.TransferRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetLedgerEntriesUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val transferRepository: TransferRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val goalRepository: GoalRepository,
) {
    operator fun invoke(): Flow<List<LedgerEntry>> {
        return combine(
            transactionRepository.getAll(),
            transferRepository.getAll(),
            accountRepository.getAll(),
            categoryRepository.getAll(),
            goalRepository.getAll(),
        ) { transactions, transfers, accounts, categories, goals ->
            val accountsById = accounts.associateBy { it.id }
            val categoriesById = categories.associateBy { it.id }
            val goalsById = goals.associateBy { it.id }

            buildList {
                transactions.forEach { transaction ->
                    add(
                        LedgerEntry.TransactionEntry(
                            transaction.copy(
                                account = transaction.account ?: accountsById[transaction.accountId],
                                category = transaction.categoryId?.let(categoriesById::get),
                            ),
                        ),
                    )
                }
                transfers.forEach { transfer ->
                    add(
                        LedgerEntry.TransferEntry(
                            transfer.withJoinedReferences(accountsById, goalsById),
                        ),
                    )
                }
            }.sortedWith(compareByDescending<LedgerEntry> { it.date }.thenByDescending { it.createdAt })
        }
    }
}

class GetLedgerEntriesForAccountUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val transferRepository: TransferRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val goalRepository: GoalRepository,
) {
    operator fun invoke(accountId: String): Flow<List<LedgerEntry>> {
        return combine(
            transactionRepository.getByAccount(accountId),
            transferRepository.getByAccount(accountId),
            accountRepository.getAll(),
            categoryRepository.getAll(),
            goalRepository.getAll(),
        ) { transactions, transfers, accounts, categories, goals ->
            val accountsById = accounts.associateBy { it.id }
            val categoriesById = categories.associateBy { it.id }
            val goalsById = goals.associateBy { it.id }

            buildList {
                transactions.forEach { transaction ->
                    add(
                        LedgerEntry.TransactionEntry(
                            transaction.copy(
                                account = transaction.account ?: accountsById[transaction.accountId],
                                category = transaction.categoryId?.let(categoriesById::get),
                            ),
                        ),
                    )
                }
                transfers.forEach { transfer ->
                    add(
                        LedgerEntry.TransferEntry(
                            transfer.withJoinedReferences(accountsById, goalsById),
                        ),
                    )
                }
            }.sortedWith(compareByDescending<LedgerEntry> { it.date }.thenByDescending { it.createdAt })
        }
    }
}

private fun Transfer.withJoinedReferences(
    accountsById: Map<String, com.raqeem.app.domain.model.Account>,
    goalsById: Map<String, com.raqeem.app.domain.model.Goal>,
): Transfer {
    return copy(
        fromAccount = fromAccount ?: accountsById[fromAccountId],
        toAccount = toAccount ?: accountsById[toAccountId],
        goal = goalId?.let(goalsById::get),
    )
}

