package com.repairshop.repair_ticket_system.repository;

import com.repairshop.repair_ticket_system.entity.DevisLigne;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DevisLigneRepository extends JpaRepository<DevisLigne, Long> {

    List<DevisLigne> findByDevisId(Long devisId);
}
