package edu.sdccd.cisc191.template;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PastOrderRepository extends JpaRepository<Order, Long> {


    Optional<Order> findById(Long id);  // Use Long here
    List<Order> findByCustomerName(String customerName);

}