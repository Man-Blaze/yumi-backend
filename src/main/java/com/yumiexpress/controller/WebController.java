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

    // ==========================================
    // 1. PUBLIC PAGES
    // ==========================================
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
            user.setCurrentLat(-17.70 - (new java.util.Random().nextDouble() * 0.1));
            user.setCurrentLng(168.25 + (new java.util.Random().nextDouble() * 0.1));
        }

        User savedUser = userRepository.save(user);

        if ("RIDER".equals(role)) {
            return "redirect:/admin-portal";
        } else {
            return "redirect:/customer-dashboard/" + savedUser.getId();
        }
    }

    // ==========================================
    // 2. B2C: CUSTOMER FLOW (Book & Track)
    // ==========================================
    @GetMapping("/customer-dashboard/{customerId}")
    public String showCustomerDashboard(@PathVariable Long customerId, Model model) {
        User customer = userRepository.findById(customerId).orElseThrow();
        List<Order> customerOrders = orderRepository.findAll().stream()
                .filter(o -> o.getCustomer() != null && o.getCustomer().getId().equals(customerId))
                .toList();
        model.addAttribute("customer", customer);
        model.addAttribute("orders", customerOrders);
        return "customer-dashboard";
    }

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
            @RequestParam String specificAddressDetails,
            @RequestParam String pickupTime,
            @RequestParam double lat,
            @RequestParam double lng,
            Model model) {

        User customer = userRepository.findById(userId).orElseThrow();

        Order newOrder = new Order();
        newOrder.setCustomer(customer);
        newOrder.setPickupAddress(customer.getFullName());
        newOrder.setExactPickupLocation(exactPickupLocation);
        newOrder.setSpecificAddressDetails(specificAddressDetails);
        newOrder.setPickupTime(pickupTime);
        newOrder.setRecipientPhone(recipientPhone);
        newOrder.setStatus("AWAITING_PICKUP");
        newOrder.setPickupLat(lat);
        newOrder.setPickupLng(lng);

        Order savedOrder = orderRepository.save(newOrder);
        model.addAttribute("order", savedOrder);
        return "success";
    }

    @PostMapping("/rate-order/{id}")
    public String rateOrder(@PathVariable Long id, @RequestParam int rating) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setCustomerRating(rating);
        orderRepository.save(order);
        return "redirect:/customer-dashboard/" + order.getCustomer().getId();
    }

    // ==========================================
    // 3. B2B: WAREHOUSE & SELLER FLOW
    // ==========================================
    @PostMapping("/warehouse/bulk-intake")
    public String receiveBulkParcels(@RequestParam String sellerName, @RequestParam int count) {
        for (int i = 0; i < count; i++) {
            Order order = new Order();
            order.setPickupAddress(sellerName);
            order.setExactPickupLocation("YumiExpress Warehouse, Tagabe");
            order.setStatus("AWAITING_WEIGH_IN");
            order.setPickupLat(-17.73);
            order.setPickupLng(168.31);
            orderRepository.save(order);
        }
        System.out.println("📦 Bulk Intake: " + count + " parcels received from " + sellerName);
        return "redirect:/admin-portal";
    }

    // The Wireless Scanner
    @PostMapping("/admin/barcode-scan")
    public String processBarcodeScan(@RequestParam String sellerBarcode) {
        Order order = orderRepository.findAll().stream()
                .filter(o -> "AWAITING_WEIGH_IN".equals(o.getStatus()) && o.getReceiverAddress() == null)
                .findFirst()
                .orElse(null);

        if (order == null) {
            System.out.println("❌ No empty parcels waiting for scan.");
            return "redirect:/admin-portal";
        }

        String[] parts = sellerBarcode.split("\\|");
        if (parts.length >= 5) {
            order.setSellerTrackingNumber(parts[0]);
            order.setRecipientPhone(parts[1]);
            order.setReceiverAddress(parts[2]);
            order.setSpecificAddressDetails(parts[3]);
            order.setDeliveryZone(parts[4]);
        } else {
            order.setSellerTrackingNumber(sellerBarcode);
            order.setRecipientPhone("2341");
            order.setReceiverAddress("Manples Area");
            order.setSpecificAddressDetails("Behind Bon Marche Manples");
            order.setDeliveryZone("CENTRAL");
        }

        order.setStatus("SCANNED");
        orderRepository.save(order);
        System.out.println("✅ Parcel scanned: " + sellerBarcode);
        return "redirect:/warehouse/scan";
    }

    // The Sorting Station
    @GetMapping("/warehouse/scan")
    public String showScanningStation(Model model) {
        Order nextOrder = orderRepository.findAll().stream()
                .filter(o -> "SCANNED".equals(o.getStatus()))
                .findFirst()
                .orElse(null);
        model.addAttribute("order", nextOrder);
        return "warehouse-scan";
    }

    @PostMapping("/warehouse/scan")
    public String processScan(@RequestParam Long orderId, @RequestParam double weight) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setWeightInKg(weight);
        order.setPrice(pricingService.calculatePrice(weight));
        order.setTrackingId("YE-" + (10000 + new java.util.Random().nextInt(90000)));
        order.setStatus("SORTED");
        orderRepository.save(order);
        System.out.println("⚖️ Parcel weighed and sorted: " + order.getTrackingId());
        return "redirect:/warehouse/scan";
    }

    @GetMapping("/seller-login")
    public String showSellerLogin() { return "seller-login"; }

    @GetMapping("/seller-dashboard")
    public String showSellerDashboard(@RequestParam String businessName, @RequestParam String phoneNumber, @RequestParam String email, Model model) {
        System.out.println("🔍 VERIFYING SELLER: " + businessName);
        model.addAttribute("businessName", businessName);
        return "seller-dashboard";
    }
    // 1. Show the scan page for a specific order
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
                                       @RequestParam String specificAddressDetails,
                                       @RequestParam String recipientPhone, // <--- Add this
                                       @RequestParam String zone) {
        Order order = orderRepository.findById(id).orElseThrow();

        order.setSellerTrackingNumber(sellerBarcode);
        order.setReceiverAddress(receiverAddress);
        order.setSpecificAddressDetails(specificAddressDetails);
        order.setRecipientPhone(recipientPhone); // <--- Save it
        order.setDeliveryZone(zone);
        order.setTrackingId("YE-" + (10000 + new java.util.Random().nextInt(90000)));
        order.setStatus("SCANNED"); // Ready for the Sorting Station

        orderRepository.save(order);
        return "redirect:/warehouse/scan"; // Send staff straight to the scale
    }

    // ==========================================
    // 4. ADMIN DASHBOARD & DISPATCH
    // ==========================================
    @GetMapping("/admin-portal")
    public String showAdminDashboard(Model model) {
        List<Order> allOrders = orderRepository.findAll();
        allOrders.sort((a, b) -> b.getId().compareTo(a.getId()));
        model.addAttribute("allOrders", allOrders);

        List<User> riders = userRepository.findAll().stream()
                .filter(u -> "RIDER".equals(u.getRole()))
                .toList();
        model.addAttribute("riders", riders);

        java.util.Map<Long, Double> riderBagWeights = new java.util.HashMap<>();
        java.util.Map<Long, Integer> riderBagCounts = new java.util.HashMap<>();

        for (User rider : riders) {
            // Find all parcels assigned to this rider that aren't delivered yet
            List<Order> activeParcels = allOrders.stream()
                    .filter(o -> o.getRider() != null && o.getRider().getId().equals(rider.getId())
                            && ("ASSIGNED".equals(o.getStatus()) || "PICKED_UP".equals(o.getStatus()))) // <--- ONLY COUNT ACTIVE PARCELS
                    .toList();
            double totalWeight = activeParcels.stream().mapToDouble(Order::getWeightInKg).sum();
            riderBagWeights.put(rider.getId(), totalWeight);
            riderBagCounts.put(rider.getId(), activeParcels.size());
        }

        model.addAttribute("riderBagWeights", riderBagWeights);
        model.addAttribute("riderBagCounts", riderBagCounts);
        return "admin";
    }

    @GetMapping("/admin/assign-rider/{id}")
    public String assignRider(@PathVariable Long id) {
        Order order = orderRepository.findById(id).orElseThrow();
        User rider = dispatchService.assignBestRider(order);

        if (rider != null) {
            // NEW: Check if the rider is a scooter and the parcel is over 15kg
            if ("OWN_SCOOTER".equals(rider.getVehicleType()) && order.getWeightInKg() > 15.0) {
                System.out.println("❌ ERROR: Parcel is too heavy for a scooter! Assign to a Truck.");
                return "redirect:/admin-portal?error=too_heavy";
            }

            order.setRider(rider);
            order.setStatus("ASSIGNED");
            orderRepository.save(order);
        }
        return "redirect:/admin-portal";
    }

    @GetMapping("/admin/riders")
    public String showAdminRiders(Model model) {
        List<User> riders = userRepository.findAll().stream()
                .filter(u -> "RIDER".equals(u.getRole()))
                .toList();
        model.addAttribute("riders", riders);
        return "admin-riders";
    }

    @GetMapping("/admin/customers")
    public String showAdminCustomers(Model model) {
        List<User> customers = userRepository.findAll().stream()
                .filter(u -> "CUSTOMER".equals(u.getRole()))
                .toList();
        model.addAttribute("customers", customers);
        return "admin-customers";
    }

    // ==========================================
    // 5. RIDER FLOW
    // ==========================================
    @GetMapping("/rider-portal/{riderId}")
    public String showRiderDashboard(@PathVariable Long riderId, Model model) {
        User rider = userRepository.findById(riderId).orElseThrow(() -> new RuntimeException("Rider not found"));
        List<Order> tasks = orderRepository.findByRider(rider);
        model.addAttribute("rider", rider);
        model.addAttribute("tasks", tasks);
        return "rider-dashboard";
    }
    @CrossOrigin(origins = "*") // <--- ADD THIS
    @GetMapping("/rider/update-status/{id}")
    public String updateRiderStatus(@PathVariable Long id, @RequestParam String status) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setStatus(status);
        orderRepository.save(order);
        return "redirect:/rider-portal/" + order.getRider().getId();
    }

    @GetMapping("/rider/load/{riderId}")
    public String showLoadPage(@PathVariable Long riderId, Model model) {
        User rider = userRepository.findById(riderId).orElseThrow();
        model.addAttribute("rider", rider);
        return "rider-load";
    }

    @PostMapping("/rider/load-parcel/{riderId}")
    public String loadParcel(@PathVariable Long riderId, @RequestParam String trackingId, Model model) {
        User rider = userRepository.findById(riderId).orElseThrow();
        Order order = orderRepository.findAll().stream()
                .filter(o -> trackingId.equals(o.getTrackingId()))
                .findFirst()
                .orElse(null);

        if (order != null && order.getRider() != null && order.getRider().getId().equals(riderId)) {
            order.setStatus("PICKED_UP");
            orderRepository.save(order);
            model.addAttribute("message", "✅ Parcel " + trackingId + " loaded into bag!");
        } else {
            model.addAttribute("message", "❌ Error: Parcel not found or not assigned to you.");
        }
        model.addAttribute("rider", rider);
        return "rider-load";
    }

    @GetMapping("/rider/weigh-parcel/{id}")
    public String showWeighParcel(@PathVariable Long id, Model model) {
        Order order = orderRepository.findById(id).orElseThrow();
        model.addAttribute("order", order);
        return "weigh-parcel";
    }

    @PostMapping("/rider/weigh-parcel/{id}")
    public String processWeighParcel(@PathVariable Long id, @RequestParam double weight) {
        Order order = orderRepository.findById(id).orElseThrow();

        // 1. Calculate price and generate QR
        order.setWeightInKg(weight);
        order.setPrice(pricingService.calculatePrice(weight));
        order.setTrackingId("YE-" + (10000 + new java.util.Random().nextInt(90000)));

        // 2. Customer pays via M-Vatu (simulated)
        order.setM_vatu_reference("CASH-" + (1000 + new java.util.Random().nextInt(9000)));

        // 3. Mark as PICKED_UP because the rider now has the parcel
        order.setStatus("PICKED_UP");
        orderRepository.save(order);

        System.out.println("⚖️ B2C Parcel Weighed: " + order.getTrackingId() + " | Price: " + order.getPrice() + " VT | Payment: " + order.getM_vatu_reference());

        return "redirect:/receipt/" + order.getId();
    }

    // ==========================================
    // 6. RECEIVER SIGN-OFF & RECEIPT
    // ==========================================
    @GetMapping("/receiver/sign-off/{id}")
    public String showSignOffPage(@PathVariable Long id, Model model) {
        Order order = orderRepository.findById(id).orElseThrow();
        model.addAttribute("order", order);
        return "sign-off";
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/receiver/sign-off/{id}")
    public org.springframework.http.ResponseEntity<String> processSignOff(@PathVariable Long id) {
        Order order = orderRepository.findById(id).orElseThrow();

        // 1. Update order status
        order.setStatus("DELIVERED");
        orderRepository.save(order);

        // 2. Finalize Payment
        paymentService.processPayment(order);

        // 3. Log the event
        System.out.println("\n🚨 FINAL DELIVERY ALERT 🚨");
        System.out.println("Order #" + order.getId() + " (Tracking: " + order.getTrackingId() + ") has been DELIVERED and SIGNED for.");

        // 4. Return a simple success message instead of a redirect
        return org.springframework.http.ResponseEntity.ok("Success");
    }

    @GetMapping("/receipt/{orderId}")
    public String showReceipt(@PathVariable Long orderId, Model model) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        model.addAttribute("order", order);
        return "receipt";
    }

    // ==========================================
    // 7. CUSTOMER OTP REGISTRATION
    // ==========================================
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
    // ==========================================
    // 8. ADMIN CONTROL PANEL (Edit & Cancel)
    // ==========================================

    // 1. Show the Edit Order page
    @GetMapping("/admin/edit-order/{id}")
    public String showEditOrderPage(@PathVariable Long id, Model model) {
        Order order = orderRepository.findById(id).orElseThrow();
        List<User> riders = userRepository.findAll().stream()
                .filter(u -> "RIDER".equals(u.getRole()))
                .toList();
        model.addAttribute("order", order);
        model.addAttribute("riders", riders);
        return "admin-edit-order";
    }

    // 2. Save the edits
    @PostMapping("/admin/edit-order/{id}")
    public String processEditOrder(
            @PathVariable Long id,
            @RequestParam String receiverAddress,
            @RequestParam String specificAddressDetails,
            @RequestParam String recipientPhone,
            @RequestParam(required = false) Long riderId,
            @RequestParam String status) {

        Order order = orderRepository.findById(id).orElseThrow();

        // Update details
        order.setReceiverAddress(receiverAddress);
        order.setSpecificAddressDetails(specificAddressDetails);
        order.setRecipientPhone(recipientPhone);
        order.setStatus(status);

        // Update Rider
        if (riderId != null) {
            User rider = userRepository.findById(riderId).orElse(null);
            order.setRider(rider);
        } else {
            order.setRider(null);
        }

        orderRepository.save(order);
        System.out.println("✏️ Admin edited Order #" + id);
        return "redirect:/admin-portal";
    }

    // 3. Cancel an order
    @GetMapping("/admin/cancel-order/{id}")
    public String cancelOrder(@PathVariable Long id) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setStatus("CANCELLED");
        order.setRider(null); // Unassign rider
        orderRepository.save(order);
        System.out.println("❌ Admin cancelled Order #" + id);
        return "redirect:/admin-portal";
    }
}