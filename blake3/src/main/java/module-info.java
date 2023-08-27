module cz.aprar.oss.blake3 {
    exports cz.aprar.oss.blake3;
    exports cz.aprar.oss.blake3.jca;
    provides java.security.Provider with cz.aprar.oss.blake3.jca.Blake3Provider;
}
