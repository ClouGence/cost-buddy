package com.costbuddy.subscription;

import com.costbuddy.motherboard.MotherboardFailureType;
import com.costbuddy.motherboard.MotherboardGateway;
import com.costbuddy.motherboard.MotherboardGatewayException;
import com.costbuddy.motherboard.MotherboardProperties;
import com.motherboard.sdk.model.request.SubscriptionPurchaseRequest;
import com.motherboard.sdk.model.response.UserSubscription;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "motherboard", name = "enabled", havingValue = "true")
public class FreeSubscriptionService {

    private static final int            LOCK_STRIPE_COUNT  = 64;
    private static final String         DUPLICATE_KEY_CODE = "DUPLICATE_KEY";

    private final MotherboardGateway    motherboardGateway;
    private final MotherboardProperties motherboardProperties;
    private final Lock[]                userLocks          = IntStream.range(0, LOCK_STRIPE_COUNT).mapToObj(ignored -> new ReentrantLock()).toArray(Lock[]::new);

    public FreeSubscriptionService(MotherboardGateway motherboardGateway, MotherboardProperties motherboardProperties){
        if (motherboardProperties.getFreePlanId() <= 0) {
            throw new IllegalStateException("motherboard.free-plan-id must be greater than zero when Motherboard is enabled");
        }
        this.motherboardGateway = motherboardGateway;
        this.motherboardProperties = motherboardProperties;
    }

    public UserSubscription ensureFreeSubscription(long userId) {
        UserSubscription existing = findProductSubscription(userId);
        if (existing != null) {
            return existing;
        }

        Lock lock = userLock(userId);
        lock.lock();
        try {
            existing = findProductSubscription(userId);
            if (existing != null) {
                return existing;
            }
            return purchaseFreeSubscription(userId);
        } finally {
            lock.unlock();
        }
    }

    private UserSubscription purchaseFreeSubscription(long userId) {
        try {
            return motherboardGateway
                .purchaseSubscription(new SubscriptionPurchaseRequest(userId, motherboardProperties.getProductId(), motherboardProperties.getFreePlanId(), true));
        } catch (MotherboardGatewayException exception) {
            if (!isDuplicateKey(exception)) {
                throw exception;
            }
            UserSubscription concurrentSubscription = findProductSubscription(userId);
            if (concurrentSubscription != null) {
                return concurrentSubscription;
            }
            throw exception;
        }
    }

    private UserSubscription findProductSubscription(long userId) {
        List<UserSubscription> subscriptions = motherboardGateway.listSubscriptions(userId);
        if (subscriptions == null) {
            return null;
        }
        return subscriptions.stream().filter(subscription -> Objects.equals(subscription.productId(), motherboardProperties.getProductId())).findFirst().orElse(null);
    }

    private boolean isDuplicateKey(MotherboardGatewayException exception) {
        return exception.getFailureType() == MotherboardFailureType.API && DUPLICATE_KEY_CODE.equals(exception.getUpstreamCode());
    }

    private Lock userLock(long userId) {
        return userLocks[Math.floorMod(Long.hashCode(userId), userLocks.length)];
    }
}
