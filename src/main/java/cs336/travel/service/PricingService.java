package cs336.travel.service;

import cs336.travel.model.TravelClass;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Class-of-service price multipliers. Lives in code (not DB) per
 * {@code docs/SCHEMA.md} "Open questions" §1. Single source of truth — any
 * other service (booking, reports) should call {@link #priceFor} rather
 * than re-multiplying.
 */
public final class PricingService {

    public static final BigDecimal ECONOMY_MULTIPLIER  = new BigDecimal("1.0");
    public static final BigDecimal BUSINESS_MULTIPLIER = new BigDecimal("2.5");
    public static final BigDecimal FIRST_MULTIPLIER    = new BigDecimal("4.0");

    private PricingService() {}

    public static BigDecimal multiplier(TravelClass cls) {
        return switch (cls) {
            case ECONOMY  -> ECONOMY_MULTIPLIER;
            case BUSINESS -> BUSINESS_MULTIPLIER;
            case FIRST    -> FIRST_MULTIPLIER;
        };
    }

    public static BigDecimal priceFor(BigDecimal basePrice, TravelClass cls) {
        return basePrice.multiply(multiplier(cls)).setScale(2, RoundingMode.HALF_UP);
    }
}
