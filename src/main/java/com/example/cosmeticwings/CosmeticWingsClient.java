package com.example.cosmeticwings;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class CosmeticWingsClient implements ClientModInitializer {
    private static final Path CONFIG = MinecraftClient.getInstance().runDirectory.toPath()
            .resolve("config").resolve("cosmeticwings.properties");

    public static Mode mode = Mode.OFF;
    public static boolean halo = true;
    public static boolean horns = false;
    public static boolean animation = true;
    public static boolean featherFall = true;

    private static KeyBinding menuKey;

    public enum Mode { OFF, ANGEL, DEMON }

    @Override
    public void onInitializeClient() {
        loadConfig();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cosmeticwings.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_CONTROL,
                "category.cosmeticwings"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) openMenu(client);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("v").executes(context -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.execute(() -> openMenu(client));
                    return 1;
                })));

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (entityType, entityRenderer, registrationHelper, context) -> {
                    if (entityType == EntityType.PLAYER && entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
                        registrationHelper.register(new CosmeticFeatureRenderer(playerRenderer));
                    }
                });
    }

    private static void openMenu(MinecraftClient client) {
        if (client.currentScreen == null) {
            client.setScreen(new CosmeticScreen());
        }
    }

    static void saveConfig() {
        try {
            Files.createDirectories(CONFIG.getParent());
            Properties p = new Properties();
            p.setProperty("mode", mode.name());
            p.setProperty("halo", Boolean.toString(halo));
            p.setProperty("horns", Boolean.toString(horns));
            p.setProperty("animation", Boolean.toString(animation));
            p.setProperty("feathers", Boolean.toString(featherFall));
            try (var out = Files.newOutputStream(CONFIG)) {
                p.store(out, "Cosmetic Wings");
            }
        } catch (IOException ignored) {
        }
    }

    static void loadConfig() {
        if (!Files.exists(CONFIG)) return;
        try {
            Properties p = new Properties();
            try (var in = Files.newInputStream(CONFIG)) {
                p.load(in);
            }
            mode = Mode.valueOf(p.getProperty("mode", "OFF"));
            halo = Boolean.parseBoolean(p.getProperty("halo", "true"));
            horns = Boolean.parseBoolean(p.getProperty("horns", "false"));
            animation = Boolean.parseBoolean(p.getProperty("animation", "true"));
            featherFall = Boolean.parseBoolean(p.getProperty("feathers", "true"));
        } catch (Exception ignored) {
        }
    }

    private static final class CosmeticScreen extends Screen {
        CosmeticScreen() {
            super(Text.literal("COSMETIC WINGS"));
        }

        @Override
        protected void init() {
            int w = 280;
            int x = width / 2 - w / 2;
            int y = height / 2 - 90;

            addDrawableChild(ButtonWidget.builder(Text.literal("Крылья: " + modeText()), b -> {
                mode = Mode.values()[(mode.ordinal() + 1) % Mode.values().length];
                saveConfig();
                b.setMessage(Text.literal("Крылья: " + modeText()));
            }).dimensions(x, y, w, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Нимб: " + yesNo(halo)), b -> {
                halo = !halo;
                saveConfig();
                b.setMessage(Text.literal("Нимб: " + yesNo(halo)));
            }).dimensions(x, y + 26, w, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Рога: " + yesNo(horns)), b -> {
                horns = !horns;
                saveConfig();
                b.setMessage(Text.literal("Рога: " + yesNo(horns)));
            }).dimensions(x, y + 52, w, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Анимация: " + yesNo(animation)), b -> {
                animation = !animation;
                saveConfig();
                b.setMessage(Text.literal("Анимация: " + yesNo(animation)));
            }).dimensions(x, y + 78, w, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Выпадение перьев: " + yesNo(featherFall)), b -> {
                featherFall = !featherFall;
                saveConfig();
                b.setMessage(Text.literal("Выпадение перьев: " + yesNo(featherFall)));
            }).dimensions(x, y + 104, w, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("ГОТОВО"), b -> close())
                    .dimensions(x, y + 140, w, 20).build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 28, 0xFFFFFF);
            super.render(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }

    private static String modeText() {
        return switch (mode) {
            case OFF -> "ВЫКЛ";
            case ANGEL -> "ANGEL";
            case DEMON -> "DEMON";
        };
    }

    private static String yesNo(boolean value) {
        return value ? "ВКЛ" : "ВЫКЛ";
    }

    private static final class CosmeticFeatureRenderer
            extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

        CosmeticFeatureRenderer(PlayerEntityRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(MatrixStack matrices, VertexConsumerProvider providers, int light,
                           PlayerEntityRenderState state, float limbAngle, float limbDistance) {
            if (mode == Mode.OFF) return;

            float glide = MathHelper.clamp(state.getGlidingProgress(), 0.0f, 1.0f);
            float time = state.age;

            if (mode == Mode.ANGEL) {
                renderAngel(matrices, providers, light, glide, time);
            } else {
                renderDemon(matrices, providers, light, glide, time);
            }

            if (halo) renderHalo(matrices, providers, light, time);
            if (horns) renderHorns(matrices, providers, light);
        }

        private void renderAngel(MatrixStack m, VertexConsumerProvider p, int light, float glide, float time) {
            VertexConsumer v = p.getBuffer(RenderLayer.getEntityCutoutNoCull(
                    Identifier.of("cosmeticwings", "textures/white.png")));

            float open = MathHelper.lerp(glide, 0.12f, 1.0f);
            float flap = animation ? MathHelper.sin(time * 0.18f) * 18f * glide : 0f;

            for (int side : new int[]{-1, 1}) {
                for (int pair = 0; pair < 2; pair++) {
                    m.push();
                    m.translate(side * 0.20f, 1.10f + pair * 0.25f, 0.10f);
                    m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * (8f + 25f * open)));
                    m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * (-8f + flap) + pair * side * 3f));

                    for (int i = 0; i < 9; i++) {
                        float spread = (i - 4) * 0.16f * open;
                        float len = 0.45f + (4 - Math.abs(i - 4)) * 0.13f;

                        m.push();
                        m.translate(side * spread, -i * 0.018f, -len * 0.30f);
                        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * (12f + i * 2.0f)));
                        box(m, v, 0.055f, 0.12f + len * 0.06f, len, 0xF4F0E7, light);
                        m.pop();
                    }
                    m.pop();
                }
            }
        }

        private void renderDemon(MatrixStack m, VertexConsumerProvider p, int light, float glide, float time) {
            VertexConsumer v = p.getBuffer(RenderLayer.getEntityCutoutNoCull(
                    Identifier.of("cosmeticwings", "textures/white.png")));

            float open = MathHelper.lerp(glide, 0.10f, 1.0f);
            float flap = animation ? MathHelper.sin(time * 0.13f) * 12f * glide : 0f;

            for (int side : new int[]{-1, 1}) {
                m.push();
                m.translate(side * 0.22f, 1.10f, 0.10f);
                m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * (14f + 32f * open)));
                m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * (-8f + flap)));

                box(m, v, 0.15f, 0.16f, 1.55f, 0x151015, light);

                for (int i = 0; i < 5; i++) {
                    m.push();
                    m.translate(side * (0.16f + i * 0.13f), 0.02f, -0.55f + i * 0.27f);
                    m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 25f));
                    box(m, v, 0.08f, 0.09f, 0.52f - i * 0.04f, 0x4B1218, light);
                    m.pop();
                }

                for (int i = 0; i < 4; i++) {
                    m.push();
                    m.translate(side * (0.28f + i * 0.19f), 0.02f, -0.62f + i * 0.40f);
                    m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 55f));
                    box(m, v, 0.065f, 0.065f, 0.38f - i * 0.04f, 0xD5C7B8, light);
                    m.pop();
                }
                m.pop();
            }
        }

        private void renderHalo(MatrixStack m, VertexConsumerProvider p, int light, float time) {
            VertexConsumer v = p.getBuffer(RenderLayer.getEntityTranslucent(
                    Identifier.of("cosmeticwings", "textures/white.png")));

            m.push();
            m.translate(0, 2.05f, 0);
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 1.5f));
            box(m, v, 0.055f, 0.055f, 0.72f, 0xFFE98D, light);
            m.pop();
        }

        private void renderHorns(MatrixStack m, VertexConsumerProvider p, int light) {
            VertexConsumer v = p.getBuffer(RenderLayer.getEntityCutoutNoCull(
                    Identifier.of("cosmeticwings", "textures/white.png")));

            for (int side : new int[]{-1, 1}) {
                m.push();
                m.translate(side * 0.20f, 1.78f, -0.02f);
                m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 20f));
                box(m, v, 0.075f, 0.11f, 0.34f, 0x28151A, light);
                m.pop();
            }
        }

        private static void box(MatrixStack m, VertexConsumer v, float x, float y, float z,
                                 int rgb, int light) {
            float[][] faces = {
                    {-x,-y,-z, x,-y,-z, x,y,-z, -x,y,-z},
                    {x,-y,z, -x,-y,z, -x,y,z, x,y,z},
                    {-x,-y,z, -x,-y,-z, -x,y,-z, -x,y,z},
                    {x,-y,-z, x,-y,z, x,y,z, x,y,-z},
                    {-x,y,-z, x,y,-z, x,y,z, -x,y,z},
                    {-x,-y,z, x,-y,z, x,-y,-z, -x,-y,-z}
            };
            for (float[] f : faces) quad(m, v, f, rgb, light);
        }

        private static void quad(MatrixStack m, VertexConsumer v, float[] f, int rgb, int light) {
            MatrixStack.Entry e = m.peek();
            vertex(v, e, f[0], f[1], f[2], rgb, 0f, 0f, light);
            vertex(v, e, f[3], f[4], f[5], rgb, 1f, 0f, light);
            vertex(v, e, f[6], f[7], f[8], rgb, 1f, 1f, light);
            vertex(v, e, f[9], f[10], f[11], rgb, 0f, 1f, light);
        }

        private static void vertex(VertexConsumer v, MatrixStack.Entry e,
                                   float x, float y, float z, int rgb,
                                   float u, float texV, int light) {
            v.vertex(e, x, y, z)
                    .color(rgb)
                    .texture(u, texV)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(light)
                    .normal(e, 0f, 1f, 0f);
        }
    }
}
