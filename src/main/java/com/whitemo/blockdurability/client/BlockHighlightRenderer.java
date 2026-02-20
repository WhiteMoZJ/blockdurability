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

        // 仅在实体渲染后执行，保证层级正确
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Player player = mc.player;
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();

        // 基础开关判断
        if (mc.isPaused() || player == null || mc.level == null || !ModClientConfig.VISUAL_ENABLED.get()) return;
        // 仅手持木棍时显示的判断
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

        // 渲染状态设置
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(lineWidth);
        if (seeThroughWalls) {
            RenderSystem.disableDepthTest();
        }

        // 遍历所有保护方块，渲染范围内的内容
        for (Map.Entry<BlockPos, Integer> entry : data.entrySet()) {
            BlockPos pos = entry.getKey();
            int durability = entry.getValue();

            // 超出渲染范围跳过
            if (!pos.closerToCenterThan(player.position(), renderRange)) continue;

            // 获取方块状态，保证渲染的轮廓和方块实际形状完全一致
            BlockState blockState = mc.level.getBlockState(pos);
            // 空方块跳过渲染
            if (blockState.isAir()) continue;

            // 获取方块的碰撞箱（兼容不规则方块：楼梯、栅栏等）
            AABB blockBox = blockState.getShape(mc.level, pos).bounds().move(pos);
            // 应用原版偏移，避免和方块重叠
            AABB outlineBox = blockBox.inflate(OUTLINE_OFFSET);

            // 获取颜色配置
            int[] color;
            if (durability == -1) {
                // 创造模式玩家看到的不可破坏方块改为黄色，生存模式保持红色
                color = ModClientConfig.getColorArray(ModClientConfig.UNBREAKABLE_COLOR); // 解析配置字符串
            } else {
                color = ModClientConfig.getColorArray(ModClientConfig.CUSTOM_DURABILITY_COLOR); // 解析配置字符串
            }
            float r = color[0] / 255f;
            float g = color[1] / 255f;
            float b = color[2] / 255f;
            float a = 0.9f;

            // 渲染线框
            // renderBlockOutline(poseStack, bufferSource, outlineBox, r, g, b, a);

            // 2. 渲染耐久悬浮文本
            if (ModClientConfig.SHOW_DURABILITY_TEXT.get()) {
                renderDurabilityText(poseStack, bufferSource, pos, durability, camera, r, g, b);
            }
        }

        bufferSource.endBatch(RenderType.lines());
        RenderSystem.lineWidth(1.0f); // 还原默认线宽
        RenderSystem.enableDepthTest(); // 还原深度测试
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    // 渲染方块上方的耐久文本
    private static void renderDurabilityText(PoseStack poseStack, MultiBufferSource bufferSource,
                                             BlockPos pos, int durability, Camera camera,
                                             float r, float g, float b) {
        Vec3 textPos = Vec3.atCenterOf(pos).add(0, 0.6, 0); // 方块中心上方
        String text = durability == -1 ? "不可破坏" : "耐久: " + durability;
        float scale = ModClientConfig.TEXT_SCALE.get().floatValue();

        poseStack.pushPose();
        poseStack.translate(textPos.x, textPos.y, textPos.z);
        // 文本始终面向玩家（billboard效果）
        poseStack.mulPose(camera.rotation());
        // 翻转Y轴，避免文字倒置
        poseStack.scale(-scale, -scale, scale);

        Minecraft mc = Minecraft.getInstance();
        int textWidth = mc.font.width(text);
        // 居中渲染
        float xOffset = -textWidth / 2f;

        // 渲染文本（带背景，更清晰）
        mc.font.drawInBatch(
                text,
                xOffset,
                0,
                0xFFFFFFFF,
                false,
                poseStack.last().pose(),
                bufferSource,
                Font.DisplayMode.SEE_THROUGH,
                0x50000000, // 背景透明度
                15728880, // 光照等级
                false
        );

        poseStack.popPose();
    }

    // private static void renderBlockOutline(PoseStack poseStack, MultiBufferSource bufferSource, AABB box, float r, float g, float b, float a) {

    // }
}
