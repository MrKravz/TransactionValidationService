package by.ares.transaction_validation_service.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "limit_locks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LimitLockEntity {

    @EmbeddedId
    private LimitLockId id;
}