package controllers;

import models.Bank;
import models.Customer;
import play.cache.Cache;
import play.data.validation.Validation;
import play.db.jpa.JPA;
import play.mvc.Before;
import play.mvc.Controller;

/**
 * Created by Phuong on 16/03/2016.
 */
public class CustomerController extends Controller {
    @Before(unless = {"registerForm", "register", "loginForm", "login", "logout"})
    static void checkNotAuthentification() {
        if (Cache.get(session.get("username")) == null) {
            redirect("../Login-form.html");
        }
    }

    @Before(only = {"registerForm", "register", "loginForm", "login"})
    static void checkAuthentification() {
        Customer customer = (Customer) Cache.get(session.get("username"));
        if (customer != null) {
            customer = Customer.findById(customer.id);
            Cache.set(session.get("username"), customer);
            redirect("../View-info.html");
        }
    }

    public static void loginForm() {
        if (session.get("noti") != null) {
            renderArgs.put("noti", session.get("noti"));
            session.remove("noti");
        }
        render("customer/login.form.html");
    }

    public static void login(String username, String pwd) {
        try {
            Customer customer = (Customer) Customer.find("FROM Customer cus " +
                    "WHERE cus.username = :username " +
                    "AND cus.pwd = :pwd").bind("username", username)
                    .bind("pwd", pwd).fetch().get(0);
            Cache.add(username, customer);
            session.put("username", username);
            redirect("../View-info.html");
        } catch (Exception e) {
            session.put("noti", "UsernameAndPassword");
            redirect("../Login-form.html");
        }
    }

    public static void logout() {
        Cache.delete(session.get("username"));
        session.remove("username");
        redirect("../Login-form.html");
    }

    public static void registerForm() {
        if (session.get("noti") != null) {
            renderArgs.put("noti", session.get("noti"));
            session.remove("noti");
        }
        render("customer/register.form.html");
    }

    public synchronized static void register(String username, String pwd, String fullName, String email,
                                             String phone, String address, String name,
                                             String cardNumber, String cardType) {
        Validation.ValidationResult result = validation.email(email);
        if (!result.ok) {
            session.put("noti", "EmailNotRequired");
            redirect("../Register-form.html");
        }
        if (Customer.find("byUsername", username).fetch().size() != 0) {
            session.put("noti", "UsernameExist");
            redirect("../Register-form.html");
        }
        Bank bank = new Bank(name, cardNumber, cardType);
        int idBank = 0;
        try {
            idBank = (Integer) JPA.em().createQuery("SELECT MAX(b.id) FROM Bank b").getSingleResult() + 1;
        } catch (NullPointerException e) {
        }
        bank.id = idBank;
        bank.save();
        Customer customer = new Customer(username, pwd, fullName, email, phone, address, 0, bank);
        int idCustomer = 0;
        try {
            idCustomer = (Integer) JPA.em().createQuery("SELECT MAX(cus.id) FROM Customer cus").getSingleResult() + 1;
        } catch (NullPointerException e) {
        }
        customer.id = idCustomer;
        customer.save();
        Cache.add(username, customer);
        session.put("username", username);
        redirect("../View-info.html");
    }

    public static void viewInfo() {
        renderArgs.put("customer", (Customer) Cache.get(session.get("username")));
        render("customer/view.info.html");
    }
}
