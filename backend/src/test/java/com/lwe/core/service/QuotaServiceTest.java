package com.lwe.core.service;

import com.lwe.core.domain.SubscriptionPlan;
import com.lwe.core.domain.User;
import com.lwe.core.repository.SubscriptionPlanRepository;
import com.lwe.core.repository.WorldMemberRepository;
import com.lwe.core.repository.WorldRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuotaServiceTest {

    @Mock private SubscriptionPlanRepository planRepo;
    @Mock private WorldRepository worldRepo;
    @Mock private WorldMemberRepository memberRepo;

    @InjectMocks
    private QuotaService service;

    @Test
    void shouldAllowWorldCreationUnderLimit() {
        var user = new User("t@t.com", "t", "h", "USER", "de");
        var p = createPlan(3, 5);
        when(planRepo.findByName("FREE")).thenReturn(Optional.of(p));
        when(worldRepo.countQuotaRelevantByOwnerId(any())).thenReturn(2L);

        service.checkCanCreateWorld(UUID.randomUUID(), user);
    }

    @Test
    void shouldRejectWorldCreationAtLimit() {
        var user = new User("t@t.com", "t", "h", "USER", "de");
        var p = createPlan(3, 5);
        when(planRepo.findByName("FREE")).thenReturn(Optional.of(p));
        when(worldRepo.countQuotaRelevantByOwnerId(any())).thenReturn(3L);

        assertThatThrownBy(() -> service.checkCanCreateWorld(UUID.randomUUID(), user))
            .isInstanceOf(QuotaService.QuotaException.class)
            .matches(e -> ((QuotaService.QuotaException) e).getErrorCode().equals("WORLD_LIMIT_REACHED"));
    }

    @Test
    void shouldAllowMemberAdditionUnderLimit() {
        var p = createPlan(3, 5);
        when(planRepo.findByName("FREE")).thenReturn(Optional.of(p));
        when(memberRepo.countByWorldId(any())).thenReturn(4L);

        service.checkCanAddMember(UUID.randomUUID(), UUID.randomUUID());
    }

    @Test
    void shouldRejectMemberAdditionAtLimit() {
        var p = createPlan(3, 5);
        when(planRepo.findByName("FREE")).thenReturn(Optional.of(p));
        when(memberRepo.countByWorldId(any())).thenReturn(5L);

        assertThatThrownBy(() -> service.checkCanAddMember(UUID.randomUUID(), UUID.randomUUID()))
            .isInstanceOf(QuotaService.QuotaException.class)
            .matches(e -> ((QuotaService.QuotaException) e).getErrorCode().equals("WORLD_MEMBER_LIMIT"));
    }

    @Test
    void shouldReturnFreePlanForUserWithoutPlan() {
        var p = createPlan(3, 5);
        when(planRepo.findByName("FREE")).thenReturn(Optional.of(p));
        var user = new User("t@t.com", "t", "h", "USER", "de");

        var plan = service.getPlan(user);
        assertThat(plan.getName()).isEqualTo("FREE");
    }

    private SubscriptionPlan createPlan(int worlds, int members) {
        var p = mock(SubscriptionPlan.class);
        when(p.getMaxWorlds()).thenReturn(worlds);
        when(p.getMaxMembersPerWorld()).thenReturn(members);
        when(p.getName()).thenReturn("FREE");
        when(p.isUnlimited(anyInt())).thenReturn(false);
        when(p.hasReachedLimit(anyInt(), anyInt())).thenCallRealMethod();
        return p;
    }
}
