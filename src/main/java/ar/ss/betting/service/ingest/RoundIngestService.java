package ar.ss.betting.service.ingest;

import ar.ss.betting.api.dto.ModelSelectionRequestDto;
import ar.ss.betting.domain.GameType;
import ar.ss.betting.persistence.repo.GameRoundRepository;
import ar.ss.betting.service.RoundApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class RoundIngestService {

    private final RoundApiService roundApiService;
    private final GameRoundRepository gameRoundRepository;
    private final ObjectMapper objectMapper;

    private final Path roundsDir;
    private final Path processedDir;
    private final Path duplicatesDir;

    public RoundIngestService(RoundApiService roundApiService,
                              GameRoundRepository gameRoundRepository,
                              ObjectMapper objectMapper,
                              @Value("${betting.ingest.rounds-dir:./ingest/rounds}") String roundsDir) {

        this.roundApiService = Objects.requireNonNull(roundApiService);
        this.gameRoundRepository = Objects.requireNonNull(gameRoundRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);

        this.roundsDir = Paths.get(roundsDir);
        Path ingestRoot = this.roundsDir.getParent() != null ? this.roundsDir.getParent() : Paths.get("./ingest");
        this.processedDir = ingestRoot.resolve("processed");
        this.duplicatesDir = ingestRoot.resolve("duplicates");
    }

    /**
     * Scans the ingest folder for *.json files, ingests rounds into DB, then moves files:
     * - processed/ if ingested
     * - duplicates/ if a round with same (gameType + startDate) already exists
     *
     * This keeps the incoming folder clean and gives you an audit trail of what was ingested.
     */
    public IngestReport ingestAll() {
        ensureDirs();

        List<IngestedFile> results = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(roundsDir, "*.json")) {
            for (Path file : stream) {
                results.add(processOneFile(file));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed reading ingest directory: " + roundsDir, e);
        }

        return new IngestReport(roundsDir.toAbsolutePath().toString(), results);
    }

    private IngestedFile processOneFile(Path file) {
        RoundFileDto dto = read(file);

        GameType gameType = GameType.valueOf(dto.gameType());
        LocalDateTime start = LocalDateTime.parse(dto.roundStartDate());

        boolean exists = gameRoundRepository.existsByGameTypeAndStartDate(gameType, start);

        if (exists) {
            moveTo(duplicatesDir, file);
            return new IngestedFile(file.getFileName().toString(), "DUPLICATE_SKIPPED", null);
        }

        long roundId = roundApiService.createRound(gameType, start, dto.matches());
        moveTo(processedDir, file);

        return new IngestedFile(file.getFileName().toString(), "INGESTED", roundId);
    }

    private RoundFileDto read(Path file) {
        try {
            return objectMapper.readValue(file.toFile(), RoundFileDto.class);
        } catch (IOException e) {
            // If the file is malformed, we *do not* move it automatically: you can fix it and retry.
            throw new IllegalArgumentException("Failed to parse JSON ingest file: " + file.toAbsolutePath(), e);
        }
    }

    private void ensureDirs() {
        try {
            Files.createDirectories(roundsDir);
            Files.createDirectories(processedDir);
            Files.createDirectories(duplicatesDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed creating ingest directories", e);
        }
    }

    private void moveTo(Path targetDir, Path file) {
        try {
            Path target = targetDir.resolve(file.getFileName().toString());
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed moving file " + file + " to " + targetDir, e);
        }
    }

    // --- DTO + Report records ---

    public record RoundFileDto(
            String gameType,
            String roundStartDate,
            List<ModelSelectionRequestDto.MatchDto> matches
    ) { }

    public record IngestReport(
            String scannedDirectory,
            List<IngestedFile> files
    ) { }

    public record IngestedFile(
            String fileName,
            String status,
            Long roundId
    ) { }
}