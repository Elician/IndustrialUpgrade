package com.denfop.componets;

import com.denfop.api.qe.IQEAcceptor;
import com.denfop.api.qe.IQEDual;
import com.denfop.api.qe.IQEEmitter;
import com.denfop.api.qe.IQESink;
import com.denfop.api.qe.IQESource;
import com.denfop.api.qe.IQETile;
import com.denfop.api.qe.event.QETileLoadEvent;
import com.denfop.api.qe.event.QETileUnloadEvent;
import ic2.core.IC2;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.comp.TileEntityComponent;
import ic2.core.block.invslot.InvSlot;
import ic2.core.network.GrowingBuffer;
import ic2.core.util.LogCategory;
import ic2.core.util.Util;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class QEComponent extends TileEntityComponent {

    public static final boolean debugLoad = System.getProperty("ic2.comp.energy.debugload") != null;
    public final World world;
    public final boolean fullEnergy;
    public double capacity;
    public double storage;
    public int sinkTier;
    public int sourceTier;
    public Set<EnumFacing> sinkDirections;
    public Set<EnumFacing> sourceDirections;
    public List<InvSlot> managedSlots;
    public boolean multiSource;
    public int sourcePackets;
    public QEComponent.EnergyNetDelegate delegate;
    public boolean loaded;
    public boolean receivingDisabled;
    public boolean sendingSidabled;
    public double tick;
    protected double pastEnergy;
    protected double perenergy;
    private double perenergy1;
    private double pastEnergy1;
    private double tick1;

    public QEComponent(TileEntityBlock parent, double capacity) {
        this(parent, capacity, Collections.emptySet(), Collections.emptySet(), 1);
    }

    public QEComponent(
            TileEntityBlock parent,
            double capacity,
            Set<EnumFacing> sinkDirections,
            Set<EnumFacing> sourceDirections,
            int tier
    ) {
        this(parent, capacity, sinkDirections, sourceDirections, tier, tier, false);
    }

    public QEComponent(
            TileEntityBlock parent,
            double capacity,
            Set<EnumFacing> sinkDirections,
            Set<EnumFacing> sourceDirections,
            int sinkTier,
            int sourceTier,
            boolean fullEnergy
    ) {
        super(parent);
        this.multiSource = false;
        this.sourcePackets = 1;
        this.capacity = capacity;
        this.sinkTier = sinkTier;
        this.sourceTier = sourceTier;
        this.sinkDirections = sinkDirections;
        this.sourceDirections = sourceDirections;
        this.fullEnergy = fullEnergy;
        this.world = parent.getWorld();
        this.pastEnergy = 0;
        this.perenergy = 0;
        this.tick = 0;
    }

    public static QEComponent asBasicSink(TileEntityBlock parent, double capacity) {
        return asBasicSink(parent, capacity, 1);
    }

    public static QEComponent asBasicSink(TileEntityBlock parent, double capacity, int tier) {
        return new QEComponent(parent, capacity, Util.allFacings, Collections.emptySet(), tier);
    }

    public static QEComponent asBasicSource(TileEntityBlock parent, double capacity) {
        return asBasicSource(parent, capacity, 1);
    }

    public static QEComponent asBasicSource(TileEntityBlock parent, double capacity, int tier) {
        return new QEComponent(parent, capacity, Collections.emptySet(), Util.allFacings, tier);
    }

    public void readFromNbt(NBTTagCompound nbt) {
        this.storage = nbt.getDouble("storage");
    }

    public NBTTagCompound writeToNbt() {
        NBTTagCompound ret = new NBTTagCompound();
        ret.setDouble("storage", this.storage);
        return ret;
    }

    public void onLoaded() {
        assert this.delegate == null;

        if (!this.parent.getWorld().isRemote) {
            if (this.sinkDirections.isEmpty() && this.sourceDirections.isEmpty()) {
                if (debugLoad) {
                    IC2.log.debug(LogCategory.Component, "Skipping Energy onLoaded for %s at %s.",
                            this.parent,
                            Util.formatPosition(this.parent)
                    );
                }
            } else {
                if (debugLoad) {
                    IC2.log.debug(LogCategory.Component, "Energy onLoaded for %s at %s.",
                            this.parent,
                            Util.formatPosition(this.parent)
                    );
                }

                this.createDelegate();
                MinecraftForge.EVENT_BUS.post(new QETileLoadEvent(this.delegate, this.parent.getWorld()));
            }

            this.loaded = true;
        }

    }

    private void createDelegate() {
        if (this.delegate != null) {
            throw new IllegalStateException();
        } else {
            assert !this.sinkDirections.isEmpty() || !this.sourceDirections.isEmpty();

            if (this.sinkDirections.isEmpty()) {
                this.delegate = new QEComponent.EnergyNetDelegateSource();
            } else if (this.sourceDirections.isEmpty()) {
                this.delegate = new QEComponent.EnergyNetDelegateSink();
            } else {
                this.delegate = new QEComponent.EnergyNetDelegateDual();
            }

            this.delegate.setWorld(this.parent.getWorld());
            this.delegate.setPos(this.parent.getPos());
        }
    }

    public void onUnloaded() {
        if (this.delegate != null) {
            if (debugLoad) {
                IC2.log.debug(LogCategory.Component, "Energy onUnloaded for %s at %s.",
                        this.parent,
                        Util.formatPosition(this.parent)
                );
            }

            MinecraftForge.EVENT_BUS.post(new QETileUnloadEvent(this.delegate, this.parent.getWorld()));
            this.delegate = null;
        } else if (debugLoad) {
            IC2.log.debug(LogCategory.Component, "Skipping Energy onUnloaded for %s at %s.",
                    this.parent,
                    Util.formatPosition(this.parent)
            );
        }

        this.loaded = false;
    }

    public void onContainerUpdate(EntityPlayerMP player) {
        GrowingBuffer buffer = new GrowingBuffer(16);
        buffer.writeDouble(this.capacity);
        buffer.writeDouble(this.storage);
        buffer.flip();
        this.setNetworkUpdate(player, buffer);
    }

    public void onNetworkUpdate(DataInput is) throws IOException {
        this.capacity = is.readDouble();
        this.storage = is.readDouble();
    }

    public boolean enableWorldTick() {
        return !this.parent.getWorld().isRemote && this.managedSlots != null;
    }

    public void onWorldTick() {


    }

    public double getCapacity() {
        return this.capacity;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public double getEnergy() {
        return this.storage;
    }

    public double getFreeEnergy() {
        return Math.max(0.0D, this.capacity - this.storage);
    }

    public double getFillRatio() {
        return this.storage / this.capacity;
    }


    public double addEnergy(double amount) {
        amount = Math.min(this.capacity - this.storage, amount);
        this.storage += amount;
        return amount;
    }


    public boolean canUseEnergy(double amount) {
        return this.storage >= amount;
    }

    public boolean useEnergy(double amount) {
        if (this.storage >= amount) {
            this.storage -= amount;
            return true;
        } else {
            return false;
        }
    }

    public double useEnergy(double amount, boolean simulate) {
        double ret = Math.abs(Math.max(0.0D, amount - this.storage) - amount);
        if (!simulate) {
            this.storage -= ret;
        }
        return ret;
    }

    public int getSinkTier() {
        return this.sinkTier;
    }

    public void setSinkTier(int tier) {
        this.sinkTier = tier;
    }

    public int getSourceTier() {
        return this.sourceTier;
    }

    public void setSourceTier(int tier) {
        this.sourceTier = tier;
    }

    public void setEnabled(boolean enabled) {
        this.receivingDisabled = this.sendingSidabled = !enabled;
    }

    public void setReceivingEnabled(boolean enabled) {
        this.receivingDisabled = !enabled;
    }

    public void setSendingEnabled(boolean enabled) {
        this.sendingSidabled = !enabled;
    }


    public void setDirections(Set<EnumFacing> sinkDirections, Set<EnumFacing> sourceDirections) {

        if (this.delegate != null) {
            if (debugLoad) {
                IC2.log.debug(
                        LogCategory.Component,
                        "Energy setDirections unload for %s at %s.",
                        this.parent, Util.formatPosition(this.parent)
                );
            }

            assert !this.parent.getWorld().isRemote;

            MinecraftForge.EVENT_BUS.post(new QETileUnloadEvent(this.delegate, this.world));
        }

        this.sinkDirections = sinkDirections;
        this.sourceDirections = sourceDirections;
        if (sinkDirections.isEmpty() && sourceDirections.isEmpty()) {
            this.delegate = null;
        } else if (this.delegate == null && this.loaded) {
            this.createDelegate();
        }

        if (this.delegate != null) {
            if (debugLoad) {
                IC2.log.debug(
                        LogCategory.Component,
                        "Energy setDirections load for %s at %s, sink: %s, source: %s.",
                        this.parent, Util.formatPosition(this.parent), sinkDirections, sourceDirections
                );
            }

            assert !this.parent.getWorld().isRemote;

            MinecraftForge.EVENT_BUS.post(new QETileLoadEvent(this.delegate, this.world));
        } else if (debugLoad) {
            IC2.log.debug(
                    LogCategory.Component,
                    "Skipping Energy setDirections load for %s at %s, sink: %s, source: %s, loaded: %b.",
                    this.parent, Util.formatPosition(this.parent), sinkDirections, sourceDirections, this.loaded
            );
        }


    }

    public Set<EnumFacing> getSourceDirs() {
        return Collections.unmodifiableSet(this.sourceDirections);
    }

    public Set<EnumFacing> getSinkDirs() {
        return Collections.unmodifiableSet(this.sinkDirections);
    }

    public IQETile getDelegate() {
        return this.delegate;
    }

    private double getSourceEnergy() {

        return this.storage;
    }

    private abstract static class EnergyNetDelegate extends TileEntity implements IQETile {

        private EnergyNetDelegate() {
        }

    }

    private class EnergyNetDelegateDual extends QEComponent.EnergyNetDelegate implements IQEDual {

        private EnergyNetDelegateDual() {
            super();
        }

        public boolean acceptsQEFrom(IQEEmitter emitter, EnumFacing dir) {
            return QEComponent.this.sinkDirections.contains(dir);
        }

        public boolean emitsQETo(IQEAcceptor receiver, EnumFacing dir) {
            return QEComponent.this.sourceDirections.contains(dir);
        }

        @Override
        public @NotNull BlockPos getBlockPos() {
            return QEComponent.this.parent.getPos();
        }

        public double getOfferedQE() {
            return !QEComponent.this.sendingSidabled && !QEComponent.this.sourceDirections.isEmpty()
                    ? QEComponent.this.getSourceEnergy()
                    : 0.0D;
        }

        public int getSinkTier() {
            return QEComponent.this.sinkTier;
        }

        public int getSourceTier() {
            return QEComponent.this.sourceTier;
        }

        @Override
        public double getDemandedQE() {
            return !QEComponent.this.receivingDisabled && !QEComponent.this.sinkDirections.isEmpty() && QEComponent.this.storage < QEComponent.this.capacity
                    ? QEComponent.this.capacity - QEComponent.this.storage
                    : 0.0D;

        }

        @Override
        public double injectQE(final EnumFacing var1, final double var2, final double var4) {
            QEComponent.this.storage = QEComponent.this.storage + var2;
            return 0.0D;
        }

        @Override
        public double getPerEnergy1() {
            return QEComponent.this.perenergy1;
        }

        @Override
        public double getPastEnergy1() {
            return QEComponent.this.pastEnergy1;
        }

        @Override
        public void setPastEnergy1(final double pastEnergy) {
            QEComponent.this.pastEnergy1 = pastEnergy;
        }

        @Override
        public void addPerEnergy1(final double setEnergy) {
            QEComponent.this.perenergy1 += setEnergy;
        }


        @Override
        public void addTick1(final double tick) {
            QEComponent.this.tick1 = tick;
        }

        @Override
        public double getTick1() {
            return QEComponent.this.tick1;
        }

        public void drawQE(double amount) {
            assert amount <= QEComponent.this.storage;

            QEComponent.this.storage = QEComponent.this.storage - amount;
        }


        @Override
        public double getPerEnergy() {
            return QEComponent.this.perenergy;
        }

        @Override
        public double getPastEnergy() {
            return QEComponent.this.pastEnergy;
        }

        @Override
        public void setPastEnergy(final double pastEnergy) {
            QEComponent.this.pastEnergy = pastEnergy;
        }

        @Override
        public void addPerEnergy(final double setEnergy) {
            QEComponent.this.perenergy += setEnergy;
        }

        @Override
        public boolean isSource() {
            return !QEComponent.this.sendingSidabled;
        }

        @Override
        public void addTick(final double tick) {
            QEComponent.this.tick = tick;
        }

        @Override
        public double getTick() {
            return QEComponent.this.tick;
        }

        @Override
        public boolean isSink() {
            return QEComponent.this.sendingSidabled;
        }

        @Override
        public TileEntity getTile() {
            return QEComponent.this.parent;
        }

    }

    private class EnergyNetDelegateSink extends QEComponent.EnergyNetDelegate implements IQESink {

        private EnergyNetDelegateSink() {
            super();
        }

        public int getSinkTier() {
            return QEComponent.this.sinkTier;
        }

        public boolean acceptsQEFrom(IQEEmitter emitter, EnumFacing dir) {
            return QEComponent.this.sinkDirections.contains(dir);
        }

        @Override
        public @NotNull BlockPos getBlockPos() {
            return QEComponent.this.parent.getPos();
        }

        public double getDemandedQE() {
            assert !QEComponent.this.sinkDirections.isEmpty();

            return !QEComponent.this.receivingDisabled && QEComponent.this.storage < QEComponent.this.capacity
                    ? QEComponent.this.capacity - QEComponent.this.storage
                    : 0.0D;
        }

        @Override
        public TileEntity getTile() {
            return QEComponent.this.parent;
        }

        public double injectQE(EnumFacing directionFrom, double amount, double voltage) {
            QEComponent.this.storage = QEComponent.this.storage + amount;
            return 0.0D;
        }

        @Override
        public double getPerEnergy() {
            return QEComponent.this.perenergy;
        }

        @Override
        public double getPastEnergy() {
            return QEComponent.this.pastEnergy;
        }

        @Override
        public void setPastEnergy(final double pastEnergy) {
            QEComponent.this.pastEnergy = pastEnergy;
        }

        @Override
        public void addPerEnergy(final double setEnergy) {
            QEComponent.this.perenergy += setEnergy;
        }

        @Override
        public void addTick(final double tick) {
            QEComponent.this.tick = tick;
        }

        @Override
        public double getTick() {
            return QEComponent.this.tick;
        }

        @Override
        public boolean isSink() {
            return true;
        }

    }

    private class EnergyNetDelegateSource extends QEComponent.EnergyNetDelegate implements IQESource {

        private EnergyNetDelegateSource() {
            super();
        }

        public int getSourceTier() {
            return QEComponent.this.sourceTier;
        }

        public boolean emitsQETo(IQEAcceptor receiver, EnumFacing dir) {
            return QEComponent.this.sourceDirections.contains(dir);
        }

        @Override
        public @NotNull BlockPos getBlockPos() {
            return QEComponent.this.parent.getPos();
        }

        public double getOfferedQE() {
            assert !QEComponent.this.sourceDirections.isEmpty();

            return !QEComponent.this.sendingSidabled ? QEComponent.this.getSourceEnergy() : 0.0D;
        }

        @Override
        public TileEntity getTile() {
            return QEComponent.this.parent;
        }

        public void drawQE(double amount) {
            assert amount <= QEComponent.this.storage;

            QEComponent.this.storage = QEComponent.this.storage - amount;
        }

        @Override
        public double getPerEnergy() {
            return QEComponent.this.perenergy;
        }

        @Override
        public double getPastEnergy() {
            return QEComponent.this.pastEnergy;
        }

        @Override
        public void setPastEnergy(final double pastEnergy) {
            QEComponent.this.pastEnergy = pastEnergy;
        }

        @Override
        public void addPerEnergy(final double setEnergy) {
            QEComponent.this.perenergy += setEnergy;
        }

        @Override
        public boolean isSource() {
            return true;
        }


    }

}
