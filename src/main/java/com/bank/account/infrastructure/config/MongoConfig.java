package com.bank.account.infrastructure.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.bson.types.Decimal128;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class MongoConfig {

    @Bean
    public ReactiveMongoTransactionManager reactiveTransactionManager(ReactiveMongoDatabaseFactory factory) {
        return new ReactiveMongoTransactionManager(factory);
    }

    @Bean
    public TransactionalOperator transactionalOperator(ReactiveMongoTransactionManager txManager) {
        return TransactionalOperator.create(txManager);
    }

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                BigDecimalToDecimal128Converter.INSTANCE,
                Decimal128ToBigDecimalConverter.INSTANCE,
                LocalDateToStringConverter.INSTANCE,
                StringToLocalDateConverter.INSTANCE,
                YearMonthToStringConverter.INSTANCE,
                StringToYearMonthConverter.INSTANCE));
    }

    @WritingConverter
    enum BigDecimalToDecimal128Converter implements Converter<BigDecimal, Decimal128> {
        INSTANCE;

        @Override
        public Decimal128 convert(BigDecimal source) {
            return new Decimal128(source);
        }
    }

    @ReadingConverter
    enum Decimal128ToBigDecimalConverter implements Converter<Decimal128, BigDecimal> {
        INSTANCE;

        @Override
        public BigDecimal convert(Decimal128 source) {
            return source.bigDecimalValue();
        }
    }

    @WritingConverter
    enum LocalDateToStringConverter implements Converter<LocalDate, String> {
        INSTANCE;

        @Override
        public String convert(LocalDate source) {
            return source.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }

    @ReadingConverter
    enum StringToLocalDateConverter implements Converter<String, LocalDate> {
        INSTANCE;

        @Override
        public LocalDate convert(String source) {
            return LocalDate.parse(source, DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }

    @WritingConverter
    enum YearMonthToStringConverter implements Converter<YearMonth, String> {
        INSTANCE;

        @Override
        public String convert(YearMonth source) {
            return source.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
    }

    @ReadingConverter
    enum StringToYearMonthConverter implements Converter<String, YearMonth> {
        INSTANCE;

        @Override
        public YearMonth convert(String source) {
            return YearMonth.parse(source, DateTimeFormatter.ofPattern("yyyy-MM"));
        }
    }
}
