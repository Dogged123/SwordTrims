package dogged.swordtrims;

import dogged.swordtrims.listeners.SmithingTableListener;
import dogged.swordtrims.listeners.SwordListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class SwordTrims extends JavaPlugin {

    @Override
    public void onEnable() {
        new SwordListener(this);
        new SmithingTableListener(this);

        saveDefaultConfig();
    }
}
