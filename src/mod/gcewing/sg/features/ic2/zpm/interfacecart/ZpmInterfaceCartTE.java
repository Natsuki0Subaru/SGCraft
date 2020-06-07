package gcewing.sg.features.ic2.zpm.interfacecart;

import static gcewing.sg.BaseUtils.min;
import static gcewing.sg.features.ic2.zpm.interfacecart.ZpmInterfaceCart.ZPM_LOADED;

import gcewing.sg.BaseTileInventory;
import gcewing.sg.features.zpm.ZPMItem;
import gcewing.sg.interfaces.ISGEnergySource;
import gcewing.sg.SGCraft;
import ic2.api.energy.prefab.BasicSource;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.energy.tile.IMultiEnergySource;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public final class ZpmInterfaceCartTE extends BaseTileInventory implements IMultiEnergySource, ISGEnergySource, IInventory, ITickable {
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    public final ZpmInterfaceCartBasicSource source;
    public static final int firstZpmSlot = 0;
    public static final int numZpmSlots = 1;
    public static final int numSlots = numZpmSlots; // future usage > 1
    public long zpmSlot0Energy = 0;

    private double energyPerSGEnergyUnit = 80;
    private int update = 0;

    public ZpmInterfaceCartTE() {
        this.source = new ZpmInterfaceCartBasicSource(this, 512, 3);
    }

    /* TileEntity */

    @Override
    public void readFromNBT(final NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.source.readFromNBT(compound);
        this.zpmSlot0Energy = compound.getLong("zpmSlot0Energy");
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound compound) {
        super.writeToNBT(compound);
        this.source.writeToNBT(compound);
        compound.setLong("zpmSlot0Energy", zpmSlot0Energy);
        return compound;
    }

    @Override
    public void update() {
        if (this.world == null || this.world.isRemote) {
            return;
        }

        this.source.update();
    }

    @Override
    public void onChunkUnload() {
        this.source.onChunkUnload();
    }

    @Override
    public void invalidate() {
        super.invalidate(); // this is important for mc!
        this.source.invalidate(); // notify the energy hubSource
    }

    @Override
    @Nonnull
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, this.getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        final NBTTagCompound result = new NBTTagCompound();
        this.writeToNBT(result);
        return result;
    }

    @Override
    public void onDataPacket(final NetworkManager net, final SPacketUpdateTileEntity packet) {
        final NBTTagCompound tag = packet.getNbtCompound();
        this.readFromNBT(tag);
    }

    /* Energy */

    @Override
    public double availableEnergy() {
        return this.zpmSlot0Energy / 640.0;
    }

    @Override
    public double totalAvailableEnergy() {
        return this.source.getCapacity();
    }

    @Override
    public double drawEnergyDouble(double amount) {
        double available = this.source.getEnergyStored();
        double supply = min(amount, available);
        this.source.drawEnergy(supply * energyPerSGEnergyUnit);

        if (isTainted(this.getStackInSlot(0))) {
            world.newExplosion(null, this.pos.getX(), this.pos.getY(), this.pos.getZ(), (float)250, true, true);
        }

        markChanged();
        return amount;
    }

    @Override
    public double getOfferedEnergy() {
        return (zpmSlot0Energy > 0) ? 512 : 0;
    }

    @Override
    public void drawEnergy(double v) {
        if (isTainted(this.getStackInSlot(0))) {
            world.newExplosion(null, this.pos.getX(), this.pos.getY(), this.pos.getZ(), (float)250, true, true);
        }

        markChanged();
    }

    @Override
    public int getSourceTier() {
        return this.source.getSourceTier();
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor iEnergyAcceptor, EnumFacing enumFacing) {
        if (isTainted(this.getStackInSlot(0))) {
            world.newExplosion(null, this.pos.getX(), this.pos.getY(), this.pos.getZ(), (float)250, true, true);
        }

        markChanged();
        return this.source.emitsEnergyTo(iEnergyAcceptor, enumFacing);
    }

    /* Inventory */

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    protected IInventory getInventory() {
        return this;
    }

    @Override
    public boolean isEmpty() {
        for(final ItemStack item : this.items) {
            if(!item.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(final int index) {
        return this.items.get(0);
    }

    @Override // This prevents the zpm from being input/extract from the console.
    public int[] getSlotsForFace(EnumFacing side) {
        return new int[0];
    }

    @Override
    public ItemStack removeStackFromSlot(final int index) {
        final ItemStack item = ItemStackHelper.getAndSplit(this.items, 0, 1);
        NBTTagCompound tag = item.getTagCompound();

        if(tag == null) {
            tag = new NBTTagCompound();
            item.setTagCompound(tag);
        }

        if(tag.hasKey(ZPMItem.ENERGY, 99 /* number */)) {
            tag.setDouble(ZPMItem.ENERGY, this.source.getEnergyStored());
            tag.setBoolean(ZPMItem.LOADED, false);
            this.source.setEnergyStored(0);
        }

        if (world != null) {
            IBlockState other = world.getBlockState(pos).withProperty(ZPM_LOADED, false);
            world.setBlockState(pos, other, 3);
            markChanged();
        }

        return ItemStackHelper.getAndRemove(this.items, 0);
    }

    @Override
    public ItemStack decrStackSize(final int index, final int quantity) {
        final ItemStack item = ItemStackHelper.getAndRemove(this.items, 0);

        NBTTagCompound tag = item.getTagCompound();

        if(tag == null) {
            tag = new NBTTagCompound();
            item.setTagCompound(tag);
        }

        if(tag != null && tag.hasKey(ZPMItem.ENERGY, 99 /* number */)) {
            tag.setDouble(ZPMItem.ENERGY, zpmSlot0Energy / 640.0);
            tag.setBoolean(ZPMItem.LOADED, false);
            this.source.setEnergyStored(0);
        }

        if (world != null) {
            IBlockState other = world.getBlockState(pos).withProperty(ZPM_LOADED, false);
            world.setBlockState(pos, other, 3);
            markChanged();
        }

        return item;
    }

    @Override
    public void setInventorySlotContents(final int index, final ItemStack item) {
        this.items.set(0, item);
        if (isValidFuelItem(item)) {
            NBTTagCompound tag = item.getTagCompound();

            if (tag == null) {
                tag = new NBTTagCompound();
                item.setTagCompound(tag);
            }

            tag.setBoolean(ZPMItem.LOADED, true);

            if (!tag.hasKey(ZPMItem.ENERGY, 99 /* number */)) {
                tag.setDouble(ZPMItem.ENERGY, Integer.MAX_VALUE);
                this.zpmSlot0Energy = ((long)tag.getDouble(ZPMItem.ENERGY)) * 640;
                this.source.setCapacity(512);
                this.source.setEnergyStored(512);
            } else {
                this.source.setEnergyStored(tag.getDouble(ZPMItem.ENERGY) == 0 ? 0 : 512);
                this.zpmSlot0Energy = ((long)tag.getDouble(ZPMItem.ENERGY)) * 640;
            }
        }

        if (world != null){ // This will be null both on the server AND client at time, no idea why....
            IBlockState other = world.getBlockState(pos).withProperty(ZPM_LOADED, isValidFuelItem(item));
            world.setBlockState(pos, other, 3);
            markChanged();
        }
    }

    public static boolean isValidFuelItem(ItemStack stack) {
        return stack != null && stack.getItem() == SGCraft.zpm && stack.getCount() > 0;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUsableByPlayer(final EntityPlayer player) {
        return player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }


    @Override
    public boolean isItemValidForSlot(final int index, final ItemStack item) {
        return item.getItem() instanceof ZPMItem;
    }

    @Override
    public int getField(final int id) {
        return 0;
    }

    @Override
    public void setField(final int id, final int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        this.items.clear();
    }

    @Override
    public String getName() {
        return "container.zero_point_module";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString("ZPM Container");
    }

    public static ZpmInterfaceCartTE at(IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof ZpmInterfaceCartTE ? (ZpmInterfaceCartTE) te : null;
    }

    public boolean isTainted(ItemStack item) {
        boolean hasTaint = false;
        NBTTagList nbttaglist = item.getEnchantmentTagList();
        for (int j = 0; j < nbttaglist.tagCount(); ++j) {
            NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(j);
            int k = nbttagcompound.getShort("id");
            int l = nbttagcompound.getShort("lvl");
            Enchantment enchantment = Enchantment.getEnchantmentByID(k);
            if (k == 51) {
                hasTaint = true;
            }
        }
        return hasTaint;
    }

    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        if (oldState.getBlock() != newState.getBlock()) { // Prevents the TE from nullifying itself when we force change the state to change it models.  Vanilla mechanics invalidate the TE.
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean sendMultipleEnergyPackets() {
        return this.source.sendMultipleEnergyPackets();
    }

    @Override
    public int getMultipleEnergyPacketAmount() {
        return this.source.getMultipleEnergyPacketAmount();
    }
}
