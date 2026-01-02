package ru.florestdev.anyabotfabric;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

public class MashaEntity extends PathAwareEntity {

    private static final UUID ANYA_UUID = UUID.fromString("a11930ba-eab7-453d-abf8-f1f352e32fde");
    private static final String ANYA_TEXTURE_VALUE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTJkODQ3ZTNmZmNhNzI4Yjk3ZjU1ZjM0ZjM3YzE3MjYyZGQzNDZjNzQ4MWU4YmM3NTU2ZTZiZTg3MTFmODRjYmMiLCJtZXRhZGF0YSI6eyJtb2RlbCI6InNsaW0ifX19fQ==";
    private static final String ANYA_TEXTURE_SIGNATURE = "DXjEXA1Ohn1xCYiGZ7d+lfuhbw1U6xCrvW+g/pDudd4d01Y7gLse8oeI7JiLUg+fBrCTxFir8BkAvcO609BNBBa0gbrwtVom0hdXgYa1Kts4dZtPQyauiqZRMllEayPTThPwWWVvbHOADt9giNmWlhXx8P8tleDL/XMVNzDDxjsHG52oO3QS/ZSe7zVfbMo532coHZwhxey93Q74EXEIoafIqwflcyrMNzf0ZelvW32mAdbiEvECUj8tgky/H/4n4mB2JIB2byJlGfhlj/SAIJ3bBP05kv2qcxzvDnnCROXjWIamfpDBQn9d/BahG3zD6pVkNzwmuXgYaZYnJBRYLI4rOqty3T9uU1XTinzzzLiXPyaIE4sKgPfq5nxDC0ev81XjK9L/1ygpixLfDDDq+32T9pjvfnKebf2uh5hP6oSMuhIyo75yq3sSwLFhplOzr3Qm3HZMZfx9QRnlzr/xPAyByhJN02MJrXzuN66qdmGhCjry+MvVibEuKmqdsylmB1uNJF81R5cQztA/IR+PHCSUuBUg6J3iXq0NlfCRBih2Wc92xjTBuz3IyQ8nKIK1kYjTWjq8wjxTwZGgdBdTPdJ4kSYsiCaHnWP0CKyYBpAg3Qx9lDiyGVLpVCQq2NjN1R46kyuliHXoudNg2AwehN14yEdAFa03+WyebbrAtpA=";

    boolean warned = false;

    private final GameProfile profile;

    protected MashaEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        setInvulnerable(true);
        profile = new GameProfile(ANYA_UUID, "KiraChan");
        profile.getProperties().put("textures", new Property("textures", ANYA_TEXTURE_VALUE, ANYA_TEXTURE_SIGNATURE));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 20)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 8f));
    }

    @Override
    public void tick() {
        super.tick();

        if (!getWorld().isClient) {
            // 1. Следуем за игроком (существующий код)
            if (Anyabotfabric.isFollowPlayerEnabled()) {
                PlayerEntity p = null;
                for (var entry : Anyabotfabric.playerToMasha.entrySet()) {
                    if (this == entry.getValue()) {
                        p = getWorld().getPlayerByUuid(entry.getKey());
                        break;
                    }
                }

                if (p != null && squaredDistanceTo(p) > 9) { // > 3 блоков
                    getNavigation().startMovingTo(p, 1.0);
                }

                // 2. Проверяем: игрок на кровати?
                if (p != null) {
                    BlockPos playerPos = p.getBlockPos();
                    if (getWorld().getBlockState(playerPos).getBlock() instanceof BedBlock) {
                        // Идём к кровати
                        getNavigation().startMovingTo(playerPos.getX(), playerPos.getY(), playerPos.getZ(), 1.0);

                        // Если достигли цели — ложимся
                        if (getBlockPos().isWithinDistance(playerPos, 1.0)) {
                            sleep(playerPos);
                        }
                    }
                }

                if (getWorld().getTimeOfDay() >= 13000 && getWorld().getTimeOfDay() <= 23000) {
                    if (isSleeping()) {
                        wakeUp();
                        warned = false;
                    } else {
                        if (!warned) {
                            if (p != null ) {
                                p.sendMessage(Text.of("<Masha> %s, я хочу спать! :(".formatted(p.getName().getString())), false);
                            }
                            warned = true;
                        }
                    }
                } else {
                    if (warned) {
                        warned = false;
                    }
                }

            }
        }
    }


    public Optional<GameProfile> getGameProfile() {
        return Optional.of(profile);
    }

    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }
}
