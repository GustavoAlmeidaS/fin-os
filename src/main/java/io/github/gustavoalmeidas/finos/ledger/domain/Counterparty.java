package io.github.gustavoalmeidas.finos.ledger.domain;

import io.github.gustavoalmeidas.finos.identity.domain.User;
import io.github.gustavoalmeidas.finos.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "counterparties")
@SQLRestriction("deleted_at IS NULL")
public class Counterparty extends BaseEntity {

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 140)
    private String name;

    @Column(length = 30)
    private String document;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CounterpartyType type = CounterpartyType.OTHER;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
