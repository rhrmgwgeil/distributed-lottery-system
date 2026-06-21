package com.lottery.system.repository;

import com.lottery.system.entity.DrawTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DrawTicketRepository extends JpaRepository<DrawTicket, String> {
}
