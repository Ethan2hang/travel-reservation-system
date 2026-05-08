package cs336.travel;

import cs336.travel.model.Customer;
import cs336.travel.model.Employee;
import cs336.travel.model.Role;

/**
 * Process-wide singleton holding the currently logged-in principal.
 *
 * <p>Set by {@code AuthService} on successful login; cleared on logout.
 * Service-layer methods that require authorization read {@link #role()} here.
 */
public final class Session {

    private static Customer customer;
    private static Employee employee;

    private Session() {}

    public static void setCustomer(Customer c) {
        customer = c;
        employee = null;
    }

    public static void setEmployee(Employee e) {
        employee = e;
        customer = null;
    }

    public static void clear() {
        customer = null;
        employee = null;
    }

    public static boolean isLoggedIn() {
        return customer != null || employee != null;
    }

    public static Role role() {
        if (employee != null) return employee.role();
        if (customer != null) return Role.CUSTOMER;
        return null;
    }

    public static Customer customer() {
        return customer;
    }

    public static Employee employee() {
        return employee;
    }

    public static String displayName() {
        if (employee != null) return employee.name();
        if (customer != null) return customer.name();
        return "(not signed in)";
    }
}
