const functions = require('firebase-functions');
const fetch = require('node-fetch');

const CLIENT_ID = 'G-CclZkqwQw7ywcMRqtnWElPbMVww0TKygu3e0UNeO_TuYpdU7o_2Wqror-rItER'; // Replace with your client ID
const CLIENT_SECRET = 'CFFhBdm-p8pa1wTNZoAMwwF8CFyPeGQp6-ZS8dtdtS49MIEgT6Sb5otK2FPfFMuB'; // Replace with your client secret
const REDIRECT_URI = 'https://rational-photon-380817.web.app/redirect_patreon';

exports.exchangePatreonCode = functions.https.onCall(async (data, context) => {
  const { code } = data;
  if (!code) throw new functions.https.HttpsError('invalid-argument', 'Authorization code required');

  const url = 'https://www.patreon.com/api/oauth2/token';
  const body = new URLSearchParams({
    code,
    grant_type: 'authorization_code',
    client_id: CLIENT_ID,
    client_secret: CLIENT_SECRET,
    redirect_uri: REDIRECT_URI
  });
  try {
    const response = await fetch(url, { method: 'POST', body });
    if (!response.ok) throw new Error(`Token exchange failed: ${response.statusText}`);
    const data = await response.json();
    return { accessToken: data.access_token, refreshToken: data.refresh_token, expiresIn: data.expires_in };
  } catch (error) {
    throw new functions.https.HttpsError('internal', `Error: ${error.message}`);
  }
});

exports.checkPatreonStatus = functions.https.onCall(async (data, context) => {
  const { accessToken } = data;
  if (!accessToken) throw new functions.https.HttpsError('invalid-argument', 'Access token required');

  const url = 'https://www.patreon.com/api/oauth2/v2/identity?include=memberships.campaign&fields[member]=patron_status';
  try {
    const response = await fetch(url, { headers: { Authorization: `Bearer ${accessToken}` } });
    if (!response.ok) throw new Error(`API request failed: ${response.statusText}`);
    const data = await response.json();
    const memberships = data.included || [];
    const isPatron = memberships.some(m => m.attributes.patron_status === 'active_patron');
    return { isPatron };
  } catch (error) {
    throw new functions.https.HttpsError('internal', `Error: ${error.message}`);
  }
});