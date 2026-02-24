package ar.ss.betting.persistence.repo;

import ar.ss.betting.persistence.entity.GameRoundEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRoundRepository extends JpaRepository<GameRoundEntity, Long> {
}