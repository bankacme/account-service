package com.bank.account.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.account.application.port.in.ProductFilter;
import com.bank.account.domain.model.AccountProduct;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerProfile;
import com.bank.account.infrastructure.fixture.AccountFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AccountProductPersistenceAdapterTest {

    @Autowired
    private AccountProductPersistenceAdapter adapter;

    @Autowired
    private AccountProductMongoRepository repository;

    @BeforeEach
    @AfterEach
    void cleanCollection() {
        repository.deleteAll().blockingAwait();
    }

    @Test
    void savedProductsAreFoundByIdAndByTypeAndProfile() {
        AccountProduct product = AccountProduct.create(AccountType.SAVINGS, CustomerProfile.VIP,
                AccountFixtures.vipSavingsConditions(), AccountFixtures.CLOCK);

        adapter.save(product).blockingGet();

        assertThat(adapter.findById(product.id()).blockingGet()).isEqualTo(product);
        assertThat(adapter.findByTypeAndProfile(AccountType.SAVINGS, CustomerProfile.VIP).blockingGet())
                .isEqualTo(product);
    }

    @Test
    void findAllCombinesTypeAndProfileFiltersWhenBothArePresent() {
        adapter.save(AccountProduct.create(AccountType.SAVINGS, CustomerProfile.STANDARD,
                AccountFixtures.standardConditions(), AccountFixtures.CLOCK)).blockingGet();
        adapter.save(AccountProduct.create(AccountType.SAVINGS, CustomerProfile.VIP,
                AccountFixtures.vipSavingsConditions(), AccountFixtures.CLOCK)).blockingGet();
        adapter.save(AccountProduct.create(AccountType.CHECKING, CustomerProfile.STANDARD,
                AccountFixtures.standardConditions(), AccountFixtures.CLOCK)).blockingGet();

        ProductFilter savingsOnly = new ProductFilter(AccountType.SAVINGS, null);
        ProductFilter savingsVip = new ProductFilter(AccountType.SAVINGS, CustomerProfile.VIP);

        assertThat(adapter.findAll(savingsOnly).toList().blockingGet()).hasSize(2);
        assertThat(adapter.findAll(savingsVip).toList().blockingGet()).hasSize(1);
        assertThat(adapter.findAll(ProductFilter.all()).toList().blockingGet()).hasSize(3);
    }
}
