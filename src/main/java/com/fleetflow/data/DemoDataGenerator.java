package com.fleetflow.data;

import com.fleetflow.model.Driver;
import com.fleetflow.model.Priority;
import com.fleetflow.model.Shipment;
import com.fleetflow.model.ShipmentDocument;
import com.fleetflow.model.ShipmentEvent;
import com.fleetflow.model.ShipmentStatus;
import com.fleetflow.model.Vehicle;
import com.fleetflow.model.VehicleStatus;
import com.fleetflow.model.Warehouse;
import com.fleetflow.model.WarehouseStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Generates a realistic, fully consistent logistics data set from a seed so that the
 * application is populated immediately after start-up.
 */
public final class DemoDataGenerator {

    private static final String[] CUSTOMERS = {"Aarav Traders", "Meera Pharma", "Kiran Electronics",
            "Sunrise Textiles", "Patel Auto Parts", "Lotus Books", "Zenith Appliances", "Nandini Foods",
            "Orion Tools", "Vertex Legal", "Apex Medical", "Bharat Stationers"};

    private static final String[] DESCRIPTIONS = {
            "Fragile electronics - handle with care - deliver before noon",
            "Pharmaceutical cold chain package - keep between 2C and 8C",
            "Bulk textile order - 12 cartons - standard delivery",
            "Automobile spare parts - urgent replacement order",
            "Books and stationery - schools consignment",
            "Perishable food items - express delivery",
            "Home appliances - installation required - signature needed",
            "Industrial tools - heavy load - forklift required",
            "Documents and legal papers - signature required",
            "Medical equipment - fragile - priority handling"};

    /** Delivery addresses; the same address recurs (and one has typos) so that repeat detection has data. */
    private static final String[][] ADDRESSES = {
            {"42 MG Road, Indiranagar, Bengaluru 560038", "Bengaluru"},
            {"17 Linking Road, Bandra West, Mumbai 400050", "Mumbai"},
            {"221 Connaught Place, New Delhi 110001", "Delhi"},
            {"8 Banjara Hills Road No 12, Hyderabad 500034", "Hyderabad"},
            {"56 Anna Salai, Teynampet, Chennai 600018", "Chennai"},
            {"12 FC Road, Shivajinagar, Pune 411005", "Pune"},
            {"90 Park Street, Kolkata 700016", "Kolkata"},
            {"5 CG Road, Navrangpura, Ahmedabad 380009", "Ahmedabad"},
            {"301 Andheri Kurla Road, Andheri East, Mumbai 400059", "Mumbai"},
            {"77 Koramangala 5th Block, Bengaluru 560095", "Bengaluru"},
            {"14 Salt Lake Sector V, Kolkata 700091", "Kolkata"},
            {"63 Jubilee Hills Check Post, Hyderabad 500033", "Hyderabad"}};

    private static final String[] DRIVER_NAMES = {"Ravi Kumar", "Sanjay Patel", "Anita Sharma", "Mohammed Irfan",
            "Deepak Nair", "Priya Menon", "Vikram Singh", "Lakshmi Iyer", "Arjun Reddy", "Fatima Khan"};

    private static final String[] VEHICLE_TYPES = {"Van", "Truck", "Mini Truck", "Container Truck", "Van",
            "Mini Truck", "Truck", "Van", "Bike Courier", "Mini Truck"};
    private static final int[] VEHICLE_CAPACITY = {1200, 8000, 2500, 15000, 1200, 2500, 8000, 1200, 40, 2500};
    private static final VehicleStatus[] VEHICLE_STATUS = {
            VehicleStatus.IN_TRANSIT, VehicleStatus.IN_TRANSIT, VehicleStatus.IN_TRANSIT,
            VehicleStatus.IN_TRANSIT, VehicleStatus.IN_TRANSIT, VehicleStatus.LOADING,
            VehicleStatus.AVAILABLE, VehicleStatus.AVAILABLE, VehicleStatus.AVAILABLE,
            VehicleStatus.MAINTENANCE};
    private static final String[] CITIES = {"Mumbai", "Delhi", "Bengaluru", "Hyderabad", "Chennai", "Pune",
            "Kolkata", "Ahmedabad"};

