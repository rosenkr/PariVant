package ar.ss.betting.service.ingest;

import ar.ss.betting.api.dto.ModelSelectionRequestDto;
import ar.ss.betting.api.internal.RoundController;
import ar.ss.betting.api.internal.dto.FileIngestResultDto;
import ar.ss.betting.api.internal.dto.IngestReportDto;
import ar.ss.betting.domain.GameType;
import ar.ss.betting.persistence.entity.GameRoundEntity;
import ar.ss.betting.persistence.repo.GameRoundRepository;
import ar.ss.betting.service.RoundApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RoundIngestService {

    private static final String STATUS_INGESTED = "INGESTED";
    private static final String STATUS_DUPLICATE = "DUPLICATE_SKIPPED";
    private static final String STATUS_FAILED = "FAILED";

    private final GameRoundRepository gameRoundRepository;
    private final RoundApiService roundApiService;
    private final ObjectMapper objectMapper;

    private final Path inboxDir;
    private final Path processedDir;
    private final Path duplicatesDir;
    private final Path failedDir;

    public RoundIngestService(
            GameRoundRepository gameRoundRepository,
            RoundApiService roundApiService,
            ObjectMapper objectMapper,
            @Value("${ingest.inboxDir:ingest/rounds}") String inboxDir,
            @Value("${ingest.processedDir:ingest/processed}") String processedDir,
            @Value("${ingest.duplicatesDir:ingest/duplicates}") String duplicatesDir,
            @Value("${ingest.failedDir:ingest/failed}") String failedDir
    ) {
        this.gameRoundRepository = Objects.requireNonNull(gameRoundRepository);
        this.roundApiService = Objects.requireNonNull(roundApiService);
        this.objectMapper = Objects.requireNonNull(objectMapper);

        this.inboxDir = Paths.get(inboxDir);
        this.processedDir = Paths.get(processedDir);
        this.duplicatesDir = Paths.get(duplicatesDir);
        this.failedDir = Paths.get(failedDir);
    }

    public IngestReportDto ingestInbox() {
        ensureDirs();

        List<FileIngestResultDto> results = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inboxDir, "*.json")) {
            for (Path file : stream) {
                results.add(processOneFile(file));
            }
        } catch (IOException e) {
            results.add(new FileIngestResultDto(
                    inboxDir.toString(),
                    STATUS_FAILED,
                    null,
                    "Failed to scan inbox directory: " + e.getMessage()
            ));
        }

        int ingested = (int) results.stream().filter(r -> STATUS_INGESTED.equals(r.status())).count();
        int dupes = (int) results.stream().filter(r -> STATUS_DUPLICATE.equals(r.status())).count();
        int failed = (int) results.stream().filter(r -> STATUS_FAILED.equals(r.status())).count();

        return new IngestReportDto(Instant.now(), ingested, dupes, failed, results);
    }

    private FileIngestResultDto processOneFile(Path file) {
        String fileName = file.getFileName().toString();

        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);

            // Reuse your existing internal round-create DTO
            RoundController.CreateRoundRequest dto =
                    objectMapper.readValue(json, RoundController.CreateRoundRequest.class);

            validate(dto);

            GameType gameType = GameType.valueOf(dto.gameType());
            LocalDateTime start = LocalDateTime.parse(dto.roundStartDate());

            Optional<GameRoundEntity> existing =
                    gameRoundRepository.findByGameTypeAndStartDate(gameType, start);

            if (existing.isPresent()) {
                move(file, duplicatesDir.resolve(fileName));
                return new FileIngestResultDto(
                        fileName,
                        STATUS_DUPLICATE,
                        existing.get().getId(),
                        "Duplicate: same (gameType,startDate) already exists"
                );
            }

            long roundId = roundApiService.createRound(gameType, start, dto.matches());

            move(file, processedDir.resolve(fileName));
            return new FileIngestResultDto(fileName, STATUS_INGESTED, roundId, "Ingested OK");

        } catch (Exception e) {
            try {
                move(file, failedDir.resolve(fileName));
                writeErrorFile(fileName, e);
            } catch (IOException ignored) { }

            return new FileIngestResultDto(fileName, STATUS_FAILED, null, shorten(e));
        }
    }

    private void validate(RoundController.CreateRoundRequest dto) {
        if (dto == null) throw new IllegalArgumentException("Request is null");
        requireNonBlank(dto.gameType(), "gameType is required");
        requireNonBlank(dto.roundStartDate(), "roundStartDate is required");
        if (dto.matches() == null) throw new IllegalArgumentException("matches is required");

        GameType gameType = GameType.valueOf(dto.gameType());
        int expected = gameType.getNumberOfMatches();

        if (dto.matches().size() != expected) {
            throw new IllegalArgumentException("Expected " + expected + " matches for " + gameType
                    + " but got " + dto.matches().size());
        }

        Set<Integer> seen = new HashSet<>();

        for (ModelSelectionRequestDto.MatchDto m : dto.matches()) {
            if (m.matchNumber() == null) throw new IllegalArgumentException("match.matchNumber is required");
            int num = m.matchNumber();

            if (num <= 0) throw new IllegalArgumentException("matchNumber must be positive");
            if (!seen.add(num)) throw new IllegalArgumentException("Duplicate matchNumber: " + num);

            requireNonBlank(m.startDate(), "match.startDate is required");
            requireNonBlank(m.homeTeamName(), "match.homeTeamName is required");
            requireNonBlank(m.awayTeamName(), "match.awayTeamName is required");

            if (m.homeTeamName().equalsIgnoreCase(m.awayTeamName())) {
                throw new IllegalArgumentException("homeTeamName and awayTeamName must differ (matchNumber=" + num + ")");
            }
        }

        for (int i = 1; i <= expected; i++) {
            if (!seen.contains(i)) {
                throw new IllegalArgumentException("Missing matchNumber " + i + " (expected 1.." + expected + ")");
            }
        }
    }

    private void requireNonBlank(String s, String msg) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException(msg);
    }

    private void ensureDirs() {
        try {
            Files.createDirectories(inboxDir);
            Files.createDirectories(processedDir);
            Files.createDirectories(duplicatesDir);
            Files.createDirectories(failedDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create ingest directories: " + e.getMessage(), e);
        }
    }

    private void move(Path from, Path to) throws IOException {
        Files.createDirectories(to.getParent());
        Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
    }

    private void writeErrorFile(String originalFileName, Exception e) throws IOException {
        String errName = originalFileName + ".error.txt";
        Path errPath = failedDir.resolve(errName);
        String msg = "ERROR: " + e.getClass().getSimpleName() + "\n"
                + "MESSAGE: " + (e.getMessage() == null ? "<no message>" : e.getMessage()) + "\n";
        Files.writeString(errPath, msg, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private String shorten(Exception e) {
        String m = e.getMessage();
        if (m == null) return e.getClass().getSimpleName();
        if (m.length() <= 180) return e.getClass().getSimpleName() + ": " + m;
        return e.getClass().getSimpleName() + ": " + m.substring(0, 180) + "...";
    }
}