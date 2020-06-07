package gcewing.sg.features.ic2.zpm.interfacecart;

import ic2.api.energy.prefab.BasicSource;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IMultiEnergySource;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

public final class ZpmInterfaceCartBasicSource extends BasicSource implements IMultiEnergySource {

    private final TileEntity parent;

    public ZpmInterfaceCartBasicSource(final TileEntity parent, final double capacity, final int tier) {
        super(parent, capacity, tier);
        this.parent = parent;
    }

    @Override
    public void drawEnergy(double amount) {
        ZpmInterfaceCartTE te = ((ZpmInterfaceCartTE) this.parent);
        if (te != null) {

            te.drawEnergy(amount);

            if(te.zpmSlot0Energy > 0)
                te.zpmSlot0Energy -= 10.0;

            te.markChanged();

            if (te.isTainted(((ZpmInterfaceCartTE)this.parent).getStackInSlot(0))) {
                world.newExplosion(null, this.pos.getX(), this.pos.getY(), this.pos.getZ(), (float)250, true, true);
            }
        }
    }

    @Override
    public double getOfferedEnergy(){
        ZpmInterfaceCartTE te = ((ZpmInterfaceCartTE) this.parent);
        if(te == null) {
            return 0;
        }

        if(te.zpmSlot0Energy > 0) {
            return 512;
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
        return 333;
    }
}
