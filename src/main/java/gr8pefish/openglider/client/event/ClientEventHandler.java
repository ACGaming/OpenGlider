package gr8pefish.openglider.client.event;

import gr8pefish.openglider.client.model.ModelGlider;
import gr8pefish.openglider.common.OpenGlider;
import gr8pefish.openglider.common.capabilities.OpenGliderCapabilities;
import gr8pefish.openglider.common.config.ConfigHandler;
import gr8pefish.openglider.common.helper.OpenGliderPlayerHelper;
import gr8pefish.openglider.common.item.ItemHangGlider;
import gr8pefish.openglider.common.lib.ModInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientEventHandler extends Gui {


    //==================================================Rotating the Player to a Flying Position (Horizontal)=====================================

    private boolean needToPop = false;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRender(RenderPlayerEvent.Pre event) {
        if (event.getEntity() instanceof EntityPlayer) {
            EntityPlayer playerEntity = (EntityPlayer) event.getEntity();
            if (OpenGliderCapabilities.getIsGliderDeployed((EntityPlayer) event.getEntity())) { //if glider deployed
                if (!OpenGliderPlayerHelper.shouldBeGliding(playerEntity)) return; //don't continue if player is not flying
                if (Minecraft.getMinecraft().currentScreen instanceof GuiInventory) return; //don't rotate if the player rendered is in an inventory
                rotateToHorizontal(event.getEntityPlayer(), event.getX(), event.getY(), event.getZ()); //rotate player to flying position
                this.needToPop = true; //mark the matrix to pop
            }
        }
    }

    /**
     * Makes the player's body rotate visually to be flat, parallel to the ground (e.g. like superman flies).
     *
     * @param playerEntity - the player to rotate
     * @param x - player's x pos
     * @param y - player's y pos
     * @param z - player's z pos
     */
    private void rotateToHorizontal(EntityPlayer playerEntity, double x, double y, double z){

        float partialTicks = Minecraft.getMinecraft().getRenderPartialTicks();
        double interpolatedYaw = (playerEntity.prevRotationYaw + (playerEntity.rotationYaw - playerEntity.prevRotationYaw) * partialTicks);

        GlStateManager.pushMatrix();
        //7. Set position back to normal
        GlStateManager.translate(x, y, z);
        //6. Redo yaw rotation
        GlStateManager.rotate((float) -interpolatedYaw, 0, 1, 0);
        //5. Move back to (0, 0, 0)
        GlStateManager.translate(0, playerEntity.height / 2f, 0);
        //4. Rotate about x, so player leans over forward (z direction is forwards)
        GlStateManager.rotate(90, 1, 0, 0);
        //3. So we rotate around the centre of the player instead of the bottom of the player
        GlStateManager.translate(0, -playerEntity.height / 2f, 0);
        //2. Undo yaw rotation (this will make the player face +z (south)
        GlStateManager.rotate((float) interpolatedYaw, 0, 1, 0);
        //1. Set position to (0, 0, 0)
        GlStateManager.translate(-x, -y, -z);

    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onRender(RenderPlayerEvent.Post event) {
        if (this.needToPop) {
            this.needToPop = false;
            GlStateManager.popMatrix();
        }
    }

    //=============================================================Rendering In-World for 1st Person Perspective==================================================

    /**
     * For rendering as a perspective projection in-world, as opposed to the slightly odd looking orthogonal projection above
     */
    @SubscribeEvent
    public void onRenderOverlay(RenderWorldLastEvent event){
        if (ConfigHandler.enableRenderingFPP && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) { //rendering enabled and first person perspective
            EntityPlayer playerEntity = Minecraft.getMinecraft().thePlayer;
            if (OpenGliderCapabilities.getIsGliderDeployed(playerEntity)) { //if glider deployed
                if (OpenGliderPlayerHelper.shouldBeGliding(playerEntity)) { //if flying
                    renderGliderFirstPersonPerspective(event); //render hang glider above head
                }
            }
        }
    }

    //The model to display
    private final ModelGlider modelGlider = new ModelGlider();

    /**
     * Renders the glider above the player
     *
     * @param event - the render world event
     */
    private void renderGliderFirstPersonPerspective(RenderWorldLastEvent event){

        EntityPlayer entityPlayer = Minecraft.getMinecraft().thePlayer;
        Minecraft.getMinecraft().getTextureManager().bindTexture(ModInfo.MODEL_GLIDER_TEXTURE_RL); //bind texture

        //push matrix
        GlStateManager.pushMatrix();
        //set the rotation correctly for fpp
        setRotationFirstPersonPerspective(entityPlayer, event.getPartialTicks());
        //set the correct lighting
        setLightingBeforeRendering(entityPlayer, event.getPartialTicks());
        //render the glider
        modelGlider.render(entityPlayer, entityPlayer.limbSwing, entityPlayer.limbSwingAmount, entityPlayer.getAge(), entityPlayer.rotationYawHead, entityPlayer.rotationPitch, 1);
        //pop matrix
        GlStateManager.popMatrix();

    }

    private void setLightingBeforeRendering(EntityPlayer player, float partialTicks) {
        GlStateManager.enableLighting();

        int i = player.getBrightnessForRender(partialTicks);
        int j = i % 65536;
        int k = i / 65536;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)j, (float)k);
        Minecraft.getMinecraft().entityRenderer.enableLightmap();

    }

    /**
     * Sets the rotation of the hang glider to work for first person rendering in-world.
     *
     * @param player - the player
     * @param partialTicks - the partial ticks
     */
    private void setRotationFirstPersonPerspective(EntityPlayer player, float partialTicks) {
        double interpolatedYaw = (player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks);
        //rotate the glider to the same orientation as the player is facing
        GlStateManager.rotate((float) -interpolatedYaw, 0, 1, 0);
        //rotate the glider so it is forwards facing, as it should be
        GlStateManager.rotate(180F, 0, 1, 0);
        //move up to correct position (above player's head)
        GlStateManager.translate(0, ConfigHandler.gliderVisibilityFPPShiftAmount, 0);

        //move away if sneaking
        if (player.isSneaking())
            GlStateManager.translate(0, 0, -0.25); //subtle speed effect (makes glider smaller looking)
    }


    //================================================================Miscellaneous===========================================


    /**
     * Disable the offhand rendering if the player has a glider deployed (and is holding a glider)
     *
     * @param event - the render hand event
     */
    @SubscribeEvent
    public void onHandRender(RenderSpecificHandEvent event){
        EntityPlayer player = OpenGlider.proxy.getClientPlayer();
        if (ConfigHandler.disableOffhandRenderingWhenGliding) { //configurable
            if (OpenGliderCapabilities.getIsGliderDeployed(player)) { //if glider deployed
                if (player.getHeldItemMainhand() != null && player.getHeldItemMainhand().getItem() instanceof ItemHangGlider && !ItemHangGlider.isBroken(player.getHeldItemMainhand())) { //if holding a deployed hang glider
                    if (event.getHand() == EnumHand.OFF_HAND) { //offhand rendering
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

}
