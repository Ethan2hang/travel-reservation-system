package cs336.travel.model;

public sealed interface CancelResult
        permits CancelResult.Success, CancelResult.Refused, CancelResult.Error {

    record Success(int reservationID, int promotedCount) implements CancelResult {}

    /** Cancellation rejected on policy grounds (economy class, not the owner, etc). */
    record Refused(String reason) implements CancelResult {}

    record Error(String message) implements CancelResult {}
}
