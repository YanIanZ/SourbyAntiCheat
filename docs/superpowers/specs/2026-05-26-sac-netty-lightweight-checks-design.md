# SAC Netty-Only Lightweight Checks — Design

Date: 2026-05-26
Branch: `feat/minigame-rewrite`
Status: Approved (design), pending implementation

## Goal

Turn the raw-netty signals the channel handler already gathers (packet rate,
oversized packets, packet-timing uniformity) — currently only `LOGGER.warning`'d —
into real, content-agnostic checks that **flag directly from
`SacNettyChannelHandler` on the netty thread**, with no PacketEvents decode and no
prediction-engine involvement. The cheapest detection layer, before packet decode.

## Constraint (why content-agnostic only)

The raw handler sees an undecoded `ByteBuf`. Content-aware checks (CPS-on-attacks,
KillAura, etc.) need the packet *type*, which only PacketEvents decodes — those
stay as `PacketCheck`s (already netty-thread). Only rate/size/timing signals need
no decode, so only those move into the handler.

## No duplication

- `NettyFlood` = cancelled-packets/tick (different signal). Untouched.
- `NettyDelay` = flying-packet timer spacing (decoded). Untouched.
- The handler's `lastWindowPacketRate` / oversized / `nettyIntervalVariance` feed
  only `CrossValidationData` today (display/cross-validate) — never flagged. This
  fills that gap.

## Components — 3 new checks (`checks/impl/netty/`)

Each extends `Check` (no listener interface — flagged externally), registered in
`CheckManager`'s `noneModules` map (full VL/alert/config/GUI plumbing, **no**
packet-loop cost). Thresholds via `@CheckData` + `onReload`. Each exposes a small
feed method the handler calls; the method holds the decision + buffer.

1. **`NettyPacketRate`** (alert-only, `setback = -1`) — `void onWindow(double ratePerSec)`.
   Flags when a completed 1s window's inbound rate `> max-rate` (default **500**;
   normal ~20-120) for `> buffer` consecutive windows. Catches packet spam/flood.
2. **`NettyOversized`** (`setback` default — blatant crash attempt) —
   `void onPacket(int sizeBytes)`. Flags immediately when a single packet
   `> max-bytes` (default **2,097,152** = 2 MiB).
3. **`NettyUniformTiming`** (alert-only, `setback = -1`) —
   `void onSample(double avgVarianceMs, int samples)`. Flags when
   `samples >= min-samples` (default **200**) and average inter-packet interval
   variance `< epsilon-ms` (default **1.0**) — bot-perfect spacing.

## Handler wiring (`SacNettyChannelHandler.channelRead`)

Where it currently `LOGGER.warning`s flood/oversized + accumulates variance, also
resolve the player's check and feed it (guarded — must never break the pipeline):

```java
SacPlayer sp = SacAPI.INSTANCE.getPlayerDataManager().getPlayer(playerUuid);
if (sp != null) {
    try { sp.checkManager.getCheck(NettyOversized.class).onPacket(size); } catch (Throwable ignored) {}
    // on window roll:
    try { sp.checkManager.getCheck(NettyPacketRate.class).onWindow(lastWindowPacketRate); } catch (Throwable ignored) {}
    // on variance sample:
    try { sp.checkManager.getCheck(NettyUniformTiming.class).onSample(avgVar, intervalSampleCount); } catch (Throwable ignored) {}
}
```

Flagging from the netty event loop is the same context a `PacketCheck` already
flags in (`flagAndAlert` is called from the netty thread throughout SAC), so no new
threading risk. Existing `LOGGER.warning` lines kept or removed (kept at low rate).

## Config (`config/en.yml`)

Per-check keys with defaults read in `onReload`:
`NettyPacketRate.max-rate` / `.buffer`, `NettyOversized.max-bytes`,
`NettyUniformTiming.min-samples` / `.epsilon-ms`. No config-version bump (additive,
default-backed; `checks.enabled.<name>` defaults true).

## Testing

- The decision in each feed method is simple; the netty/SacAPI flag path is
  runtime-verified (consistent with NettyFlood/NettyDelay, which have no unit
  tests). Where trivial, the threshold comparison is the whole logic.
- Build green; manual smoke: a packet-flood/timer client trips
  NettyPacketRate/NettyUniformTiming; a giant payload trips NettyOversized; normal
  play does not.

## Risks

- Raw packet-rate can spike legitimately (chunk/login bursts) → `NettyPacketRate`
  is alert-only with a high default + multi-window buffer. `NettyUniformTiming`
  alert-only (proxies can normalize spacing → FP) — tune `epsilon-ms` up if noisy.
- `NettyOversized` at 2 MiB is far above any legit packet → safe to setback.

## Out of scope

- Content-aware checks in the handler (need decode; stay PacketEvents-driven).
- Replacing NettyFlood/NettyDelay (different signals; kept).
