package com.intuit.catapp.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface ICatService {

    // Task 1 - Updated from hardcoded "breeds?limit=30&page=0" to
    // support dynamic pagination via @Query parameter.
    // This allow fetching all breeds regardless of total count,
    // rather than assuming there are fewer than 30.
    @GET("breeds")
    suspend fun getBreeds(
        @Query("limit") limit: Int,
        @Query("page") page: Int
    ): List<Breed>
}

/**
 * Find out more information on the Cat API by checking out the documentation here:
 * https://documenter.getpostman.com/view/5578104/RWgqUxxh#intro
 */
object CatService {

    private lateinit var service: ICatService

    fun init() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.thecatapi.com/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create(ICatService::class.java)
    }

    fun getService(): ICatService {
        return service
    }
}
