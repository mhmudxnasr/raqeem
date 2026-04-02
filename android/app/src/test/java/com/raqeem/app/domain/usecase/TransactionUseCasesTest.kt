package com.raqeem.app.domain.usecase

import com.raqeem.app.domain.model.Currency
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.Transaction
import com.raqeem.app.domain.model.TransactionType
import com.raqeem.app.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionUseCasesTest {

    private val repository = mockk<TransactionRepository>()

    @Test
    fun `update transaction rejects zero amount`() = runTest {
        val useCase = UpdateTransactionUseCase(repository)

        val result = useCase(sampleTransaction(amountCents = 0))

        assertTrue(result is Result.Error)
        assertEquals("Amount must be greater than 0", (result as Result.Error).message)
        coVerify(exactly = 0) { repository.update(any()) }
    }

    @Test
    fun `update transaction forwards valid transactions`() = runTest {
        val useCase = UpdateTransactionUseCase(repository)
        val transaction = sampleTransaction(amountCents = 1_250)
        coEvery { repository.update(transaction) } returns Result.Success(Unit)

        val result = useCase(transaction)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.update(transaction) }
    }

    @Test
    fun `delete transaction rejects blank ids`() = runTest {
        val useCase = DeleteTransactionUseCase(repository)

        val result = useCase("   ")

        assertTrue(result is Result.Error)
        assertEquals("Choose a transaction to delete.", (result as Result.Error).message)
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    private fun sampleTransaction(amountCents: Int): Transaction {
        return Transaction(
            id = "transaction-1",
            userId = "user-1",
            accountId = "account-1",
            categoryId = "category-1",
            type = TransactionType.EXPENSE,
            amountCents = amountCents,
            currency = Currency.USD,
            date = LocalDate.parse("2026-04-01"),
        )
    }
}
