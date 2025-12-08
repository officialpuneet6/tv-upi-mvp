const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

// 1. Receives data from Mobile App
exports.api = functions.https.onRequest(async (req, res) => {
    if (req.method !== "POST") {
        return res.status(405).send("Method Not Allowed");
    }

    try {
        const { amount, sender } = req.body;

        if (!amount || !sender) {
            return res.status(400).send("Missing amount or sender");
        }

        const event = {
            amount: amount.toString(),
            sender: sender,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            read: false // Mark as unread initially
        };

        // Add to Firestore
        await db.collection("tv_notifications").add(event);

        return res.status(200).send("Notification sent");
    } catch (error) {
        console.error("Error adding document: ", error);
        return res.status(500).send("Internal Server Error");
    }
});

// 2. TV App Polls this URL (e.g., every 3 seconds)
exports.getLatest = functions.https.onRequest(async (req, res) => {
    try {
        // Get the most recent unread notification from the last minute
        // (This prevents re-reading old alerts if system restarts)
        const now = admin.firestore.Timestamp.now();
        const oneMinuteAgo = new admin.firestore.Timestamp(now.seconds - 60, now.nanoseconds);

        const snapshot = await db.collection("tv_notifications")
            .where("createdAt", ">", oneMinuteAgo)
            .orderBy("createdAt", "desc")
            .limit(1)
            .get();

        if (snapshot.empty) {
            return res.status(200).json({ found: false });
        }

        const doc = snapshot.docs[0];
        const data = doc.data();

        // Optional: Mark as read so we don't fetch it again immediately 
        // (Logic depends on polling freq, but simple timestamp check is safer for stateless TV)
        // For MVP, if it sends "found: true", client shows it. 
        // Client should track "lastId" to avoid duplicate popups, but let's send ID back.

        return res.status(200).json({
            found: true,
            id: doc.id,
            amount: data.amount,
            sender: data.sender,
            timestamp: data.createdAt ? data.createdAt.toMillis() : Date.now()
        });

    } catch (error) {
        console.error("Error fetching documents: ", error);
        return res.status(500).send("Internal Error");
    }
});
