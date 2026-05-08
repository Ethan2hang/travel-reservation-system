package cs336.travel.service;

import cs336.travel.Session;
import cs336.travel.dao.AuthDAO;
import cs336.travel.model.Customer;
import cs336.travel.model.Employee;
import cs336.travel.model.Role;

import java.util.Optional;

public final class AuthService {

    private AuthService() {}

    /**
     * Try Employee then Customer (employees use their work creds; same
     * username on both tables would be a setup bug). On success populates
     * {@link Session} and returns the role; on failure returns empty and
     * leaves Session untouched.
     */
    public static Optional<Role> login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            return Optional.empty();
        }

        Optional<Employee> emp = AuthDAO.authenticateEmployee(username, password);
        if (emp.isPresent()) {
            Session.setEmployee(emp.get());
            return Optional.of(emp.get().role());
        }

        Optional<Customer> cust = AuthDAO.authenticateCustomer(username, password);
        if (cust.isPresent()) {
            Session.setCustomer(cust.get());
            return Optional.of(Role.CUSTOMER);
        }

        return Optional.empty();
    }

    public static void logout() {
        Session.clear();
    }
}
