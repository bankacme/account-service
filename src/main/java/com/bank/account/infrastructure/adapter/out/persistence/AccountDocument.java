package com.bank.account.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * The {@code accounts} collection (data-model.md 2.1). {@code balance} and every money
 * field under {@code conditions}/{@code balanceTracker} are plain {@link BigDecimal}: with
 * {@link com.bank.account.infrastructure.config.MongoConfig}'s {@code MongoCustomConversions}
 * registered, Spring Data stores/reads them as {@code Decimal128} without a per-field
 * annotation (bank-spike, spike-report.md check #4). {@code currency} IS a separate
 * top-level field here (unlike the domain's {@link com.bank.account.domain.model.Money},
 * which carries its own currency) — data-model.md 2.1 lists it explicitly, and {@link
 * com.bank.account.domain.model.Account}'s own javadoc says the mapper denormalizes it
 * from {@code balance.currency()}.
 */
@Document("accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDocument {

    @Id
    private String id;

    private String accountNumber;

    private String customerId;

    private String customerType;

    private String customerProfile;

    private String type;

    private String alias;

    private BigDecimal balance;

    private String currency;

    private AccountConditionsData conditions;

    private Integer movementDayOfMonth;

    @Builder.Default
    private List<AccountPartyData> holders = new ArrayList<>();

    @Builder.Default
    private List<AccountPartyData> signers = new ArrayList<>();

    private MonthlyActivityData monthlyActivity;

    private BalanceTrackerData balanceTracker;

    private String status;

    @Version
    private long version;

    private Instant createdAt;

    private Instant updatedAt;
}
