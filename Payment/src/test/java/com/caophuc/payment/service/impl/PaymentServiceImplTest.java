package com.caophuc.payment.service.impl;

import com.caophuc.payment.client.BookingClient;
import com.caophuc.payment.client.BookingDto;
import com.caophuc.payment.dto.MomoIpnRequest;
import com.caophuc.payment.dto.PaymentEvent;
import com.caophuc.payment.dto.PaymentResponse;
import com.caophuc.payment.model.Payment;
import com.caophuc.payment.repository.PaymentRepository;
import com.caophuc.payment.service.MomoSecurityService;
import com.caophuc.payment.service.PaymentStrategy;
import com.caophuc.payment.service.strategy.PaymentStrategyFactory;
import com.caophuc.payment.util.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingClient bookingClient;

    @Mock
    private PaymentStrategyFactory paymentStrategyFactory;

    @Mock
    private PaymentStrategy momoStrategy;

    // THÊM CÁC MOCK CÒN THIẾU
    @Mock
    private MomoSecurityService momoSecurityService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void whenCreatePayment_withValidNewBooking_shouldReturnPaymentLinkAndSavePayment() {
        Integer bookingId = 1;
        Integer userId = 100;
        double amount = 500000.0;
        String paymentMethod = "MOMO";

        BookingDto fakeBooking = new BookingDto();
        fakeBooking.setId(bookingId);
        fakeBooking.setUserId(userId);
        fakeBooking.setTotalPrice(amount);
        fakeBooking.setStatus("PENDING");
        fakeBooking.setPaymentMethod(paymentMethod);

        when(bookingClient.getBookingById(bookingId)).thenReturn(fakeBooking);
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(paymentStrategyFactory.getStrategy(paymentMethod)).thenReturn(momoStrategy);

        PaymentResponse expectedResponse = PaymentResponse.builder()
                .paymentUrl("https://momo.vn/pay/12345")
                .build();
        when(momoStrategy.createPaymentRequest(anyString(), any(Double.class))).thenReturn(expectedResponse);

        PaymentResponse actualResponse = paymentService.createPayment(bookingId, userId);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse.getPaymentUrl(), actualResponse.getPaymentUrl());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertNotNull(savedPayment);
        assertEquals(bookingId, savedPayment.getBookingId());
        assertEquals(userId, savedPayment.getUserId());
        assertEquals(amount, savedPayment.getAmount());
        assertEquals(PaymentStatus.PENDING, savedPayment.getStatus());
        assertNotNull(savedPayment.getTransactionId());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when booking status is not PENDING")
    void whenCreatePayment_withNonPendingBooking_shouldThrowIllegalStateException() {
        int bookingId = 2;
        int userId = 101;

        BookingDto fakeBooking = new BookingDto();
        fakeBooking.setId(bookingId);
        fakeBooking.setUserId(userId);
        fakeBooking.setStatus("COMPLETED");

        when(bookingClient.getBookingById(bookingId)).thenReturn(fakeBooking);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            paymentService.createPayment(bookingId, userId);
        });

        assertEquals("Đơn hàng không ở trạng thái chờ thanh toán.", exception.getMessage());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should reuse existing payment record and return new payment link")
    void whenCreatePayment_withExistingPendingPayment_shouldReusePaymentAndReturnNewLink() {
        int bookingId = 3;
        int userId = 102;
        double amount = 750000.0;
        String paymentMethod = "MOMO";

        BookingDto fakeBooking = new BookingDto();
        fakeBooking.setId(bookingId);
        fakeBooking.setUserId(userId);
        fakeBooking.setTotalPrice(amount);
        fakeBooking.setStatus("PENDING");
        fakeBooking.setPaymentMethod(paymentMethod);

        Payment existingPayment = Payment.builder()
                .id(99)
                .bookingId(bookingId)
                .userId(userId)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .paymentMethod(paymentMethod)
                .build();

        when(bookingClient.getBookingById(bookingId)).thenReturn(fakeBooking);
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(existingPayment));
        when(paymentStrategyFactory.getStrategy(paymentMethod)).thenReturn(momoStrategy);

        PaymentResponse expectedResponse = PaymentResponse.builder().paymentUrl("https://momo.vn/pay/abc").build();
        when(momoStrategy.createPaymentRequest(anyString(), anyDouble())).thenReturn(expectedResponse);

        paymentService.createPayment(bookingId, userId);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());

        Payment savedPayment = paymentCaptor.getValue();
        assertEquals(existingPayment.getId(), savedPayment.getId());
        assertNotNull(savedPayment.getTransactionId());
        verify(paymentRepository, never()).save(argThat(p -> p.getId() == null));
    }

    @Test
    @DisplayName("Should throw RuntimeException when user ID does not match booking's user ID")
    void whenCreatePayment_withMismatchedUserId_shouldThrowRuntimeException() {
        int bookingId = 4;
        int ownerId = 200;
        int requesterId = 201;

        BookingDto fakeBooking = new BookingDto();
        fakeBooking.setId(bookingId);
        fakeBooking.setUserId(ownerId);
        fakeBooking.setStatus("PENDING");

        when(bookingClient.getBookingById(bookingId)).thenReturn(fakeBooking);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            paymentService.createPayment(bookingId, requesterId);
        });

        assertTrue(exception.getMessage().contains("Bạn không có quyền"));
        verify(paymentRepository, never()).findByBookingId(anyInt());
        verify(paymentStrategyFactory, never()).getStrategy(anyString());
    }

    // ----- TEST CASES FOR handleMomoWebhook -----

    @Test
    @DisplayName("[handleMomoWebhook] Should do nothing when signature is invalid")
    void whenHandleMomoWebhook_withInvalidSignature_shouldDoNothing() throws Exception {
        MomoIpnRequest fakePayload = new MomoIpnRequest();
        when(momoSecurityService.validateSignature(fakePayload)).thenReturn(false);

        paymentService.handleMomoWebhook(fakePayload);

        verify(paymentRepository, never()).findByTransactionId(anyString());
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    @DisplayName("[handleMomoWebhook] Should process successful payment when signature is valid and result code is 0")
    void whenHandleMomoWebhook_withValidSignatureAndSuccessCode_shouldProcessSuccess() throws Exception {
        String transactionId = "MOMO_SUCCESS_123";
        MomoIpnRequest fakePayload = new MomoIpnRequest();
        fakePayload.setOrderId(transactionId);
        fakePayload.setResultCode(0);

        Payment pendingPayment = Payment.builder()
                .bookingId(1)
                .status(PaymentStatus.PENDING)
                .build();

        when(momoSecurityService.validateSignature(fakePayload)).thenReturn(true);
        when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(pendingPayment));

        paymentService.handleMomoWebhook(fakePayload);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        assertEquals(PaymentStatus.SUCCESS, savedPayment.getStatus());

        ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());
        assertEquals("SUCCESS", eventCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("[handleMomoWebhook] Should process failed payment when signature is valid and result code is not 0")
    void whenHandleMomoWebhook_withValidSignatureAndErrorCode_shouldProcessFailure() throws Exception {
        String transactionId = "MOMO_FAIL_456";
        MomoIpnRequest fakePayload = new MomoIpnRequest();
        fakePayload.setOrderId(transactionId);
        fakePayload.setResultCode(1001);
        fakePayload.setMessage("Giao dịch bị từ chối");

        Payment pendingPayment = Payment.builder()
                .bookingId(2)
                .status(PaymentStatus.PENDING)
                .build();

        when(momoSecurityService.validateSignature(fakePayload)).thenReturn(true);
        when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(pendingPayment));

        paymentService.handleMomoWebhook(fakePayload);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        assertEquals(PaymentStatus.FAILED, savedPayment.getStatus());

        ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());
        assertEquals("FAILED", eventCaptor.getValue().getStatus());
        assertEquals("Giao dịch bị từ chối", eventCaptor.getValue().getMessage());
    }
}
