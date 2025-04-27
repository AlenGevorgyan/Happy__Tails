package com.app.happytails.utils;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface PatreonApiService {

    @GET("api/oauth2/v2/campaigns")
    Call<CampaignResponse> getCampaigns(
            @Header("Authorization") String authHeader
    );
}
