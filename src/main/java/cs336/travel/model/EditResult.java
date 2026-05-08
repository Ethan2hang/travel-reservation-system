package cs336.travel.model;

import java.math.BigDecimal;

public sealed interface EditResult
        permits EditResult.Success, EditResult.Refused, EditResult.Error {

    record Success(int reservationID, BigDecimal newTotalFare) implements EditResult {}

    record Refused(String reason) implements EditResult {}

    record Error(String message) implements EditResult {}
}
