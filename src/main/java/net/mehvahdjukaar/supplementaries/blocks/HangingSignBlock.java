package net.mehvahdjukaar.supplementaries.blocks;

import net.mehvahdjukaar.supplementaries.common.CommonUtil;
import net.mehvahdjukaar.supplementaries.gui.HangingSignGui;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;


public class HangingSignBlock extends Block {
    public static final DirectionProperty FACING = HorizontalBlock.HORIZONTAL_FACING;
    public static final BooleanProperty TILE = CommonUtil.TILE; // is it renderer by tile entity? animated part
    public static final IntegerProperty EXTENSION = CommonUtil.EXTENSION;
    public HangingSignBlock(Properties properties) {
        super(properties);
        this.setDefaultState(this.stateContainer.getBaseState().with(EXTENSION, 0).with(FACING, Direction.NORTH).with(TILE, false));
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos) {
        return true;
    }

    //for player bed spawn
    @Override
    public boolean canSpawnInBlock() {
        return true;
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
                                             BlockRayTraceResult hit) {
        TileEntity tileentity = worldIn.getTileEntity(pos);
        if (tileentity instanceof HangingSignBlockTile) {
            HangingSignBlockTile te = (HangingSignBlockTile) tileentity;
            ItemStack itemstack = player.getHeldItem(handIn);
            boolean server = !worldIn.isRemote();
            boolean emptyhand = itemstack.isEmpty();
            boolean flag = itemstack.getItem() instanceof DyeItem && player.abilities.allowEdit;
            boolean flag1 = te.isEmpty() && !emptyhand;
            boolean flag2 = !te.isEmpty() && emptyhand;
            //color
            if (flag){
                if(te.setTextColor(((DyeItem) itemstack.getItem()).getDyeColor())){
                    if (!player.isCreative()) {
                        itemstack.shrink(1);
                    }
                    if(server){
                        te.markDirty();
                    }
                    return ActionResultType.SUCCESS;
                }
            }
            //not an else to allow to place dye items after coloring
            //place item
            if (flag1) {
                ItemStack it = (ItemStack) itemstack.copy();
                it.setCount((int) 1);
                NonNullList<ItemStack> stacks = NonNullList.<ItemStack>withSize(1, it);
                te.setItems(stacks);
                if (!player.isCreative()) {
                    itemstack.shrink(1);
                }
                if (!worldIn.isRemote()) {
                    worldIn.playSound((PlayerEntity) null, pos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 1.0F,
                            worldIn.rand.nextFloat() * 0.10F + 0.95F);
                    te.markDirty();
                }
                return ActionResultType.SUCCESS;
            }
            //remove item
            else if (flag2) {
                ItemStack it = te.removeStackFromSlot(0);
                player.setHeldItem(handIn, it);
                if (!worldIn.isRemote()) {
                    te.markDirty();
                }
                return ActionResultType.SUCCESS;
            }
            // open gui (edit sign with empty hand)
            else if (player instanceof PlayerEntity && !server && emptyhand) {
                HangingSignGui.open(te);
                return ActionResultType.SUCCESS;
            }
        }
        return ActionResultType.PASS;
    }

    public boolean isValidPosition(BlockState state, IWorldReader worldIn, BlockPos pos) {
        return worldIn.getBlockState(pos.offset(state.get(FACING).getOpposite())).getMaterial().isSolid();
    }

    @Override
    public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos,
                                          BlockPos facingPos) {
        return facing == stateIn.get(FACING).getOpposite() && !stateIn.isValidPosition(worldIn, currentPos)
                ? Blocks.AIR.getDefaultState()
                : super.updatePostPlacement(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        switch (state.get(FACING)) {
            case UP :
            case DOWN :
            case SOUTH :
            default :
                return VoxelShapes.create(0.5625D, 0D, 1D, 0.4375D, 1D, 0D);
            case NORTH :
                return VoxelShapes.create(0.4375D, 0D, 0D, 0.5625D, 1D, 1D);
            case WEST :
                return VoxelShapes.create(0D, 0D, 0.5625D, 1D, 1D, 0.4375D);
            case EAST :
                return VoxelShapes.create(1D, 0D, 0.4375D, 0D, 1D, 0.5625D);
        }
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING, TILE, EXTENSION);
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.with(FACING, rot.rotate(state.get(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.toRotation(state.get(FACING)));
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        super.onEntityCollision(state, world, pos, entity);

        TileEntity tileentity = world.getTileEntity(pos);
        if (tileentity instanceof HangingSignBlockTile) {
            ((HangingSignBlockTile) tileentity).counter = 0;
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        if (context.getFace() == Direction.UP || context.getFace() == Direction.DOWN)
            return this.getDefaultState().with(FACING, Direction.NORTH);
        BlockPos blockpos = context.getPos();
        IBlockReader world = context.getWorld();
        Block block = world.getBlockState(blockpos.offset(context.getFace().getOpposite())).getBlock();

        int flag = 0;
        if(block instanceof FenceBlock || block instanceof SignPostBlock) flag = 1;
        else if(block instanceof WallBlock) flag = 2;
        return this.getDefaultState().with(FACING, context.getFace()).with(EXTENSION, flag);
    }

    @Override
    public PathNodeType getAiPathNodeType(BlockState state, IBlockReader world, BlockPos pos, MobEntity entity) {
        return PathNodeType.OPEN;
    }

    @Override
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            TileEntity tileentity = world.getTileEntity(pos);
            if (tileentity instanceof HangingSignBlockTile) {
                InventoryHelper.dropInventoryItems(world, pos, (HangingSignBlockTile) tileentity);
                world.updateComparatorOutputLevel(pos, this);
            }
            super.onReplaced(state, world, pos, newState, isMoving);
        }
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new HangingSignBlockTile();
    }

    @Override
    public boolean eventReceived(BlockState state, World world, BlockPos pos, int eventID, int eventParam) {
        super.eventReceived(state, world, pos, eventID, eventParam);
        TileEntity tileentity = world.getTileEntity(pos);
        return tileentity == null ? false : tileentity.receiveClientEvent(eventID, eventParam);
    }
}
