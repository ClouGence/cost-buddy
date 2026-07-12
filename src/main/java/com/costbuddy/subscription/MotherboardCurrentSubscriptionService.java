package com.costbuddy.subscription;

import com.costbuddy.auth.CurrentUserProvider;
import com.costbuddy.dto.response.CurrentSubscriptionResponse;
import com.costbuddy.motherboard.MotherboardGateway;
import com.costbuddy.motherboard.MotherboardProperties;
import com.motherboard.sdk.model.SubscriptionStatus;
import com.motherboard.sdk.model.response.UserSubscription;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "motherboard", name = "enabled", havingValue = "true")
public class MotherboardCurrentSubscriptionService implements CurrentSubscriptionService {

    private final MotherboardGateway    motherboardGateway;
    private final MotherboardProperties motherboardProperties;
    private final CurrentUserProvider   currentUserProvider;

    public MotherboardCurrentSubscriptionService(MotherboardGateway motherboardGateway, MotherboardProperties motherboardProperties, CurrentUserProvider currentUserProvider){
        this.motherboardGateway = motherboardGateway;
        this.motherboardProperties = motherboardProperties;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public CurrentSubscriptionResponse getCurrent() {
        List<UserSubscription> subscriptions = motherboardGateway.listSubscriptions(currentUserProvider.motherboardUserId());
        UserSubscription current = subscriptions == null ? null : subscriptions.stream()
            .filter(subscription -> Objects.equals(subscription.productId(), motherboardProperties.getProductId()))
            .max(currentSubscriptionComparator())
            .orElse(null);
        if (current == null) {
            return new CurrentSubscriptionResponse(false, null, null, null, "NOT_SUBSCRIBED", null, false);
        }
        return new CurrentSubscriptionResponse(true,
            current.id(),
            current.planId(),
            planName(current.planId()),
            current.status() == null ? null : current.status().name(),
            current.currentPeriodEnd(),
            current.autoRenew());
    }

    private Comparator<UserSubscription> currentSubscriptionComparator() {
        return Comparator.comparing((UserSubscription subscription) -> subscription.status() == SubscriptionStatus.ACTIVE)
            .thenComparing(UserSubscription::currentPeriodEnd, Comparator.nullsFirst(LocalDateTime::compareTo))
            .thenComparing(UserSubscription::id, Comparator.nullsFirst(Long::compareTo));
    }

    private String planName(Long planId) {
        if (Objects.equals(planId, motherboardProperties.getFreePlanId())) {
            return "Free";
        }
        return planId == null ? null : "Plan #" + planId;
    }
}
