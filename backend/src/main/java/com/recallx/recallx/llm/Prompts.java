package com.recallx.recallx.llm;

/**
 * The wording both sides of the Ask screen share, kept in one place so the comparison can be reviewed (NFR-03).
 * Both answers get the same role, question and format limit. The memory side additionally asks for the team's
 * history and citations; the two sides also run on different models, and the UI labels each one.
 */
public final class Prompts {

    public static final String ROLE = "You are an on-call assistant for Acme Pay's payment-service.";
    public static final String FORMAT = "Answer in at most 5 short bullet points.";

    private Prompts() { }

    /** System message for the memory-off answer. The user message is the question itself. */
    public static String baselineSystem() {
        return ROLE + " " + FORMAT;
    }

    /** The reflect query for the memory-on answer. Hindsight's docs put situational context in the query itself. */
    public static String memoryQuery(String question) {
        return ROLE + "\n"
                + "Question: " + question + "\n"
                + "Use this team's history: which past incidents match, which fixes failed, what resolved them, "
                + "and which decisions apply. Cite the record ID for each point.\n"
                + FORMAT;
    }
}
