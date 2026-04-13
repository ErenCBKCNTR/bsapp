package com.blind.social.data

import com.blind.social.SupabaseModul
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

class OdaDeposu {

    fun odalariGercekZamanliDinle(): Flow<Result<List<Oda>>> = flow {
        while (true) {
            try {
                val odalar = SupabaseModul.client.postgrest["odalar"]
                    .select()
                    .decodeList<Oda>()

                emit(Result.success(odalar))
            } catch (e: Exception) {
                emit(Result.failure(e))
            }
            delay(3000)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun odaOlustur(oda: Oda): Result<Unit> {
        return try {
            SupabaseModul.client.postgrest["odalar"].insert(oda)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
