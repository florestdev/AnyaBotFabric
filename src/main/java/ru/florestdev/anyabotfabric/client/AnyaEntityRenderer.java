package ru.florestdev.anyabotfabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.util.Identifier;
import ru.florestdev.anyabotfabric.Anyabotfabric;
import ru.florestdev.anyabotfabric.AnyaEntity;

@Environment(EnvType.CLIENT)
public class AnyaEntityRenderer implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(Anyabotfabric.ANYA, AnyaRenderer::new);
    }

    @Environment(EnvType.CLIENT)
    public static class AnyaRenderState extends BipedEntityRenderState {
        // Здесь больше не нужны SkinTextures, так как мы берем локальный файл
    }

    @Environment(EnvType.CLIENT)
    public static class AnyaRenderer extends BipedEntityRenderer<AnyaEntity, AnyaRenderState, BipedEntityModel<AnyaRenderState>> {

        // Путь к твоему скину в ресурсах: assets/anyabotfabric/textures/entity/anya.png
        private static final Identifier ANYA_SKIN = Identifier.of("anyabotfabric", "textures/entity/anya.png");

        public AnyaRenderer(EntityRendererFactory.Context ctx) {
            // Используем PLAYER_SLIM для женских скинов (тонкие руки)
            super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_SLIM)), 0.5f);
        }

        @Override
        public AnyaRenderState createRenderState() {
            return new AnyaRenderState();
        }

        @Override
        public Identifier getTexture(AnyaRenderState state) {
            return ANYA_SKIN;
        }

        @Override
        public void updateRenderState(AnyaEntity entity, AnyaRenderState state, float tickDelta) {
            super.updateRenderState(entity, state, tickDelta);
            // Тут больше ничего делать не нужно, текстура статична
        }
    }
}