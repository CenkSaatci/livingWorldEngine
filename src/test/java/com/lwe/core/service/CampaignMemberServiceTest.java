package com.lwe.core.service;

import com.lwe.core.domain.Campaign;
import com.lwe.core.domain.CampaignMember;
import com.lwe.core.domain.User;
import com.lwe.core.repository.CampaignMemberRepository;
import com.lwe.core.repository.CampaignRepository;
import com.lwe.core.repository.UserRepository;
import com.lwe.core.util.WorldAccess;
import com.lwe.core.util.WorldAccess.WorldAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignMemberServiceTest {

    @Mock private CampaignMemberRepository memberRepo;
    @Mock private CampaignRepository campaignRepo;
    @Mock private UserRepository userRepo;
    @Mock private WorldAccess worldAccess;

    private CampaignMemberService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();
    private final UUID gameSystemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CampaignMemberService(memberRepo, campaignRepo, userRepo, worldAccess);
        lenient().doNothing().when(worldAccess).requireAccess(any(), any());
    }

    private Campaign campaign() {
        var c = new Campaign(worldId, gameSystemId, "Runde 1");
        setId(c, campaignId);
        return c;
    }

    @Test
    void creatorBecomesDmAutomatically() {
        var campaign = campaign();
        when(memberRepo.existsByCampaignIdAndUserId(campaignId, userId)).thenReturn(false);
        when(memberRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var member = service.addCreatorAsDm(campaign, userId);

        assertThat(member.getRole()).isEqualTo("DM");
        assertThat(member.getCampaignId()).isEqualTo(campaignId);
        verify(memberRepo).save(argThat(m -> m.getRole().equals("DM")));
    }

    @Test
    void creatorAlreadyMemberReturnsExisting() {
        var campaign = campaign();
        var existing = new CampaignMember(campaignId, userId, "DM");
        when(memberRepo.existsByCampaignIdAndUserId(campaignId, userId)).thenReturn(true);
        when(memberRepo.findByCampaignIdAndUserId(campaignId, userId)).thenReturn(Optional.of(existing));

        var member = service.addCreatorAsDm(campaign, userId);

        assertThat(member.getRole()).isEqualTo("DM");
        verify(memberRepo, never()).save(any());
    }

    @Test
    void addPlayerByDm() {
        var campaign = campaign();
        var user = new User("spieler@test.de", "spieler", "hash", "PLAYER", "de");
        setId(user, otherUserId);
        when(campaignRepo.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(userRepo.findById(otherUserId)).thenReturn(Optional.of(user));
        when(memberRepo.existsByCampaignIdAndUserId(campaignId, otherUserId)).thenReturn(false);
        when(memberRepo.findByCampaignIdAndUserId(campaignId, userId)).thenReturn(Optional.of(new CampaignMember(campaignId, userId, "DM")));
        when(memberRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var member = service.addMember(campaignId, userId, otherUserId, "PLAYER");

        assertThat(member.getRole()).isEqualTo("PLAYER");
        verify(worldAccess).requireAccess(worldId, userId);
    }

    @Test
    void onlyDmCanAddMembers() {
        var campaign = campaign();
        when(campaignRepo.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(memberRepo.findByCampaignIdAndUserId(campaignId, userId)).thenReturn(Optional.empty());
        when(userRepo.findById(otherUserId)).thenReturn(Optional.of(new User("x@test.de", "x", "hash", "PLAYER", "de")));

        assertThatThrownBy(() -> service.addMember(campaignId, userId, otherUserId, "PLAYER"))
            .isInstanceOf(CampaignMemberService.CampaignMemberException.class)
            .matches(e -> ((CampaignMemberService.CampaignMemberException) e).getErrorCode().equals("DM_REQUIRED"));
    }

    @Test
    void addMemberRejectsUnknownUser() {
        var campaign = campaign();
        when(campaignRepo.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(userRepo.findById(otherUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addMember(campaignId, userId, otherUserId, "PLAYER"))
            .isInstanceOf(CampaignMemberService.CampaignMemberException.class)
            .matches(e -> ((CampaignMemberService.CampaignMemberException) e).getErrorCode().equals("USER_NOT_FOUND"));
    }

    @Test
    void addMemberRejectsDuplicate() {
        var campaign = campaign();
        when(campaignRepo.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(userRepo.findById(otherUserId)).thenReturn(Optional.of(new User("x@test.de", "x", "hash", "PLAYER", "de")));
        when(memberRepo.existsByCampaignIdAndUserId(campaignId, otherUserId)).thenReturn(true);

        assertThatThrownBy(() -> service.addMember(campaignId, userId, otherUserId, "PLAYER"))
            .isInstanceOf(CampaignMemberService.CampaignMemberException.class)
            .matches(e -> ((CampaignMemberService.CampaignMemberException) e).getErrorCode().equals("MEMBER_ALREADY"));
    }

    @Test
    void removePlayerByDm() {
        var campaign = campaign();
        var member = new CampaignMember(campaignId, otherUserId, "PLAYER");
        setId(member, UUID.randomUUID());
        when(campaignRepo.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(memberRepo.findByCampaignIdAndUserId(campaignId, otherUserId)).thenReturn(Optional.of(member));
        when(memberRepo.findByCampaignIdAndUserId(campaignId, userId)).thenReturn(Optional.of(new CampaignMember(campaignId, userId, "DM")));

        service.removeMember(campaignId, userId, otherUserId);

        verify(memberRepo).delete(member);
    }

    @Test
    void removeMemberRejectsNonDm() {
        var campaign = campaign();
        var member = new CampaignMember(campaignId, otherUserId, "PLAYER");
        setId(member, UUID.randomUUID());
        when(campaignRepo.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(memberRepo.findByCampaignIdAndUserId(campaignId, otherUserId)).thenReturn(Optional.of(member));
        when(memberRepo.findByCampaignIdAndUserId(campaignId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeMember(campaignId, userId, otherUserId))
            .isInstanceOf(CampaignMemberService.CampaignMemberException.class)
            .matches(e -> ((CampaignMemberService.CampaignMemberException) e).getErrorCode().equals("DM_REQUIRED"));
    }

    @Test
    void removeDmRejected() {
        var campaign = campaign();
        var dmMember = new CampaignMember(campaignId, otherUserId, "DM");
        setId(dmMember, UUID.randomUUID());
        when(campaignRepo.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(memberRepo.findByCampaignIdAndUserId(campaignId, otherUserId)).thenReturn(Optional.of(dmMember));

        assertThatThrownBy(() -> service.removeMember(campaignId, userId, otherUserId))
            .isInstanceOf(CampaignMemberService.CampaignMemberException.class)
            .matches(e -> ((CampaignMemberService.CampaignMemberException) e).getErrorCode().equals("DM_REMOVAL_DENIED"));
    }

    @Test
    void isDm() {
        var dm = new CampaignMember(campaignId, userId, "DM");
        when(memberRepo.findByCampaignIdAndUserId(campaignId, userId)).thenReturn(Optional.of(dm));

        assertThat(service.isDm(campaignId, userId)).isTrue();
    }

    @Test
    void isNotDmForNonMember() {
        when(memberRepo.findByCampaignIdAndUserId(campaignId, userId)).thenReturn(Optional.empty());

        assertThat(service.isDm(campaignId, userId)).isFalse();
    }

    @Test
    void worldAccessDeniedPropagates() {
        var campaign = campaign();
        when(campaignRepo.findById(campaignId)).thenReturn(Optional.of(campaign));
        doThrow(new WorldAccessException("WORLD_ACCESS_DENIED", "Access denied"))
            .when(worldAccess).requireAccess(worldId, userId);

        assertThatThrownBy(() -> service.addMember(campaignId, userId, otherUserId, "PLAYER"))
            .isInstanceOf(WorldAccessException.class);
    }

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
