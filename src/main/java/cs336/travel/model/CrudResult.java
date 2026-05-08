package cs336.travel.model;

public sealed interface CrudResult
        permits CrudResult.Success, CrudResult.Refused, CrudResult.Error {

    /** Operation succeeded; {@code id} is the affected row's primary key. */
    record Success(int id) implements CrudResult {}

    /** Domain rule blocked the operation (e.g. delete with dependent rows). */
    record Refused(String reason) implements CrudResult {}

    record Error(String message) implements CrudResult {}
}
