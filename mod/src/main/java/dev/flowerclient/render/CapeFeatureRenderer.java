package dev.flowerclient.render;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Draws a Flower cape on players who own one. Kinematic sway (not full vanilla cape physics). */
public class CapeFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    public CapeFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) {
        super(ctx);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       AbstractClientPlayerEntity entity, float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {
        if (entity.isInvisible()) return;
        Identifier cape = CapeManager.capeFor(entity.getUuid());
        if (cape == null) return;

        matrices.push();
        matrices.translate(0.0F, 0.0F, 0.125F);
        // gentle sway: idle wobble + lean from walking
        float wobble = MathHelper.sin((entity.age + tickDelta) * 0.1F) * 2.0F;
        float walk = Math.min(18.0F, limbDistance * 55.0F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6.0F + wobble + walk));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntitySolid(cape));
        this.getContextModel().renderCape(matrices, vc, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }
}
