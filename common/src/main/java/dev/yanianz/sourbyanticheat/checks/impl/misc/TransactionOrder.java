package dev.yanianz.sourbyanticheat.checks.impl.misc;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

@CheckData(name = "TransactionOrder", stableKey = "sac.ping.invalid_transaction_order")
public class TransactionOrder extends Check {
    public TransactionOrder(SacPlayer player) {
        super(player);
    }
}
