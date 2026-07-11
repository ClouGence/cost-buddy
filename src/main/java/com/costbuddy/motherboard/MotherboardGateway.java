package com.costbuddy.motherboard;

import com.motherboard.sdk.MotherboardClient;
import com.motherboard.sdk.exception.MotherboardApiException;
import com.motherboard.sdk.exception.MotherboardSerializationException;
import com.motherboard.sdk.exception.MotherboardSigningException;
import com.motherboard.sdk.exception.MotherboardTransportException;
import com.motherboard.sdk.model.WalletCurrency;
import com.motherboard.sdk.model.request.AuthzBatchCheckRequest;
import com.motherboard.sdk.model.request.AuthzCheckRequest;
import com.motherboard.sdk.model.request.PermissionGrantRequest;
import com.motherboard.sdk.model.request.RechargeOrderRequest;
import com.motherboard.sdk.model.request.RechargePaymentResultRequest;
import com.motherboard.sdk.model.request.SsoSessionRequest;
import com.motherboard.sdk.model.request.SubscriptionPurchaseRequest;
import com.motherboard.sdk.model.request.UsageEventBatchRequest;
import com.motherboard.sdk.model.request.UsageEventRequest;
import com.motherboard.sdk.model.response.AuthzCheckResponse;
import com.motherboard.sdk.model.response.LedgerEntry;
import com.motherboard.sdk.model.response.PermissionGrant;
import com.motherboard.sdk.model.response.RechargeOrder;
import com.motherboard.sdk.model.response.RechargePaymentResultResponse;
import com.motherboard.sdk.model.response.RenewalAttempt;
import com.motherboard.sdk.model.response.SsoSessionResponse;
import com.motherboard.sdk.model.response.UsageEvent;
import com.motherboard.sdk.model.response.UsageReportResponse;
import com.motherboard.sdk.model.response.UserDetailResponse;
import com.motherboard.sdk.model.response.UserSubscription;
import com.motherboard.sdk.model.response.Wallet;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "motherboard", name = "enabled", havingValue = "true")
public class MotherboardGateway {

    private final MotherboardClient client;

    public MotherboardGateway(MotherboardClient client){
        this.client = client;
    }

    public SsoSessionResponse createSsoSession(SsoSessionRequest request) {
        return execute(() -> client.sso().createSession(request));
    }

    public UserDetailResponse getUser(long userId) {
        return execute(() -> client.users().get(userId));
    }

    public PermissionGrant grantPermission(PermissionGrantRequest request) {
        return execute(() -> client.permissions().grant(request));
    }

    public List<PermissionGrant> listPermissions(long userId) {
        return execute(() -> client.permissions().listByUser(userId));
    }

    public void revokePermission(long grantId) {
        execute(() -> {
            client.permissions().revoke(grantId);
            return null;
        });
    }

    public AuthzCheckResponse checkAuthorization(AuthzCheckRequest request) {
        return execute(() -> client.authz().check(request));
    }

    public List<AuthzCheckResponse> batchCheckAuthorization(AuthzBatchCheckRequest request) {
        return execute(() -> client.authz().batchCheck(request));
    }

    public Wallet getWallet(long userId) {
        return execute(() -> client.wallets().get(userId));
    }

    public Wallet getWallet(long userId, WalletCurrency currency) {
        return execute(() -> client.wallets().get(userId, currency));
    }

    public List<LedgerEntry> listWalletLedger(long userId) {
        return execute(() -> client.wallets().listLedger(userId));
    }

    public List<LedgerEntry> listWalletLedger(long userId, WalletCurrency currency) {
        return execute(() -> client.wallets().listLedger(userId, currency));
    }

    public RechargeOrder createRechargeOrder(RechargeOrderRequest request) {
        return execute(() -> client.wallets().createRechargeOrder(request));
    }

    public RechargePaymentResultResponse reportRechargePaymentResult(long orderId, RechargePaymentResultRequest request) {
        return execute(() -> client.wallets().reportPaymentResult(orderId, request));
    }

    public List<RechargeOrder> listRechargeOrders(long userId) {
        return execute(() -> client.wallets().listRechargeOrders(userId));
    }

    public UserSubscription purchaseSubscription(SubscriptionPurchaseRequest request) {
        return execute(() -> client.subscriptions().purchase(request));
    }

    public List<UserSubscription> listSubscriptions(long userId) {
        return execute(() -> client.subscriptions().listByUser(userId));
    }

    public UserSubscription updateSubscriptionAutoRenew(long subscriptionId, boolean autoRenew) {
        return execute(() -> client.subscriptions().updateAutoRenew(subscriptionId, autoRenew));
    }

    public List<RenewalAttempt> listRenewalAttempts(long subscriptionId) {
        return execute(() -> client.subscriptions().listRenewalAttempts(subscriptionId));
    }

    public UsageReportResponse reportUsage(UsageEventRequest request) {
        return execute(() -> client.usage().report(request));
    }

    public List<UsageReportResponse> reportUsageBatch(UsageEventBatchRequest request) {
        return execute(() -> client.usage().reportBatch(request));
    }

    public List<UsageEvent> listUsageEvents(long userId) {
        return execute(() -> client.usage().listByUser(userId));
    }

    private <T> T execute(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (MotherboardApiException exception) {
            throw new MotherboardGatewayException(MotherboardFailureType.API, exception.getMessage(), exception.getStatusCode(), exception.getCode(), exception);
        } catch (MotherboardTransportException exception) {
            throw new MotherboardGatewayException(MotherboardFailureType.TRANSPORT, exception.getMessage(), null, null, exception);
        } catch (MotherboardSerializationException exception) {
            throw new MotherboardGatewayException(MotherboardFailureType.SERIALIZATION, exception.getMessage(), null, null, exception);
        } catch (MotherboardSigningException exception) {
            throw new MotherboardGatewayException(MotherboardFailureType.SIGNING, exception.getMessage(), null, null, exception);
        }
    }
}
