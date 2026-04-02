package com.raqeem.app.domain.usecase

import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.Subscription
import com.raqeem.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllSubscriptionsUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    operator fun invoke(): Flow<List<Subscription>> {
        return repository.getAll()
    }
}

class GetSubscriptionsUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    operator fun invoke(): Flow<List<Subscription>> {
        return repository.getActive()
    }
}

class AddSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(subscription: Subscription): Result<Unit> {
        return repository.add(subscription)
    }
}

class UpdateSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(subscription: Subscription): Result<Unit> {
        return repository.update(subscription)
    }
}

class DeleteSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(subscriptionId: String): Result<Unit> {
        return repository.delete(subscriptionId)
    }
}
