package com.howlite.cobblemoncards.compat;

/** A single optional extra reward; no player balance, completion event, or claim state. */
public record CompatReward(String id, CompatConfig.Integration integration,
                           BoosterRewardHelper.Selection selection) {
    public double chance() { return CompatConfig.chance(id); }
    public boolean isEnabled() {
        return CompatManager.isDetected(integration) && CompatConfig.enabled(integration);
    }
}
