package com.orderops.repository;

import com.orderops.model.AgentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AgentLogRepository extends JpaRepository<AgentLog, Long> {
    List<AgentLog> findTop50ByOrderByTimestampDesc();
}

