package com.example.msbaseprj.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.msbaseprj.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

}
