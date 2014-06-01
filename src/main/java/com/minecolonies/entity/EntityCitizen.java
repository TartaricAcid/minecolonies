package com.minecolonies.entity;

import com.minecolonies.entity.ai.EntityAIGoHome;
import com.minecolonies.inventory.InventoryCitizen;
import com.minecolonies.lib.Constants;
import com.minecolonies.tileentities.TileEntityHut;
import com.minecolonies.tileentities.TileEntityHutWorker;
import com.minecolonies.tileentities.TileEntityTownHall;
import com.minecolonies.util.LanguageHandler;
import com.minecolonies.util.Utils;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.INpc;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInvBasic;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class EntityCitizen extends EntityAgeable implements IInvBasic, INpc
{
    public  ResourceLocation texture;
    //public  EnumCitizenLevel level;
    private String           job;
    private InventoryCitizen inventory;

    private TileEntityTownHall tileEntityTownHall;
    private int                townPosX, townPosY, townPosZ;
    private TileEntityHutWorker tileEntityWorkHut;
    private int                 workPosX, workPosY, workPosZ;
    private TileEntityHut tileEntityHomeHut;//TODO TileEntityHutCitizen
    int homePosX, homePosY, homePosZ;

    public EntityCitizen(World world)
    {
        super(world);
        setSize(0.6F, 1.8F);
        this.func_110163_bv();//Set persistenceRequired = true;
        //this.level = worldObj.rand.nextBoolean() ? EnumCitizenLevel.CITIZENMALE : EnumCitizenLevel.CITIZENFEMALE;
        setTexture();
        this.job = initJob();
        this.inventory = new InventoryCitizen("Minecolony Inventory", false, 27);

        this.getNavigator().setAvoidsWater(true);
        this.getNavigator().setEnterDoors(true);
        initTasks();
    }

    @Override
    public void entityInit()
    {
        super.entityInit();
        dataWatcher.addObject(13, worldObj.rand.nextInt(3) + 1);//textureID
        dataWatcher.addObject(14, 0);//level
        dataWatcher.addObject(15, worldObj.rand.nextInt(2));//sex
    }

    @Override
    public boolean interact(EntityPlayer player)
    {
        System.out.println(player.worldObj.isRemote + " " + getSex());
        return true;
    }

    protected void initTasks()
    {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(1, new EntityAIAvoidEntity(this, EntityMob.class, 8.0F, 0.6D, 0.6D));
        this.tasks.addTask(2, new EntityAIGoHome(this));
        //this.tasks.addTask(2, new EntityAISleep(this));
        this.tasks.addTask(3, new EntityAIOpenDoor(this, true));
        this.tasks.addTask(4, new EntityAIWatchClosest2(this, EntityPlayer.class, 3.0F, 1.0F));
        this.tasks.addTask(5, new EntityAIWatchClosest2(this, EntityCitizen.class, 5.0F, 0.02F));
        this.tasks.addTask(6, new EntityAIWander(this, 0.6D));
        this.tasks.addTask(7, new EntityAIWatchClosest(this, EntityLiving.class, 6.0F));
    }

    protected String initJob()
    {
        return "Citizen";
    }

    @Override
    public boolean isAIEnabled()
    {
        return true;
    }

    @Override
    public void onLivingUpdate()
    {
        this.setTexture();
        super.onLivingUpdate();
        updateTileEntities();
    }

    private void updateTileEntities()
    {
        if(tileEntityTownHall == null)
        {
            tileEntityTownHall = (TileEntityTownHall) worldObj.getTileEntity(townPosX, townPosY, townPosZ);
        }
        if(tileEntityWorkHut == null)
        {
            tileEntityWorkHut = (TileEntityHutWorker) worldObj.getTileEntity(workPosX, workPosY, workPosZ);
        }
        if(tileEntityHomeHut == null)
        {
            tileEntityHomeHut = (TileEntityHut) worldObj.getTileEntity(homePosX, homePosY, homePosZ);//TODO TileEntityHutCitizen
        }
    }

    @Override
    public EntityAgeable createChild(EntityAgeable var1)
    {
        //TODO ???
        return null;
    }

    @Override
    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(20.0d);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.5d);
    }

    @Override
    public void onDeath(DamageSource par1DamageSource)
    {
        if(this.getTownHall() != null)
        {
            LanguageHandler.sendPlayersLocalizedMessage(Utils.getPlayersFromUUID(worldObj, tileEntityTownHall.getOwners()), "tile.blockHutTownhall.messageColonistDead");

            tileEntityTownHall.removeCitizen(this);
        }
        //TODO addHome when CitizenHut is in game
        if(this.getWorkHut() != null)
        {
            this.removeFromWorkHut(this.getWorkHut());
        }
        super.onDeath(par1DamageSource);
    }

    public void setTexture()
    {
        String textureBase = "textures/entity/Entity";
        /*switch(getLevel())
        {
            case 0:
                textureBase += "Settler";
                break;
            case 1:
                textureBase += "Citizen";
                break;
            case 2:
                textureBase += "Noble";
                break;
            case 3:
                textureBase += "Aristocrat";
                break;
        }*/
        textureBase += "Citizen";

        textureBase += getSex() == 0 ? "Male" : "Female";

        texture = new ResourceLocation(Constants.MODID, textureBase + getTextureID() + ".png");
    }

    public int getTextureID()
    {
        return dataWatcher.getWatchableObjectInt(13);
    }

    public void setTextureID(int textureID)
    {
        dataWatcher.updateObject(13, textureID);
    }

    public int getLevel()
    {
        return dataWatcher.getWatchableObjectInt(14);
    }

    public void setLevel(int level)
    {
        dataWatcher.updateObject(14, level);
    }

    public int getSex()
    {
        return dataWatcher.getWatchableObjectInt(15);
    }

    public void setSex(int sex)
    {
        dataWatcher.updateObject(15, sex);
    }

    public String getJob()
    {
        return job;
    }

    public void setJob(String job, TileEntity tileEntity)
    {
        this.job = job;
        this.tileEntityWorkHut = (TileEntityHutWorker) tileEntity;
    }

    public TileEntityTownHall getTownHall()
    {
        return tileEntityTownHall;
    }

    public TileEntityHut getHomeHut()//TODO TileEntityHutCitizen
    {
        return tileEntityHomeHut;
    }

    public TileEntityHutWorker getWorkHut()
    {
        return tileEntityWorkHut;
    }

    public void setTownHall(TileEntityTownHall tileEntityTownHall)
    {
        this.tileEntityTownHall = tileEntityTownHall;
    }

    //public void setHomeHut(TileEntityHutCitizen home) { this.tileEntityHomeHut = home};

    public void setWorkHut(TileEntityHutWorker work)
    {
        this.tileEntityWorkHut = work;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        compound.setString("job", job);
        compound.setInteger("level", getLevel());
        compound.setInteger("textureID", getTextureID());
        compound.setInteger("sex", getSex());

        if(tileEntityTownHall != null)
        {
            NBTTagCompound nbtTagTownhallCompound = new NBTTagCompound();
            nbtTagTownhallCompound.setInteger("x", tileEntityTownHall.xCoord);
            nbtTagTownhallCompound.setInteger("y", tileEntityTownHall.yCoord);
            nbtTagTownhallCompound.setInteger("z", tileEntityTownHall.zCoord);
            compound.setTag("townhall", nbtTagTownhallCompound);
        }
        if(tileEntityWorkHut != null)
        {
            NBTTagCompound nbtTagWorkHutCompound = new NBTTagCompound();
            nbtTagWorkHutCompound.setInteger("x", tileEntityWorkHut.xCoord);
            nbtTagWorkHutCompound.setInteger("y", tileEntityWorkHut.yCoord);
            nbtTagWorkHutCompound.setInteger("z", tileEntityWorkHut.zCoord);
            compound.setTag("workhut", nbtTagWorkHutCompound);
        }
        if(tileEntityHomeHut != null)
        {
            NBTTagCompound nbtTagHomeHutCompound = new NBTTagCompound();
            nbtTagHomeHutCompound.setInteger("x", tileEntityHomeHut.xCoord);
            nbtTagHomeHutCompound.setInteger("y", tileEntityHomeHut.yCoord);
            nbtTagHomeHutCompound.setInteger("z", tileEntityHomeHut.zCoord);
            compound.setTag("homehut", nbtTagHomeHutCompound);
        }
        NBTTagList inventoryList = new NBTTagList();
        for(int i = 0; i < inventory.getSizeInventory(); i++)
        {
            if(inventory.getStackInSlot(i) != null)
            {
                NBTTagCompound tag = new NBTTagCompound();
                inventory.getStackInSlot(i).writeToNBT(tag);
                inventoryList.appendTag(tag);
            }
        }
        compound.setTag("Inventory", inventoryList);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);

        this.job = compound.getString("job");

        setTextureID(compound.getInteger("textureID"));
        setLevel(compound.hasKey("level") ? compound.getInteger("level") : this.getLevel());
        setSex(compound.hasKey("sex") ? compound.getInteger("sex") : this.getSex());
        setTexture();

        if(compound.hasKey("townhall"))
        {
            NBTTagCompound nbtTagTownhallCompound = compound.getCompoundTag("townhall");
            townPosX = nbtTagTownhallCompound.getInteger("x");
            townPosY = nbtTagTownhallCompound.getInteger("y");
            townPosZ = nbtTagTownhallCompound.getInteger("z");
        }
        if(compound.hasKey("workhut"))
        {
            NBTTagCompound nbtTagWorkHutCompound = compound.getCompoundTag("workhut");
            workPosX = nbtTagWorkHutCompound.getInteger("x");
            workPosY = nbtTagWorkHutCompound.getInteger("y");
            workPosZ = nbtTagWorkHutCompound.getInteger("z");
        }
        if(compound.hasKey("homehut"))
        {
            NBTTagCompound nbtTagHomeHutCompound = compound.getCompoundTag("homehut");
            homePosX = nbtTagHomeHutCompound.getInteger("x");
            homePosY = nbtTagHomeHutCompound.getInteger("y");
            homePosZ = nbtTagHomeHutCompound.getInteger("z");
        }
        NBTTagList nbttaglist = compound.getTagList("Inventory", net.minecraftforge.common.util.Constants.NBT.TAG_COMPOUND);
        for(int i = 0; i < nbttaglist.tagCount(); i++)
        {
            NBTTagCompound tag = nbttaglist.getCompoundTagAt(i);
            ItemStack itemstack = ItemStack.loadItemStackFromNBT(tag);
            if(itemstack != null)
            {
                inventory.setStackInInventory(itemstack);
            }
        }
    }

    public int getOffsetTicks()
    {
        return this.ticksExisted + 7 * this.getEntityId();
    }

    public boolean isWorkTime()
    {
        return worldObj.isDaytime() && !worldObj.isRaining();
    }

    @Override
    public void onInventoryChanged(InventoryBasic inventoryBasic){}

    public InventoryCitizen getInventory()
    {
        return inventory;
    }

    public void addToWorkHut(TileEntityHutWorker tileEntityHutWorker)
    {
        setJob(tileEntityHutWorker.getJobName(), tileEntityHutWorker);
        //TEST
        NBTTagCompound nbt = new NBTTagCompound();
        this.writeToNBT(nbt);
        getTownHall().removeCitizen(this);
        this.setDead();

        EntityCitizen worker = tileEntityHutWorker.createWorker();
        worker.readFromNBT(nbt);
        getTownHall().addCitizenToTownhall(worker);
        tileEntityHutWorker.bindWorker(worker);
        worldObj.spawnEntityInWorld(worker);
    }

    public void removeFromWorkHut(TileEntityHutWorker tileEntityHutWorker)
    {
        setJob(initJob(), null);
        NBTTagCompound nbt = new NBTTagCompound();
        this.writeToNBT(nbt);
        getTownHall().removeCitizen(this);
        tileEntityHutWorker.unbindWorker(this);
        this.setDead();

        EntityCitizen citizen = new EntityCitizen(worldObj);
        citizen.readFromNBT(nbt);
        getTownHall().addCitizenToTownhall(citizen);
        worldObj.spawnEntityInWorld(citizen);
    }

    public void writeVecToNBT(NBTTagCompound compound, String name, Vec3 vec)
    {
        NBTTagCompound vecCompound = new NBTTagCompound();
        vecCompound.setInteger("x", (int) vec.xCoord);
        vecCompound.setInteger("y", (int) vec.yCoord);
        vecCompound.setInteger("z", (int) vec.zCoord);
        compound.setTag("name", vecCompound);
    }

    public Vec3 readVecFromNBT(NBTTagCompound compound, String name)
    {
        NBTTagCompound vecCompound = compound.getCompoundTag(name);
        int x = vecCompound.getInteger("x");
        int y = vecCompound.getInteger("y");
        int z = vecCompound.getInteger("z");
        return Vec3.createVectorHelper(x, y, z);
    }
}
