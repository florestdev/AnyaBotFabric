package ru.florestdev.anyabotfabric;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.Component;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroup;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
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

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import net.minecraft.item.ItemGroups;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Anyabotfabric implements ModInitializer {

    public static final String MODID = "anyabotfabric";
    private static final File CONFIG_FILE = new File("config/anya_config.json");

    // === Параметры Конфига ===
    private static JsonObject configData = new JsonObject();
    private final HttpClient http = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    // === Память Ани (Список последних сообщений) ===
    private final List<JsonObject> chatHistory = new ArrayList<>();
    private static final int MAX_MEMORY = 500; // Помнит 5 последних диалогов (вопрос-ответ)

    public static Map<UUID, AnyaEntity> playerToAnya = new HashMap<>();

    public static EntityType<AnyaEntity> ANYA;
    public static final RegistryKey<EntityType<?>> ANYA_KEY =
            RegistryKey.of(Registries.ENTITY_TYPE.getKey(), Identifier.of(MODID, "anya"));

    public void placeStructure(java.io.File file, net.minecraft.util.math.BlockPos pos, net.minecraft.server.world.ServerWorld world) {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            // Читаем NBT файл
            net.minecraft.nbt.NbtCompound nbt = net.minecraft.nbt.NbtIo.readCompressed(fis, net.minecraft.nbt.NbtSizeTracker.ofUnlimitedBytes());

            // Создаем шаблон структуры
            net.minecraft.structure.StructureTemplate template = new net.minecraft.structure.StructureTemplate();

            // Магия реестров (для 1.20+)
            var registryLookup = world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.BLOCK);
            template.readNbt(registryLookup, nbt);

            // Настройки размещения
            net.minecraft.structure.StructurePlacementData settings = new net.minecraft.structure.StructurePlacementData()
                    .setIgnoreEntities(false);

            // Важно: выполняем на главном потоке сервера, чтобы не крашнулось
            world.getServer().execute(() -> {
                template.place(world, pos, pos, settings, world.getRandom(), 2);
                System.out.println("Аня: Построила успешно!");
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractSchematicUrl(String text) {
        String[] words = text.split("\\s+");
        for (String word : words) {
            // Проверяем, что это ссылка и она ведет на файл постройки
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
            villager.setCustomName(Text.literal("Anya's & %s Baby".formatted(playerName))); // можно добавить эмодзи
            villager.setBaby(true); // делает его детёнышем
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

        // Удар по Ане
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!world.isClient && entity instanceof AnyaEntity && player instanceof ServerPlayerEntity sp) {
                askAI("Игрок ударил меня!", "Ай! За что? 😢", sp);
            }
            Anyabotfabric.playerToAnya.remove(player.getUuid());
            player.sendMessage(Text.literal("Аня больше не будет за тобой следовать."), false);
            return ActionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && entity instanceof AnyaEntity anya && player instanceof ServerPlayerEntity sp) {
                playerToAnya.put(sp.getUuid(), anya);
                sp.sendMessage(Text.literal("Теперь Anya будет вашей!"), true);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });


        // Чат рядом с Аней
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String playerText = message.getContent().getString();
            if (playerText.toLowerCase().contains("anya, приди") || playerText.toLowerCase().contains("anya, come on")) {spawnIfFirst(sender);}

            // 1. Ищем Аню рядом
            AnyaEntity anya = sender.getWorld().getEntitiesByClass(AnyaEntity.class,
                    sender.getBoundingBox().expand(10.0), e -> true).stream().findFirst().orElse(null);

            if (anya != null) {
                if (playerText.toLowerCase().contains("anya, go play") || playerText.toLowerCase().contains("аня, давай поиграем")) {
                    if (!playerToAnya.containsKey(sender.getUuid())) {
                        sender.sendMessage(Text.of("Бро, это не твоя Аня! ты не можешь иметь ребенка с ней."));
                        return;
                    }
                    askAI("Ты с %s занялась ... и появился ребенок у вас совместный.".formatted(sender.getName().getString()), "Ой… кто-то новый появился!", sender);
                    sender.getWorld().spawnParticles(
                            ParticleTypes.HEART,  // тип частицы — сердечко
                            sender.getX(),        // X координата
                            sender.getY() + 1.2,  // Y (чуть выше сущности, чтобы видно было)
                            sender.getZ(),        // Z
                            10,                   // количество частиц
                            0.3, 0.3, 0.3,        // разброс по XYZ
                            0.0                   // скорость
                    );
                    spawnLittleVillager(sender.getWorld(), sender.getBlockPos(), sender.getName().getString());
                }
            }

            if (anya != null) {
                if (playerText.toLowerCase().startsWith("создай ")
                        || playerText.toLowerCase().startsWith("create ")) {

                    String idea = playerText.substring(playerText.indexOf(" ") + 1);

                    askAIStructure(idea, sender);
                    return;
                }
            }

            if (anya != null) {
                // 2. Проверяем, есть ли в сообщении ссылка и команда на постройку
                String foundUrl = extractSchematicUrl(playerText);

                if (foundUrl != null && (playerText.toLowerCase().contains("построй") || playerText.toLowerCase().contains("build"))) {

                    // Запускаем стройку!
                    AnyaSchematicHelper.downloadAndProcess(foundUrl, (file) -> {
                        // Строим чуть впереди игрока
                        System.out.println("Так.. Ну, началось!");
                        BlockPos buildPos = sender.getBlockPos().offset(sender.getHorizontalFacing(), 5);

                        // Вызываем метод вставки из твоего главного класса
                        // (Если метод в Anyabotfabric, вызывай через Anyabotfabric.INSTANCE или как у тебя настроено)
                        System.out.println("Окей.. Делаем.");
                        this.placeStructure(file, buildPos, (ServerWorld) sender.getWorld());
                    });

                    // Отправляем Ане запрос, чтобы она прокомментировала стройку
                    askAI(sender.getName().getString() + " просит тебя построить это по ссылке: " + foundUrl, "...", sender);
                } else {
                    // Если ссылки нет — просто обычный разговор
                    askAI(sender.getName().getString() + " говорит: " + playerText, "...", sender);
                }
            }
        });
    }

    private void askAI(String prompt, String fallback, ServerPlayerEntity player) {
        processAI(prompt, fallback, false).thenAccept(reply ->
                player.getServer().execute(() -> player.sendMessage(Text.literal("§d<Anya>§f " + reply), false))
        );
    }

    public NbtCompound snbt_to_nbt(String snbt) {
        try {
            return StringNbtReader.readCompound(snbt);
        } catch (CommandSyntaxException e) {
            return null;
        }
    }

    private void askAIStructure(String idea, ServerPlayerEntity player) {
        processAI(
                "Создай структуру: " + idea,
                "",
                true
        ).thenAccept(snbt -> {

            player.getServer().execute(() -> {
                try {
                    // 1. SNBT → NBT
                    NbtCompound nbt = StringNbtReader.readCompound(snbt);
                    System.out.println("SNBT is ->" + " " + snbt);

                    // 2. Сохраняем во временный файл
                    File file = new File("anya_generated.nbt");
                    NbtIo.writeCompressed(nbt, file.toPath());

                    // 3. Ставим рядом с игроком
                    BlockPos pos = player.getBlockPos()
                            .offset(player.getHorizontalFacing(), 5);

                    placeStructure(file, pos, (ServerWorld) player.getWorld());

                    player.sendMessage(
                            Text.literal("§d<Anya>§f Я построила это для тебя 💕"),
                            false
                    );

                } catch (Exception e) {
                    e.printStackTrace();
                    player.sendMessage(
                            Text.literal("§c<Anya>§f Я не смогла это построить 😢"),
                            false
                    );
                }
            });
        });
    }

    private CompletableFuture<String> processAI(String userText, String fallback, boolean isNBT) {
        boolean isOllama = configData.get("is_ollama").getAsBoolean();
        String model = configData.get("model").getAsString();

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("temperature", configData.get("temperature").getAsDouble());
        if (!isOllama) body.addProperty("max_tokens", configData.get("max_tokens").getAsInt());
        if (isOllama) body.addProperty("stream", false);

        // Формируем пакет сообщений
        JsonArray messages = new JsonArray();
        if (!isNBT) {
            messages.add(createMsg("system", configData.get("system_prompt").getAsString()));
        } else {
            String prompt =
                    "Ты генерируешь СТРОГО валидный SNBT для StructureTemplate Minecraft 1.21.x.\n" +
                            "\n" +
                            "ОБЯЗАТЕЛЬНЫЕ ТРЕБОВАНИЯ:\n" +
                            "1. Ответ должен содержать ТОЛЬКО один SNBT-объект.\n" +
                            "2. Любой символ вне SNBT считается ошибкой.\n" +
                            "3. Используй ТОЛЬКО ASCII (запрещена кириллица).\n" +
                            "4. Используй ТОЛЬКО vanilla блоки Minecraft.\n" +
                            "5. Запрещены комментарии, пояснения, форматирование, markdown.\n" +
                            "\n" +
                            "СТРУКТУРА SNBT (ОБЯЗАТЕЛЬНО):\n" +
                            "- size: [X, Y, Z]\n" +
                            "- palette: список блоков вида {Name:\"minecraft:block\"}\n" +
                            "- blocks: список блоков вида {pos:[x,y,z], state:index}\n" +
                            "\n" +
                            "ПРАВИЛА:\n" +
                            "- size должна соответствовать координатам blocks.\n" +
                            "- state ссылается на индекс блока в palette.\n" +
                            "- Используй air для пустоты.\n" +
                            "- Минимальный размер структуры: 3x3x3.\n" +
                            "- Структура должна быть ЗАВЕРШЁННОЙ и корректной.\n" +
                            "\n" +
                            "НЕ ДЕЛАЙ:\n" +
                            "- JSON\n" +
                            "- Абстрактные поля\n" +
                            "- Описания комнат\n" +
                            "- Текстовые комментарии\n" +
                            "- Любые ключи кроме size, palette, blocks\n" +
                            "\n" +
                            "Верни ТОЛЬКО SNBT.";
            messages.add(createMsg("system", prompt));
        }

        // Добавляем ПАМЯТЬ
        for (JsonObject oldMsg : chatHistory) messages.add(oldMsg);

        // Текущее сообщение
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
                        String content;
                        if (isOllama) {
                            content = resObj.getAsJsonObject("message").get("content").getAsString();
                        } else {
                            content = resObj.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
                        }

                        // Сохраняем в память
                        updateMemory(userText, content);
                        return content.trim();
                    } catch (Exception e) {
                        return fallback;
                    }
                }).exceptionally(e -> fallback);
    }

    private void updateMemory(String user, String assistant) {
        chatHistory.add(createMsg("user", user));
        chatHistory.add(createMsg("assistant", assistant));
        if (chatHistory.size() > MAX_MEMORY) {
            chatHistory.remove(0);
            chatHistory.remove(0);
        }
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
                def.addProperty("system_prompt", "Ты — милая Аня, подруга игрока в Minecraft (модификация: AnyaBot-Fabric)");
                def.addProperty("temperature", 0.7);
                def.addProperty("follow_player", true);
                def.addProperty("max_tokens", 200);
                def.addProperty("ollama_url", "http://localhost:11434/api/chat");
                def.addProperty("cloud_url", "https://api.sambanova.ai/v1/chat/completions");
                try (FileWriter writer = new FileWriter(CONFIG_FILE)) { gson.toJson(def, writer); }
            }
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                configData = gson.fromJson(reader, JsonObject.class);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Добавь это в Anyabotfabric.java
    public static boolean isFollowPlayerEnabled() {
        try {
            if (configData != null && configData.has("follow_player")) {
                return configData.get("follow_player").getAsBoolean();
            }
        } catch (Exception e) {
            System.err.println("[AnyaBot] Ошибка при чтении follow_player: " + e.getMessage());
        }
        return true; // Значение по умолчанию, если что-то пошло не так
    }

    private void spawnIfFirst(ServerPlayerEntity player) {
        if (player.getWorld().getPlayers().size() == 1) {;
            AnyaEntity anya = new AnyaEntity(ANYA, player.getWorld());
            anya.refreshPositionAndAngles(player.getBlockPos().add(2, 0, 2), 0, 0);
            player.getWorld().spawnEntity(anya);
        }
    }
}