package cs336.travel.model;

import java.util.Optional;

/**
 * Caller-supplied diff for one segment of a reservation. {@link Optional}
 * fields mean "leave unchanged"; {@link Optional#of(Object)} means "replace
 * with this value." Empty deltas (all three Optionals empty) are allowed
 * and skipped by the service.
 *
 * <p>"Reschedule" (changing flight or date) is intentionally out of scope
 * here; that path is handled via Cancel + Re-book to keep capacity / pricing
 * checks in one place.
 */
public record SegmentEditDelta(
        int segmentOrder,
        Optional<TravelClass> newClass,
        Optional<String> newSeat,
        Optional<String> newMeal) {

    public boolean hasAnyChange() {
        return newClass.isPresent() || newSeat.isPresent() || newMeal.isPresent();
    }
}
