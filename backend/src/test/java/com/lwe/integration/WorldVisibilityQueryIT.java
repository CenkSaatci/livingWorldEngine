package com.lwe.integration;

import com.lwe.core.domain.User;
import com.lwe.core.domain.World;
import com.lwe.core.domain.WorldMember;
import com.lwe.core.repository.UserRepository;
import com.lwe.core.repository.WorldMemberRepository;
import com.lwe.core.repository.WorldRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T33-02/Audit: Sichtbarkeits-Query (PUBLIC rein, PRIVATE-Mitglieder raus, keine Duplikate).
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class WorldVisibilityQueryIT {

    @Autowired private WorldRepository worldRepo;
    @Autowired private WorldMemberRepository memberRepo;
    @Autowired private UserRepository userRepo;

    @Test
    void accessibleQueryRespectsVisibility() {
        var tag = UUID.randomUUID().toString().substring(0, 8);
        var owner = userRepo.save(new User("vis-owner+" + tag + "@test.de", "vo_" + tag,
            "$2a$10$dummyhash", "USER", "de"));
        var member = userRepo.save(new User("vis-member+" + tag + "@test.de", "vm_" + tag,
            "$2a$10$dummyhash", "USER", "de"));
        var stranger = userRepo.save(new User("vis-stranger+" + tag + "@test.de", "vs_" + tag,
            "$2a$10$dummyhash", "USER", "de"));

        var publicWorld = worldRepo.save(new World("PUB_" + tag, owner.getId(), "{}"));
        publicWorld.setVisibility("PUBLIC");
        publicWorld = worldRepo.save(publicWorld);

        var inviteWorld = worldRepo.save(new World("INV_" + tag, owner.getId(), "{}"));
        inviteWorld.setVisibility("INVITE_ONLY");
        inviteWorld = worldRepo.save(inviteWorld);
        memberRepo.save(new WorldMember(inviteWorld.getId(), member.getId(), "PLAYER"));

        var privateWorld = worldRepo.save(new World("PRIV_" + tag, owner.getId(), "{}"));
        privateWorld.setVisibility("PRIVATE");
        privateWorld = worldRepo.save(privateWorld);
        memberRepo.save(new WorldMember(privateWorld.getId(), member.getId(), "PLAYER"));

        var page = PageRequest.of(0, 50);
        var forMember = worldRepo.findAccessibleByUserId(member.getId(), page)
            .getContent().stream().map(World::getName).toList();
        assertThat(forMember).contains("PUB_" + tag).contains("INV_" + tag);
        assertThat(forMember).doesNotContain("PRIV_" + tag);

        var forStranger = worldRepo.findAccessibleByUserId(stranger.getId(), page)
            .getContent().stream().map(World::getName).toList();
        assertThat(forStranger).contains("PUB_" + tag);
        assertThat(forStranger).doesNotContain("INV_" + tag).doesNotContain("PRIV_" + tag);

        var forOwner = worldRepo.findAccessibleByUserId(owner.getId(), page)
            .getContent().stream().map(World::getName).toList();
        // Eigene PUBLIC-Welt darf nicht doppelt auftauchen (UNION, nicht UNION ALL)
        assertThat(forOwner.stream().filter(n -> n.equals("PUB_" + tag)).count()).isEqualTo(1);
    }
}
