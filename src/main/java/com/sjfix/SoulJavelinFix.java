package com.sjfix;

import com.sjfix.client.FixedSoulJavelinRenderer;
import com.sjfix.entity.SoulJavelinFixMod;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(SoulJavelinFix.MODID)
public class SoulJavelinFix {
    public static final String MODID = "souljavelfix";
    public static final Logger LOGGER = LogManager.getLogger("SoulJavelFix");

    public SoulJavelinFix() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        SoulJavelinFixMod.ENTITIES.register(bus);
        bus.addListener(this::onRegisterRenderers);
        LOGGER.info("SoulJavelinFix loaded: original SoulJavelin will be replaced with optimized version.");
    }

    @SubscribeEvent
    public void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
            SoulJavelinFixMod.FIXED_SOUL_JAVELIN.get(),
            FixedSoulJavelinRenderer::new
        );
    }
}
