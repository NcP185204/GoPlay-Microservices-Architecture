package com.caophuc.payment.repository;

import com.caophuc.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    // Tìm thanh toán bằng mã giao dịch của cổng thanh toán
    Optional<Payment> findByTransactionId(String transactionId);

    // Tìm thanh toán bằng mã đơn hàng (bookingId)
    Optional<Payment> findByBookingId(Integer bookingId);
}
