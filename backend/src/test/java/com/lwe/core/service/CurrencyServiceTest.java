package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrencyServiceTest {

    private final CurrencyService service = new CurrencyService(new ObjectMapper());

    private static Map<String, Object> rulesWithCurrency() {
        return Map.of("currency", Map.of(
            "name", "Dukaten",
            "denominations", List.of(
                Map.of("name", "Kupfer", "abbr", "K", "factor", 1),
                Map.of("name", "Silber", "abbr", "S", "factor", 10),
                Map.of("name", "Gold", "abbr", "G", "factor", 100))));
    }

    private static GameEntity actor() {
        return new GameEntity(UUID.randomUUID(), "PC", "Mira");
    }

    @Test
    void formatsBaseValueIntoDenominations() {
        assertThat(service.format(312, rulesWithCurrency())).isEqualTo("3 G, 1 S, 2 K");
    }

    @Test
    void formatsZero() {
        assertThat(service.format(0, rulesWithCurrency())).isEqualTo("0");
    }

    @Test
    void withoutCurrencyConfigFallsBackToPlainNumber() {
        assertThat(service.format(312, Map.of())).isEqualTo("312");
    }

    @Test
    void malformedDenominationsAreIgnored() {
        var rules = Map.<String, Object>of("currency", Map.of("denominations", List.of(
            Map.of("name", "Kaputt", "factor", 0))));
        assertThat(service.denominations(rules)).isEmpty();
        assertThat(service.format(42, rules)).isEqualTo("42");
    }

    @Test
    void breakdownOnlyNonZeroParts() {
        var parts = service.breakdown(312, rulesWithCurrency());
        assertThat(parts).hasSize(3);
        assertThat(parts.get(0)).containsEntry("abbr", "G").containsEntry("count", 3);
    }

    @Test
    void payRequiresSufficientFunds() {
        var actor = actor();
        service.setMoney(actor, 20);
        assertThatThrownBy(() -> service.pay(actor, 25))
            .isInstanceOf(CurrencyService.CurrencyException.class)
            .extracting(e -> ((CurrencyService.CurrencyException) e).getErrorCode())
            .isEqualTo("MONEY_INSUFFICIENT");
        assertThat(service.money(actor)).isEqualTo(20); // keine Teilwirkung
    }

    @Test
    void payAndCreditKeepBaseValue() {
        var actor = actor();
        service.setMoney(actor, 100);
        service.pay(actor, 30);
        service.credit(actor, 5);
        assertThat(service.money(actor)).isEqualTo(75);
    }

    @Test
    void moneyNeverNegative() {
        var actor = actor();
        service.setMoney(actor, 0);
        assertThatThrownBy(() -> service.setMoney(actor, -1))
            .isInstanceOf(CurrencyService.CurrencyException.class);
        assertThat(service.money(actor)).isEqualTo(0);
    }

    @Test
    void readsMoneyFromMetadata() {
        var actor = actor();
        actor.setMetadataJson("{\"money\":17,\"other\":true}");
        assertThat(service.money(actor)).isEqualTo(17);
        service.credit(actor, 3);
        assertThat(service.money(actor)).isEqualTo(20);
        assertThat(actor.getMetadataJson()).contains("\"other\":true");
    }

    @Test
    void corruptMetadataFallsBackToZero() {
        var actor = actor();
        actor.setMetadataJson("not-json");
        assertThat(service.money(actor)).isEqualTo(0);
    }
}
