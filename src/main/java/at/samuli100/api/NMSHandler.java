package at.samuli100.api;

import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;

/**
 * The class that handles some NMS/Reflection methods.
 * @author CroaBeast
 * @since 1.0
 */
public abstract class NMSHandler {

    /**
     * Server version. Example: 1.16.5
     */
    private static final String VERSION = Bukkit.getBukkitVersion().split("-")[0];
    /**
     * Server major version. Example: if {@link #VERSION} is 1.16.5, the result will be 16.
     */
    public static final int MAJOR_VERSION = Integer.parseInt(VERSION.split("\\.")[1]);

    /**
     * Get a class from the NMS package
     * @param pack Package name
     * @param name Class name
     * @param useVs Whether to use the version string
     * @return The class, or null if not found
     */
    protected Class<?> getNMSClass(String pack, String name, boolean useVs) {
        Package aPackage = Bukkit.getServer().getClass().getPackage();

        String version = aPackage.getName().split("\\.")[3];

        // FIXED: Update to handle 1.21+ versions
        // For Minecraft 1.17+, NMS classes are in different packages
        if (MAJOR_VERSION >= 17) {
            if (pack != null && pack.equals("net.minecraft.server")) {
                // For 1.17+, most classes moved to just "net.minecraft"
                pack = "net.minecraft";
            }
        } else {
            pack = pack != null ? pack : "net.minecraft.server";
        }

        try {
            return Class.forName(pack + (useVs ? "." + version : "") + "." + name);
        } catch (Exception e) {
            // Fallback for newer versions (1.17+)
            if (pack.equals("net.minecraft") && MAJOR_VERSION >= 17) {
                // Try the fully qualified name if it's a net.minecraft class
                try {
                    return Class.forName("net.minecraft." + name);
                } catch (Exception ex) {
                    return null;
                }
            }
            return null;
        }
    }

    protected Class<?> getBukkitClass(String name) {
        return getNMSClass("org.bukkit.craftbukkit", name, true);
    }

    protected Object getObject(Class<?> clazz, Object initial, String method) {
        try {
            clazz = clazz != null ? clazz : initial.getClass();
            return clazz.getDeclaredMethod(method).invoke(initial);
        } catch (Exception e) {
            return null;
        }
    }

    protected Object getObject(Object initial, String method) {
        return getObject(null, initial, method);
    }

    protected Object getBukkitItem(Object nmsItem) {
        Class<?> clazz = getBukkitClass("inventory.CraftItemStack");
        if (clazz == null) return null;

        Constructor<?> ct;
        try {
            ct = clazz.getDeclaredConstructor(nmsItem.getClass());
        } catch (NoSuchMethodException e) {
            return null;
        }

        ct.setAccessible(true);
        try {
            return ct.newInstance(nmsItem);
        } catch (Exception e) {
            return null;
        }
    }

    protected String checkValue(Object value, String def) {
        return value == null ? def : value.toString();
    }

    protected String checkValue(Object value) {
        return checkValue(value, null);
    }
}