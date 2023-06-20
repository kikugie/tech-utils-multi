package dev.kikugie.techutils;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TechUtilsMod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(Reference.MOD_ID);
    @Override
    public void onInitializeClient() {
        Reference.init();
    }
}
