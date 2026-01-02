package ru.florestdev.anyabotfabric;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Anyabotfabric implements ModInitializer {

    public static final String MODID = "anyabotfabric";
    private static final File CONFIG_FILE = new File("config/anya_config.json");

    private static JsonObject configData = new JsonObject();
    private final HttpClient http = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    private final List<JsonObject> chatHistory = new ArrayList<>();
    private static final int MAX_MEMORY = 500;

    public static Map<UUID, AnyaEntity> playerToAnya = new HashMap<>();
    public static Map<UUID, KiraEntity> playerToKira = new HashMap<>();
    public static Map<UUID, MashaEntity> playerToMasha = new HashMap<>();

    public static EntityType<AnyaEntity> ANYA;
    public static final RegistryKey<EntityType<?>> ANYA_KEY =
            RegistryKey.of(Registries.ENTITY_TYPE.getKey(), Identifier.of(MODID, "anya"));

    public static EntityType<KiraEntity> KIRA;
    public static final RegistryKey<EntityType<?>> KIRA_KEY =
            RegistryKey.of(Registries.ENTITY_TYPE.getKey(), Identifier.of(MODID, "kira"));

    public static EntityType<MashaEntity> MASHA;
    public static final RegistryKey<EntityType<?>> MASHA_KEY =
            RegistryKey.of(Registries.ENTITY_TYPE.getKey(), Identifier.of(MODID, "masha"));

    public void placeStructure(java.io.File file, net.minecraft.util.math.BlockPos pos, net.minecraft.server.world.ServerWorld world) {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            net.minecraft.nbt.NbtCompound nbt = net.minecraft.nbt.NbtIo.readCompressed(fis, net.minecraft.nbt.NbtSizeTracker.ofUnlimitedBytes());
            net.minecraft.structure.StructureTemplate template = new net.minecraft.structure.StructureTemplate();
            var registryLookup = world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.BLOCK);
            template.readNbt(registryLookup, nbt);
            net.minecraft.structure.StructurePlacementData settings = new net.minecraft.structure.StructurePlacementData().setIgnoreEntities(false);
            world.getServer().execute(() -> {
                template.place(world, pos, pos, settings, world.getRandom(), 2);
                System.out.println("Построила успешно!");
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractSchematicUrl(String text) {
        String[] words = text.split("\\s+");
        for (String word : words) {
            if (word.startsWith("http") && (word.contains(".nbt") || word.contains(".schem"))) {
                return word;
            }
        }
        return null;
    }

    public static void spawnLittleVillager(ServerWorld world, BlockPos pos, String playerName) {
        VillagerEntity villager = EntityType.VILLAGER.create(world, SpawnReason.TRIGGERED);
        if (villager != null) {
            villager.refreshPositionAndAngles(pos, 0, 0);
            villager.setCustomName(Text.literal("%s's Baby".formatted(playerName)));
            villager.setBaby(true);
            world.spawnEntity(villager);
        }
    }

    @Override
    public void onInitialize() {
        loadConfig();

        ANYA = Registry.register(Registries.ENTITY_TYPE, Identifier.of(MODID, "anya"),
                FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, AnyaEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                        .build(ANYA_KEY));
        FabricDefaultAttributeRegistry.register(ANYA, AnyaEntity.createAttributes());

        KIRA = Registry.register(Registries.ENTITY_TYPE, Identifier.of(MODID, "kira"),
                FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, KiraEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                        .build(KIRA_KEY));
        FabricDefaultAttributeRegistry.register(KIRA, KiraEntity.createAttributes());

        MASHA = Registry.register(Registries.ENTITY_TYPE, Identifier.of(MODID, "masha"),
                FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, MashaEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                        .build(MASHA_KEY));
        FabricDefaultAttributeRegistry.register(MASHA, MashaEntity.createAttributes());

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp) {
                if (entity instanceof AnyaEntity) {
                    askAI(entity, "%s ударил тебя!".formatted(player.getName().getString()), "Ай! За что? 😢", sp);
                    playerToAnya.remove(player.getUuid());
                } else if (entity instanceof KiraEntity) {
                    askAI(entity, "%s ударил тебя!".formatted(player.getName().getString()), "Эй! Это было больно!", sp);
                    playerToKira.remove(player.getUuid());
                } else if (entity instanceof MashaEntity) {
                    askAI(entity, "%s ударил тебя!".formatted(player.getName().getString()), "Прекрати сейчас же! 😡", sp);
                    playerToMasha.remove(player.getUuid());
                }
            }
            return ActionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp) {
                if (entity instanceof AnyaEntity anya) {
                    playerToAnya.put(sp.getUuid(), anya);
                    sp.sendMessage(Text.literal("Теперь Anya будет вашей!"), true);
                    return ActionResult.SUCCESS;
                } else if (entity instanceof KiraEntity kira) {
                    playerToKira.put(sp.getUuid(), kira);
                    sp.sendMessage(Text.literal("Теперь Kira будет вашей!"), true);
                    return ActionResult.SUCCESS;
                } else if (entity instanceof MashaEntity masha) {
                    playerToMasha.put(sp.getUuid(), masha);
                    sp.sendMessage(Text.literal("Теперь Masha будет вашей!"), true);
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String playerText = message.getContent().getString();
            String lowText = playerText.toLowerCase();

            if (lowText.contains("аня, приди") || lowText.contains("anya, come on")) spawnIfFirst(sender, ANYA);
            if (lowText.contains("кира, приди") || lowText.contains("kira, come on")) spawnIfFirst(sender, KIRA);
            if (lowText.contains("маша, приди") || lowText.contains("masha, come on")) spawnIfFirst(sender, MASHA);

            Entity bot = sender.getWorld().getEntitiesByClass(Entity.class,
                    sender.getBoundingBox().expand(10.0),
                    e -> e instanceof AnyaEntity || e instanceof KiraEntity || e instanceof MashaEntity).stream().findFirst().orElse(null);

            if (bot != null) {
                if (lowText.contains("давай поиграем") || lowText.contains("go play")) {
                    boolean isMine = (bot instanceof AnyaEntity && playerToAnya.containsKey(sender.getUuid())) ||
                            (bot instanceof KiraEntity && playerToKira.containsKey(sender.getUuid())) ||
                            (bot instanceof MashaEntity && playerToMasha.containsKey(sender.getUuid()));

                    if (!isMine) {
                        sender.sendMessage(Text.of("Это не твоя подруга!"));
                        return;
                    }

                    askAI(bot, "Ты с %s завела ребенка.".formatted(sender.getName().getString()), "Ой!", sender);
                    sender.getWorld().spawnParticles(ParticleTypes.HEART, sender.getX(), sender.getY() + 1.2, sender.getZ(), 10, 0.3, 0.3, 0.3, 0.0);
                    spawnLittleVillager(sender.getWorld(), sender.getBlockPos(), sender.getName().getString());
                }

                if (lowText.startsWith("создай ") || lowText.startsWith("create ")) {
                    String idea = playerText.substring(playerText.indexOf(" ") + 1);
                    askAIStructure(bot, idea, sender);
                    return;
                }

                String foundUrl = extractSchematicUrl(playerText);
                if (foundUrl != null && (lowText.contains("построй") || lowText.contains("build"))) {
                    AnyaSchematicHelper.downloadAndProcess(foundUrl, (file) -> {
                        BlockPos buildPos = sender.getBlockPos().offset(sender.getHorizontalFacing(), 5);
                        this.placeStructure(file, buildPos, (ServerWorld) sender.getWorld());
                    });
                    askAI(bot, sender.getName().getString() + " просит построить это по ссылке: " + foundUrl, "...", sender);
                } else {
                    askAI(bot, sender.getName().getString() + " говорит: " + playerText, "...", sender);
                }
            }
        });
    }

    private void askAI(Entity bot, String prompt, String fallback, ServerPlayerEntity player) {
        processAI(bot, prompt, fallback, false).thenAccept(reply ->
                player.getServer().execute(() -> player.sendMessage(Text.literal("§d<" + bot.getName().getString() + ">§f " + reply), false))
        );
    }

    private void askAIStructure(Entity bot, String idea, ServerPlayerEntity player) {
        processAI(bot, "Создай структуру: " + idea, "", true).thenAccept(snbt -> {
            player.getServer().execute(() -> {
                try {
                    NbtCompound nbt = StringNbtReader.readCompound(snbt);
                    File file = new File("generated_structure.nbt");
                    NbtIo.writeCompressed(nbt, file.toPath());
                    BlockPos pos = player.getBlockPos().offset(player.getHorizontalFacing(), 5);
                    placeStructure(file, pos, (ServerWorld) player.getWorld());
                    player.sendMessage(Text.literal("§d<" + bot.getName().getString() + ">§f Я построила это для тебя 💕"), false);
                } catch (Exception e) {
                    player.sendMessage(Text.literal("§c<" + bot.getName().getString() + ">§f Не смогла построить... 😢"), false);
                }
            });
        });
    }

    private CompletableFuture<String> processAI(Entity bot, String userText, String fallback, boolean isNBT) {
        boolean isOllama = configData.get("is_ollama").getAsBoolean();
        String model = configData.get("model").getAsString();
        String botName = bot.getName().getString().toLowerCase();

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("temperature", configData.get("temperature").getAsDouble());
        if (isOllama) body.addProperty("stream", false);

        JsonArray messages = new JsonArray();
        String sysPrompt;

        if (isNBT) {
            sysPrompt = "Верни ТОЛЬКО SNBT Minecraft 1.21.";
        } else {
            // Поддержка раздельных промптов из конфига
            JsonObject prompts = configData.getAsJsonObject("system_prompt");
            if (prompts.has(botName)) {
                sysPrompt = prompts.get(botName).getAsString();
            } else {
                sysPrompt = "Ты — персонаж Minecraft по имени " + bot.getName().getString();
            }
        }

        messages.add(createMsg("system", sysPrompt));
        for (JsonObject oldMsg : chatHistory) messages.add(oldMsg);
        messages.add(createMsg("user", userText));
        body.add("messages", messages);

        String url = isOllama ? configData.get("ollama_url").getAsString() : configData.get("cloud_url").getAsString();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)));

        if (!isOllama) builder.header("Authorization", "Bearer " + configData.get("api_key").getAsString());

        return http.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    try {
                        JsonObject resObj = gson.fromJson(resp.body(), JsonObject.class);
                        String content = isOllama ? resObj.getAsJsonObject("message").get("content").getAsString() :
                                resObj.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
                        updateMemory(userText, content);
                        return content.trim();
                    } catch (Exception e) { return fallback; }
                }).exceptionally(e -> fallback);
    }

    private void updateMemory(String user, String assistant) {
        chatHistory.add(createMsg("user", user));
        chatHistory.add(createMsg("assistant", assistant));
        if (chatHistory.size() > MAX_MEMORY) { chatHistory.remove(0); chatHistory.remove(0); }
    }

    private JsonObject createMsg(String role, String content) {
        JsonObject m = new JsonObject();
        m.addProperty("role", role);
        m.addProperty("content", content);
        return m;
    }

    private void loadConfig() {
        try {
            if (!CONFIG_FILE.exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
                JsonObject def = new JsonObject();
                def.addProperty("is_ollama", true);
                def.addProperty("model", "DeepSeek-V3-0324");
                def.addProperty("api_key", "sk-...");

                // Новая структура system_prompt
                JsonObject sysPrompts = new JsonObject();
                sysPrompts.addProperty("anya", "Ты — милая Аня, подруга игрока в Minecraft.");
                sysPrompts.addProperty("kira", "Ты — энергичная Кира, любишь приключения в Minecraft.");
                sysPrompts.addProperty("masha", "Ты — спокойная Маша, эксперт по строительству в Minecraft.");
                def.add("system_prompt", sysPrompts);

                def.addProperty("temperature", 0.7);
                def.addProperty("follow_player", true);
                def.addProperty("max_tokens", 200);
                def.addProperty("ollama_url", "http://localhost:11434/api/chat");
                def.addProperty("cloud_url", "https://api.sambanova.ai/v1/chat/completions");
                try (FileWriter writer = new FileWriter(CONFIG_FILE)) { gson.toJson(def, writer); }
            }
            try (FileReader reader = new FileReader(CONFIG_FILE)) { configData = gson.fromJson(reader, JsonObject.class); }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static boolean isFollowPlayerEnabled() {
        return configData != null && configData.has("follow_player") && configData.get("follow_player").getAsBoolean();
    }

    private void spawnIfFirst(ServerPlayerEntity player, EntityType<?> type) {
        if (player.getWorld().getPlayers().size() == 1) {
            Entity e = type.create(player.getWorld(), SpawnReason.TRIGGERED);
            if (e != null) {
                e.refreshPositionAndAngles(player.getBlockPos().add(2, 0, 2), 0, 0);
                player.getWorld().spawnEntity(e);
            }
        }
    }
}