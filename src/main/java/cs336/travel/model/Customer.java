package cs336.travel.model;

public record Customer(
        int customerID,
        String username,
        String name,
        String email,
        String phone) {
}
