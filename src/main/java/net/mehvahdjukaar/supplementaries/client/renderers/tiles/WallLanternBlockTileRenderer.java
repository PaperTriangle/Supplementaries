package net.mehvahdjukaar.supplementaries.client.renderers.tiles;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.supplementaries.block.tiles.WallLanternBlockTile;
import net.mehvahdjukaar.supplementaries.client.renderers.LOD;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;


public class WallLanternBlockTileRenderer extends EnhancedLanternBlockTileRenderer<WallLanternBlockTile> {
    public WallLanternBlockTileRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WallLanternBlockTile tile, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn,
                       int combinedOverlayIn) {

        if(tile.shouldRenderFancy()) {
            this.renderLantern(tile, tile.mimic, partialTicks, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn, false);
        }

        LOD lod = new LOD(this.renderer,tile.getBlockPos());

        tile.setFancyRenderer(lod.isNear());

    }
}