package com.airconsole.games.trivia.engine;

import com.airconsole.common.engine.ControllerLayout;
import com.airconsole.common.engine.GameEngine;
import com.airconsole.common.enums.GameStatus;
import com.airconsole.common.enums.GameType;
import com.airconsole.common.model.GameContext;
import com.airconsole.common.model.GameInput;
import com.airconsole.common.model.GameSnapshot;
import com.airconsole.common.util.EventSerializer;
import com.airconsole.games.trivia.domain.TriviaControllerLayout;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TriviaGame implements GameEngine {

    public static final int TOTAL_ROUNDS = 5;
    public static final int ANSWER_TIME_SECONDS = 10;
    public static final int TICK_RATE_MS = 1000;
    public static final int POINTS_CORRECT = 10;
    public static final int POINTS_WRONG = 0;

    // Actions for answers: A=5, B=6, C=7, D=8
    private static final int ACTION_A = 5;
    private static final int ACTION_B = 6;
    private static final int ACTION_C = 7;
    private static final int ACTION_D = 8;

    private GameContext context;
    private final TriviaControllerLayout layout = new TriviaControllerLayout();
    private GameStatus status = GameStatus.WAITING;
    private long tickNumber = 0;

    // Game state
    private int currentRound = 0;
    private int tickInRound = 0;
    private final Map<UUID, Integer> scores = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> answers = new ConcurrentHashMap<>(); // playerId -> selected answer

    // Hardcoded questions
    private final List<TriviaQuestion> questions = List.of(
        new TriviaQuestion("What is the capital of France?", "A", "Paris", "London", "Berlin", "Madrid"),
        new TriviaQuestion("What is 2 + 2?", "B", "3", "4", "5", "6"),
        new TriviaQuestion("Which planet is known as the Red Planet?", "C", "Venus", "Jupiter", "Mars", "Saturn"),
        new TriviaQuestion("What is the largest ocean on Earth?", "A", "Pacific", "Atlantic", "Indian", "Arctic"),
        new TriviaQuestion(
            "Who wrote 'Romeo and Juliet'?", "B", "Charles Dickens",
            "William Shakespeare", "Jane Austen", "Mark Twain")
    );

    // State machine
    private enum RoundPhase { QUESTION, TIMEOUT, ROUND_END, GAME_OVER }
    private RoundPhase phase = RoundPhase.QUESTION;

    @Override
    public GameType getType() {
        return GameType.TRIVIA;
    }

    @Override
    public ControllerLayout getControllerLayout() {
        return layout;
    }

    @Override
    public void initialize(GameContext context) {
        this.context = context;

        // Initialize scores for all players
        for (UUID playerId : context.getPlayerIds()) {
            scores.put(playerId, 0);
        }

        currentRound = 0;
        tickInRound = 0;
        phase = RoundPhase.QUESTION;
        status = GameStatus.RUNNING;
    }

    @Override
    public void processInput(GameInput input) {
        if (status != GameStatus.RUNNING) {
            return;
        }

        // Only accept answers during QUESTION phase
        if (phase != RoundPhase.QUESTION) {
            return;
        }

        int action = input.getAction();
        // Accept A(5), B(6), C(7), D(8)
        if (action >= 5 && action <= 8) {
            // Store answer if player hasn't answered yet
            answers.putIfAbsent(input.getPlayerId(), action);
        }
    }

    @Override
    public void tick() {
        if (status != GameStatus.RUNNING) {
            return;
        }

        tickNumber++;
        tickInRound++;

        switch (phase) {
            case QUESTION -> {
                // Check if time is up for this question
                if (tickInRound >= ANSWER_TIME_SECONDS) {
                    // Evaluate answers for this round
                    evaluateAnswers();
                    phase = RoundPhase.ROUND_END;
                    tickInRound = 0;
                }
            }
            case ROUND_END -> {
                // Brief pause between rounds (2 seconds)
                if (tickInRound >= 2) {
                    currentRound++;
                    tickInRound = 0;
                    answers.clear();

                    if (currentRound >= TOTAL_ROUNDS) {
                        phase = RoundPhase.GAME_OVER;
                        status = GameStatus.FINISHED;
                    } else {
                        phase = RoundPhase.QUESTION;
                    }
                }
            }
            case GAME_OVER -> {
            // Already finished, nothing to do
        }
        }
    }

    private void evaluateAnswers() {
        TriviaQuestion question = questions.get(currentRound);
        int correctAction = actionForAnswer(question.correct());

        for (Map.Entry<UUID, Integer> entry : answers.entrySet()) {
            UUID playerId = entry.getKey();
            int answer = entry.getValue();

            if (answer == correctAction) {
                scores.computeIfPresent(playerId, (k, v) -> v + POINTS_CORRECT);
            }
            // Wrong answer gives 0 points (no change needed)
        }
    }

    private int actionForAnswer(String answer) {
        return switch (answer.toUpperCase()) {
            case "A" -> ACTION_A;
            case "B" -> ACTION_B;
            case "C" -> ACTION_C;
            case "D" -> ACTION_D;
            default -> ACTION_A;
        };
    }

    @Override
    public GameSnapshot snapshot() {
        Map<String, Object> state = new HashMap<>();
        state.put("gameStatus", status.name());
        state.put("tick", tickNumber);
        state.put("currentRound", currentRound + 1); // 1-indexed for display
        state.put("totalRounds", TOTAL_ROUNDS);
        state.put("tickInRound", tickInRound);
        state.put("timeRemaining", Math.max(0, ANSWER_TIME_SECONDS - tickInRound));
        state.put("phase", phase.name());

        // Include current question if in QUESTION phase
        if (currentRound < questions.size() && phase == RoundPhase.QUESTION) {
            TriviaQuestion q = questions.get(currentRound);
            Map<String, Object> questionMap = new HashMap<>();
            questionMap.put("question", q.question());
            questionMap.put("optionA", q.optionA());
            questionMap.put("optionB", q.optionB());
            questionMap.put("optionC", q.optionC());
            questionMap.put("optionD", q.optionD());
            state.put("currentQuestion", questionMap);
        }

        // Include answer if in ROUND_END phase (show correct answer)
        if (phase == RoundPhase.ROUND_END && currentRound < questions.size()) {
            state.put("correctAnswer", questions.get(currentRound).correct());
        }

        // Scores
        Map<String, Integer> stringScores = new HashMap<>();
        for (Map.Entry<UUID, Integer> entry : scores.entrySet()) {
            stringScores.put(entry.getKey().toString(), entry.getValue());
        }
        state.put("scores", stringScores);

        // Determine winner if game is over
        if (phase == RoundPhase.GAME_OVER) {
            UUID winnerId = getWinner();
            state.put("winnerId", winnerId != null ? winnerId.toString() : null);
            state.put("isTie", isTie());
        }

        byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);
        try {
            payload = EventSerializer.getMapper().writeValueAsBytes(state);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new GameSnapshot(
            context.getGameId(),
            context.getRoomId(),
            context.getGameType(),
            status,
            tickNumber,
            new HashMap<>(scores),
            payload
        );
    }

    private UUID getWinner() {
        UUID winner = null;
        int maxScore = -1;
        for (Map.Entry<UUID, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                winner = entry.getKey();
            }
        }
        return winner;
    }

    private boolean isTie() {
        if (scores.isEmpty()) {
            return false;
        }
        int maxScore = scores.values().stream().max(Integer::compare).orElse(0);
        return scores.values().stream().filter(v -> v == maxScore).count() > 1;
    }

    @Override
    public boolean isFinished() {
        return status == GameStatus.FINISHED;
    }

    // Inner record for trivia questions
    private record TriviaQuestion(
        String question,
        String correct,
        String optionA,
        String optionB,
        String optionC,
        String optionD) { }
}