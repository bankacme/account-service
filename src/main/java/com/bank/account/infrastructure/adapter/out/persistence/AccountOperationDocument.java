package com.bank.account.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * The {@code account_operations} collection (data-model.md 2.3) — the "receipt" of every
 * requested movement, keyed by its own natural id, the {@code operationId}, which is what
 * gives {@code ApplyMovementUseCase}/{@code ReverseMovementUseCase} their idempotency. No
 * {@code @Version}: this document is never concurrently updated by two different requests
 * for the same reason {@link AccountDocument} is — its own natural-id uniqueness is the
 * concurrency guard (a repeated {@code operationId} either replays or is rejected as
 * {@code OPERATION_ID_REUSED}, data-model.md 2.3).
 */
@Document("account_operations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountOperationDocument {

    @Id
    private String id;

    private String accountId;

    private String type;

    private BigDecimal amount;

    private LocalDate date;

    private YearMonth yearMonth;

    private String status;

    private BigDecimal fee;

    private BigDecimal newBalance;

    private Integer movementNumber;

    private String reasonCode;

    private BigDecimal reversedBalance;

    private Instant createdAt;

    private Instant updatedAt;
}
