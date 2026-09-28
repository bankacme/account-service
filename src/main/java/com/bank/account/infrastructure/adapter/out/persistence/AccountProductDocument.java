package com.bank.account.infrastructure.adapter.out.persistence;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("account_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountProductDocument {

    @Id
    private String id;

    private String accountType;

    private String profile;

    private AccountConditionsData conditions;

    private Instant updatedAt;
}
