package com.ruslan.glitch.client;

import com.ruslan.glitch.GlitchMod;
import com.ruslan.glitch.entity.GlitchEntity;
import com.ruslan.glitch.entity.GlitchMinionEntity;
import com.ruslan.glitch.network.ScarePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.Random;

public class GlitchClient implements ClientModInitializer {
    private static final Identifier[] FACES = {
            GlitchMod.id("textures/gui/scare_0.png"),
            GlitchMod.id("textures/gui/scare_1.png"),
            GlitchMod.id("textures/gui/scare_2.png"),
            GlitchMod.id("textures/gui/scare_3.png")
    };
    private static final Random RAND = new Random();

    private static int ticksLeft = 0;
    private static int total = 1;
    private static int variant = 0;
    private static int face = 0;
    private static int layout = 0;
    private static boolean loud = false;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(GlitchMod.GLITCH, ctx ->
                new GlitchRenderer<GlitchEntity>(ctx, GlitchMod.id("textures/entity/glitch.png"), 30));
        EntityRendererRegistry.register(GlitchMod.MINION, ctx ->
                new GlitchRenderer<GlitchMinionEntity>(ctx, GlitchMod.id("textures/entity/glitch_minion.png"), 12));

        ClientPlayNetworking.registerGlobalReceiver(ScarePayload.ID, (payload, context) ->
                trigger(context.client(), payload.durationTicks(), payload.variant()));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ticksLeft > 0) ticksLeft--;
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> render(ctx));
    }

    /** Каждый скример случайный: другое лицо, другая раскладка, другой звук и немного другая длительность. */
    private static void trigger(MinecraftClient client, int duration, int v) {
        loud = v == 0 && duration >= 12;
        ticksLeft = Math.max(2, duration + (loud ? RAND.nextInt(12) : 0));
        total = ticksLeft;
        variant = v;
        face = RAND.nextInt(FACES.length);
        layout = RAND.nextInt(4);

        var sm = client.getSoundManager();
        switch (v) {
            case 0 -> {
                if (loud) {
                    SoundEvent[] pool = {
                            SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundEvents.ENTITY_ENDERMAN_SCREAM,
                            SoundEvents.ENTITY_GHAST_SCREAM, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE,
                            SoundEvents.ENTITY_WARDEN_ROAR
                    };
                    sm.play(PositionedSoundInstance.master(pool[RAND.nextInt(pool.length)], 0.5f + RAND.nextFloat() * 0.7f, 0.8f));
                    sm.play(PositionedSoundInstance.master(pool[RAND.nextInt(pool.length)], 0.4f + RAND.nextFloat() * 0.5f, 0.6f));
                } else {
                    sm.play(PositionedSoundInstance.master(SoundEvents.ENTITY_ENDERMAN_STARE, 0.3f, 0.4f));
                }
            }
            case 1 -> sm.play(PositionedSoundInstance.master(SoundEvents.ENTITY_ENDERMAN_STARE, 0.5f + RAND.nextFloat() * 0.4f, 0.6f));
            case 2 -> sm.play(PositionedSoundInstance.master(SoundEvents.ENTITY_WARDEN_ROAR, 0.5f, 0.5f));
            default -> sm.play(PositionedSoundInstance.master(SoundEvents.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.0f));
        }
    }

    private static void bands(DrawContext ctx, int w, int h, int count, int maxAlpha) {
        int[] colors = {0xFFFFFF, 0x00FFFF, 0xFF00FF, 0xFF0000, 0x000000};
        for (int i = 0; i < count; i++) {
            int y = RAND.nextInt(h);
            int bh = 1 + RAND.nextInt(Math.max(2, h / 25));
            int x = RAND.nextInt(Math.max(1, w / 2));
            int bw = w / 4 + RAND.nextInt(Math.max(1, w / 2));
            int alpha = 40 + RAND.nextInt(maxAlpha);
            ctx.fill(x, y, Math.min(w, x + bw), Math.min(h, y + bh), (alpha << 24) | colors[RAND.nextInt(colors.length)]);
        }
    }

    private static void drawFace(DrawContext ctx, int x, int y, int fw, int fh) {
        ctx.drawTexture(FACES[face], x, y, fw, fh, 0, 0, 256, 256, 256, 256);
    }

    private static void render(DrawContext ctx) {
        if (ticksLeft <= 0) return;
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        float progress = ticksLeft / (float) total; // 1 -> 0

        switch (variant) {
            case 0 -> {
                boolean flash = !loud; // короткий кадр-вспышка: без чёрного фона, как "подсознательный" кадр
                if (!flash) ctx.fill(0, 0, w, h, 0xFF000000);
                // лицо моргает: иногда пропадает на пару кадров
                if (flash || ticksLeft % (2 + layout) != 0) {
                    int size = Math.min(w, h);
                    int jx = RAND.nextInt(17) - 8, jy = RAND.nextInt(17) - 8;
                    switch (layout) {
                        case 0 -> drawFace(ctx, (w - size) / 2 + jx, (h - size) / 2 + jy, size, size);
                        case 1 -> { // огромное лицо, часть за краем экрана
                            int big = (int) (Math.max(w, h) * 1.6);
                            drawFace(ctx, (w - big) / 2 + RAND.nextInt(w / 4 + 1) - w / 8,
                                    (h - big) / 2 + RAND.nextInt(h / 4 + 1) - h / 8, big, big);
                        }
                        case 2 -> { // четыре лица
                            drawFace(ctx, 0, 0, w / 2, h / 2);
                            drawFace(ctx, w / 2, 0, w / 2, h / 2);
                            drawFace(ctx, 0, h / 2, w / 2, h / 2);
                            drawFace(ctx, w / 2, h / 2, w / 2, h / 2);
                        }
                        default -> { // растянутое по ширине лицо в случайном месте
                            int fh = h / 2;
                            drawFace(ctx, jx, RAND.nextInt(Math.max(1, h - fh)), w, fh);
                        }
                    }
                }
                if (RAND.nextInt(6) == 0) ctx.fill(0, 0, w, h, 0x78FFFFFF); // белая вспышка
                bands(ctx, w, h, flash ? 10 : 16, 130);
            }
            case 1 -> bands(ctx, w, h, 6 + RAND.nextInt(6), 90);
            case 2 -> {
                int dark = (int) ((1f - progress) * 140);
                ctx.fill(0, 0, w, h, (Math.min(200, dark + 30) << 24));
                bands(ctx, w, h, 30, 160);
            }
            default -> {
                int alpha = (int) (progress * 230);
                ctx.fill(0, 0, w, h, (alpha << 24) | 0xFFFFFF);
            }
        }
    }
}
