package com.blind.social.data

import com.blind.social.SupabaseModul
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.gotrue.auth
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

    suspend fun odaOlustur(oda: Oda): Result<Oda> {
        return try {
            val user = SupabaseModul.client.auth.currentUserOrNull()
            val odaWithCreator = if (user != null) oda.copy(kurucuId = user.id) else oda
            val newOda = SupabaseModul.client.postgrest["odalar"]
                .insert(odaWithCreator) { select() }
                .decodeSingle<Oda>()
            Result.success(newOda)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun odayiSil(odaId: String): Result<Unit> {
        return try {
            SupabaseModul.client.postgrest["odalar"].delete {
                filter { eq("id", odaId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
