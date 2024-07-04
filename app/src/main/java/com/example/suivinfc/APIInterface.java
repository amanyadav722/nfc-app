package com.example.suivinfc;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface APIInterface {
    @POST("verifyToken")
    Call<TokenResponse> verifyToken(@Body TokenRequest tokenRequest);
}
