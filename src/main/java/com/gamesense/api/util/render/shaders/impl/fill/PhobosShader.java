package com.gamesense.api.util.render.shaders.impl.fill;

import com.gamesense.api.util.render.shaders.FramebufferShader;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.awt.*;
import java.util.HashMap;

public class PhobosShader extends FramebufferShader {

    public static final PhobosShader INSTANCE;
    public float time;

    public PhobosShader( ) {
        super( "phobos.frag" );
    }

    @Override public void setupUniforms ( ) {
        this.setupUniform( "resolution" );
        this.setupUniform( "time" );
        this.setupUniform("color");
        this.setupUniform("texelSize");
        this.setupUniform("texture");
    }

    public void updateUniforms (float duplicate, Color color, int lines, double tau) {
        GL20.glUniform2f( getUniform( "resolution" ), new ScaledResolution( mc ).getScaledWidth( ) / duplicate, new ScaledResolution( mc ).getScaledHeight( ) / duplicate );
        GL20.glUniform1i(this.getUniform("texture"), 0);
        GL20.glUniform1f( getUniform( "time" ), this.time );
        GL20.glUniform4f(getUniform("color"), color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f );
        GL20.glUniform2f(this.getUniform("texelSize"), 1.0f / this.mc.displayWidth * (radius * quality), 1.0f / this.mc.displayHeight * (radius * quality));
    }

    public void stopDraw(final Color color, final float radius, final float quality, float duplicate, final int lines, final double tau ) {
        mc.gameSettings.entityShadows = entityShadows;
        framebuffer.unbindFramebuffer( );
        GL11.glEnable( 3042 );
        GL11.glBlendFunc( 770, 771 );
        mc.getFramebuffer( ).bindFramebuffer( true );
        red = color.getRed( ) / 255.0f;
        green = color.getGreen( ) / 255.0f;
        blue = color.getBlue( ) / 255.0f;
        alpha = color.getAlpha( ) / 255.0f;
        this.radius = radius;
        this.quality = quality;
        mc.entityRenderer.disableLightmap( );
        RenderHelper.disableStandardItemLighting( );
        GL11.glPushMatrix();
        startShader(duplicate, color, lines, tau);
        mc.entityRenderer.setupOverlayRendering( );
        drawFramebuffer( framebuffer );
        stopShader( );
        mc.entityRenderer.disableLightmap( );
        GlStateManager.popMatrix( );
        GlStateManager.popAttrib( );
    }

    public void startShader(float duplicate, Color color, int lines, double tau) {
        GL20.glUseProgram(this.program);
        if (this.uniformsMap == null) {
            this.uniformsMap = new HashMap<String, Integer>();
            this.setupUniforms();
        }
        this.updateUniforms(duplicate, color, lines, tau);
    }


    static {
        INSTANCE = new PhobosShader();
    }

    public void update(double speed) {
        this.time += speed;
    }
}
