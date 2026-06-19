package com.sjfix.client;

import com.sjfix.entity.FixedSoulJavelinEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class FixedSoulJavelinRenderer extends ArrowRenderer<FixedSoulJavelinEntity> {

    private static final ResourceLocation TRIDENT_TEXTURE =
        new ResourceLocation("minecraft", "textures/entity/trident.png");

    public FixedSoulJavelinRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(FixedSoulJavelinEntity entity) {
        return TRIDENT_TEXTURE;
    }
}
