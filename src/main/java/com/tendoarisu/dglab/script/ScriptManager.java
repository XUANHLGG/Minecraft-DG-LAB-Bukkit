package com.tendoarisu.dglab.script;

import org.bukkit.plugin.java.JavaPlugin;
import org.mozilla.javascript.Context;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ScriptManager {

    private final JavaPlugin plugin;
    private final Path scriptsDir;
    private final ScriptRuntime runtime;

    private volatile boolean loaded = false;

    public ScriptManager(JavaPlugin plugin, ScriptRuntime runtime) {
        this.plugin = plugin;
        this.runtime = runtime;
        this.scriptsDir = plugin.getDataFolder().toPath().resolve("scripts");
    }

    public Path scriptsDir() {
        return scriptsDir;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void ensureExampleScript() {
        try {
            Files.createDirectories(scriptsDir);
            Path example = scriptsDir.resolve("example.js");
            if (Files.exists(example)) {
                return;
            }

            try (var in = plugin.getResource("example.js")) {
                if (in != null) {
                    Files.copy(in, example);
                    return;
                }
            }

            Files.writeString(example, DEFAULT_EXAMPLE_JS, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().warning("创建脚本目录或 example.js 失败: " + e.getMessage());
        }
    }

    public void reloadAll() {
        loaded = false;

        ensureExampleScript();

        runtime.resetScope();

        List<Path> jsFiles = listJsFiles();
        if (jsFiles.isEmpty()) {
            plugin.getLogger().info("在 " + scriptsDir.toAbsolutePath() + " 下未找到脚本");
            loaded = true;
            return;
        }

        for (Path file : jsFiles) {
            runOne(file);
        }

        loaded = true;
        plugin.getLogger().info("已加载 " + jsFiles.size() + " 个脚本");
    }

    private List<Path> listJsFiles() {
        try {
            Files.createDirectories(scriptsDir);
            List<Path> list = new ArrayList<>();
            try (var stream = Files.list(scriptsDir)) {
                stream.filter(p -> p.getFileName().toString().endsWith(".js"))
                        .sorted()
                        .forEach(list::add);
            }
            return list;
        } catch (IOException e) {
            plugin.getLogger().warning("获取脚本列表失败: " + e.getMessage());
            return List.of();
        }
    }

    private void runOne(Path file) {
        String code;
        try {
            code = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().warning("读取脚本 " + file + " 失败: " + e.getMessage());
            return;
        }

        Context cx = SandboxContextFactory.instance().enter();
        try {
            var scope = runtime.scope();
            if (scope == null) {
                plugin.getLogger().warning("脚本作用域未初始化，跳过 " + file.getFileName());
                return;
            }

            cx.evaluateString(scope, code, file.getFileName().toString(), 1, null);
        } catch (Exception e) {
            plugin.getLogger().severe("脚本 " + file.getFileName() + " 运行出错: " + e.getMessage());
            e.printStackTrace();
        } finally {
            SandboxContextFactory.instance().exit();
        }
    }

    private static final String DEFAULT_EXAMPLE_JS = """
            
            EntityEvents.death('player', event => {
                let connection = DgLabManager.getByUUID(event.getEntity().getUuid())
                if (connection != null) {
                    connection.addStrength('a', 10)
                }
            })
            """;
}
