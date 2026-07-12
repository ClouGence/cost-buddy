package com.costbuddy.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.costbuddy.motherboard.MotherboardFailureType;
import com.costbuddy.motherboard.MotherboardGateway;
import com.costbuddy.motherboard.MotherboardGatewayException;
import com.costbuddy.motherboard.MotherboardProperties;
import com.motherboard.sdk.model.SubscriptionStatus;
import com.motherboard.sdk.model.request.SubscriptionPurchaseRequest;
import com.motherboard.sdk.model.response.UserSubscription;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FreeSubscriptionServiceTest {

    private static final long       USER_ID      = 42L;
    private static final long       PRODUCT_ID   = 10L;
    private static final long       FREE_PLAN_ID = 100L;

    private MotherboardGateway      motherboardGateway;
    private MotherboardProperties   motherboardProperties;
    private FreeSubscriptionService service;

    @BeforeEach
    void setUp() {
        motherboardGateway = mock(MotherboardGateway.class);
        motherboardProperties = new MotherboardProperties();
        motherboardProperties.setProductId(PRODUCT_ID);
        motherboardProperties.setFreePlanId(FREE_PLAN_ID);
        service = new FreeSubscriptionService(motherboardGateway, motherboardProperties);
    }

    @Test
    void keepsAnyExistingSubscriptionForCurrentProduct() {
        UserSubscription paidSubscription = subscription(900L);
        when(motherboardGateway.listSubscriptions(USER_ID)).thenReturn(List.of(paidSubscription));

        UserSubscription result = service.ensureFreeSubscription(USER_ID);

        assertThat(result).isSameAs(paidSubscription);
        verify(motherboardGateway, never()).purchaseSubscription(any());
    }

    @Test
    void checksAgainUnderLockBeforePurchasingFreePlan() {
        UserSubscription concurrentSubscription = subscription(FREE_PLAN_ID);
        when(motherboardGateway.listSubscriptions(USER_ID)).thenReturn(List.<UserSubscription>of()).thenReturn(List.of(concurrentSubscription));

        UserSubscription result = service.ensureFreeSubscription(USER_ID);

        assertThat(result).isSameAs(concurrentSubscription);
        verify(motherboardGateway, times(2)).listSubscriptions(USER_ID);
        verify(motherboardGateway, never()).purchaseSubscription(any());
    }

    @Test
    void purchasesConfiguredFreePlanWhenSubscriptionIsStillMissing() {
        UserSubscription created = subscription(FREE_PLAN_ID);
        when(motherboardGateway.listSubscriptions(USER_ID)).thenReturn(List.<UserSubscription>of());
        when(motherboardGateway.purchaseSubscription(any())).thenReturn(created);

        UserSubscription result = service.ensureFreeSubscription(USER_ID);

        assertThat(result).isSameAs(created);
        ArgumentCaptor<SubscriptionPurchaseRequest> requestCaptor = ArgumentCaptor.forClass(SubscriptionPurchaseRequest.class);
        verify(motherboardGateway).purchaseSubscription(requestCaptor.capture());
        assertThat(requestCaptor.getValue().userId()).isEqualTo(USER_ID);
        assertThat(requestCaptor.getValue().productId()).isEqualTo(PRODUCT_ID);
        assertThat(requestCaptor.getValue().planId()).isEqualTo(FREE_PLAN_ID);
        assertThat(requestCaptor.getValue().autoRenew()).isTrue();
    }

    @Test
    void recoversFromCrossInstanceDuplicateByReadingCreatedSubscription() {
        UserSubscription created = subscription(FREE_PLAN_ID);
        MotherboardGatewayException duplicate = mock(MotherboardGatewayException.class);
        when(duplicate.getFailureType()).thenReturn(MotherboardFailureType.API);
        when(duplicate.getUpstreamCode()).thenReturn("DUPLICATE_KEY");
        when(motherboardGateway.listSubscriptions(USER_ID))
            .thenReturn(List.<UserSubscription>of())
            .thenReturn(List.<UserSubscription>of())
            .thenReturn(List.of(created));
        when(motherboardGateway.purchaseSubscription(any())).thenThrow(duplicate);

        UserSubscription result = service.ensureFreeSubscription(USER_ID);

        assertThat(result).isSameAs(created);
        verify(motherboardGateway).purchaseSubscription(any());
        verify(motherboardGateway, times(3)).listSubscriptions(USER_ID);
    }

    @Test
    void doesNotRetryPurchaseWhenDuplicateCannotBeConfirmed() {
        MotherboardGatewayException duplicate = mock(MotherboardGatewayException.class);
        when(duplicate.getFailureType()).thenReturn(MotherboardFailureType.API);
        when(duplicate.getUpstreamCode()).thenReturn("DUPLICATE_KEY");
        when(motherboardGateway.listSubscriptions(USER_ID)).thenReturn(List.<UserSubscription>of());
        when(motherboardGateway.purchaseSubscription(any())).thenThrow(duplicate);

        assertThatThrownBy(() -> service.ensureFreeSubscription(USER_ID)).isSameAs(duplicate);
        verify(motherboardGateway).purchaseSubscription(any());
    }

    @Test
    void serializesConcurrentLoginPurchasesWithinOneApplicationInstance() throws Exception {
        UserSubscription created = subscription(FREE_PLAN_ID);
        AtomicReference<UserSubscription> stored = new AtomicReference<>();
        AtomicInteger listCalls = new AtomicInteger();
        CountDownLatch initialLookups = new CountDownLatch(2);
        when(motherboardGateway.listSubscriptions(USER_ID)).thenAnswer(invocation -> {
            int call = listCalls.incrementAndGet();
            if (call <= 2) {
                initialLookups.countDown();
                if (!initialLookups.await(2, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("concurrent initial lookups did not complete");
                }
                return List.<UserSubscription>of();
            }
            UserSubscription value = stored.get();
            return value == null ? List.<UserSubscription>of() : List.of(value);
        });
        when(motherboardGateway.purchaseSubscription(any())).thenAnswer(invocation -> {
            stored.set(created);
            return created;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<UserSubscription> first = executor.submit(() -> service.ensureFreeSubscription(USER_ID));
            Future<UserSubscription> second = executor.submit(() -> service.ensureFreeSubscription(USER_ID));

            assertThat(first.get(3, TimeUnit.SECONDS)).isSameAs(created);
            assertThat(second.get(3, TimeUnit.SECONDS)).isSameAs(created);
        } finally {
            executor.shutdownNow();
        }
        verify(motherboardGateway).purchaseSubscription(any());
    }

    @Test
    void requiresConfiguredFreePlan() {
        motherboardProperties.setFreePlanId(0L);

        assertThatThrownBy(() -> new FreeSubscriptionService(motherboardGateway, motherboardProperties)).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("motherboard.free-plan-id");
    }

    private UserSubscription subscription(long planId) {
        return new UserSubscription(1L, USER_ID, PRODUCT_ID, planId, SubscriptionStatus.ACTIVE, true, null, null, null, null, null, null, null);
    }
}