    private static final String LETTERS = "ABCDEFGHJKMNPQRSTUVWXYZ";

    private DemoDataGenerator() {
    }

    /** Builds a deterministic data set for the given seed. */
    public static FleetData generate(long seed) {
        Random rnd = new Random(seed);
        List<Warehouse> warehouses = warehouses();
        List<Vehicle> vehicles = vehicles(rnd);
        List<Shipment> shipments = shipments(rnd, warehouses, vehicles);
        List<Driver> drivers = drivers(rnd, vehicles, shipments);
        List<ShipmentEvent> events = events(rnd, shipments);
        List<ShipmentDocument> documents = documents(shipments);
        return new FleetData(shipments, vehicles, drivers, warehouses, events, documents);
    }

    private static List<Warehouse> warehouses() {
        List<Warehouse> list = new ArrayList<>();
        list.add(new Warehouse("WH-01", "Mumbai Central Hub", "Mumbai, MH", 5000, 4300, WarehouseStatus.CONGESTED, 0.22, 0.60));
        list.add(new Warehouse("WH-02", "Delhi North Distribution Center", "Delhi, DL", 4000, 2600, WarehouseStatus.OPERATIONAL, 0.40, 0.14));
        list.add(new Warehouse("WH-03", "Bengaluru Tech Park Depot", "Bengaluru, KA", 3500, 2900, WarehouseStatus.OPERATIONAL, 0.40, 0.86));
        list.add(new Warehouse("WH-04", "Hyderabad Logistics Park", "Hyderabad, TS", 3000, 1500, WarehouseStatus.OPERATIONAL, 0.55, 0.58));
        list.add(new Warehouse("WH-05", "Chennai Port Warehouse", "Chennai, TN", 4500, 4100, WarehouseStatus.CONGESTED, 0.72, 0.84));
        return list;
    }

    private static List<Vehicle> vehicles(Random rnd) {
        List<Vehicle> list = new ArrayList<>();
        for (int i = 0; i < VEHICLE_TYPES.length; i++) {
            VehicleStatus status = VEHICLE_STATUS[i];
            boolean crewed = status == VehicleStatus.IN_TRANSIT || status == VehicleStatus.LOADING;
            String driverId = crewed ? String.format("DRV-%02d", i + 1) : "";
            list.add(new Vehicle(String.format("VEH-%02d", i + 1), VEHICLE_TYPES[i], VEHICLE_CAPACITY[i],
                    CITIES[rnd.nextInt(CITIES.length)], status, driverId, 20 + rnd.nextInt(81)));
        }
        return list;
    }

