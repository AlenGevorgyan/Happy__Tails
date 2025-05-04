const functions = require("firebase-functions/v2/https");
const { onDocumentCreated } = require("firebase-functions/v2/firestore"); // Correct import for Firestore triggers
const admin = require("firebase-admin");
const axios = require("axios");
const querystring = require("querystring");

admin.initializeApp();
const db = admin.firestore();

// ---- 1. Patreon Token Exchange Function ----
exports.exchangePatreonCode = functions.onRequest(async (req, res) => {
  const { code, firebaseUid, redirect_uri } = req.body;
  console.log("Function received code:", code);
  console.log("Function received firebaseUid:", firebaseUid);
  console.log("Function received redirect_uri:", redirect_uri);

  if (!code || !firebaseUid || !redirect_uri) {
    return res.status(400).send({ error: "Missing code, firebaseUid, or redirect_uri" });
  }

  const tokenEndpoint = 'https://www.patreon.com/api/oauth2/token';
  const requestBody = {
    code,
    grant_type: 'authorization_code',
    client_id: process.env.PATREON_CLIENT_ID,
    client_secret: process.env.PATREON_CLIENT_SECRET,
    redirect_uri,
  };

  try {
    const response = await axios.post(tokenEndpoint, querystring.stringify(requestBody), {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    });

    if (response.status !== 200) {
      console.error("Patreon token exchange failed:", response.status, response.data);
      return res.status(500).send({ error: "Failed to exchange code for tokens" });
    }

    const { access_token, refresh_token, expires_in } = response.data;
    console.log("Successfully exchanged code for tokens");

    const userPatreonTokensRef = db.collection('userPatreonTokens').doc(firebaseUid);
    await userPatreonTokensRef.set({
      access_token,
      refresh_token,
      expires_at: admin.firestore.Timestamp.fromDate(new Date(Date.now() + expires_in * 1000)),
    });

    console.log("Tokens stored in Firestore");
    return res.status(200).send({ success: true, message: "Tokens stored successfully" });
  } catch (error) {
    console.error("Error during token exchange or storage:", error);
    return res.status(500).send({ error: "Error processing request", details: error.message });
  }
});

// ---- 2. Firestore Trigger for Sending Chat Notifications ----
exports.sendChatNotification = onDocumentCreated('chatrooms/{chatroomId}/chats/{messageId}', async (event) => {
  const snap = event.data;
  if (!snap) {
    console.log("No document data found!");
    return;
  }

  const messageData = snap.data();
  const senderId = messageData.senderId;
  const messageText = messageData.message;

  try {
    // Fetch chatroom document
    const chatroomDoc = await db.collection('chatrooms').doc(event.params.chatroomId).get();
    if (!chatroomDoc.exists) {
      console.error("Chatroom not found:", event.params.chatroomId);
      return;
    }

    const { user1, user2 } = chatroomDoc.data(); // adjust if your field names differ
    const recipientId = senderId === user1 ? user2 : user1;

    // Fetch recipient's FCM token
    const userDoc = await db.collection('users').doc(recipientId).get();
    if (!userDoc.exists) {
      console.error("Recipient user not found:", recipientId);
      return;
    }

    const recipientFcmToken = userDoc.data().fcmToken;
    if (!recipientFcmToken) {
      console.error("Recipient FCM token not found for:", recipientId);
      return;
    }

    const payload = {
      notification: {
        title: `New message`,
        body: messageText,
      }
    };

    const response = await admin.messaging().sendToDevice(recipientFcmToken, payload);
    console.log("Notification sent successfully:", response);
  } catch (error) {
    console.error("Error sending notification:", error);
  }
});
