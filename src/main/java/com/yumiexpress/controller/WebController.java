package com.yumiexpress.controller;

import com.yumiexpress.model.Order;
import com.yumiexpress.model.User;
import com.yumiexpress.repository.UserRepository;
import com.yumiexpress.repository.OrderRepository;
import com.yumiexpress.service.DispatchService;
import com.yumiexpress.service.PaymentService;
import com.yumiexpress.service.PricingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class WebController {

    @Autowired
    private PricingService pricingService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private DispatchService dispatchService;
    @Autowired
    private UserRepository userRepository;

    // --- PUBLIC PAGES ---
    @GetMapping("/")
    public String home() { return "index"; }

    @GetMapping("/register")
    public String showRegister() { return "register"; }

    @GetMapping("/login")
    public String showLogin() { return "login"; }

    @PostMapping("/process-register")
    public String processRegister(
            @RequestParam String role,
            @RequestParam String fullName,
            @RequestParam String nationalId,
            @RequestParam String phoneNumber,
            @RequestParam String area,
            @RequestParam(required = false) String vehicleType) {

        User user = new User();
        user.setFullName(fullName);
        user.setNationalId(nationalId);
        user.setPhoneNumber(phoneNumber);
        user.setArea(area);
        user.setRole(role);
        user.setPassword(nationalId);

        if ("RIDER".equals(role)) {
            user.setAvailable(true);
            user.setVehicleType(vehicleType);
            user.setPayRate("OWN_SCOOTER".equals(vehicleType) ? 350.0 : 150.0);
            user.setCurrentLat(-17.73);
            user.setCurrentLng(168.31);
        }

        User savedUser = userRepository.save(user);

        if ("RIDER".equals(role)) {
            return "redirect:/admin-portal";
        } else {
            return "redirect:/customer-dashboard/" + savedUser.getId();
        }
    }

    // 1. Show the Customer Dashboard
    @GetMapping("/customer-dashboard/{customerId}")
    public String showCustomerDashboard(@PathVariable Long customerId, Model model) {
        User customer = userRepository.findById(customerId).orElseThrow();

        // FETCH ORDERS SPECIFICALLY LINKED TO THIS CUSTOMER
        List<Order> customerOrders = orderRepository.findAll().stream()
                .filter(o -> o.getCustomer() != null && o.getCustomer().getId().equals(customerId))
                .toList();

        model.addAttribute("customer", customer);
        model.addAttribute("orders", customerOrders);
        return "customer-dashboard";
    }

    // --- BOOKING FORM ---
    @GetMapping("/book")
    public String showBookingForm(@RequestParam(required = false) Long userId, Model model) {
        if (userId != null) {
            User customer = userRepository.findById(userId).orElse(null);
            model.addAttribute("customer", customer);
        }
        return "book";
    }

    @PostMapping("/submit-booking")
    public String submitBooking(
            @RequestParam Long userId,
            @RequestParam String recipientPhone,
            @RequestParam String exactPickupLocation,
            @RequestParam String specificAddressDetails, // <--- Catch the new field
            @RequestParam String pickupTime,
            @RequestParam double lat,
            @RequestParam double lng,
            Model model) {

        User customer = userRepository.findById(userId).orElseThrow();

        Order newOrder = new Order();
        newOrder.setCustomer(customer);
        newOrder.setPickupAddress(customer.getFullName());
        newOrder.setExactPickupLocation(exactPickupLocation);
        newOrder.setSpecificAddressDetails(specificAddressDetails); // <--- Save it
        newOrder.setPickupTime(pickupTime);
        newOrder.setRecipientPhone(recipientPhone);
        newOrder.setStatus("AWAITING_PICKUP");
        newOrder.setPickupLat(lat);
        newOrder.setPickupLng(lng);

        Order savedOrder = orderRepository.save(newOrder);
        model.addAttribute("order", savedOrder);
        return "success";
    }

    // --- RIDER PAGES ---
    @GetMapping("/rider-portal/{riderId}")
    public String showRiderDashboard(@PathVariable Long riderId, Model model) {
        User rider = userRepository.findById(riderId).orElseThrow(() -> new RuntimeException("Rider not found"));
        List<Order> tasks = orderRepository.findByRider(rider);
        model.addAttribute("rider", rider);
        model.addAttribute("tasks", tasks);
        return "rider-dashboard";
    }

    @GetMapping("/rider/update-status/{id}")
    public String updateRiderStatus(@PathVariable Long id, @RequestParam String status) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setStatus(status);
        orderRepository.save(order);
        return "redirect:/rider-portal/" + order.getRider().getId();
    }

    @GetMapping("/admin-portal")
    public String showAdminDashboard(Model model) {
        // Fetch all orders
        List<Order> allOrders = orderRepository.findAll();
        allOrders.sort((a, b) -> b.getId().compareTo(a.getId()));
        model.addAttribute("allOrders", allOrders);

        // Fetch all Riders
        List<User> riders = userRepository.findAll().stream()
                .filter(u -> "RIDER".equals(u.getRole()))
                .toList();
        model.addAttribute("riders", riders);

        return "admin";
    }

    @GetMapping("/admin/assign-rider/{id}")
    public String assignRider(@PathVariable Long id) {
        Order order = orderRepository.findById(id).orElseThrow();
        User rider = dispatchService.assignBestRider(order);

        if (rider != null) {
            order.setRider(rider);
            order.setStatus("ASSIGNED");
            orderRepository.save(order);
        }
        return "redirect:/admin-portal";
    }

    // --- WEIGH & PAY FLOW ---
    @GetMapping("/rider/weigh-parcel/{id}")
    public String showWeighParcel(@PathVariable Long id, Model model) {
        Order order = orderRepository.findById(id).orElseThrow();
        model.addAttribute("order", order);
        return "weigh-parcel";
    }

    @PostMapping("/rider/weigh-parcel/{id}")
    public String processWeighParcel(@PathVariable Long id, @RequestParam double weight) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setWeightInKg(weight);
        order.setPrice(pricingService.calculatePrice(weight));

        if (order.getTrackingId() == null) {
            order.setTrackingId("YE-" + (10000 + new java.util.Random().nextInt(90000)));
        }

        paymentService.createPaymentRequest(order);
        paymentService.confirmPayment(order.getM_vatu_reference());

        order.setStatus("ASSIGNED");
        orderRepository.save(order);
        return "redirect:/receipt/" + order.getId();
    }

    @GetMapping("/receipt/{orderId}")
    public String showReceipt(@PathVariable Long orderId, Model model) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        model.addAttribute("order", order);
        return "receipt";
    }

    // --- CUSTOMER REGISTRATION VIA OTP ---
    @GetMapping("/customer-register")
    public String showCustomerRegister() { return "customer-register"; }

    @PostMapping("/send-otp")
    public String sendOtp(@RequestParam String phoneNumber, Model model) {
        System.out.println("📱 SIMULATED SMS SENT TO: " + phoneNumber + " | Code: 1234");
        model.addAttribute("phoneNumber", phoneNumber);
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String phoneNumber, @RequestParam String otpCode) {
        if (!"1234".equals(otpCode)) throw new RuntimeException("Invalid OTP Code");

        User user = userRepository.findByPhoneNumber(phoneNumber).orElse(null);

        if (user == null) {
            user = new User();
            user.setPhoneNumber(phoneNumber);
            user.setFullName("Customer " + phoneNumber);
            user.setRole("CUSTOMER");
            user.setPassword("default123");
            userRepository.save(user);
        }
        return "redirect:/customer-dashboard/" + user.getId();
    }

    // --- RATING SYSTEM ---
    @PostMapping("/rate-order/{id}")
    public String rateOrder(@PathVariable Long id, @RequestParam int rating) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setCustomerRating(rating);
        orderRepository.save(order);
        return "redirect:/customer-dashboard/" + order.getCustomer().getId();
    }
    // 1. Show the sign-off page to the receiver
    @GetMapping("/receiver/sign-off/{id}")
    public String showSignOffPage(@PathVariable Long id, Model model) {
        Order order = orderRepository.findById(id).orElseThrow();
        model.addAttribute("order", order);
        return "sign-off";
    }

    // 2. Process the receiver's signature and finalize the delivery
    @PostMapping("/receiver/sign-off/{id}")
    public String processSignOff(@PathVariable Long id) {
        Order order = orderRepository.findById(id).orElseThrow();

        // 1. Update order status
        order.setStatus("DELIVERED");
        orderRepository.save(order);

        // 2. Finalize Payment (Confirm M-Vatu transfer)
        paymentService.processPayment(order);

        // 3. Send SMS alerts to everyone
        System.out.println("\n🚨 FINAL DELIVERY ALERT 🚨");
        System.out.println("Order #" + order.getId() + " (Tracking: " + order.getTrackingId() + ") has been DELIVERED and SIGNED for.");
        System.out.println("Notification sent to Customer, Admin, and Rider.");

        // 4. Redirect to a final success screen
        return "redirect:/receipt/" + order.getId();
    }
    @GetMapping("/seller-login")
    public String showSellerLogin() {
        return "seller-login";
    }

    @GetMapping("/seller-dashboard")
    public String showSellerDashboard(@RequestParam String businessName, @RequestParam String phoneNumber, @RequestParam String email, Model model) {

        // In production, this is where you would call the Vodafone/Digicel API
        // to verify the phone number is active and belongs to the business.
        System.out.println("🔍 VERIFYING SELLER: " + businessName + " | Phone: " + phoneNumber + " | Email: " + email);
        System.out.println("✅ VERIFICATION PASSED (Simulated via Vodafone/Digicel)");

        model.addAttribute("businessName", businessName);
        return "seller-dashboard";
    }
    // 1. This simulates receiving the 500 parcels at the warehouse
    @PostMapping("/warehouse/bulk-intake")
    public String receiveBulkParcels(@RequestParam String sellerName, @RequestParam int count) {
        for (int i = 0; i < count; i++) {
            Order order = new Order();
            order.setPickupAddress(sellerName);
            order.setExactPickupLocation("YumiExpress Warehouse, Tagabe");
            order.setStatus("AWAITING_WEIGH_IN");
            // Set a dummy pickup location so the TSP algorithm works later
            order.setPickupLat(-17.73);
            order.setPickupLng(168.31);
            orderRepository.save(order);
        }
        System.out.println("📦 Bulk Intake: " + count + " parcels received from " + sellerName);
        return "redirect:/admin-portal";
    }

    // 2. Show the fast-paced Scanning Station
    @GetMapping("/warehouse/scan")
    public String showScanningStation(Model model) {
        // Find the first parcel that hasn't been weighed yet
        Order nextOrder = orderRepository.findAll().stream()
                .filter(o -> "AWAITING_WEIGH_IN".equals(o.getStatus()))
                .findFirst()
                .orElse(null);

        model.addAttribute("order", nextOrder);
        return "warehouse-scan";
    }

    // 3. Process the weight, calculate price, generate QR, and loop to the next
    @PostMapping("/warehouse/scan")
    public String processScan(@RequestParam Long orderId, @RequestParam double weight) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        // Calculate price (950 VT for 3kg, 1200 VT for 5kg, etc.)
        order.setWeightInKg(weight);
        order.setPrice(pricingService.calculatePrice(weight));
        order.setTrackingId("YE-" + (10000 + new java.util.Random().nextInt(90000)));
        order.setStatus("SORTED"); // Ready for rider assignment

        orderRepository.save(order);

        // Refresh the page to load the NEXT parcel automatically
        return "redirect:/warehouse/scan";
    }
    // Simulate scanning a parcel at the warehouse
    @GetMapping("/warehouse/scan/{orderId}")
    public String scanParcelAtWarehouse(@PathVariable Long orderId, Model model) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        model.addAttribute("order", order);
        return "warehouse-weigh-in";
    }

    // Process the weigh-in at the warehouse
    @PostMapping("/warehouse/scan/{orderId}")
    public String processWarehouseWeighIn(@PathVariable Long orderId, @RequestParam double weight) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        order.setWeightInKg(weight);
        order.setPrice(pricingService.calculatePrice(weight));
        order.setTrackingId("YE-" + (10000 + new java.util.Random().nextInt(90000)));
        order.setStatus("AWAITING_RIDER_ASSIGNMENT");

        orderRepository.save(order);

        return "redirect:/admin-portal";
    }
    // 1. Show the scan page
    @GetMapping("/admin/scan-seller-barcode/{id}")
    public String showScanSellerPage(@PathVariable Long id, Model model) {
        Order order = orderRepository.findById(id).orElseThrow();
        model.addAttribute("order", order);
        return "scan-seller";
    }

    // 2. Process the seller barcode and address
    @PostMapping("/admin/scan-seller-barcode/{id}")
    public String processSellerBarcode(@PathVariable Long id,
                                       @RequestParam String sellerBarcode,
                                       @RequestParam String receiverAddress,
                                       @RequestParam String specificAddressDetails, // <--- Catch it
                                       @RequestParam String zone) {
        Order order = orderRepository.findById(id).orElseThrow();

        order.setSellerTrackingNumber(sellerBarcode);
        order.setReceiverAddress(receiverAddress);
        order.setSpecificAddressDetails(specificAddressDetails); // <--- Save it
        order.setDeliveryZone(zone);
        order.setTrackingId("YE-" + (10000 + new java.util.Random().nextInt(90000)));
        order.setStatus("AWAITING_RIDER");

        orderRepository.save(order);
        return "redirect:/admin-portal";
    }
    // Show the loading page
    @GetMapping("/rider/load/{riderId}")
    public String showLoadPage(@PathVariable Long riderId, Model model) {
        User rider = userRepository.findById(riderId).orElseThrow();
        model.addAttribute("rider", rider);
        return "rider-load";
    }

    // Process the scan
    @PostMapping("/rider/load-parcel/{riderId}")
    public String loadParcel(@PathVariable Long riderId, @RequestParam String trackingId, Model model) {
        User rider = userRepository.findById(riderId).orElseThrow();

        // Find the order by tracking ID
        Order order = orderRepository.findAll().stream()
                .filter(o -> trackingId.equals(o.getTrackingId()))
                .findFirst()
                .orElse(null);

        if (order != null && order.getRider() != null && order.getRider().getId().equals(riderId)) {
            order.setStatus("PICKED_UP"); // Rider has it in the bag
            orderRepository.save(order);
            model.addAttribute("message", "✅ Parcel " + trackingId + " loaded into bag!");
        } else {
            model.addAttribute("message", "❌ Error: Parcel not found or not assigned to you.");
        }

        model.addAttribute("rider", rider);
        return "rider-load";
    }

}