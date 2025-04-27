package com.app.happytails.utils;

import android.util.Log;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.Lists;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AccessToken {
    private static final String firebaseMessagingScope = "https://www.googleapis.com/auth/firebase.messaging";

    // This function retrieves the access token
    public static String getAccessToken(){
        try{
            // For production, avoid hardcoding the key. Store securely and load it from a file or environment variable.
            String jsonString = "{\n" +
                    "  \"type\": \"service_account\",\n" +
                    "  \"project_id\": \"rational-photon-380817\",\n" +
                    "  \"private_key_id\": \"ac84eaf87b4f708e5e0e7df84ce139caafaabc38\",\n" +
                    "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDivcXkBq3/he/a\\nwiKQzRFrnzO5XWP8EilFhUUoegtC85o+DM+Z17WSPf+AnlwOLq+5jCtdnMW8kQTz\\nDmVAOwPCzuG5t7DC383fCkIL1Kl2YhSrf3u8FVJ5FEWt36ZOHrbQW0r7HPqfWwbI\\nzSbgHr2buRtNx1Qhzu7VMkUzIgm/t73c74bhrMUseKerHUvnMq0XdZa4TnXPgOfT\\noEC4/1WScmv+HRZ73s9aB4Oq0gpFV53LEvTkFMXt3gX6I5BrTxEM+BUWne+MUTCX\\n3S5m8VQuD1UdRbr/ppn6D5Q3sWU3nh6uIQMlo+84IBjpq6jFeWLAs9t1n2pKmNfM\\n7pM6JG03AgMBAAECggEAUWt++6ZwZdDFNANBYUjaKBDHhJkmFba5zkaQcnv8vkJ2\\nkTCNfbtpboXH0XpMKSWXoWPVkKyCjvduVQ9GyX1HmPUsNkHhfeDa7uwiklf/sEyB\\nCqJhHsVzU5o1eT3V3LiHwiL+NlUOJBoOQh31B0bSpHAgf1oD+o3x0mnWUom3AXn+\\nyN1j5D07C71Oe1atwIoZnbwpddvkQRJcWCtLuuYGcJwwi7gFPSvgiMSSmiiqwUqF\\numdoRho1aM1NuSIO+WziCBJohhhmkUtob2rEkU8c8M9mC3xngMZTl5eooSdrVsHS\\nNjyd9tDXaswa2V2m/QOjvPAFHNMcAouhShlJUufTOQKBgQD7mIWyXOjFbbDWVBC/\\nufeUuGs7nerKXbV64yNwcrxmvcdh/7nKD8yxjg6jlOpSQJTJKA8V3v48khZtVT9m\\nIoGFyXdTZP/JK4Jd+hpd6xXCrj+rp3i/QpdOKXmXBBYuFS/wOR8gCfqigtyrAZXY\\nPF1JfzXnHA1s1PANbcUZpPGCTwKBgQDmtd7EXPuowNlXB8oTbWLXlUEBX/CtDKd2\\nowzaCneXFiAwa+9nUlZbpIboI2bms9SRMXv4EQ3cc2pdeo+9LrwtdPBuJPsfk8RE\\nuV4PvqAk2t+jVkgtleO4Ssx5oKBOcF1oY0clWHjZ+D1blS18qHmzuWHd3dRQ0oNr\\nqhbwgXK0mQKBgQDmFAjPn59OTI1WsvHOIyaB3lRR5Iv+G8wGYQjboFEiM5LNz6n7\\nWo96H8rLVTcjmON3QSbqfU5J3d6chUTBBfUkf6SbotU3Bo7lmf3avUzdB7Q6KaCG\\nZ0Muu0byD06pPb7lE5efGQEW9E0QJRb+89TrjWWhv0mXqPMNlMCWPvyMiQKBgQDg\\nUxhun+aGmDT7nXRL3YFNEy/o0UtoR7SQ80ssux67BmV4D4rxUKrtYpVWJA4K5fIa\\n1x2t/48VuhdDG0el8EpCfMDGqCiQ9JHTLNYbwwNdsn/fBqcZw/NunzQgUyFsA2+f\\nb2CfHF4tumSWpv9ahUoIiYlyPB4UFAx65CB367YHiQKBgAZ9Y3wEr9taDlHlbXxl\\ngr6SndeQWL2Q68V4InRukIpdpI/Shx3KQayA/2vgdne71bDdy/q9jywCFB+Vw+ie\\nYvu+5v8HEsT96N9FDz29zQA2h6ZRJensYn2zVE8BilU8ioYJWstpFRCzv1y8AyIL\\nYtNBVXrcy2w3NjU5DDD+J2ut\\n-----END PRIVATE KEY-----\\n\",\n" +
                    "  \"client_email\": \"firebase-adminsdk-kq8br@rational-photon-380817.iam.gserviceaccount.com\",\n" +
                    "  \"client_id\": \"109336487132653487451\",\n" +
                    "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                    "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                    "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                    "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-kq8br%40rational-photon-380817.iam.gserviceaccount.com\",\n" +
                    "  \"universe_domain\": \"googleapis.com\"\n" +
                    "}\n";
            // Load the service account credentials from the json string
            InputStream inputStream = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));
            GoogleCredentials googleCredentials = GoogleCredentials.fromStream(inputStream)
                    .createScoped(Lists.newArrayList(firebaseMessagingScope));
            googleCredentials.refreshIfExpired();

            // Log the token retrieval process
            Log.d("AccessToken", "Access token retrieved successfully");

            // Return the access token
            return googleCredentials.getAccessToken().getTokenValue();

        } catch(IOException e){
            Log.e("AccessToken", "Error generating access token: " + e.getMessage());
            return null;
        }
    }
}
