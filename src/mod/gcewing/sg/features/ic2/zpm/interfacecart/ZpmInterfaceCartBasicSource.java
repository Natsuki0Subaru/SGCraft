package gcewing.sg.features.ic2.zpm.interfacecart;

import ic2.api.energy.prefab.BasicSource;
import ic2.api.energy.tile.IEnergyAcceptor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

public final class ZpmInterfaceCartBasicSource extends BasicSource {

    private final TileEntity parent;

    public ZpmInterfaceCartBasicSource(final TileEntity parent, final double capacity, final int tier) {
        super(parent, capacity, tier);
        this.parent = parent;
    }

    @Override
    public void drawEnergy(double amount) {
        super.drawEnergy(amount);
        if (this.parent instanceof ZpmInterfaceCartTE) {
            ((ZpmInterfaceCartTE) this.parent).markChanged();
            if (((ZpmInterfaceCartTE)this.parent).isTainted(((ZpmInterfaceCartTE)this.parent).getStackInSlot(0))) {
                world.newExplosion(null, this.pos.getX(), this.pos.getY(), this.pos.getZ(), (float)250, true, true);
            }
        }
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor iEnergyAcceptor, EnumFacing direction) {
        // Todo: math needed to only output energy on one side of the zpm interface cart
        return true;
    }
}