    private static List<Shipment> shipments(Random rnd, List<Warehouse> warehouses, List<Vehicle> vehicles) {
        List<Vehicle> onRoad = vehicles.stream().filter(v -> v.status() == VehicleStatus.IN_TRANSIT).toList();
        List<Shipment> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 24; i++) {
            Warehouse origin = warehouses.get(rnd.nextInt(warehouses.size()));
            String[] address = ADDRESSES[i < 12 ? i : rnd.nextInt(ADDRESSES.length)];
            ShipmentStatus status = pickStatus(rnd);
            String driverId = "";
            String vehicleId = "";
            if (status == ShipmentStatus.IN_TRANSIT || status == ShipmentStatus.DELAYED) {
                Vehicle v = onRoad.get(rnd.nextInt(onRoad.size()));
                vehicleId = v.id();
                driverId = v.driverId();
            } else if (status == ShipmentStatus.DELIVERED) {
                driverId = String.format("DRV-%02d", 1 + rnd.nextInt(DRIVER_NAMES.length));
                vehicleId = String.format("VEH-%02d", 1 + rnd.nextInt(VEHICLE_TYPES.length));
            }
            String code = "PKG-" + randomLetters(rnd, 3) + "-" + (1000 + rnd.nextInt(9000));
            double weight = Math.round((3 + rnd.nextDouble() * 290) * 10) / 10.0;
            list.add(new Shipment(String.format("SHP-%d", 2001 + i), code, CUSTOMERS[rnd.nextInt(CUSTOMERS.length)],
                    origin.location().split(",")[0], address[1], address[0],
                    DESCRIPTIONS[rnd.nextInt(DESCRIPTIONS.length)], status, pickPriority(rnd), weight,
                    driverId, vehicleId, today.minusDays(rnd.nextInt(7))));
        }
        return list;
    }

    private static ShipmentStatus pickStatus(Random rnd) {
        int roll = rnd.nextInt(100);
        if (roll < 28) return ShipmentStatus.PENDING;
        if (roll < 58) return ShipmentStatus.IN_TRANSIT;
        if (roll < 90) return ShipmentStatus.DELIVERED;
        return ShipmentStatus.DELAYED;
    }

    private static Priority pickPriority(Random rnd) {
        int roll = rnd.nextInt(100);
        if (roll < 15) return Priority.LOW;
        if (roll < 65) return Priority.NORMAL;
        if (roll < 90) return Priority.HIGH;
        return Priority.URGENT;
    }

    private static String randomLetters(Random rnd, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.append(LETTERS.charAt(rnd.nextInt(LETTERS.length())));
        return sb.toString();
    }

    private static List<Driver> drivers(Random rnd, List<Vehicle> vehicles, List<Shipment> shipments) {
        List<Driver> list = new ArrayList<>();
        for (int i = 0; i < DRIVER_NAMES.length; i++) {
            String id = String.format("DRV-%02d", i + 1);
            boolean crewed = vehicles.stream().anyMatch(v -> v.driverId().equals(id));
            long active = shipments.stream().filter(s -> s.driverId().equals(id)
                    && (s.status() == ShipmentStatus.IN_TRANSIT || s.status() == ShipmentStatus.DELAYED)).count();
            double rating = Math.round((3.8 + rnd.nextDouble() * 1.1) * 10) / 10.0;
            list.add(new Driver(id, DRIVER_NAMES[i], !crewed, (int) active, rating, 1 + rnd.nextInt(15)));
        }
        return list;
    }

    private static List<ShipmentEvent> events(Random rnd, List<Shipment> shipments) {
        List<ShipmentEvent> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Shipment s : shipments) {
            list.add(new ShipmentEvent(now.minusHours(30 + rnd.nextInt(60)), s.id(), "CREATED",
                    "Order registered for " + s.customer()));
            if (s.status() == ShipmentStatus.PENDING) continue;
            list.add(new ShipmentEvent(now.minusHours(20 + rnd.nextInt(8)), s.id(), "DISPATCHED",
                    "Left " + s.source() + " warehouse on " + s.vehicleId()));
            list.add(new ShipmentEvent(now.minusHours(8 + rnd.nextInt(10)), s.id(), "HUB_SCAN",
                    "Scanned at " + s.source() + " sorting hub"));
            switch (s.status()) {
                case DELIVERED -> list.add(new ShipmentEvent(now.minusHours(1 + rnd.nextInt(6)), s.id(),
                        "DELIVERED", "Delivered to " + s.destination() + " and signed for"));
                case DELAYED -> list.add(new ShipmentEvent(now.minusHours(1 + rnd.nextInt(5)), s.id(),
                        "DELAYED", "Delayed near " + s.destination() + " - traffic congestion"));
                default -> list.add(new ShipmentEvent(now.minusHours(1 + rnd.nextInt(5)), s.id(),
                        "OUT_FOR_DELIVERY", "Out for delivery in " + s.destination()));
            }
        }
        list.sort(Comparator.comparing(ShipmentEvent::time).reversed());
        return list;
    }

    private static List<ShipmentDocument> documents(List<Shipment> shipments) {
        List<ShipmentDocument> list = new ArrayList<>();
        int n = 1;
        for (Shipment s : shipments.subList(0, 8)) {
            String text = "Delivery note for " + s.id() + " (" + s.packageCode() + "). Consignee: " + s.customer()
                    + ". Deliver to " + s.address() + ". Contents: " + s.description() + ". Weight "
                    + s.weightKg() + " kg.";
            list.add(new ShipmentDocument(String.format("DOC-%03d", n++), s.id(), "Delivery Note " + s.id(), text));
        }
        return list;
    }
}
