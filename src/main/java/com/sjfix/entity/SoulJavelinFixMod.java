package com.sjfix.entity;

import com.sjfix.SoulJavelinFix;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SoulJavelinFixMod {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SoulJavelinFix.MODID);

    public static final RegistryObject<EntityType<FixedSoulJavelinEntity>> FIXED_SOUL_JAVELIN =
        ENTITIES.register("fixed_soul_javelin",
            () -> EntityType.Builder.<FixedSoulJavelinEntity>of(FixedSoulJavelinEntity::new, MobCategory.MISC)
                .sized(0.5F, 0.5F)
                .clientTrackingRange(4)
                .updateInterval(20)
                .build(SoulJavelinFix.MODID + ":fixed_soul_javelin")
        );
}
