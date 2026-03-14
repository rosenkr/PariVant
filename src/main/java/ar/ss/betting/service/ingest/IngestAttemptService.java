package ar.ss.betting.service.ingest;

import ar.ss.betting.persistence.entity.IngestAttemptEntity;
import ar.ss.betting.persistence.repo.IngestAttemptRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class IngestAttemptService {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    private final IngestAttemptRepository repository;

    public IngestAttemptService(IngestAttemptRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public long recordSuccess(String source, String gameType, String endpoint, String reason) {
        IngestAttemptEntity e = new IngestAttemptEntity(
                source,
                gameType,
                endpoint,
                STATUS_SUCCESS,
                reason,
                null,
                OffsetDateTime.now()
        );
        return repository.save(e).getId();
    }

    public long recordFailure(String source, String gameType, String endpoint, Throwable ex) {
        String reason = ex.getClass().getSimpleName() + ": " + safeMessage(ex.getMessage());
        String details = stackTraceFirstLines(ex, 30);

        IngestAttemptEntity e = new IngestAttemptEntity(
                source,
                gameType,
                endpoint,
                STATUS_FAILED,
                reason,
                details,
                OffsetDateTime.now()
        );
        return repository.save(e).getId();
    }

    public List<IngestAttemptEntity> latest(int limit) {
        return repository.findLatest(PageRequest.of(0, Math.max(1, Math.min(limit, 200))));
    }

    public List<IngestAttemptEntity> latestFailed(int limit) {
        return repository.findLatestByStatus(STATUS_FAILED, PageRequest.of(0, Math.max(1, Math.min(limit, 200))));
    }

    private static String safeMessage(String msg) {
        return msg == null ? "" : msg;
    }

    private static String stackTraceFirstLines(Throwable ex, int maxLines) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        String[] lines = sw.toString().split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(lines.length, maxLines); i++) {
            sb.append(lines[i]).append("\n");
        }
        return sb.toString();
    }
}
