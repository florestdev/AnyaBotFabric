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
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import ru.florestdev.anyabotfabric.Anyabotfabric;

@Environment(EnvType.CLIENT)
public class AnyaEntityRenderer implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Регистрация рендерера для Ани
        EntityRendererRegistry.register(Anyabotfabric.ANYA, ctx ->
                new BotRenderer(ctx, Identifier.of("anyabotfabric", "textures/entity/anya.png")));

        // Регистрация рендерера для Киры
        EntityRendererRegistry.register(Anyabotfabric.KIRA, ctx ->
                new BotRenderer(ctx, Identifier.of("anyabotfabric", "textures/entity/kira.png")));

        // Регистрация рендерера для Маши
        EntityRendererRegistry.register(Anyabotfabric.MASHA, ctx ->
                new BotRenderer(ctx, Identifier.of("anyabotfabric", "textures/entity/masha.png")));
    }

    @Environment(EnvType.CLIENT)
    public static class BotRenderState extends BipedEntityRenderState {
        // Состояние рендера
    }

    // Заменили LivingEntity на MobEntity, чтобы пройти проверку типов (bound)
    @Environment(EnvType.CLIENT)
    public static class BotRenderer extends BipedEntityRenderer<MobEntity, BotRenderState, BipedEntityModel<BotRenderState>> {

        private final Identifier texture;

        public BotRenderer(EntityRendererFactory.Context ctx, Identifier texture) {
            // Используем PLAYER_SLIM для женских моделей
            super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_SLIM)), 0.5f);
            this.texture = texture;
        }

        @Override
        public BotRenderState createRenderState() {
            return new BotRenderState();
        }

        @Override
        public Identifier getTexture(BotRenderState state) {
            return this.texture;
        }

        @Override
        public void updateRenderState(MobEntity entity, BotRenderState state, float tickDelta) {
            super.updateRenderState(entity, state, tickDelta);
        }
    }
}