package com.costbuddy.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.costbuddy.auth.CurrentUserProvider;
import com.costbuddy.dto.response.CurrentSubscriptionResponse;
import com.costbuddy.motherboard.MotherboardGateway;
import com.costbuddy.motherboard.MotherboardProperties;
import com.motherboard.sdk.model.SubscriptionStatus;
import com.motherboard.sdk.model.response.UserSubscription;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MotherboardCurrentSubscriptionServiceTest {

    private static final long                     USER_ID      = 42L;
    private static final long                     PRODUCT_ID   = 7L;
    private static final long                     FREE_PLAN_ID = 8L;

    private MotherboardGateway                    gateway;
    private MotherboardCurrentSubscriptionService service;

    @BeforeEach
    void setUp() {
        gateway = mock(MotherboardGateway.class);
        MotherboardProperties properties = new MotherboardProperties();
        properties.setProductId(PRODUCT_ID);
        properties.setFreePlanId(FREE_PLAN_ID);
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        when(currentUserProvider.motherboardUserId()).thenReturn(USER_ID);
        service = new MotherboardCurrentSubscriptionService(gateway, properties, currentUserProvider);
    }

    @Test
    void returnsActiveFreeSubscriptionForCurrentProduct() {
        LocalDateTime periodEnd = LocalDateTime.of(2026, 8, 12, 10, 30);
        when(gateway.listSubscriptions(USER_ID)).thenReturn(List.of(subscription(1L, PRODUCT_ID, FREE_PLAN_ID, SubscriptionStatus.EXPIRED, periodEnd
            .minusMonths(1), false), subscription(2L, PRODUCT_ID + 1, FREE_PLAN_ID, SubscriptionStatus.ACTIVE, periodEnd
                .plusMonths(1), true), subscription(3L, PRODUCT_ID, FREE_PLAN_ID, SubscriptionStatus.ACTIVE, periodEnd, true)));

        CurrentSubscriptionResponse response = service.getCurrent();

        assertThat(response.isSubscribed()).isTrue();
        assertThat(response.getSubscriptionId()).isEqualTo(3L);
        assertThat(response.getPlanName()).isEqualTo("Free");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getCurrentPeriodEnd()).isEqualTo(periodEnd);
        assertThat(response.getAutoRenew()).isTrue();
    }

    @Test
    void returnsNotSubscribedWhenCurrentProductHasNoSubscription() {
        when(gateway.listSubscriptions(USER_ID)).thenReturn(List.of());

        CurrentSubscriptionResponse response = service.getCurrent();

        assertThat(response.isSubscribed()).isFalse();
        assertThat(response.getStatus()).isEqualTo("NOT_SUBSCRIBED");
        assertThat(response.getPlanName()).isNull();
    }

    private UserSubscription subscription(Long id, Long productId, Long planId, SubscriptionStatus status, LocalDateTime periodEnd, Boolean autoRenew) {
        return new UserSubscription(id, USER_ID, productId, planId, status, autoRenew, periodEnd.minusMonths(1), periodEnd, null, null, null, null, null);
    }
}
