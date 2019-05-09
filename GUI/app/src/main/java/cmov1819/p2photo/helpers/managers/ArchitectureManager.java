package cmov1819.p2photo.helpers.managers;

import cmov1819.p2photo.helpers.architectures.BaseArchitecture;
import cmov1819.p2photo.helpers.architectures.cloudBackedArchitecture.CloudBackedArchitecture;
import cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture.WirelessP2PArchitecture;

public class ArchitectureManager {

    public static BaseArchitecture systemArchitecture;

    public static void setCloudBackedArch() {
        systemArchitecture = new CloudBackedArchitecture();
    }

    public static void setWirelessP2PArch() {
        systemArchitecture = new WirelessP2PArchitecture();
    }
}
