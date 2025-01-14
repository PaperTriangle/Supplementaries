package net.mehvahdjukaar.supplementaries.client.gui;


import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.supplementaries.block.blocks.DoormatBlock;
import net.mehvahdjukaar.supplementaries.block.tiles.DoormatBlockTile;
import net.mehvahdjukaar.supplementaries.client.renderers.Const;
import net.mehvahdjukaar.supplementaries.client.renderers.TextUtil;
import net.mehvahdjukaar.supplementaries.client.renderers.tiles.DoormatBlockTileRenderer;
import net.mehvahdjukaar.supplementaries.network.NetworkHandler;
import net.mehvahdjukaar.supplementaries.network.ServerBoundSetTextHolderPacket;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import com.mojang.math.Matrix4f;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.client.model.data.EmptyModelData;

import java.util.stream.IntStream;


import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;

public class DoormatGui extends Screen {
    private TextFieldHelper textInputUtil;
    // The index of the line that is being edited.
    private int editLine = 0;
    //for ticking cursor
    private int updateCounter;
    private final DoormatBlockTile tileSign;
    private final String[] cachedLines;

    public DoormatGui(DoormatBlockTile teSign) {
        super(new TranslatableComponent("gui.supplementaries.doormat.edit"));
        this.tileSign = teSign;
        this.cachedLines = IntStream.range(0, DoormatBlockTile.MAX_LINES)
                .mapToObj(teSign.textHolder::getLine)
                .map(Component::getString).toArray(String[]::new);


    }

    public static void open(DoormatBlockTile sign) {
        Minecraft.getInstance().setScreen(new DoormatGui(sign));
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        this.textInputUtil.charTyped(codePoint);
        return true;
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.scrollText((int)delta);
        return true;
    }

    public void scrollText(int amount){
        this.editLine = Math.floorMod(this.editLine - amount, DoormatBlockTile.MAX_LINES);
        this.textInputUtil.setCursorToEnd();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // up arrow
        if (keyCode == 265) {
            this.scrollText(1);
            return true;
        }
        // !down arrow, !enter, !enter, handles special keys
        else if (keyCode != 264 && keyCode != 257 && keyCode != 335) {
            return this.textInputUtil.keyPressed(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
        }
        // down arrow, enter
        else {
            this.scrollText(-1);
            return true;
        }
    }

    @Override
    public void tick() {
        ++this.updateCounter;
        if (!this.tileSign.getType().isValid(this.tileSign.getBlockState())) {
            this.close();
        }
    }


    @Override
    public void onClose() {
        this.close();
    }

    @Override
    public void removed() {
        this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
        // send new text to the server
        NetworkHandler.INSTANCE.sendToServer(new ServerBoundSetTextHolderPacket(this.tileSign.getBlockPos(), this.tileSign.getTextHolder()));
        //this.tileSign.setEditable(true);
    }

    private void close() {
        this.tileSign.setChanged();
        this.minecraft.setScreen(null);
    }

    @Override
    protected void init() {

        this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
        this.addWidget(new Button(this.width / 2 - 100, this.height / 4 + 120, 200, 20, CommonComponents.GUI_DONE, (button) -> this.close()));
        //this.tileSign.setEditable(false);
        this.textInputUtil = new TextFieldHelper(() -> this.cachedLines[this.editLine], (h) -> {
            this.cachedLines[this.editLine] = h;
            this.tileSign.textHolder.setLine(this.editLine, new TextComponent(h));
        }, TextFieldHelper.createClipboardGetter(this.minecraft), TextFieldHelper.createClipboardSetter(this.minecraft),
                (s) -> this.minecraft.font.width(s) <= DoormatBlockTileRenderer.LINE_MAX_WIDTH);
    }

    @Override

    public void render(PoseStack matrixstack, int  mouseX, int mouseY, float partialTicks) {
        Lighting.setupForFlatItems();
        this.renderBackground(matrixstack);
        drawCenteredString(matrixstack, this.font, this.title, this.width / 2, 40, 16777215);


        MultiBufferSource.BufferSource bufferSource = this.minecraft.renderBuffers().bufferSource();

        matrixstack.pushPose();

        matrixstack.translate((this.width / 2d), 0.0D, 50.0D);
        matrixstack.scale(93.75F, -93.75F, 93.75F);
        matrixstack.translate(0.0D, -1.25D, 0.0D);

        // renders sign
        matrixstack.pushPose();

        matrixstack.mulPose(Const.Y90);
        matrixstack.translate(0, - 0.5, -0.5);
        matrixstack.mulPose(Const.Z90);

        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        BlockState state = this.tileSign.getBlockState().getBlock().defaultBlockState().setValue(DoormatBlock.FACING, Direction.EAST);
        blockRenderer.renderSingleBlock(state, matrixstack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY, EmptyModelData.INSTANCE);

        matrixstack.popPose();


        //renders text
        boolean blink = this.updateCounter / 6 % 2 == 0;

        matrixstack.translate(0, 0.0625-2*0.010416667F, 0.0625 + 0.005);
        matrixstack.scale(0.010416667F, -0.010416667F, 0.010416667F);

        TextUtil.renderGuiText(this.tileSign.textHolder, this.cachedLines, this.font, matrixstack, bufferSource,
                this.textInputUtil.getCursorPos(), this.textInputUtil.getSelectionPos(), this.editLine, blink, DoormatBlockTileRenderer.LINE_SEPARATION);

        matrixstack.popPose();
        Lighting.setupFor3DItems();
        super.render(matrixstack, mouseX, mouseY, partialTicks);
    }
}

