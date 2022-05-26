package com.powerutils;

import com.denfop.IUCore;
import com.denfop.api.IModelRegister;
import com.powerutils.Constants;
import com.powerutils.PowerUtils;
import ic2.core.init.BlocksItems;
import ic2.core.item.ItemIC2;
import ic2.core.ref.ItemName;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PUItemBase extends ItemIC2 implements IModelRender {

    private final String name;
    private final String path;

    public PUItemBase(String name) {
        this(name, "");
    }

    public String getUnlocalizedName() {
        return "pu." + super.getUnlocalizedName().substring(4) + ".name";
    }

    public PUItemBase(String name, String path) {
        super(null);
        this.setCreativeTab(IUCore.ItemTab);
        this.setMaxStackSize(64);

        this.name = name;
        this.path = path;
        setUnlocalizedName(name);
        BlocksItems.registerItem((Item) this, PowerUtils.getIdentifier(name)).setUnlocalizedName(name);
        PowerUtils.addIModelRegister(this);
    }

    @Override
    public void registerModels() {
        registerModels(null);
    }

    @SideOnly(Side.CLIENT)
    protected void registerModel(final int meta, final ItemName name, final String extraName) {
        ModelLoader.setCustomModelResourceLocation(
                this,
                meta,
                new ModelResourceLocation(Constants.MOD_ID + ":" + path + this.name, null)
        );
    }

}
