package com.mrlacar.univom.config;

import com.gregtechceu.gtceu.api.GTValues;
import com.mrlacar.univom.UNIVOM;
import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.format.ConfigFormats;

@Config(id = UNIVOM.MOD_ID)
public class ConfigHolder {
    public static ConfigHolder INSTANCE;
    private static final Object LOCK = new Object();
    private static final long BASE_CAPACITY = 1_000_000_000L;

    public static void init() {
        synchronized (LOCK) {
            if (INSTANCE == null) {
                INSTANCE = Configuration.registerConfig(ConfigHolder.class, ConfigFormats.yaml()).getConfigInstance();
            }
        }
    }

    @Configurable
    public DevModeConfigs devModeSetting = new DevModeConfigs();
    @Configurable
    public WirelessEnergyNetworkConfigs WirelessEnergyNetworkConfigs = new WirelessEnergyNetworkConfigs();
    @Configurable
    public SpaceFederationArmorConfigs spaceFederationArmorSetting = new SpaceFederationArmorConfigs();
    @Configurable
    public MoleculeTooltipConfigs moleculeTooltipSetting = new MoleculeTooltipConfigs();

    public static class DevModeConfigs {

        @Configurable
        @Configurable.Comment({ "Dev Mode", "Default: false" })
        public boolean devMode = false;
    }

    public static class SpaceFederationArmorConfigs {

        @Configurable
        @Configurable.Comment({ "Space Federation Armor Voltage Tier.", "Default: 8 (UV)" })
        @Configurable.Range(min = 0, max = 14)
        public int voltageTier = GTValues.UV;

        @Configurable
        @Configurable.Comment({ "Space Federation Armor energy consumed per operation (EU).", "Default: 8192" })
        @Configurable.Range(min = 1, max = Integer.MAX_VALUE)
        public int energyPerUse = 8192;

        @Configurable
        @Configurable.Comment({ "Energy consumed per oxygen supply cycle, once every 12 ticks (EU).",
                "Default: 256" })
        @Configurable.Range(min = 0, max = Integer.MAX_VALUE)
        public int oxygenEnergyCost = 256;

        @Configurable
        @Configurable.Comment({ "Energy consumed per second to survive extreme temperatures (EU).", "Default: 1024" })
        @Configurable.Range(min = 0, max = Integer.MAX_VALUE)
        public int temperatureEnergyCost = 1024;

        @Configurable
        @Configurable.Comment({ "Energy consumed per acid rain tick (EU).", "Default: 512" })
        @Configurable.Range(min = 0, max = Integer.MAX_VALUE)
        public int acidRainEnergyCost = 512;

        @Configurable
        @Configurable.Comment({ "Space Federation Armor Chestplate oxygen tank capacity (mB).", "Default: 8000" })
        @Configurable.Range(min = 1, max = Integer.MAX_VALUE)
        public int oxygenTankCapacity = 8000;

        public long getCapacity() {
            return BASE_CAPACITY * (long) Math.max(1, Math.pow(4, this.voltageTier - 6));
        }
    }

    public static class MoleculeTooltipConfigs {

        @Configurable
        @Configurable.Comment({ "Draw molecular structures in GregTech material tooltips.", "Default: true" })
        public boolean enabled = true;

        @Configurable
        @Configurable.Comment({ "Only draw structures and pie charts while holding Shift.", "Default: false" })
        public boolean onlyShowOnShift = false;

        @Configurable
        @Configurable.Comment({ "Pixels per bond length in molecular structures.", "Default: 20" })
        @Configurable.Range(min = 10, max = 50)
        public int moleculeScale = 20;

        @Configurable
        @Configurable.Comment({ "Draw aromatic rings as a circle (Thiele) instead of alternating double bonds (Kekule).",
                "Default: false" })
        public boolean aromaticCircle = false;

        @Configurable
        @Configurable.Comment({ "Colour the plain chemical formula line by element.", "Default: true" })
        public boolean colorizeFormula = true;

        @Configurable
        @Configurable.Comment({ "Default drawing colour, as a plain RRGGBB hex string.",
                "Do not prefix it with '#', that starts a comment in YAML.", "Default: FFFF55" })
        public String defaultColor = "FFFF55";

        @Configurable
        @Configurable.Comment({ "Draw component pie charts for alloy materials.", "Default: true" })
        public boolean showAlloyPieChart = true;

        @Configurable
        @Configurable.Comment({ "Pie chart every material that has two or more components, not just the listed ones.",
                "Default: true" })
        public boolean alloyAutoDetect = true;

        @Configurable
        @Configurable.Comment({ "Alloy pie chart radius in pixels.", "Default: 32" })
        @Configurable.Range(min = 16, max = 64)
        public int alloyPieRadius = 32;

        @Configurable
        @Configurable.Comment({ "Split alloy pie charts by mass instead of by atom count.", "Default: true" })
        public boolean alloyByMass = true;

        @Configurable
        @Configurable.Comment({ "Recursively expand alloy components down to elements.", "Default: true" })
        public boolean alloyRecursive = true;
    }

    public static class WirelessEnergyNetworkConfigs {
        @Configurable
        @Configurable.Comment({ "Wireless energy network auto-balancing interval (Tick)", "Default: 6000" })
        @Configurable.Range(min = 20, max = 12000)
        public int AutoBalancingInterval = 6000;
    }
}
