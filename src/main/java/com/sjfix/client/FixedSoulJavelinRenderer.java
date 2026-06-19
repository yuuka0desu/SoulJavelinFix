package com.sjfix.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sjfix.entity.FixedSoulJavelinEntity;
import net.miauczel.legendary_monsters.entity.ProjectileEntityRenderer.SoulJavelinModel;
import net.miauczel.legendary_monsters.entity.client.ModModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Renderer for FixedSoulJavelinEntity.
 * Identical appearance to the original SoulJavelinEntity: same 3D model
 * (SoulJavelinModel), same texture, same blend/opacity settings.
 */
public class FixedSoulJavelinRenderer extends EntityRenderer<FixedSoulJavelinEntity> {

    private static final ResourceLocation TEXTURE =
        SoulJavelinModel.TEXTURE; // "legendary_monsters:textures/entity/resurrected_knight/javelin.png"

    private final SoulJavelinModel model;

    public FixedSoulJavelinRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new SoulJavelinModel(
            context.bakeLayer(ModModelLayers.SOUL_JAVELIN_LAYER)
        );
    }

    @Override
    public void render(FixedSoulJavelinEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(
            Mth.lerp(partialTicks, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.XN.rotationDegrees(
            Mth.lerp(partialTicks, -entity.xRotO, -entity.getXRot())));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        VertexConsumer vertexConsumer = ItemRenderer.getFoilBufferDirect(
            buffer,
            this.model.renderType(this.getTextureLocation(entity)),
            false,
            entity.isFoil()
        );
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight,
            OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.5F);
        RenderSystem.disableBlend();
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FixedSoulJavelinEntity entity) {
        return TEXTURE;
    }
}
