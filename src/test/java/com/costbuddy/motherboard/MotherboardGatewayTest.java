package com.costbuddy.motherboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.motherboard.sdk.MotherboardClient;
import com.motherboard.sdk.UsersApi;
import com.motherboard.sdk.exception.MotherboardApiException;
import com.motherboard.sdk.exception.MotherboardSerializationException;
import com.motherboard.sdk.exception.MotherboardSigningException;
import com.motherboard.sdk.exception.MotherboardTransportException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MotherboardGatewayTest {

    private UsersApi           usersApi;
    private MotherboardGateway gateway;

    @BeforeEach
    void setUp() {
        MotherboardClient client = mock(MotherboardClient.class);
        usersApi = mock(UsersApi.class);
        when(client.users()).thenReturn(usersApi);
        gateway = new MotherboardGateway(client);
    }

    @Test
    void translatesApiExceptionAndKeepsUpstreamDetails() {
        when(usersApi.get(1L)).thenThrow(new MotherboardApiException(401, "INVALID_SIGNATURE", "invalid signature"));

        assertThatThrownBy(() -> gateway.getUser(1L))
                .isInstanceOfSatisfying(MotherboardGatewayException.class, exception -> {
                    assertThat(exception.getFailureType()).isEqualTo(MotherboardFailureType.API);
                    assertThat(exception.getUpstreamStatusCode()).isEqualTo(401);
                    assertThat(exception.getUpstreamCode()).isEqualTo("INVALID_SIGNATURE");
                });
    }

    @Test
    void translatesTransportException() {
        when(usersApi.get(1L)).thenThrow(new MotherboardTransportException("connection failed", new RuntimeException()));

        assertFailureType(MotherboardFailureType.TRANSPORT);
    }

    @Test
    void translatesSerializationException() {
        when(usersApi.get(1L)).thenThrow(new MotherboardSerializationException("invalid response"));

        assertFailureType(MotherboardFailureType.SERIALIZATION);
    }

    @Test
    void translatesSigningException() {
        when(usersApi.get(1L)).thenThrow(new MotherboardSigningException("signing failed", new RuntimeException()));

        assertFailureType(MotherboardFailureType.SIGNING);
    }

    private void assertFailureType(MotherboardFailureType expected) {
        assertThatThrownBy(() -> gateway.getUser(1L))
            .isInstanceOfSatisfying(MotherboardGatewayException.class, exception -> assertThat(exception.getFailureType()).isEqualTo(expected));
    }
}
