package io.github.gustavoalmeidas.finos.identity.domain;

import io.github.gustavoalmeidas.finos.shared.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "user_preferences")
public class UserPreferences extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "default_locale", nullable = false, length = 10)
    private String defaultLocale = "pt-BR";

    @Column(name = "default_timezone", nullable = false, length = 80)
    private String defaultTimezone = "America/Sao_Paulo";

    @Column(name = "default_currency", nullable = false, length = 3)
    private String defaultCurrency = "BRL";

    @Column(name = "date_format", nullable = false, length = 20)
    private String dateFormat = "dd/MM/yyyy";

    @Column(name = "number_format_locale", nullable = false, length = 10)
    private String numberFormatLocale = "pt-BR";

    public UserPreferences(User user) {
        this.user = user;
    }
}
