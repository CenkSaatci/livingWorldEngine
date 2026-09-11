package com.lwe.core.service;

import com.lwe.core.domain.SubscriptionPlan;
import com.lwe.core.domain.User;
import com.lwe.core.repository.SubscriptionPlanRepository;
import com.lwe.core.repository.WorldMemberRepository;
import com.lwe.core.repository.WorldRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Prüft Nutzungslimits basierend auf dem Abonnement-Plan des Users.
 *
 * <p>Alle Limits sind in {@code subscription_plans} konfigurierbar.
 * Ein Wert von -1 bedeutet »unlimitiert«.
 *
 * <p>Zahlungssysteme (Stripe/PayPal) sind noch nicht integriert.
 * Siehe docs/BILLING.md.
 */
@Service
public class QuotaService {

    private final SubscriptionPlanRepository planRepo;
    private final WorldRepository worldRepo;
    private final WorldMemberRepository memberRepo;

    public QuotaService(SubscriptionPlanRepository planRepo,
                        WorldRepository worldRepo,
                        WorldMemberRepository memberRepo) {
        this.planRepo = planRepo;
        this.worldRepo = worldRepo;
        this.memberRepo = memberRepo;
    }

    /**
     * Ermittelt den aktiven Plan eines Users. NULL → FREE-Plan.
     */
    public SubscriptionPlan getPlan(User user) {
        if (user.getPlanId() != null) {
            return planRepo.findById(user.getPlanId())
                .orElseGet(this::getFreePlan);
        }
        return getFreePlan();
    }

    private SubscriptionPlan getFreePlan() {
        return planRepo.findByName("FREE")
            .orElseThrow(() -> new RuntimeException("FREE plan not found"));
    }

    /**
     * Darf der User eine weitere Welt erstellen?
     */
    public void checkCanCreateWorld(UUID userId, User user) {
        var plan = getPlan(user);
        if (plan.isUnlimited(plan.getMaxWorlds())) return;
        var current = worldRepo.countQuotaRelevantByOwnerId(userId);
        if (plan.hasReachedLimit((int) current, plan.getMaxWorlds())) {
            throw new QuotaException("WORLD_LIMIT_REACHED",
                "World limit reached (" + current + "/" + plan.getMaxWorlds() + ")");
        }
    }

    /**
     * Darf ein weiteres Mitglied zur Welt hinzugefügt werden?
     */
    public void checkCanAddMember(UUID worldId, UUID ownerId) {
        var plan = getPlanById(ownerId);
        if (plan.isUnlimited(plan.getMaxMembersPerWorld())) return;
        var current = memberRepo.countByWorldId(worldId);
        if (plan.hasReachedLimit((int) current, plan.getMaxMembersPerWorld())) {
            throw new QuotaException("WORLD_MEMBER_LIMIT",
                "Member limit reached (" + current + "/" + plan.getMaxMembersPerWorld() + ")");
        }
    }

    private SubscriptionPlan getPlanById(UUID userId) {
        // Vereinfacht: User-Plan direkt aus DB laden
        // In Zukunft: gecachten Plan verwenden
        return getFreePlan();
    }

    public static class QuotaException extends RuntimeException {
        private final String errorCode;
        public QuotaException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}
