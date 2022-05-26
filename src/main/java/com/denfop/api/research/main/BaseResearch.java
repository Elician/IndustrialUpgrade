package com.denfop.api.research.main;

import net.minecraft.item.ItemStack;

public class BaseResearch implements IResearch{

    public final String name;
    public final boolean unique;
    public final int minlevel;
    public final int points;
    public final IResearchPages page;
    public final boolean depends;
    public final IResearch dependencies;
    public final ItemStack itemstack;

    public BaseResearch(String name, IResearchPages page, boolean unique,ItemStack itemstack){
        this(name,page,0,0,unique,itemstack);

    }
    public BaseResearch(String name, IResearchPages page,int minlevel, int points, boolean unique,ItemStack itemstack){
        this(name,page,minlevel,points,unique,null,itemstack);
    }
    public BaseResearch(String name, IResearchPages page,int minlevel, int points,ItemStack itemstack){
        this(name,page,minlevel,points,false,null,itemstack);
    }
    public BaseResearch(String name, IResearchPages page,int minlevel, int points,IResearch research,ItemStack itemstack){
        this(name,page,minlevel,points,false,research,itemstack);
    }
    public BaseResearch(String name, IResearchPages page,int minlevel, int points,boolean unique, IResearch research,ItemStack itemstack){
        this.name= name;
        this.unique=unique;
        this.minlevel = minlevel;
        this.points = points;
        this.page = page;
        this.depends= research != null;
        this.dependencies =research;
        this.itemstack = itemstack;
    }
    public BaseResearch(String name, IResearchPages page,ItemStack itemstack){
        this(name,page,0,0,false,itemstack);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getMinLevel() {
        return this.minlevel;
    }

    @Override
    public int pointsPractise() {
        return this.points;
    }

    @Override
    public boolean IsUnique() {
        return this.unique;
    }

    @Override
    public boolean dependsOnOther() {
        return this.depends;
    }

    @Override
    public IResearch getDependencies() {
        return this.dependencies;
    }

    @Override
    public IResearchPages getResearchPage() {
        return this.page;
    }

    @Override
    public ItemStack getIcon() {
        return this.itemstack;
    }

}
