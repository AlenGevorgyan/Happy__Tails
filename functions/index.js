const functions = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const axios = require("axios");
const querystring = require("querystring");

admin.initializeApp();
const db = admin.firestore();

exports.exchangePatreonCode = functions.onRequest(async (req, res) => {
  const { code, firebaseUid, redirect_uri } = req.body;
  console.log("Function received code:", code);
  console.log("Function received firebaseUid:", firebaseUid);
  console.log("Function received redirect_uri:", redirect_uri);

  // 1.  Validate input
  if (!code || !firebaseUid || !redirect_uri) {
    return res.status(400).send({ error: "Missing code, firebaseUid, or redirect_uri" });
  }

  // 2.  Exchange code for tokens
  const tokenEndpoint = 'https://www.patreon.com/api/oauth2/token';
  const requestBody = {
    code: code,
    grant_type: 'authorization_code',
    client_id: process.env.PATREON_CLIENT_ID, // Use secret from env
    client_secret: process.env.PATREON_CLIENT_SECRET, // Use secret from env
    redirect_uri: redirect_uri, // Use provided redirect_uri
  };

  try {
    const response = await axios.post(tokenEndpoint, querystring.stringify(requestBody), {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });

    if (response.status !== 200) {
      console.error("Patreon token exchange failed:", response.status, response.data);
      return res.status(500).send({ error: "Failed to exchange code for tokens" });
    }

    const { access_token, refresh_token, expires_in } = response.data;
    console.log("Successfully exchanged code for tokens");
    console.log("Access Token: ", access_token);
    console.log("Refresh Token: ", refresh_token);

    // 3. Store tokens in Firestore
    const userPatreonTokensRef = admin.firestore().collection('userPatreonTokens').doc(firebaseUid);
    await userPatreonTokensRef.set({
      access_token: access_token,
      refresh_token: refresh_token,
      expires_at: admin.firestore.Timestamp.fromDate(new Date(Date.now() + expires_in * 1000)),
    });

    console.log("Tokens stored in Firestore");
    return res.status(200).send({ success: true, message: "Tokens stored successfully" });
  } catch (error) {
    console.error("Error during token exchange or storage:", error);
    return res.status(500).send({ error: "Error processing request", details: error.message });
  }
});