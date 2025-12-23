package com.kodesalon.kopang.storage.payment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, Long> {
}
