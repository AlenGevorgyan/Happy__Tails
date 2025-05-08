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
                    "  \"private_key_id\": \"29f1096e5191a88d7db500cca782dbced1a65d02\",\n" +
                    "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCrUlHNk3enuvc9\\nskY5tfZ4hi8gSruRSxbM91scXDpGNbmMGfw+rzWPW+OdZGyUv9Ve/62DhzFE3ulS\\n2za6MAsRDPCJoeh6tl9B29jxEnhRgWLOkYP2GwkdwyLCj+4Ng7w+G3FDjMiHsjNG\\n4NT6LJJPVMRLIJkbrs7yqbfnwhhF1zRtQyVa/rIEcm2P5DGwqu7XYplTvh+OhSmL\\nC1ILu2MueF6P6BJKXBTMvQnbh2lSmOqbRDv48TUsqyYBKALyf3gREyJtBSOV+6LH\\nO8sTOLc3uHAqG/Q0epaGGZKSLzs/be5RweyofqiiiX0r75Nz/YqwyrJtgVPAULm4\\nJraX4tM/AgMBAAECggEACwyMFLi7XCiauXKTROA6EnEqU5LToKiPF+OmufWDbJ5v\\nu7Fmq0RG2AVnxfGBbzZ6tF3Dpa/vutla2D/QZRb5dRK4kKfN2TGEgzgtOEml/jEH\\nW6XW35BuzhSVBqdHBb1xQ9rROzdhLMnkCguGBicfEy63BNays9kE+nF9At1puKya\\nwXVLIZ+qdZLi7SuRUD8LY6iDcvl3zFzh0KX8zhKsPrtxr7i0pQI++zLhZ0Aacnzx\\nT/dRLLopxMudr2ozSoqxUIzv39oqpNzYdMFJj580pYRqlzwEZ6x5sEQR/7rG+rke\\nqtvNtT0L0pkHe+jSS+0MqJzekb35xHzaaI654QkoAQKBgQDXhHXBOE3MmkpD0y9w\\neYZc9QKL6NVCQ0OD/AaYiJUeVUdKAa7qhoHxu+8MsNa9PN749RQID+u2QEn8TQhE\\nffIAVeu87ht4KlgULwZkaMLq4FWuQFrrW7LK89g/JTCGrlk+Rr4M9RPM7hKf1kMI\\nTKhJUMPbYzoyLgkk2eLJlBXwgQKBgQDLgJ9nl8foKoUA3uB0lpN4r5W6rC/DQpL/\\nq/EDPWgwFuiyUyAt4SdCtzaIbKh+YYNx6ssv81FWSOyPmNXEoO37KzBqX8dHBHZV\\ne60vX3ibwzZ+/JLxsC/jrDuyfS4bcO74e8OOjr+WwouS5oiicnoLkipC2q3NmKmG\\npnlmpkLjvwKBgCbAYowDm7mWZJQdfQI0V7yPMY4Gp5HqllE9F9KFfIqfutnLw6/X\\nz7WvnSQjB/mu4EjTiG6Krk9u79612y61lrYIvQKTrczFkGDQs82KKMIi5EUYvUBk\\nlJl5jwCLp1YQM2vHPJUH3C+U/zROSm9LmVbVduITzXsIXAyMQQMN5YQBAoGAMbBM\\nO70hwJxxMU86Ov1xMKyewJ++Yczlm6veA06BHAzb8H6/grRRI8ccd58gXQtuTuhn\\niGO+3nSb3kwRFhu9P/DBt5d/TuXWBvVVyhVjm5TEv6joMD820j6BrQlHsseWcfG2\\nRB6yxRrnfYzwm8mb5ytfSkee3G6EJi6Lwfaiqn0CgYEAtUVE/ZfS/ec8u7jwsTBR\\ntEZbbKib2Z32E6DnmwzHK7sOvQElLkEKOCnMsgGA3C3h8fawOEpoaIT3n/uo3Go2\\nvS0mLveuIylugqqkR3eV5URoaqxQQbZrKsQ5/S5NifE3v2JPCvVjrKE0CvtG05g/\\n2yyPxdfbJhVMvwhHOLXjUDM=\\n-----END PRIVATE KEY-----\\n\",\n" +
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
