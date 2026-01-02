package ru.florestdev.anyabotfabric;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;

public class AnyaSchematicHelper {

    private static final HttpClient client = HttpClient.newHttpClient();

    /**
     * Скачивает файл и выполняет действие после загрузки
     * @param url Link to Discord attachment
     * @param callback Logic to execute with the downloaded file
     */
    public static void downloadAndProcess(String url, SchematicCallback callback) {
        CompletableFuture.runAsync(() -> {
            Path tempFile = null;
            try {
                // 1. Создаем временный файл с расширением .nbt
                tempFile = Files.createTempFile("anya_build_", ".nbt");

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                // 2. Качаем файл
                HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tempFile));

                if (response.statusCode() == 200) {
                    // 3. Передаем файл в твой метод вставки в мир
                    callback.onReady(response.body().toFile());
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // 4. Удаляем файл после использования
                if (tempFile != null) {
                    try {
                        Files.deleteIfExists(tempFile);
                        System.out.println("Anya: Временный чертеж удален.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    // Интерфейс для обработки файла
    public interface SchematicCallback {
        void onReady(File file);
    }

    public void placeStructure(File file, BlockPos pos, ServerWorld world) {
        try (FileInputStream fis = new FileInputStream(file)) {
            NbtCompound nbt = NbtIo.readCompressed(fis, NbtSizeTracker.ofUnlimitedBytes());
            StructureTemplate template = new StructureTemplate();

            // Пытаемся достать реестр через прямой доступ к Wrapper
            var registryManager = world.getRegistryManager();
            var blockRegistry = registryManager.getOrThrow(RegistryKeys.BLOCK);

            template.readNbt(blockRegistry, nbt);

            StructurePlacementData settings = new StructurePlacementData().setIgnoreEntities(false);

            world.getServer().execute(() -> {
                template.place(world, pos, pos, settings, world.getRandom(), 2);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}