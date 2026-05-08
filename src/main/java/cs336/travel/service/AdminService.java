package cs336.travel.service;

import cs336.travel.Session;
import cs336.travel.dao.CustomerDAO;
import cs336.travel.dao.EmployeeDAO;
import cs336.travel.dao.InquiryDAO;
import cs336.travel.dao.NotificationDAO;
import cs336.travel.dao.ReportDAO;
import cs336.travel.dao.ReservationDAO;
import cs336.travel.dao.WaitlistDAO;
import cs336.travel.model.CrudResult;
import cs336.travel.model.Customer;
import cs336.travel.model.Employee;
import cs336.travel.model.AggregateRow;
import cs336.travel.model.MonthlySales;
import cs336.travel.model.ReservationLookupRow;
import cs336.travel.model.RevenueDetail;
import cs336.travel.model.Role;
import cs336.travel.model.TopCustomerRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AdminService {

    private AdminService() {}

    public static List<Employee> listCustomerReps() {
        requireAdmin();
        return EmployeeDAO.listCustomerReps();
    }

    public static List<Customer> listCustomers() {
        requireAdmin();
        return CustomerDAO.listAll();
    }

    public static CrudResult createCustomerRep(String username, String password, String name) {
        requireAdmin();
        try {
            int id = EmployeeDAO.insertCustomerRep(username, password, name);
            return new CrudResult.Success(id);
        } catch (EmployeeDAO.DuplicateUsernameException dup) {
            return new CrudResult.Error(dup.getMessage());
        } catch (RuntimeException e) {
            return new CrudResult.Error(e.getMessage());
        }
    }

    public static CrudResult updateCustomerRep(int employeeID, String username,
                                               String newPassword, String name) {
        requireAdmin();
        try {
            int rows = EmployeeDAO.updateEmployee(employeeID, username, newPassword, name);
            if (rows != 1) return new CrudResult.Error("No employee row updated.");
            return new CrudResult.Success(employeeID);
        } catch (EmployeeDAO.DuplicateUsernameException dup) {
            return new CrudResult.Error(dup.getMessage());
        } catch (RuntimeException e) {
            return new CrudResult.Error(e.getMessage());
        }
    }

    public static CrudResult deleteCustomerRep(int employeeID) {
        requireAdmin();
        int created    = ReservationDAO.countCreatedByEmployee(employeeID);
        int answered   = InquiryDAO.countAnsweredByEmployee(employeeID);
        List<String> blockers = new ArrayList<>();
        if (created  > 0) blockers.add(plural(created,  "reservation"));
        if (answered > 0) blockers.add(plural(answered, "answered inquiry"));
        if (!blockers.isEmpty()) {
            String label = EmployeeDAO.findById(employeeID)
                    .map(e -> e.username() + " (" + e.name() + ")")
                    .orElse("employee #" + employeeID);
            return new CrudResult.Refused(
                    "Cannot delete: " + label + " has " + joinList(blockers)
                            + ". Resolve them first.");
        }
        try {
            int rows = EmployeeDAO.deleteEmployee(employeeID);
            if (rows != 1) return new CrudResult.Error("No employee row deleted.");
            return new CrudResult.Success(employeeID);
        } catch (RuntimeException e) {
            return new CrudResult.Error(
                    "Could not delete; the database refused the operation.");
        }
    }

    public static CrudResult createCustomer(String username, String password, String name,
                                            String email, String phone) {
        requireAdmin();
        try {
            int id = CustomerDAO.insertCustomer(username, password, name, email, phone);
            return new CrudResult.Success(id);
        } catch (CustomerDAO.DuplicateFieldException dup) {
            return new CrudResult.Error(dup.getMessage());
        } catch (RuntimeException e) {
            return new CrudResult.Error(e.getMessage());
        }
    }

    public static CrudResult updateCustomer(int customerID, String username, String newPassword,
                                            String name, String email, String phone) {
        requireAdmin();
        try {
            int rows = CustomerDAO.updateCustomer(
                    customerID, username, newPassword, name, email, phone);
            if (rows != 1) return new CrudResult.Error("No customer row updated.");
            return new CrudResult.Success(customerID);
        } catch (CustomerDAO.DuplicateFieldException dup) {
            return new CrudResult.Error(dup.getMessage());
        } catch (RuntimeException e) {
            return new CrudResult.Error(e.getMessage());
        }
    }

    public static CrudResult deleteCustomer(int customerID) {
        requireAdmin();
        // Customer.customerID is a FK target on Reservation, WaitlistEntry,
        // Notification, and Inquiry. Sum every dependent count and tell the
        // admin exactly what's blocking the delete.
        int reservations  = ReservationDAO.countConfirmedForCustomer(customerID);
        int waitlist      = WaitlistDAO.countForCustomer(customerID);
        int notifications = NotificationDAO.countForCustomer(customerID);
        int inquiries     = InquiryDAO.countForCustomer(customerID);

        List<String> blockers = new ArrayList<>();
        if (reservations  > 0) blockers.add(plural(reservations,  "active reservation"));
        if (waitlist      > 0) blockers.add(plural(waitlist,      "waitlist entry", "waitlist entries"));
        if (notifications > 0) blockers.add(plural(notifications, "notification"));
        if (inquiries     > 0) blockers.add(plural(inquiries,     "inquiry",        "inquiries"));

        if (!blockers.isEmpty()) {
            String label = CustomerDAO.findById(customerID)
                    .map(c -> c.username() + " (" + c.name() + ")")
                    .orElse("customer #" + customerID);
            return new CrudResult.Refused(
                    "Cannot delete: " + label + " has " + joinList(blockers)
                            + ". Resolve them first.");
        }
        try {
            int rows = CustomerDAO.deleteCustomer(customerID);
            if (rows != 1) return new CrudResult.Error("No customer row deleted.");
            return new CrudResult.Success(customerID);
        } catch (RuntimeException e) {
            return new CrudResult.Error(
                    "Could not delete; the database refused the operation.");
        }
    }

    private static String plural(int n, String singular) {
        return plural(n, singular, singular + "s");
    }

    private static String plural(int n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }

    /** "A", "A and B", "A, B, and C" — comma-Oxford join for the refusal message. */
    private static String joinList(List<String> parts) {
        int n = parts.size();
        if (n == 1) return parts.get(0);
        if (n == 2) return parts.get(0) + " and " + parts.get(1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n - 1; i++) sb.append(parts.get(i)).append(", ");
        sb.append("and ").append(parts.get(n - 1));
        return sb.toString();
    }

    public static MonthlySales salesForMonth(int year, int month) {
        requireAdmin();
        return ReportDAO.salesForMonth(year, month);
    }

    public static List<ReservationLookupRow> reservationsForFlight(
            String airlineID, String flightNumber) {
        requireAdmin();
        return ReportDAO.reservationsForFlight(airlineID, flightNumber);
    }

    public static List<ReservationLookupRow> reservationsForCustomerName(String query) {
        requireAdmin();
        return ReportDAO.reservationsForCustomerName(query);
    }

    public static RevenueDetail revenueByFlight(String airlineID, String flightNumber) {
        requireAdmin();
        return ReportDAO.revenueByFlight(airlineID, flightNumber);
    }

    public static RevenueDetail revenueByAirline(String airlineID) {
        requireAdmin();
        return ReportDAO.revenueByAirline(airlineID);
    }

    public static RevenueDetail revenueByCustomer(String nameFragment) {
        requireAdmin();
        return ReportDAO.revenueByCustomer(nameFragment);
    }

    public static Optional<TopCustomerRow> topCustomerByRevenue() {
        requireAdmin();
        return ReportDAO.topCustomerByRevenue();
    }

    public static List<AggregateRow> mostActiveFlights(int limit) {
        requireAdmin();
        return ReportDAO.mostActiveFlights(limit);
    }

    private static void requireAdmin() {
        if (Session.role() != Role.ADMIN) {
            throw new IllegalStateException("Admin access required.");
        }
    }
}
