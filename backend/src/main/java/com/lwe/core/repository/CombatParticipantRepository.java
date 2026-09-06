package com.lwe.core.repository;

import com.lwe.core.domain.CombatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CombatParticipantRepository extends JpaRepository<CombatParticipant, UUID> {
    List<CombatParticipant> findByCombatIdOrderByInitiativeDesc(UUID combatId);
    List<CombatParticipant> findByCombatId(UUID combatId);
}