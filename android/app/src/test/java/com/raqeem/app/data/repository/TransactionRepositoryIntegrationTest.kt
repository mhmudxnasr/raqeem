package com.raqeem.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.raqeem.app.data.local.AppDatabase
import com.raqeem.app.data.local.LocalFinanceSyncer
import com.raqeem.app.data.local.entity.AccountEntity
import com.raqeem.app.data.local.entity.CategoryEntity
import com.raqeem.app.domain.model.Currency
import com.raqeem.app.domain.model.Transaction
import com.raqeem.app.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransactionRepositoryIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: TransactionRepositoryImpl
    private val now: Instant = Instant.parse("2026-04-01T12:00:00Z")

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()

        repository = TransactionRepositoryImpl(
            transactionDao = database.transactionDao(),
            syncQueueDao = database.syncQueueDao(),
            localFinanceSyncer = LocalFinanceSyncer(
                accountDao = database.accountDao(),
                transactionDao = database.transactionDao(),
                transferDao = database.transferDao(),
                goalDao = database.goalDao(),
            ),
        )

        database.accountDao().insert(
            AccountEntity(
                id = "account-1",
                userId = "user-1",
                name = "Binance Free",
                type = "checking",
                currency = "USD",
                initialAmountCents = 20_000,
                balanceCents = 20_000,
                sortOrder = 1,
                createdAt = now,
                updatedAt = now,
            ),
        )
        database.categoryDao().insert(
            CategoryEntity(
                id = "category-food",
                userId = "user-1",
                name = "Food & Dining",
                type = "expense",
                createdAt = now,
                updatedAt = now,
            ),
        )
        database.categoryDao().insert(
            CategoryEntity(
                id = "category-income",
                userId = "user-1",
                name = "Outlier",
                type = "income",
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `search matches category name and exact amount`() = runTest {
        repository.add(
            sampleTransaction(
                id = "tx-food",
                amountCents = 1_250,
                categoryId = "category-food",
                note = "Lunch with friends",
            ),
        )
        repository.add(
            sampleTransaction(
                id = "tx-income",
                type = TransactionType.INCOME,
                amountCents = 5_000,
                categoryId = "category-income",
                note = "Salary",
            ),
        )

        val categoryMatches = repository.search("Food").first()
        val amountMatches = repository.search("12.50").first()

        assertEquals(listOf("tx-food"), categoryMatches.map(Transaction::id))
        assertEquals(listOf("tx-food"), amountMatches.map(Transaction::id))
    }

    @Test
    fun `update recalculates account balance`() = runTest {
        repository.add(
            sampleTransaction(
                id = "tx-update",
                amountCents = 1_000,
                categoryId = "category-food",
            ),
        )

        repository.update(
            sampleTransaction(
                id = "tx-update",
                amountCents = 3_000,
                categoryId = "category-food",
            ),
        )

        val account = database.accountDao().findById("account-1")

        assertEquals(17_000, account?.balanceCents)
    }

    @Test
    fun `delete hides transaction and recalculates account balance`() = runTest {
        repository.add(
            sampleTransaction(
                id = "tx-delete",
                type = TransactionType.INCOME,
                amountCents = 5_000,
                categoryId = "category-income",
            ),
        )

        repository.delete("tx-delete")

        val account = database.accountDao().findById("account-1")
        val visibleTransactions = repository.getAll().first()

        assertEquals(20_000, account?.balanceCents)
        assertTrue(visibleTransactions.isEmpty())
    }

    @Test
    fun `get all orders by date desc then created time desc`() = runTest {
        repository.add(
            sampleTransaction(
                id = "tx-oldest",
                amountCents = 700,
                categoryId = "category-food",
                date = LocalDate.parse("2026-03-29"),
                createdAt = Instant.parse("2026-03-29T08:00:00Z"),
            ),
        )
        repository.add(
            sampleTransaction(
                id = "tx-newer-time",
                amountCents = 900,
                categoryId = "category-food",
                date = LocalDate.parse("2026-04-01"),
                createdAt = Instant.parse("2026-04-01T11:00:00Z"),
            ),
        )
        repository.add(
            sampleTransaction(
                id = "tx-older-time",
                amountCents = 800,
                categoryId = "category-food",
                date = LocalDate.parse("2026-04-01"),
                createdAt = Instant.parse("2026-04-01T09:00:00Z"),
            ),
        )

        val orderedIds = repository.getAll().first().map(Transaction::id)

        assertEquals(
            listOf("tx-newer-time", "tx-older-time", "tx-oldest"),
            orderedIds,
        )
    }

    private fun sampleTransaction(
        id: String,
        type: TransactionType = TransactionType.EXPENSE,
        amountCents: Int,
        categoryId: String,
        note: String? = null,
        date: LocalDate = LocalDate.parse("2026-04-01"),
        createdAt: Instant = Clock.System.now(),
    ): Transaction {
        return Transaction(
            id = id,
            userId = "user-1",
            accountId = "account-1",
            categoryId = categoryId,
            type = type,
            amountCents = amountCents,
            currency = Currency.USD,
            note = note,
            date = date,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }
}
