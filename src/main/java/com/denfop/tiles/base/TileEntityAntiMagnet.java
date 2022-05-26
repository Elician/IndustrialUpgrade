package com.denfop.tiles.base;

import com.denfop.tiles.mechanism.TileEntityMagnet;
import ic2.core.ExplosionIC2;
import ic2.core.block.TileEntityInventory;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.List;

public class TileEntityAntiMagnet extends TileEntityInventory {
    public String player;
    public TileEntityAntiMagnet(){
        this.player = "";
    }

    @Override
    protected boolean onActivated(
            final EntityPlayer player,
            final EnumHand hand,
            final EnumFacing side,
            final float hitX,
            final float hitY,
            final float hitZ
    ) {
        return false;
    }
    private static final List<AxisAlignedBB> aabbs = Collections.singletonList(new AxisAlignedBB(0, 0.0D, 0, 1, 1.4D, 1));

    protected List<AxisAlignedBB> getAabbs(boolean forCollision) {
        return aabbs;
    }

    @Override
    public void onPlaced(final ItemStack stack, final EntityLivingBase placer, final EnumFacing facing) {
        super.onPlaced(stack, placer, facing);
        if(placer instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) placer;
            this.player = player.getName();
            for (int x = this.pos.getX() - 10; x <= this.pos.getX() + 10; x++) {
                for (int y = this.pos.getY() - 10; y <= this.pos.getY() + 10; y++) {
                    for (int z = this.pos.getZ() - 10; z <= this.pos.getZ() + 10; z++) {
                        if (getWorld().getTileEntity(new BlockPos(x, y, z)) != null && !(new BlockPos(
                                x,
                                y,
                                z
                        ).equals(this.pos))) {
                            if (getWorld().getTileEntity(new BlockPos(x, y, z)) instanceof TileEntityMagnet) {
                                TileEntityMagnet tile = (TileEntityMagnet) getWorld().getTileEntity(new BlockPos(x, y, z));
                                if(!tile.player.equals(this.player)){
                                    tile.work= false;
                                }
                            }
                        }
                    }
                }
            }

        }else{
            final ExplosionIC2 explosion = new ExplosionIC2(this.world, null, pos.getX(), pos.getY(), pos.getZ(),
                    1,
                    1f);
            explosion.doExplosion();
        }
    }
    public void readFromNBT(NBTTagCompound nbttagcompound) {
        super.readFromNBT(nbttagcompound);
        this.player = nbttagcompound.getString("player");
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound) {
        super.writeToNBT(nbttagcompound);
        nbttagcompound.setString("player", this.player);
        return nbttagcompound;
    }
}
