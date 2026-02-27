package ar.ss.betting.persistence.repo;

import ar.ss.betting.domain.GameType;
import ar.ss.betting.persistence.entity.GameRoundEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GameRoundRepository extends CrudRepository<GameRoundEntity, Long> {

    boolean existsById(long id);

    @Query("""
            SELECT gr
            FROM GameRoundEntity gr
            WHERE gr.gameType = :gameType
              AND gr.startDate <= :now
              AND gr.endDate > :now
            ORDER BY gr.startDate DESC
            """)
    List<GameRoundEntity> findRunningByGameType(@Param("gameType") GameType gameType,
                                                @Param("now") LocalDateTime now,
                                                Pageable pageable);

    @Query("""
            SELECT gr
            FROM GameRoundEntity gr
            WHERE gr.gameType = :gameType
              AND gr.startDate > :now
            ORDER BY gr.startDate ASC
            """)
    List<GameRoundEntity> findNextUpcomingByGameType(@Param("gameType") GameType gameType,
                                                     @Param("now") LocalDateTime now,
                                                     Pageable pageable);

    /**
     * Scheduler use: upcoming rounds within a horizon.
     * IMPORTANT: excludes started rounds (startDate must be > now).
     */
    @Query("""
            SELECT gr
            FROM GameRoundEntity gr
            WHERE gr.startDate > :from
              AND gr.startDate <= :to
            ORDER BY gr.startDate ASC
            """)
    List<GameRoundEntity> findUpcomingRounds(@Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);
}