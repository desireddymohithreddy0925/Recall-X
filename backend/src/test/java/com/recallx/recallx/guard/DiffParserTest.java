package com.recallx.recallx.guard;

import com.recallx.recallx.store.ConfigChange;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiffParserTest {

    @Test
    void parsesOneChangePerLineAndSkipsBlankLines() {
        List<ConfigChange> changes = DiffParser.parse(
                "spring.datasource.hikari.maximum-pool-size: 20 -> 60\n\nlogging.level.com.acmepay: INFO -> DEBUG");
        assertThat(changes).containsExactly(
                new ConfigChange("spring.datasource.hikari.maximum-pool-size", "20", "60"),
                new ConfigChange("logging.level.com.acmepay", "INFO", "DEBUG"));
    }

    @Test
    void valuesMayContainSpaces() {
        List<ConfigChange> changes = DiffParser.parse("payment.settlement.cron: 0 30 1 * * * -> 0 30 23 * * *");
        assertThat(changes.get(0).oldValue()).isEqualTo("0 30 1 * * *");
        assertThat(changes.get(0).newValue()).isEqualTo("0 30 23 * * *");
    }

    @Test
    void rejectsABadLineAndNamesIt() {
        assertThatThrownBy(() -> DiffParser.parse("a.b: 1 -> 2\nthis is not a change"))
                .isInstanceOf(DiffParseException.class)
                .hasMessageContaining("Line 2");
    }

    @Test
    void rejectsAnEmptyDiff() {
        assertThatThrownBy(() -> DiffParser.parse("  \n ")).isInstanceOf(DiffParseException.class);
    }

    @Test
    void rejectsTheSameKeyTwice() {
        assertThatThrownBy(() -> DiffParser.parse("a.b: 1 -> 2\na.b: 2 -> 3"))
                .isInstanceOf(DiffParseException.class)
                .hasMessageContaining("Line 2");
    }

    @Test
    void rejectsMoreThanTwentyChanges() {
        String diff = IntStream.rangeClosed(1, 21).mapToObj(i -> "key" + i + ": 1 -> 2").collect(Collectors.joining("\n"));
        assertThatThrownBy(() -> DiffParser.parse(diff)).isInstanceOf(DiffParseException.class).hasMessageContaining("20");
    }

    @Test
    void rejectsAnOverlongLine() {
        assertThatThrownBy(() -> DiffParser.parse("a.b: 1 -> " + "9".repeat(400)))
                .isInstanceOf(DiffParseException.class)
                .hasMessageContaining("Line 1");
    }
}
