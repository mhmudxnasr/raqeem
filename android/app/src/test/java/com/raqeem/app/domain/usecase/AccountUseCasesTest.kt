package com.raqeem.app.domain.usecase

import com.raqeem.app.domain.model.Account
import com.raqeem.app.domain.model.AccountType
import com.raqeem.app.domain.model.Currency
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.repository.AccountRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountUseCasesTest {

    private val repository = mockk<AccountRepository>()

    @Test
    fun `add account rejects blank names`() = runTest {
        val useCase = AddAccountUseCase(repository)

        val result = useCase(sampleAccount(name = "   "))

        assertTrue(result is Result.Error)
        assertEquals("Account name can't be blank.", (result as Result.Error).message)
        coVerify(exactly = 0) { repository.add(any()) }
    }

    @Test
    fun `add account rejects negative balances`() = runTest {
        val useCase = AddAccountUseCase(repository)

        val result = useCase(
            sampleAccount(
                initialAmountCents = -100,
                balanceCents = -100,
            ),
        )

        assertTrue(result is Result.Error)
        assertEquals("Account balances can't be negative.", (result as Result.Error).message)
        coVerify(exactly = 0) { repository.add(any()) }
    }

    @Test
    fun `add account trims names before saving`() = runTest {
        val useCase = AddAccountUseCase(repository)
        coEvery { repository.add(any()) } returns Result.Success(Unit)

        val result = useCase(sampleAccount(name = "  Wallet  "))

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) {
            repository.add(
                match { account ->
                    account.name == "Wallet" &&
                        account.initialAmountCents == 25_000 &&
                        account.balanceCents == 25_000
                },
            )
        }
    }

    @Test
    fun `delete account rejects blank ids`() = runTest {
        val useCase = DeleteAccountUseCase(repository)

        val result = useCase("   ")

        assertTrue(result is Result.Error)
        assertEquals("Choose an account to delete.", (result as Result.Error).message)
        coVerify(exactly = 0) { repository.delete(any()) }
    }

    private fun sampleAccount(
        name: String = "Wallet",
        initialAmountCents: Int = 25_000,
        balanceCents: Int = 25_000,
    ): Account {
        return Account(
            id = "account-1",
            userId = "user-1",
            name = name,
            type = AccountType.CASH,
            currency = Currency.USD,
            initialAmountCents = initialAmountCents,
            balanceCents = balanceCents,
        )
    }
}
