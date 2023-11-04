package com.prasannjeet.vaxjobostader.enums;

public enum PlaceName {
    VXJ("Vxj"),
    LAMMHULT("Lammhult"),
    BRAAS("Braås"),
    INGELSTAD("Ingelstad"),
    ROTTNE("Rottne"),
    VEDERSLV("Vederslv"),
    VAXJO("Växjö"),
    BRAS("Bras"),
    RYD("ryd"),
    GEMLA("Gemla"),
    ARYD("Åryd"),
    VAREND_NOBBELE("Värends Nöbbele"),
    FURUBY("Furuby"),
    VEDERSLOV("Vederslöv"),
    KALVSVIK("Kalvsvik"),
    OR("Ör"),
    TAVELSAAS("Tävelsås"),
    TJUREDA("Tjureda"),
    EMPTY(""),
    TOLG("Tolg");

    private final String displayName;

    PlaceName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

