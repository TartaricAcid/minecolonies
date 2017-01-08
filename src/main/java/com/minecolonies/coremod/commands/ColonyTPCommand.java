package com.minecolonies.coremod.commands;

import com.minecolonies.coremod.colony.Colony;
import com.minecolonies.coremod.colony.ColonyManager;
import com.minecolonies.coremod.configuration.Configurations;
import net.minecraft.block.Block;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 *  this command is made to TP a player to a safe random spot that is not to close to another colony
 */
public class ColonyTPCommand extends AbstractSingleCommand
{
    public static final  String DESC = "ctp";
    private static final int ATTEMPTS = 3;
    private static final int UPPER_BOUNDS = 100_000;
    private static final int LOWER_BOUNDS = 10;
    private static final int STARTING_Y = 250;
    private static final double ADDS_TWENTY_PERCENT = 1.20;
    private static final double SAFETY_DROP = 4;


    private Random rnd = new Random();



    ColonyTPCommand(@NotNull final String... parents)
    {
        super(parents);
    }

    @NotNull
    @Override
    public String getCommandUsage(@NotNull final ICommandSender sender)
    {
        return super.getCommandUsage(sender) + "ctp";
    }

    /**
     * this checks that you are not in the air or underground
     * and if so it will look up and down for a good landing spot
     * before TP
     *
     * @param server for the current server
     * @param sender for the player that is to be TP'd
     */

    @Override
    public void execute(@NotNull MinecraftServer server,  @NotNull ICommandSender sender, @NotNull String... args) throws CommandException
    {
        int b = 0;
         sender.getCommandSenderEntity().addChatMessage(new TextComponentString("Buckle up buttercup, this aint no joy ride!!!"));
        /* This is where all the magic happens */
        /*we will try up to 4 times to locate a safe area. then we have the player try again*/

        while (b <= ATTEMPTS)
        {
            b++;
            int x = rnd.nextInt(UPPER_BOUNDS) - LOWER_BOUNDS;
            int z = rnd.nextInt(UPPER_BOUNDS) - LOWER_BOUNDS;
            boolean areaSafe;
            boolean closetoCol= true;

            BlockPos blockPos = new BlockPos(x,STARTING_Y,z);
            /* send info to look for land */
            blockPos = findLand(blockPos, sender.getEntityWorld());
            /* only continue if the findLand has a good return*/
            if (blockPos != null)
            {
                /* ok now we take the new coords and do our other checks */
                /* send info to look for lava or water */
                areaSafe = findLavaWater(sender, blockPos);
                closetoCol = !areaSafe || findColony(blockPos, sender.getEntityWorld());
            }
            /* Take the return and determine if good or bad */
             /* send info to look to see if another colony is near */

            if (!closetoCol)
            {
                /* everything checks out good make the TP and jump out*/
                        sender.getCommandSenderEntity().setPositionAndUpdate(blockPos.getX(), blockPos.getY() + SAFETY_DROP, blockPos.getZ());
                break;
            }
            else
            {
                if (b > ATTEMPTS)
                {
                    sender.getCommandSenderEntity().addChatMessage(new TextComponentString("Couldn't find a safe spot.  Try again in a moment."));
                }
            }
        }

    }

    /**
     * this checks that you are not in the air or underground
     * and if so it will look up and down for a good landing spot
     * before TP
     *
     * @param blockPos for the current block LOC
     * @return blockPos to be used for the TP
     */
    private static BlockPos findLand(BlockPos blockPos, World world)
    {
        int top = STARTING_Y;
        int bot = 0;
        int mid = STARTING_Y;

        BlockPos foundland = null;
        /* we are doing a binary check to limit the amount of checks (usually 9 this wa) */
        while (top >= bot)
        {
            blockPos = new BlockPos( blockPos.getX(),mid, blockPos.getZ());
            Block blocks = world.getBlockState(blockPos).getBlock();
            /* this jumps us out when the y is good */
            /*if (blocks != Blocks.AIR && world.canSeeSky(blockPos))
            {
                foundland = blockPos;
                return foundland;
            }*/

            if (blocks == Blocks.AIR && world.canSeeSky(blockPos))
            {
                top = mid - 1;
                foundland = blockPos;
            }
            else
            {
                bot = mid + 1;
                foundland = blockPos;
            }
            mid = (bot + top)/2;
        }

        return foundland;
    }

    /**
     * this checks that you are not in water or lava
     * before TP
     *
     * @param blockPos for the current block LOC
     * @param sender uses the player to get the world
     * @return isSafe true=safe false=water or lava
     */
    private static boolean findLavaWater(@NotNull ICommandSender sender, BlockPos blockPos)
    {
        Boolean isSafe;
        /* take the coords and check to see if that block is any Liquid*/
        /* we check for all liquids this way, including ones added in by other mods */
        boolean liquidCheck = sender.getEntityWorld().getBlockState(blockPos).getMaterial().isLiquid();
        Block blocks = sender.getEntityWorld().getBlockState(blockPos).getBlock();
        isSafe = blocks != Blocks.AIR && !liquidCheck;

        return isSafe;
    }
    /**
     * this checks that you are not in water or lava
     * before TP
     *
     * @param blockPos for the current block LOC
     * @return colNear false=no true=yes
     */
    private boolean findColony(BlockPos blockPos, World world)
    {
        Colony nearestCol = ColonyManager.getClosestColony(world, blockPos);
        Boolean colNear;
        /* get individual coords to do the math */
        int cx = nearestCol != null ? nearestCol.getCenter().getX() : 0;
        int cz = nearestCol != null ? nearestCol.getCenter().getZ() : 0;
        /* from the random X for the TP */
        double px = blockPos.getX();
        /* from the random Z for the TP */
        double pz = blockPos.getZ();

        double dist = Math.sqrt(Math.pow(cx - px, 2.0) + (Math.pow(cz - pz, 2.0)));
                        /* grab the working distance and do our check now that we have distance from nearest colony*/
                        /* just to understand this better::::
                            I am taking the working distance from the town hall and doubling it this will give me
                            the distance needed from both town halls -if you placed it here.  Then im adding on 20%
                            just to get a padding between the two.*/
        double wd = (Configurations.workingRangeTownHall * 2) * ADDS_TWENTY_PERCENT;
        /* bad tp, bad -- abort TP  Too close to a colony */
        colNear = dist < wd;
        return colNear;
    }
    @NotNull
    @Override
    public List<String> getTabCompletionOptions(
            @NotNull final MinecraftServer server,
            @NotNull final ICommandSender sender,
            @NotNull final String[] args,
            final BlockPos pos)
    {
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(@NotNull String[] args, int index)
    {
        return false;
    }

}


