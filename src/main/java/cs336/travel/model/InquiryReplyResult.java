package cs336.travel.model;

public sealed interface InquiryReplyResult permits
        InquiryReplyResult.Success,
        InquiryReplyResult.AlreadyAnswered,
        InquiryReplyResult.Refused,
        InquiryReplyResult.Error {

    record Success(int inquiryID) implements InquiryReplyResult {}

    /** Someone else replied between the CR's load and click. UI should refresh. */
    record AlreadyAnswered(int inquiryID) implements InquiryReplyResult {}

    record Refused(String reason) implements InquiryReplyResult {}

    record Error(String message) implements InquiryReplyResult {}
}
