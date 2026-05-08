package cs336.travel.model;

public record Employee(
        int employeeID,
        String username,
        String name,
        Role role) {
}
