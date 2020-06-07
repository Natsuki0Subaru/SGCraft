package gcewing.sg.features.ic2.zpm.modulehub;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.prefab.BasicSource;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IMultiEnergySource;
import ic2.api.info.Info;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

public final class ZpmHubBasicSource extends BasicSource implements IMultiEnergySource  {

    private final TileEntity parent;
    private int tier;
    private boolean addedToEnet;

    public ZpmHubBasicSource(final TileEntity parent, final int tier) {
        super(parent, 32768, 3);
        this.parent = parent;
        this.tier = tier;
        this.addedToEnet = false;
    }

    @Override
    public void drawEnergy(double amount) {
        ZpmHubTE te = ((ZpmHubTE) this.parent);
        if (te != null) {
            te.drawEnergy(amount);

            int zpmCount = ((ZpmHubTE) this.parent).getZpmSlotsloaded();

            double drawAmount = 30;
            int perZpmDrawAmount = (int) (drawAmount / zpmCount);

            if (te.zpmSlot0Energy > 0) {
                te.zpmSlot0Energy = te.zpmSlot0Energy - perZpmDrawAmount;
            }
            if (te.zpmSlot1Energy > 0) {
                te.zpmSlot1Energy = te.zpmSlot1Energy - perZpmDrawAmount;
            }
            if (te.zpmSlot2Energy > 0) {
                te.zpmSlot2Energy = te.zpmSlot2Energy - perZpmDrawAmount;
            }

            te.markChanged();
            // Todo: fix this.
            /*
            if (((ZpmHubTE)this.parent).isTainted(((ZpmHubTE)this.parent).getStackInSlot(0))) {
                world.newExplosion(null, this.pos.getX(), this.pos.getY(), this.pos.getZ(), (float)250, true, true);
            } */
        }
    }

    @Override
    public int getSourceTier() {
        return tier;
    }

    public void setSourceTier(int tier){
        this.tier = tier;
    }

    @Override
    public double getOfferedEnergy(){
        ZpmHubTE te = ((ZpmHubTE) this.parent);
        if(te == null) {
            return 0;
        }

        if(te.zpmSlot0Energy + te.zpmSlot1Energy + te.zpmSlot1Energy > 0) {
            return 32768;
        }

        return 0;
    }

    public double getCapacity(){
        return getOfferedEnergy();
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor iEnergyAcceptor, EnumFacing direction) {
        // Todo: math needed to only output energy on one side of the zpm interface cart
        return true;
    }

    @Override
    public boolean sendMultipleEnergyPackets() {
        return true;
    }

    @Override
    public int getMultipleEnergyPacketAmount() {
        return 1024;
    }

    public double getEnergyStored(){
        ZpmHubTE te = ((ZpmHubTE) this.parent);
        return ((te.zpmSlot0Energy + te.zpmSlot1Energy + te.zpmSlot2Energy) * 30.0) * 32768;
    }

    public void update() {
        if (!this.addedToEnet) {
            this.onLoad();
        }
    }

    public void readFromNBT(NBTTagCompound tag) {
        NBTTagCompound data = tag.getCompoundTag(this.getNbtTagName());
    }

    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        NBTTagCompound data = new NBTTagCompound();
        tag.setTag(this.getNbtTagName(), data);
        return tag;
    }

    protected String getNbtTagName(){
        return "ZpmHubSource";
    }
}
