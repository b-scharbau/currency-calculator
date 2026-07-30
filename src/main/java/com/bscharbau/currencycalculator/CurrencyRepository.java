package com.bscharbau.currencycalculator;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface CurrencyRepository extends JpaRepository<CurrencyEntity, Long> {

    List<CurrencyEntity> findAllByOrderByCodeAsc();
}
