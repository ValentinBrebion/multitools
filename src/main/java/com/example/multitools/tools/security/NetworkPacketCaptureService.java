package com.example.multitools.tools.security;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class NetworkPacketCaptureService {

    private static final int SNAPSHOT_LENGTH = 65_535;
    private static final int READ_TIMEOUT_MILLIS = 250;
    private static final String WINDOWS_WIFI_ADAPTERS_SCRIPT = """
        $ErrorActionPreference = 'Stop'
        Get-NetAdapter -Physical |
          Where-Object { $_.Status -eq 'Up' -and [int]$_.NdisPhysicalMedium -eq 9 } |
          ForEach-Object {
            $interfaceGuid = [string]$_.InterfaceGuid
            if ($null -ne $interfaceGuid) {
              $interfaceGuid -replace '[{}-]', ''
            }
          }
        """;

    private final Object handleLock = new Object();
    private volatile PcapHandle activeHandle;
    private volatile boolean stopRequested;
    private volatile List<PcapNetworkInterface> availableWifiDevices = List.of();

    public List<CaptureInterface> findInterfaces() throws PcapNativeException {
        List<PcapNetworkInterface> devices = discoverWifiDevices();
        availableWifiDevices = devices;
        return devices.stream()
            .map(NetworkPacketCaptureService::toCaptureInterface)
            .toList();
    }

    private List<PcapNetworkInterface> discoverWifiDevices() throws PcapNativeException {
        List<PcapNetworkInterface> devices = Pcaps.findAllDevs();
        if (isWindows()) {
            Set<String> wifiAdapterIds = findActiveWindowsWifiAdapterIds();
            return devices.stream()
                .filter(device -> device.isUp() && !device.isLoopBack())
                .filter(device -> hasMatchingWindowsWifiId(device, wifiAdapterIds))
                .toList();
        }

        return devices.stream()
            .filter(device -> device.isUp() && !device.isLoopBack())
            .filter(NetworkPacketCaptureService::looksLikeWifiInterface)
            .toList();
    }

    private Set<String> findActiveWindowsWifiAdapterIds() {
        Process process = null;
        try {
            process = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                WINDOWS_WIFI_ADAPTERS_SCRIPT
            ).redirectErrorStream(true).start();
            if (!process.waitFor(6, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("La détection des interfaces Wi-Fi a dépassé le délai prévu.");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IllegalStateException(
                    "Impossible de détecter les interfaces Wi-Fi avec Windows : " + output.strip()
                );
            }
            return output.lines()
                .map(String::strip)
                .filter(line -> line.matches("(?i)[0-9a-f]{32}"))
                .map(line -> line.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lancer la détection des interfaces Wi-Fi Windows.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La détection des interfaces Wi-Fi a été interrompue.", e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private static boolean hasMatchingWindowsWifiId(
        PcapNetworkInterface device,
        Set<String> wifiAdapterIds
    ) {
        String deviceId = device.getName().replaceAll("[^A-Fa-f0-9]", "").toLowerCase(Locale.ROOT);
        return wifiAdapterIds.stream().anyMatch(deviceId::contains);
    }

    private static boolean looksLikeWifiInterface(PcapNetworkInterface device) {
        String name = device.getName() == null ? "" : device.getName().toLowerCase(Locale.ROOT);
        String description = device.getDescription() == null
            ? ""
            : device.getDescription().toLowerCase(Locale.ROOT);
        return name.matches(".*(^|[^a-z0-9])(wi-?fi|wireless|wlan)([^a-z0-9]|$).*")
            || description.matches(".*(^|[^a-z0-9])(wi-?fi|wireless|wlan)([^a-z0-9]|$).*")
            || name.matches(".*(^|[^a-z0-9])wl[a-z0-9_.-]*");
    }

    private static CaptureInterface toCaptureInterface(PcapNetworkInterface device) {
        String description = device.getDescription();
        return new CaptureInterface(
            device.getName(),
            description == null || description.isBlank() ? device.getName() : description
        );
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public void capture(String interfaceName, Consumer<PacketInfo> packetConsumer)
        throws PcapNativeException, NotOpenException, InterruptedException {
        stopRequested = false;
        PcapNetworkInterface device = availableWifiDevices.stream()
            .filter(candidate -> candidate.getName().equals(interfaceName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "L'interface Wi-Fi sélectionnée n'est plus dans la liste détectée. Actualisez la liste avant de démarrer la capture."
            ));

        PcapHandle handle = device.openLive(
            SNAPSHOT_LENGTH,
            PromiscuousMode.NONPROMISCUOUS,
            READ_TIMEOUT_MILLIS
        );
        try {
            synchronized (handleLock) {
                activeHandle = handle;
                if (stopRequested) {
                    handle.breakLoop();
                }
            }
            try {
                handle.loop(-1, (PacketListener) packet -> packetConsumer.accept(toPacketInfo(packet)));
            } catch (InterruptedException e) {
                if (!stopRequested) {
                    throw e;
                }
            }
        } finally {
            synchronized (handleLock) {
                activeHandle = null;
                handle.close();
            }
        }
    }

    public void stopCapture() throws NotOpenException {
        stopRequested = true;
        synchronized (handleLock) {
            PcapHandle handle = activeHandle;
            if (handle != null) {
                handle.breakLoop();
            }
        }
    }

    private PacketInfo toPacketInfo(Packet packet) {
        String source = "—";
        String destination = "—";
        String protocol = "Autre";

        IpV4Packet ipv4 = packet.get(IpV4Packet.class);
        IpV6Packet ipv6 = packet.get(IpV6Packet.class);
        if (ipv4 != null) {
            source = ipv4.getHeader().getSrcAddr().getHostAddress();
            destination = ipv4.getHeader().getDstAddr().getHostAddress();
            protocol = transportProtocol(packet, "IPv4");
        } else if (ipv6 != null) {
            source = ipv6.getHeader().getSrcAddr().getHostAddress();
            destination = ipv6.getHeader().getDstAddr().getHostAddress();
            protocol = transportProtocol(packet, "IPv6");
        }

        TcpPacket tcp = packet.get(TcpPacket.class);
        if (tcp != null) {
            source += ":" + tcp.getHeader().getSrcPort().valueAsInt();
            destination += ":" + tcp.getHeader().getDstPort().valueAsInt();
        } else {
            UdpPacket udp = packet.get(UdpPacket.class);
            if (udp != null) {
                source += ":" + udp.getHeader().getSrcPort().valueAsInt();
                destination += ":" + udp.getHeader().getDstPort().valueAsInt();
            }
        }

        return new PacketInfo(Instant.now(), protocol, source, destination, packet.length());
    }

    private String transportProtocol(Packet packet, String fallback) {
        if (packet.get(TcpPacket.class) != null) {
            return "TCP";
        }
        if (packet.get(UdpPacket.class) != null) {
            return "UDP";
        }
        return fallback;
    }

    public record CaptureInterface(String name, String description) {
        @Override
        public String toString() {
            return description;
        }
    }

    public record PacketInfo(Instant capturedAt, String protocol, String source, String destination, int length) {
    }
}
