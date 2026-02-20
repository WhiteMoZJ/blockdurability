package com.whitemo.blockdurability.client;

import com.whitemo.blockdurability.BlockDurabilityMod;
import com.whitemo.blockdurability.config.ModClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = BlockDurabilityMod.MOD_ID, value = Dist.CLIENT)
public class BlockHighlightRenderer {
    private static final double OUTLINE_OFFSET = 0.0020000000949949026D;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();

        // Only execute after entity rendering to ensure correct layer order
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Player player = mc.player;
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();

        // Basic switch judgment
        if (mc.isPaused() || player == null || mc.level == null || !ModClientConfig.VISUAL_ENABLED.get()) return;

        // Only show when holding a stick
        if (ModClientConfig.ONLY_SHOW_WITH_STICK.get() && !player.getMainHandItem().is(Items.STICK)) return;

        Map<BlockPos, Integer> data = ClientDurabilityCache.getAllData();
        if (data.isEmpty()) return;

        int renderRange = ModClientConfig.RENDER_RANGE.get();
        boolean seeThroughWalls = ModClientConfig.SEE_THROUGH_WALLS.get();
        float lineWidth = ModClientConfig.LINE_WIDTH.get().floatValue();

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // Render state settings
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(lineWidth);
        if (seeThroughWalls) {
            RenderSystem.disableDepthTest();
        }

        // Traverse all protected blocks, render only those within the render range
        for (Map.Entry<BlockPos, Integer> entry : data.entrySet()) {
            BlockPos pos = entry.getKey();
            int durability = entry.getValue();

            // Skip if outside render range
            if (!pos.closerToCenterThan(player.position(), renderRange)) continue;

            // Get block state to ensure consistent outline rendering
            BlockState blockState = mc.level.getBlockState(pos);
            // Skip rendering if the block is air
            if (blockState.isAir()) continue;

            // Get block collision box (compatible with irregular blocks: stairs, fences, etc.)
            AABB blockBox = blockState.getShape(mc.level, pos).bounds().move(pos);

            // Apply original offset to avoid overlap with the block
            AABB outlineBox = blockBox.inflate(OUTLINE_OFFSET);

            // Get color configuration
            int[] color;
            if (durability == -1) {
                // In creative mode, unbreakable blocks are yellow; in survival mode, they are red
                color = ModClientConfig.getColorArray(ModClientConfig.UNBREAKABLE_COLOR); // Parse color string
            } else {
                color = ModClientConfig.getColorArray(ModClientConfig.CUSTOM_DURABILITY_COLOR); // Parse color string
            }
            float r = color[0] / 255f;
            float g = color[1] / 255f;
            float b = color[2] / 255f;
            float a = 0.9f;

            // renderBlockOutline(poseStack, bufferSource, outlineBox, r, g, b, a);

            // Render durability text above the block
            if (ModClientConfig.SHOW_DURABILITY_TEXT.get()) {
                renderDurabilityText(poseStack, bufferSource, pos, durability, camera, r, g, b);
            }
        }

        bufferSource.endBatch(RenderType.lines());
        RenderSystem.lineWidth(1.0f); // Restore default line width
        RenderSystem.enableDepthTest(); // Restore depth test
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    // Render durability text above block
    private static void renderDurabilityText(PoseStack poseStack, MultiBufferSource bufferSource,
                                             BlockPos pos, int durability, Camera camera,
                                             float r, float g, float b) {
        Vec3 textPos = Vec3.atCenterOf(pos).add(0, 0.6, 0); // Above the block center
        String key = durability == -1 ? "blockdurability.text.unbreakable" : "blockdurability.text.durability";
        String text = durability == -1 
                ? Component.translatable(key).getString()
                : Component.translatable(key, durability).getString();
        float scale = ModClientConfig.TEXT_SCALE.get().floatValue();

        poseStack.pushPose();
        poseStack.translate(textPos.x, textPos.y, textPos.z);
        // Text always faces the player (billboard effect)
        poseStack.mulPose(camera.rotation());
        // Flip Y axis to avoid text being upside down
        poseStack.scale(-scale, -scale, scale);

        Minecraft mc = Minecraft.getInstance();
        int textWidth = mc.font.width(text);
        // Centered rendering
        float xOffset = -textWidth / 2f;

        // Render text (with background for better visibility)
        mc.font.drawInBatch(
                text,
                xOffset,
                0,
                0xFFFFFFFF,
                false,
                poseStack.last().pose(),
                bufferSource,
                Font.DisplayMode.SEE_THROUGH,
                0x50000000, // Background transparency
                15728880, // Light level
                false
        );

        poseStack.popPose();
    }

    // private static void renderBlockOutline(PoseStack poseStack, MultiBufferSource bufferSource, AABB box, float r, float g, float b, float a) {

    // }
}
