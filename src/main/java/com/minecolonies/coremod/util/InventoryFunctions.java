package com.minecolonies.coremod.util;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.*;

/**
 * Java8 functional interfaces for {@link net.minecraft.inventory.IInventory}
 * Most methods will be remapping of parameters to reduce duplication.
 * Because of erasure clashes, not all combinations are supported.
 */
public class InventoryFunctions
{

    /**
     * A NOOP Consumer to use for any function.
     *
     * @param o will be consumed and ignored
     */
    public static void doNothing(final Object... o)
    {
        //Intentionally left blank to do nothing.
    }

    /**
     * Search for a stack in an Inventory matching the predicate.
     *
     * @param provider the provider to search in
     * @param tester   the function to use for testing slots
     * @param action   the function to use if a slot matches
     * @return true if it found a stack
     */
    public static boolean matchFirstInProvider(
                                                      final ICapabilityProvider provider, @NotNull final Predicate<ItemStack> tester,
                                                      @NotNull final Consumer<Integer> action)
    {
        return matchFirstInProvider(provider, inv -> slot -> stack ->
        {
            if (tester.test(stack))
            {
                action.accept(slot);
                return true;
            }
            return false;
        });
    }

    /**
     * Topmost matchFirst function, will stop after it finds the first
     * itemstack.
     *
     * @param provider the provider to search in
     * @param tester   the function to use for testing slots
     * @return true if it found a stack
     */
    private static boolean matchFirstInProvider(
                                                       final ICapabilityProvider provider, @NotNull final Function<ICapabilityProvider, Function<Integer,
            Predicate<ItemStack>>> tester)
    {
        return matchInProvider(provider, tester, true);
    }

    /**
     * Topmost function to actually loop over the provider.
     * Will return if it found something.
     *
     * @param provider       the provider to loop over
     * @param tester         the function to use for testing slots
     * @param stopAfterFirst if it should stop executing after finding one stack
     *                       that applies
     * @return true if it found a stack
     */
    private static boolean matchInProvider(
                                                  @Nullable final ICapabilityProvider provider,
                                                  @NotNull final Function<ICapabilityProvider, Function<Integer,
                                                          Predicate<ItemStack>>> tester,
                                                  final boolean stopAfterFirst)
    {
        if (provider == null)
        {
            return false;
        }

        boolean foundOne = false;
        for (final IItemHandler handler : InventoryUtils.getItemHandlersFromProvider(provider))
        {
            final int size = handler.getSlots();
            for (int slot = 0; slot < size; slot++)
            {
                final ItemStack stack = handler.getStackInSlot(slot);
                //Unchain the function and apply it
                if (tester.apply(provider).apply(slot).test(stack))
                {
                    foundOne = true;
                    if (stopAfterFirst)
                    {
                        return true;
                    }
                }
            }
        }

        return foundOne;
    }

    /**
     * Search for a stack in an Inventory matching the predicate.
     *
     * @param provider the provider to search in
     * @param tester   the function to use for testing slots
     * @param action   the function to use if a slot matches
     * @return true if it found a stack
     */
    public static boolean matchFirstInProviderWithAction(
                                                                final ICapabilityProvider provider, @NotNull final Predicate<ItemStack> tester,
                                                                @NotNull final IMatchActionResult action)
    {
        return matchFirstInProvider(provider, inv -> slot -> stack ->
        {
            if (tester.test(stack))
            {
                action.accept(provider, slot);
                return true;
            }
            return false;
        });
    }

    /**
     * Search for a stack in an Inventory matching the predicate.
     * (IInventory, Integer) -&gt; Boolean
     *
     * @param inventory the inventory to search in
     * @param tester    the function to use for testing slots
     * @return true if it found a stack
     */
    public static boolean matchFirstInProvider(final ICapabilityProvider inventory, @NotNull final BiPredicate<Integer, ItemStack> tester)
    {
        return matchFirstInProvider(inventory, inv -> slot -> stack -> tester.test(slot, stack));
    }

    /**
     * Functional interface describing a Action that is executed ones a Match
     * (the given ItemStack) is found in the given slot.
     */
    @FunctionalInterface
    public interface IMatchActionResult extends ObjIntConsumer<ICapabilityProvider>
    {
        /**
         * Method executed when a match has been found.
         *
         * @param provider  The itemstack that matches the predicate for the
         *                  search.
         * @param slotIndex The slotindex in which this itemstack was found.
         */
        @Override
        void accept(ICapabilityProvider provider, int slotIndex);
    }
}
