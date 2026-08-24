const functions = require("firebase-functions");
const admin = require("firebase-admin");

const DATABASE_URL = "https://safetap-2fb1e-default-rtdb.asia-southeast1.firebasedatabase.app";

admin.initializeApp({
  databaseURL: DATABASE_URL
});

/**
 * Validates a 256-bit cryptographic hex token
 */
function isValidToken(token) {
  return typeof token === "string" && /^[a-f0-9]{64}$/i.test(token.trim());
}

/**
 * Helper to get safe token prefix for diagnostic logs
 */
function safeTokenPrefix(token) {
  if (!token) return "none";
  return token.substring(0, 8) + "... (" + token.length + " chars)";
}

/**
 * HTTPS Cloud Function: getLiveLocation (Polling / Single Request Endpoint)
 *
 * Security Controls:
 * 1. Requires a 256-bit unguessable cryptographic token.
 * 2. Does NOT expose full SOS records, Firebase UIDs, phone numbers, or metadata.
 * 3. Returns only sanitized GPS coordinates, accuracy, battery, and status while ACTIVE.
 * 4. Revocation: When status is ENDED, coordinates are refused and only the ENDED status is returned.
 */
exports.getLiveLocation = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "GET, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") {
    res.status(204).send("");
    return;
  }

  if (req.method !== "GET") {
    res.status(405).json({ error: "Method not allowed. Use GET." });
    return;
  }

  const token = req.query.token;
  const sosId = req.query.sosId;

  if (!token || !isValidToken(token)) {
    res.status(400).json({ error: "A valid 256-bit liveLocationToken is required." });
    return;
  }

  try {
    const sanitizedToken = token.trim().toLowerCase();
    const db = admin.database();

    // 1. Primary lookup: sanitized live_locations/$token
    let liveLocSnapshot = await db.ref(`live_locations/${sanitizedToken}`).get();
    let data = liveLocSnapshot.val();

    // 2. Secondary fallback lookup: sos/$sosId (if provided and token matches)
    if (!liveLocSnapshot.exists() && sosId && typeof sosId === "string" && sosId.trim().length > 0 && sosId !== "Live SOS") {
      const sanitizedSosId = sosId.trim();
      const sosSnapshot = await db.ref(`sos/${sanitizedSosId}`).get();
      if (sosSnapshot.exists()) {
        const sosData = sosSnapshot.val();
        const storedToken = (sosData.liveLocationToken || "").trim().toLowerCase();
        if (storedToken === sanitizedToken) {
          const loc = sosData.currentLocation || {};
          data = {
            latitude: loc.latitude || sosData.latitude || 0.0,
            longitude: loc.longitude || sosData.longitude || 0.0,
            accuracy: loc.accuracy || sosData.locationAccuracy || 0.0,
            battery: sosData.battery != null ? sosData.battery : null,
            status: sosData.status || "ACTIVE",
            timestamp: loc.timestamp || sosData.timestamp || Date.now(),
            startedAt: sosData.startedAt || Date.now(),
            endedAt: sosData.endedAt || null
          };

          // Backfill live_locations node for fast subsequent reads
          await db.ref(`live_locations/${sanitizedToken}`).set(data).catch(() => {});
        }
      }
    }

    functions.logger.info("getLiveLocation Query", {
      sosId: sosId || "none",
      tokenPrefix: safeTokenPrefix(sanitizedToken),
      recordFound: !!data,
      status: data ? data.status : "NOT_FOUND",
      databaseUrl: DATABASE_URL
    });

    if (!data) {
      res.status(404).json({ error: "Live tracking session not found or has expired." });
      return;
    }

    const status = data.status || "ACTIVE";
    const isEnded = status === "ENDED" || status === "CANCELLED" || status === "RESOLVED";

    if (isEnded) {
      res.status(200).json({
        status: "ENDED",
        endedAt: data.endedAt || Date.now()
      });
      return;
    }

    // Active session: Return sanitized location
    const sanitizedResponse = {
      status: "ACTIVE",
      latitude: data.latitude || 0.0,
      longitude: data.longitude || 0.0,
      accuracy: data.accuracy || 0.0,
      battery: data.battery != null ? data.battery : null,
      timestamp: data.timestamp || Date.now(),
      startedAt: data.startedAt || null
    };

    res.status(200).json(sanitizedResponse);
  } catch (error) {
    functions.logger.error("Error retrieving live location:", error);
    res.status(500).json({ error: "Internal server error." });
  }
});

/**
 * HTTPS Cloud Function: liveStream (Server-Sent Events Realtime Stream)
 *
 * Streams sanitized realtime updates directly to the web client using SSE.
 * When SOS ends, sends the final ENDED event and immediately terminates the stream.
 */
exports.liveStream = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "GET, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") {
    res.status(204).send("");
    return;
  }

  const token = req.query.token;
  const sosId = req.query.sosId;

  if (!token || !isValidToken(token)) {
    res.status(400).json({ error: "A valid 256-bit liveLocationToken is required." });
    return;
  }

  // Set SSE Headers
  res.writeHead(200, {
    "Content-Type": "text/event-stream",
    "Cache-Control": "no-cache",
    "Connection": "keep-alive"
  });

  const sanitizedToken = token.trim().toLowerCase();
  const db = admin.database();
  const tokenRef = db.ref(`live_locations/${sanitizedToken}`);

  let initialCheckDone = false;

  const onValueChange = async (snapshot) => {
    let data = snapshot.val();

    if (!snapshot.exists()) {
      if (!initialCheckDone && sosId && typeof sosId === "string" && sosId.trim().length > 0 && sosId !== "Live SOS") {
        initialCheckDone = true;
        const sosSnapshot = await db.ref(`sos/${sosId.trim()}`).get().catch(() => null);
        if (sosSnapshot && sosSnapshot.exists()) {
          const sosData = sosSnapshot.val();
          const storedToken = (sosData.liveLocationToken || "").trim().toLowerCase();
          if (storedToken === sanitizedToken) {
            const loc = sosData.currentLocation || {};
            data = {
              latitude: loc.latitude || sosData.latitude || 0.0,
              longitude: loc.longitude || sosData.longitude || 0.0,
              accuracy: loc.accuracy || sosData.locationAccuracy || 0.0,
              battery: sosData.battery != null ? sosData.battery : null,
              status: sosData.status || "ACTIVE",
              timestamp: loc.timestamp || sosData.timestamp || Date.now(),
              startedAt: sosData.startedAt || Date.now()
            };
            await tokenRef.set(data).catch(() => {});
          }
        }
      }

      if (!data) {
        res.write(`event: error\ndata: ${JSON.stringify({ error: "Session pending or not found" })}\n\n`);
        return;
      }
    }

    initialCheckDone = true;
    const status = data.status || "ACTIVE";
    const isEnded = status === "ENDED" || status === "CANCELLED" || status === "RESOLVED";

    if (isEnded) {
      res.write(`event: message\ndata: ${JSON.stringify({ status: "ENDED", endedAt: data.endedAt || Date.now() })}\n\n`);
      tokenRef.off("value", onValueChange);
      res.end();
      return;
    }

    const payload = {
      status: "ACTIVE",
      latitude: data.latitude || 0.0,
      longitude: data.longitude || 0.0,
      accuracy: data.accuracy || 0.0,
      battery: data.battery != null ? data.battery : null,
      timestamp: data.timestamp || Date.now()
    };

    res.write(`event: message\ndata: ${JSON.stringify(payload)}\n\n`);
  };

  tokenRef.on("value", onValueChange);

  req.on("close", () => {
    tokenRef.off("value", onValueChange);
  });
});
