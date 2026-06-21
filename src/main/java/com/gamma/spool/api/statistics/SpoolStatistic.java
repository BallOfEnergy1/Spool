package com.gamma.spool.api.statistics;

import java.util.UUID;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * @param serverMSPT               The server's MSPT. Will be -1 on clients.
 * @param worldMSPT                Each world's MSPT (dimensionID -> MSPT). Will be null on clients.
 * @param allThreadManagers        A list of all Spool's thread managers (views).
 * @param dimensionThreadManagers  A map of dimensionIDs to thread manager's controlled by spool.
 *                                 If dimension threading is disabled (or if this is a client), this will be null.
 * @param distanceThreadingManager The thread manager for Spool's distance threading system.
 *                                 If distance threading is disabled (or if this is a client), this will be null.
 * @param distanceThreadingCache   The internal cache for the distance threading system.
 *                                 <p>
 *                                 <b>
 *                                 This should NEVER be provided to the client side under ANY circumstances.
 *                                 </b>
 *                                 All processing using this variable should be done ONLY on the server side.
 *                                 </p>
 *                                 <p>
 *                                 This cache contains the following information:
 *                                 <br>
 *                                 1. A cached representation of ALL persistent chunks across ALL worlds (in
 *                                 long-encoded form).
 *                                 <br>
 *                                 2. A cached representation of ALL players and their nearest players ALL worlds
 *                                 (server-side; player hashcode ->
 *                                 `Nearby` map).
 *                                 <br>
 *                                 3. A cached representation of ALL players nearby to chunks ALL worlds (a `Nearby`
 *                                 mapping for every loaded
 *                                 chunk).
 *                                 <br>
 *                                 4. The cached number of loaded chunks in ALL worlds combined.
 *                                 </p>
 *                                 <p>
 *                                 Generally speaking, all of this information is extremely sensitive. Don't let it get
 *                                 out of your reach (and into a client's hands).
 *                                 </p>
 * @param playerHashcodeMap        A map for finding a player's hashcode from their instance.
 *                                 If distance threading is disabled (or if this is a client), this will be null.
 */
public record SpoolStatistic(double serverMSPT, ImmutableMap<Integer, Double> worldMSPT,
    ImmutableList<IThreadManagerView> allThreadManagers,
    ImmutableMap<Integer, IThreadManagerView> dimensionThreadManagers, IThreadManagerView distanceThreadingManager,
    ICache distanceThreadingCache, ImmutableMap<UUID, Integer> playerHashcodeMap) {}
