package com.raqeem.app.data.local

import com.raqeem.app.data.local.dao.AccountDao
import com.raqeem.app.data.local.dao.GoalDao
import com.raqeem.app.data.local.dao.TransactionDao
import com.raqeem.app.data.local.dao.TransferDao
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalFinanceSyncer @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val transferDao: TransferDao,
    private val goalDao: GoalDao,
) {

    suspend fun syncAccounts(accountIds: Set<String>) {
        accountIds.filter { it.isNotBlank() }.forEach { accountId ->
            val initialAmount = accountDao.getInitialAmountCents(accountId) ?: return@forEach
            val balance = initialAmount +
                transactionDao.getBalanceDeltaForAccount(accountId) +
                transferDao.getIncomingTotal(accountId) -
                transferDao.getOutgoingTotal(accountId)

            accountDao.updateBalance(
                id = accountId,
                balanceCents = balance,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            )
        }
    }

    suspend fun syncGoals(goalIds: Set<String>) {
        goalIds.filter { it.isNotBlank() }.forEach { goalId ->
            if (goalDao.findById(goalId) == null) return@forEach

            goalDao.updateCurrentCents(
                id = goalId,
                currentCents = transferDao.getGoalFundingTotal(goalId),
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            )
        }
    }
}
