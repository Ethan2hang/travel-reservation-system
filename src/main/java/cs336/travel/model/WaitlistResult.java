package cs336.travel.model;

import java.util.List;

/**
 * Outcome of {@code WaitlistService.addToWaitlist(...)}. Sealed so the UI
 * exhausts both cases without a default branch.
 */
public sealed interface WaitlistResult
        permits WaitlistResult.Success, WaitlistResult.Error {

    /**
     * One {@code WaitlistEntry} was inserted per segment. {@code positions}
     * is parallel to the input segment list — entry i is position positions[i]
     * in its flight's queue.
     */
    record Success(List<Integer> waitlistIDs, List<Integer> positions) implements WaitlistResult {}

    record Error(String message) implements WaitlistResult {}
}
