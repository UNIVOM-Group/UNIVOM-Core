package com.mrlacar.univom.client;

import com.mrlacar.univom.UNIVOM;
import com.mrlacar.univom.client.renderer.machine.PartTierCasingRender;
import com.mrlacar.univom.common.CommonProxy;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientProxy extends CommonProxy {
    public ClientProxy() {
        init();
    }

    public static void init() {
        DynamicRenderManager.register(UNIVOM.id("part_tier_casing"), PartTierCasingRender.TYPE);
    }
}
