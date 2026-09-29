package com.recallx.recallx.store;

/** precisionPct is null when the month has no rated warnings, so the chart shows a gap instead of zero. */
public record MonthPrecision(String month, int useful, int falsePositive, Integer precisionPct) {
}
