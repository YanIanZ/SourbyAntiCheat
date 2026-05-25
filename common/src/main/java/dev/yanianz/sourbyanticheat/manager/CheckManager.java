package dev.yanianz.sourbyanticheat.manager;

import dev.yanianz.sourbyanticheat.SacAPI;
import ac.grim.grimac.api.AbstractCheck;
import dev.yanianz.sourbyanticheat.checks.impl.aim.AimAssist;
import dev.yanianz.sourbyanticheat.checks.impl.aim.processor.AimProcessor;
import dev.yanianz.sourbyanticheat.checks.impl.badpackets.*;
import dev.yanianz.sourbyanticheat.checks.impl.breaking.*;
import dev.yanianz.sourbyanticheat.checks.impl.combat.*;
import dev.yanianz.sourbyanticheat.checks.impl.crash.*;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.BackTrack;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.BlockReach;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossAntiKB;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossAutoClicker;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossCriticals;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossElytraMove;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossEntitySpeed;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossFastBow;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossFastBreak;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossFastBreakB;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossFastEat;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossFastLadder;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossFastPlace;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossFlight;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossFlightB;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossFoodSprint;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossFreecam;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossJesus;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossJump;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossKillAura;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossKillAuraB;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossNoFall;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossPhase;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossPhaseB;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossReach;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossScaffold;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossSpeed;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossSpeedB;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossSpider;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossStep;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossTeleport;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossTimer;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossTower;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossVehicle;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.FastFall;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.FightBot;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.ForceField;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.Freecam;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.GhostHand;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.IrregularMovements;
import dev.yanianz.sourbyanticheat.checks.crossapi.CrossValidationData;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.Liquids;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.MorePackets;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.NoClip;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.Nuker;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.PingSpoof;
import dev.yanianz.sourbyanticheat.checks.impl.elytra.*;
import dev.yanianz.sourbyanticheat.checks.impl.exploit.ExploitA;
import dev.yanianz.sourbyanticheat.checks.impl.exploit.ExploitB;
import dev.yanianz.sourbyanticheat.checks.impl.exploit.ExploitC;
import dev.yanianz.sourbyanticheat.checks.impl.flight.FlightA;
import dev.yanianz.sourbyanticheat.checks.impl.groundspoof.NoFall;
import dev.yanianz.sourbyanticheat.checks.impl.misc.APIBypass;
import dev.yanianz.sourbyanticheat.checks.impl.misc.ClientBrand;
import dev.yanianz.sourbyanticheat.checks.impl.misc.GhostBlockMitigation;
import dev.yanianz.sourbyanticheat.checks.impl.misc.NettyDelay;
import dev.yanianz.sourbyanticheat.checks.impl.misc.NettyFlood;
import dev.yanianz.sourbyanticheat.checks.impl.misc.PayloadCheck;
import dev.yanianz.sourbyanticheat.checks.impl.misc.Post;
import dev.yanianz.sourbyanticheat.checks.impl.misc.SpartanDivergence;
import dev.yanianz.sourbyanticheat.checks.impl.misc.SpartanSync;
import dev.yanianz.sourbyanticheat.checks.impl.misc.TransactionOrder;
import dev.yanianz.sourbyanticheat.checks.impl.movement.Jesus;
import dev.yanianz.sourbyanticheat.checks.impl.movement.FastLadder;
import dev.yanianz.sourbyanticheat.checks.impl.movement.NoWeb;
import dev.yanianz.sourbyanticheat.checks.impl.movement.Step;
import dev.yanianz.sourbyanticheat.checks.impl.movement.EntitySpeed;
import dev.yanianz.sourbyanticheat.checks.impl.movement.SafeWalk;
import dev.yanianz.sourbyanticheat.checks.impl.movement.Blink;
import dev.yanianz.sourbyanticheat.checks.impl.movement.InventoryMove;
import dev.yanianz.sourbyanticheat.checks.impl.movement.InventoryWalk;
import dev.yanianz.sourbyanticheat.checks.impl.movement.Tower;
import dev.yanianz.sourbyanticheat.checks.impl.movement.NoRotate;
import dev.yanianz.sourbyanticheat.checks.impl.movement.NoSlow;
import dev.yanianz.sourbyanticheat.checks.impl.movement.Speed;
import dev.yanianz.sourbyanticheat.checks.impl.movement.Spider;
import dev.yanianz.sourbyanticheat.checks.impl.movement.PredictionRunner;
import dev.yanianz.sourbyanticheat.checks.impl.movement.SetbackBlocker;
import dev.yanianz.sourbyanticheat.checks.impl.movement.VehiclePredictionRunner;
import dev.yanianz.sourbyanticheat.checks.impl.multiactions.*;
import dev.yanianz.sourbyanticheat.checks.impl.multiactions.FastSwitch;
import dev.yanianz.sourbyanticheat.checks.impl.packetorder.*;
import dev.yanianz.sourbyanticheat.checks.impl.prediction.DebugHandler;
import dev.yanianz.sourbyanticheat.checks.impl.prediction.GroundSpoof;
import dev.yanianz.sourbyanticheat.checks.impl.prediction.OffsetHandler;
import dev.yanianz.sourbyanticheat.checks.impl.prediction.Phase;
import dev.yanianz.sourbyanticheat.checks.impl.scaffolding.*;
import dev.yanianz.sourbyanticheat.checks.impl.sprint.*;
import dev.yanianz.sourbyanticheat.checks.impl.timer.*;
import dev.yanianz.sourbyanticheat.checks.impl.vehicle.*;
import dev.yanianz.sourbyanticheat.checks.impl.velocity.ExplosionHandler;
import dev.yanianz.sourbyanticheat.checks.impl.velocity.KnockbackHandler;
import dev.yanianz.sourbyanticheat.checks.type.*;
import dev.yanianz.sourbyanticheat.events.packets.PacketChangeGameState;
import dev.yanianz.sourbyanticheat.events.packets.PacketEntityReplication;
import dev.yanianz.sourbyanticheat.events.packets.PacketPlayerAbilities;
import dev.yanianz.sourbyanticheat.events.packets.PacketWorldBorder;
import dev.yanianz.sourbyanticheat.manager.init.start.SuperDebug;
import dev.yanianz.sourbyanticheat.platform.api.permissions.PermissionDefaultValue;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.predictionengine.GhostBlockDetector;
import dev.yanianz.sourbyanticheat.predictionengine.SneakingEstimator;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.*;
import dev.yanianz.sourbyanticheat.utils.latency.CompensatedCameraEntity;
import dev.yanianz.sourbyanticheat.utils.latency.CompensatedCooldown;
import dev.yanianz.sourbyanticheat.utils.latency.CompensatedFireworks;
import dev.yanianz.sourbyanticheat.utils.latency.CompensatedInventory;
import dev.yanianz.sourbyanticheat.utils.team.TeamHandler;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CheckManager {
    private static final AtomicBoolean initedAtomic = new AtomicBoolean(false);
    private static boolean inited;
    private final SacPlayer player;
    public final ClassToInstanceMap<AbstractCheck> allChecks;
    private final ClassToInstanceMap<PacketCheck> packetChecks;
    private final ClassToInstanceMap<PositionCheck> positionChecks;
    private final ClassToInstanceMap<RotationCheck> rotationChecks;
    private final ClassToInstanceMap<VehicleCheck> vehicleChecks;
    private final ClassToInstanceMap<PacketCheck> prePredictionChecks;
    private final ClassToInstanceMap<BlockBreakCheck> blockBreakChecks;
    private final ClassToInstanceMap<BlockPlaceCheck> blockPlaceChecks;
    private final ClassToInstanceMap<PostPredictionCheck> postPredictionChecks;
    @Getter
    private final PacketEntityReplication packetEntityReplication;

    private final List<PacketCheck> packetChecksValues;
    private final List<PositionCheck> positionChecksValues;
    private final List<RotationCheck> rotationChecksValues;
    private final List<VehicleCheck> vehicleChecksValues;
    private final List<PacketCheck> prePredictionChecksValues;
    private final List<BlockBreakCheck> blockBreakChecksValues;
    private final List<BlockPlaceCheck> blockPlaceChecksValues;
    private final List<PostPredictionCheck> postPredictionChecksValues;

    public CheckManager(SacPlayer player) {
        this.player = player;
        packetEntityReplication = new PacketEntityReplication(player);

        packetChecks = new ImmutableClassToInstanceMap.Builder<PacketCheck>()
                .put(CompensatedCameraEntity.class, player.cameraEntity)
                .put(PacketOrderProcessor.class, player.packetOrderProcessor)
                .put(Reach.class, new Reach(player))
                .put(PacketEntityReplication.class, packetEntityReplication)
                .put(PacketChangeGameState.class, new PacketChangeGameState(player))
                .put(CompensatedInventory.class, player.inventory)
                .put(PacketPlayerAbilities.class, new PacketPlayerAbilities(player))
                .put(PacketWorldBorder.class, new PacketWorldBorder(player))
                .put(AttackCooldownHandler.class, player.attackCooldown)
                .put(TeamHandler.class, new TeamHandler(player))
                .put(ClientBrand.class, new ClientBrand(player))
                .put(NettyFlood.class, new NettyFlood(player))
                .put(NettyDelay.class, new NettyDelay(player))
                .put(CrossPhase.class, new CrossPhase(player))
                .put(CrossTimer.class, new CrossTimer(player))
                .put(CrossStep.class, new CrossStep(player))
                .put(CrossSpider.class, new CrossSpider(player))
                .put(CrossElytraMove.class, new CrossElytraMove(player))
                .put(CrossKillAura.class, new CrossKillAura(player))
                .put(CrossKillAuraB.class, new CrossKillAuraB(player))
                .put(CrossFastLadder.class, new CrossFastLadder(player))
                .put(MorePackets.class, new MorePackets(player))
                .put(CrossAutoClicker.class, new CrossAutoClicker(player))
                .put(PingSpoof.class, new PingSpoof(player))
                .put(GhostHand.class, new GhostHand(player))
                .put(PayloadCheck.class, new PayloadCheck(player))
                .put(SpartanSync.class, new SpartanSync(player))
                .put(SpartanDivergence.class, new SpartanDivergence(player))
                .put(CrossSpeedB.class, new CrossSpeedB(player))
                .put(CrossReach.class, new CrossReach(player))
                .put(CrossFastBow.class, new CrossFastBow(player))
                .put(ForceField.class, new ForceField(player))
                .put(APIBypass.class, new APIBypass(player))
                .put(Jesus.class, new Jesus(player))
                .put(FastLadder.class, new FastLadder(player))
                .put(NoWeb.class, new NoWeb(player))
                .put(Step.class, new Step(player))
                .put(EntitySpeed.class, new EntitySpeed(player))
                .put(SafeWalk.class, new SafeWalk(player))
                .put(Blink.class, new Blink(player))
                .put(NoFall.class, new NoFall(player))
                .put(ExploitA.class, new ExploitA(player))
                .put(ExploitB.class, new ExploitB(player))
                .put(BadPacketsA.class, new BadPacketsA(player))
                .put(BadPacketsB.class, new BadPacketsB(player))
                .put(BadPacketsC.class, new BadPacketsC(player))
                .put(BadPacketsD.class, new BadPacketsD(player))
                .put(BadPacketsE.class, new BadPacketsE(player))
                .put(BadPacketsF.class, new BadPacketsF(player))
                .put(BadPacketsG.class, new BadPacketsG(player))
                .put(BadPacketsI.class, new BadPacketsI(player))
                .put(BadPacketsJ.class, new BadPacketsJ(player))
                .put(BadPacketsK.class, new BadPacketsK(player))
                .put(BadPacketsL.class, new BadPacketsL(player))
                .put(BadPacketsM.class, new BadPacketsM(player))
                .put(BadPacketsO.class, new BadPacketsO(player))
                .put(BadPacketsP.class, new BadPacketsP(player))
                .put(BadPacketsQ.class, new BadPacketsQ(player))
                .put(BadPacketsR.class, new BadPacketsR(player))
                .put(BadPacketsS.class, new BadPacketsS(player))
                .put(BadPacketsT.class, new BadPacketsT(player))
                .put(BadPacketsU.class, new BadPacketsU(player))
                .put(BadPacketsV.class, new BadPacketsV(player))
                .put(BadPacketsY.class, new BadPacketsY(player))
                .put(BadPacketsZ.class, new BadPacketsZ(player))
                .put(BadPacketsAA.class, new BadPacketsAA(player))
                .put(BadPacketsAB.class, new BadPacketsAB(player))
                .put(BadPacketsAC.class, new BadPacketsAC(player))
                .put(BadPacketsAD.class, new BadPacketsAD(player))
                .put(BadPacketsAE.class, new BadPacketsAE(player))
                .put(BadPacketsAF.class, new BadPacketsAF(player))
                .put(BadPacketsAG.class, new BadPacketsAG(player))
                .put(BadPacketsAH.class, new BadPacketsAH(player))
                .put(SelfInteract.class, new SelfInteract(player))
                .put(AutoClicker.class, new AutoClicker(player))
                .put(Criticals.class, new Criticals(player))
                .put(FastBow.class, new FastBow(player))
                .put(FastEat.class, new FastEat(player))
                .put(AutoArmor.class, new AutoArmor(player))
                .put(AntiVelocity.class, new AntiVelocity(player))
                .put(FastSwitch.class, new FastSwitch(player))
                .put(MultiAttack.class, new MultiAttack(player))
                .put(AttackFrequency.class, new AttackFrequency(player))
                .put(InventoryMove.class, new InventoryMove(player))
                .put(InventoryWalk.class, new InventoryWalk(player))
                .put(BadPacketsAI.class, new BadPacketsAI(player))
                .put(BadPacketsAJ.class, new BadPacketsAJ(player))
                .put(AimSnap.class, new AimSnap(player))
                .put(AimSuspicion.class, new AimSuspicion(player))
                .put(Tower.class, new Tower(player))
                .put(NoRotate.class, new NoRotate(player))
                .put(ExploitC.class, new ExploitC(player))
                .put(FlightA.class, new FlightA(player))
                .put(MultiActionsA.class, new MultiActionsA(player))
                .put(MultiActionsC.class, new MultiActionsC(player))
                .put(MultiActionsD.class, new MultiActionsD(player))
                .put(MultiActionsE.class, new MultiActionsE(player))
                .put(PacketOrderB.class, new PacketOrderB(player))
                .put(PacketOrderC.class, new PacketOrderC(player))
                .put(PacketOrderD.class, new PacketOrderD(player))
                .put(PacketOrderO.class, new PacketOrderO(player))
//                .put(PacketOrderP.class, new PacketOrderP(player))
                .put(SprintA.class, new SprintA(player))
                .put(VehicleA.class, new VehicleA(player))
                .put(VehicleB.class, new VehicleB(player))
                .put(VehicleD.class, new VehicleD(player))
                .put(VehicleE.class, new VehicleE(player))
                .put(VehicleF.class, new VehicleF(player))
                .put(CrashB.class, new CrashB(player))
                .put(CrashD.class, new CrashD(player))
                .put(CrashE.class, new CrashE(player))
                .put(CrashF.class, new CrashF(player))
                .put(CrashH.class, new CrashH(player))
                .put(CrashI.class, new CrashI(player))
                .put(SetbackBlocker.class, new SetbackBlocker(player)) // Must be last class otherwise we can't check while blocking packets
                .build();

        positionChecks = new ImmutableClassToInstanceMap.Builder<PositionCheck>()
                .put(PredictionRunner.class, new PredictionRunner(player))
                .put(CompensatedCooldown.class, new CompensatedCooldown(player))
                .build();
        rotationChecks = new ImmutableClassToInstanceMap.Builder<RotationCheck>()
                .put(AimProcessor.class, new AimProcessor(player))
                .put(AimAssist.class, new AimAssist(player))
                .build();
        vehicleChecks = new ImmutableClassToInstanceMap.Builder<VehicleCheck>()
                .put(VehiclePredictionRunner.class, new VehiclePredictionRunner(player))
                .build();

        postPredictionChecks = new ImmutableClassToInstanceMap.Builder<PostPredictionCheck>()
                .put(NegativeTimer.class, new NegativeTimer(player))
                .put(ExplosionHandler.class, new ExplosionHandler(player))
                .put(KnockbackHandler.class, new KnockbackHandler(player))
                .put(GhostBlockDetector.class, new GhostBlockDetector(player))
                .put(Phase.class, new Phase(player))
                .put(Post.class, new Post(player))
                .put(PacketOrderA.class, new PacketOrderA(player))
                .put(PacketOrderE.class, new PacketOrderE(player))
                .put(PacketOrderF.class, new PacketOrderF(player))
                .put(PacketOrderG.class, new PacketOrderG(player))
                .put(PacketOrderH.class, new PacketOrderH(player))
                .put(PacketOrderI.class, new PacketOrderI(player))
                .put(PacketOrderJ.class, new PacketOrderJ(player))
                .put(PacketOrderK.class, new PacketOrderK(player))
                .put(PacketOrderL.class, new PacketOrderL(player))
                .put(PacketOrderM.class, new PacketOrderM(player))
                .put(GroundSpoof.class, new GroundSpoof(player))
                .put(OffsetHandler.class, new OffsetHandler(player))
                .put(SuperDebug.class, new SuperDebug(player))
                .put(DebugHandler.class, new DebugHandler(player))
                .put(BadPacketsX.class, new BadPacketsX(player))
                .put(NoSlow.class, new NoSlow(player))
                .put(Speed.class, new Speed(player))
                .put(Spider.class, new Spider(player))
                .put(SprintB.class, new SprintB(player))
                .put(SprintC.class, new SprintC(player))
                .put(SprintD.class, new SprintD(player))
                .put(SprintE.class, new SprintE(player))
                .put(SprintF.class, new SprintF(player))
                .put(SprintG.class, new SprintG(player))
                .put(MultiInteractA.class, new MultiInteractA(player))
                .put(MultiInteractB.class, new MultiInteractB(player))
                .put(ElytraA.class, new ElytraA(player))
                .put(ElytraB.class, new ElytraB(player))
                .put(ElytraC.class, new ElytraC(player))
                .put(ElytraD.class, new ElytraD(player))
                .put(ElytraE.class, new ElytraE(player))
                .put(ElytraF.class, new ElytraF(player))
                .put(ElytraG.class, new ElytraG(player))
                .put(ElytraH.class, new ElytraH(player))
                .put(ElytraI.class, new ElytraI(player))
                .put(CrossSpeed.class, new CrossSpeed(player))
                .put(CrossAntiKB.class, new CrossAntiKB(player))
                .put(CrossFlight.class, new CrossFlight(player))
                .put(CrossFlightB.class, new CrossFlightB(player))
                .put(CrossPhaseB.class, new CrossPhaseB(player))
                .put(CrossJesus.class, new CrossJesus(player))
                .put(CrossNoFall.class, new CrossNoFall(player))
                .put(CrossFreecam.class, new CrossFreecam(player))
                .put(FastFall.class, new FastFall(player))
                .put(CrossJump.class, new CrossJump(player))
                .put(CrossTeleport.class, new CrossTeleport(player))
                .put(CrossFastEat.class, new CrossFastEat(player))
                .put(CrossFoodSprint.class, new CrossFoodSprint(player))
                .put(CrossCriticals.class, new CrossCriticals(player))
                .put(FightBot.class, new FightBot(player))
                .put(BackTrack.class, new BackTrack(player))
                .put(Freecam.class, new Freecam(player))
                .put(NoClip.class, new NoClip(player))
                .put(IrregularMovements.class, new IrregularMovements(player))
                // CrossNoSlowdown (configName "crossnoslowdown") merged into CrossFoodSprint (2026-05-18)
                .put(CrossVehicle.class, new CrossVehicle(player))
                .put(CrossEntitySpeed.class, new CrossEntitySpeed(player))
                .put(SetbackTeleportUtil.class, new SetbackTeleportUtil(player)) // Avoid teleporting to new position, update safe pos last
                .put(CompensatedFireworks.class, player.fireworks)
                .put(SneakingEstimator.class, new SneakingEstimator(player))
                .put(LastInstanceManager.class, player.lastInstanceManager)
                .put(Liquids.class, new Liquids(player))
                .put(CrossTower.class, new CrossTower(player))
                .build();

        blockPlaceChecks = new ImmutableClassToInstanceMap.Builder<BlockPlaceCheck>()
                .put(InvalidPlaceA.class, new InvalidPlaceA(player))
                .put(InvalidPlaceB.class, new InvalidPlaceB(player))
                .put(AirLiquidPlace.class, new AirLiquidPlace(player))
                .put(MultiPlace.class, new MultiPlace(player))
                .put(MultiActionsF.class, new MultiActionsF(player))
                .put(MultiActionsG.class, new MultiActionsG(player))
                .put(BadPacketsH.class, new BadPacketsH(player))
                .put(CrashG.class, new CrashG(player))
                .put(FarPlace.class, new FarPlace(player))
                .put(FabricatedPlace.class, new FabricatedPlace(player))
                .put(PositionPlace.class, new PositionPlace(player))
                .put(RotationPlace.class, new RotationPlace(player))
                .put(PacketOrderN.class, new PacketOrderN(player))
                .put(DuplicateRotPlace.class, new DuplicateRotPlace(player))
                .put(GhostBlockMitigation.class, new GhostBlockMitigation(player))
                .put(ScaffoldA.class, new ScaffoldA(player))
                .put(ScaffoldB.class, new ScaffoldB(player))
                .put(CrossScaffold.class, new CrossScaffold(player))
                .put(CrossFastPlace.class, new CrossFastPlace(player))
                .put(BlockReach.class, new BlockReach(player))
                .build();

        prePredictionChecks = new ImmutableClassToInstanceMap.Builder<PacketCheck>()
                .put(Timer.class, new Timer(player))
                .put(TickTimer.class, new TickTimer(player))
                .put(TimerLimit.class, new TimerLimit(player))
                .put(CrashA.class, new CrashA(player))
                .put(CrashC.class, new CrashC(player))
                .put(VehicleTimer.class, new VehicleTimer(player))
                .build();

        blockBreakChecks = new ImmutableClassToInstanceMap.Builder<BlockBreakCheck>()
                .put(AirLiquidBreak.class, new AirLiquidBreak(player))
                .put(WrongBreak.class, new WrongBreak(player))
                .put(RotationBreak.class, new RotationBreak(player))
                .put(FastBreak.class, new FastBreak(player))
                .put(MultiBreak.class, new MultiBreak(player))
                .put(FarBreak.class, new FarBreak(player))
                .put(InvalidBreak.class, new InvalidBreak(player))
                .put(PositionBreakA.class, new PositionBreakA(player))
                .put(PositionBreakB.class, new PositionBreakB(player))
                .put(MultiActionsB.class, new MultiActionsB(player))
                .put(CrossFastBreak.class, new CrossFastBreak(player))
                .put(CrossFastBreakB.class, new CrossFastBreakB(player))
                .put(Nuker.class, new Nuker(player))
                .build();

        // All checks that have no listeners, generally invoked by other code to flag
        // TODO migrate more checks to here
        ClassToInstanceMap<AbstractCheck> noneModules = new ImmutableClassToInstanceMap.Builder<AbstractCheck>()
                // BadPacketsN/W + VehicleC + TransactionOrder are packet checks with no listener
                .put(BadPacketsN.class, new BadPacketsN(player))
                .put(BadPacketsW.class, new BadPacketsW(player))
                .put(TransactionOrder.class, new TransactionOrder(player))
                .put(VehicleC.class, new VehicleC(player))
                .put(Hitboxes.class, new Hitboxes(player)) // Hitboxes is invoked by Reach
                .build();

        allChecks = new ImmutableClassToInstanceMap.Builder<AbstractCheck>()
                .putAll(packetChecks)
                .putAll(positionChecks)
                .putAll(rotationChecks)
                .putAll(vehicleChecks)
                .putAll(postPredictionChecks)
                .putAll(blockPlaceChecks)
                .putAll(prePredictionChecks)
                .putAll(blockBreakChecks)
                .putAll(noneModules)
                .build();

        packetChecksValues = new ArrayList<>(packetChecks.values());
        positionChecksValues = new ArrayList<>(positionChecks.values());
        rotationChecksValues = new ArrayList<>(rotationChecks.values());
        vehicleChecksValues = new ArrayList<>(vehicleChecks.values());
        prePredictionChecksValues = new ArrayList<>(prePredictionChecks.values());
        blockBreakChecksValues = new ArrayList<>(blockBreakChecks.values());
        blockPlaceChecksValues = new ArrayList<>(blockPlaceChecks.values());
        postPredictionChecksValues = new ArrayList<>(postPredictionChecks.values());

        init();
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractCheck> T getCheck(Class<T> check) {
        return (T) allChecks.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends PositionCheck> T getPositionCheck(Class<T> check) {
        return (T) positionChecks.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends RotationCheck> T getRotationCheck(Class<T> check) {
        return (T) rotationChecks.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends BlockPlaceCheck> T getBlockPlaceCheck(Class<T> check) {
        return (T) blockPlaceChecks.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends PacketCheck> T getPacketCheck(Class<T> check) {
        return (T) packetChecks.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends PacketCheck> T getPrePredictionCheck(Class<T> check) {
        return (T) prePredictionChecks.get(check);
    }

    @SuppressWarnings("unchecked")
    public <T extends PostPredictionCheck> T getPostPredictionCheck(Class<T> check) {
        return (T) postPredictionChecks.get(check);
    }

    public void onPrePredictionReceivePacket(final PacketReceiveEvent packet) {
        for (PacketCheck check : prePredictionChecksValues) {
            check.onPacketReceive(packet);
        }
    }

    public void onPacketReceive(final PacketReceiveEvent packet) {
        for (PacketCheck check : packetChecksValues) {
            check.onPacketReceive(packet);
        }
        for (PostPredictionCheck check : postPredictionChecksValues) {
            check.onPacketReceive(packet);
        }
        for (BlockPlaceCheck check : blockPlaceChecksValues) {
            check.onPacketReceive(packet);
        }
        for (BlockBreakCheck check : blockBreakChecksValues) {
            check.onPacketReceive(packet);
        }
    }

    public void onPacketSend(final PacketSendEvent packet) {
        for (PacketCheck check : prePredictionChecksValues) {
            check.onPacketSend(packet);
        }
        for (PacketCheck check : packetChecksValues) {
            check.onPacketSend(packet);
        }
        for (PostPredictionCheck check : postPredictionChecksValues) {
            check.onPacketSend(packet);
        }
        for (BlockPlaceCheck check : blockPlaceChecksValues) {
            check.onPacketSend(packet);
        }
        for (BlockBreakCheck check : blockBreakChecksValues) {
            check.onPacketSend(packet);
        }
    }

    public void onPositionUpdate(final PositionUpdate position) {
        for (PositionCheck check : positionChecksValues) {
            check.onPositionUpdate(position);
        }
    }

    public void onRotationUpdate(final RotationUpdate rotation) {
        for (RotationCheck check : rotationChecksValues) {
            check.process(rotation);
        }
        for (BlockPlaceCheck check : blockPlaceChecksValues) {
            check.process(rotation);
        }
    }

    public void onVehiclePositionUpdate(final VehiclePositionUpdate update) {
        for (VehicleCheck check : vehicleChecksValues) {
            check.process(update);
        }
    }

    public void onPredictionFinish(final PredictionComplete complete) {
        CrossValidationData cvd = player.crossValidationData;
        cvd.offsetFromPrediction = complete.getOffset();
        if (complete.getData() != null) {
            cvd.predictedDeltaX = complete.getData().getTo().getX() - complete.getData().getFrom().getX();
            cvd.predictedDeltaY = complete.getData().getTo().getY() - complete.getData().getFrom().getY();
            cvd.predictedDeltaZ = complete.getData().getTo().getZ() - complete.getData().getFrom().getZ();
        }
        for (PostPredictionCheck check : postPredictionChecksValues) {
            check.onPredictionComplete(complete);
        }
        for (BlockPlaceCheck check : blockPlaceChecksValues) {
            check.onPredictionComplete(complete);
        }
        for (BlockBreakCheck check : blockBreakChecksValues) {
            check.onPredictionComplete(complete);
        }
    }

    public void onBlockPlace(final BlockPlace place) {
        for (BlockPlaceCheck check : blockPlaceChecksValues) {
            check.onBlockPlace(place);
        }
    }

    public void onPostFlyingBlockPlace(final BlockPlace place) {
        for (BlockPlaceCheck check : blockPlaceChecksValues) {
            check.onPostFlyingBlockPlace(place);
        }
    }

    public void onBlockBreak(final BlockBreak blockBreak) {
        for (BlockBreakCheck check : blockBreakChecksValues) {
            check.onBlockBreak(blockBreak);
        }
        for (BlockPlaceCheck check : blockPlaceChecksValues) {
            check.onBlockBreak(blockBreak);
        }
    }

    public void onPostFlyingBlockBreak(final BlockBreak blockBreak) {
        for (BlockBreakCheck check : blockBreakChecksValues) {
            check.onPostFlyingBlockBreak(blockBreak);
        }
        for (BlockPlaceCheck check : blockPlaceChecksValues) {
            check.onPostFlyingBlockBreak(blockBreak);
        }
    }

    public ExplosionHandler getExplosionHandler() {
        return getPostPredictionCheck(ExplosionHandler.class);
    }

    public NoFall getNoFall() {
        return getPacketCheck(NoFall.class);
    }

    public KnockbackHandler getKnockbackHandler() {
        return getPostPredictionCheck(KnockbackHandler.class);
    }

    public CompensatedCooldown getCompensatedCooldown() {
        return getPositionCheck(CompensatedCooldown.class);
    }

    public NoSlow getNoSlow() {
        return getPostPredictionCheck(NoSlow.class);
    }

    public SetbackTeleportUtil getSetbackUtil() {
        return getPostPredictionCheck(SetbackTeleportUtil.class);
    }

    public DebugHandler getDebugHandler() {
        return getPostPredictionCheck(DebugHandler.class);
    }

    private void init() {
        if (inited || initedAtomic.getAndSet(true)) return;
        inited = true;

        final String[] permissions = {
                "sac.exempt.",
                "sac.nosetback.",
                "sac.nomodifypacket.",
        };

        for (final AbstractCheck check : allChecks.values()) {
            if (check.getConfigName() == null) continue;
            final String id = check.getConfigName().toLowerCase();
            for (String permissionName : permissions) {
                permissionName += id;
                SacAPI.INSTANCE.getPermissionManager().registerPermission(permissionName, PermissionDefaultValue.FALSE);
            }
        }
    }
}
