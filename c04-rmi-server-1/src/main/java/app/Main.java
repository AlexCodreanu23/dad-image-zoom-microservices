package app;

import rmi.ZoomService;
import rmi.ZoomServiceImpl;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("RMI_PORT", "1099"));
        String name = System.getenv().getOrDefault("RMI_NAME", "ZoomService");

        Registry reg = LocateRegistry.createRegistry(port);
        ZoomService svc = new ZoomServiceImpl();
        reg.rebind(name, svc);

        System.out.println("C04 RMI Server started on port " + port + " name=" + name);
        Thread.sleep(Long.MAX_VALUE);
    }
}
