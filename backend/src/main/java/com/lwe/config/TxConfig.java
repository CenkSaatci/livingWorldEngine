package com.lwe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/** P34-T03: programmatic Tx pro Bulk-Eintrag (REQUIRES_NEW-Semantik je Eintrag). */
@Configuration
public class TxConfig {

    @Bean
    public TransactionOperations transactionOperations(PlatformTransactionManager txManager) {
        return new TransactionTemplate(txManager);
    }
}
